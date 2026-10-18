## 05 — Secret Manager y configuración por ambiente

### Objetivo

Definir una estrategia simple y segura para manejar variables sensibles (credenciales DB, URLs internas, keys) usando **Secret Manager**, y configurar los **Pods de GKE** para consumirlas por ambiente (dev/stg/prod).

### Alcance (importante)

- Esta guía describe configuración en GCP y cómo "inyectar" secretos en los Pods de GKE.
- No modifica el código del repositorio.

### Resumen: secretos por microservicio

| Secreto | Consumido por | Mecanismo de inyección |
|---|---|---|
| `wizard-db-url` / `-user` / `-password` | `wizard-service` | Secret Manager CSI driver o `Secret` de Kubernetes → env var |
| `inventory-db-url` / `-user` / `-password` | `inventory-service` | idem |
| `duel-db-url` / `-user` / `-password` | `duel-service` | idem |

---

## 0) Principios recomendados

- **No hardcodear secretos** en código ni en repositorio.
- **No usar** `.env` en producción.
- En GKE:
  - valores no sensibles como env vars normales en el `Deployment`
  - valores sensibles sincronizados desde Secret Manager hacia el Pod (CSI driver, o un `Secret` de Kubernetes)
- Roles mínimos (least privilege): solo el service account de Workload Identity (`GKE_SA`) debe leer los secretos necesarios.

---

## 1) Convención de nombres de secretos

Recomendación de nombres:

- `wizard-db-url`
- `wizard-db-user`
- `wizard-db-password`
- `inventory-db-url`
- `inventory-db-user`
- `inventory-db-password`
- `duel-db-url`
- `duel-db-user`
- `duel-db-password`

Opcionalmente, por ambiente:

- `wizard-db-password--dev`
- `wizard-db-password--prod`

La convención exacta depende de tu governance; lo importante es consistencia.

---

## 2) Crear secretos (CLI)

Define variables:

```bash
export PROJECT_ID="hogwarts-dev-123"
export REGION="us-central1"
export GKE_SA="hogwarts-gke-sa@$PROJECT_ID.iam.gserviceaccount.com"
```

### 2.1 Crear un secreto (ejemplo)

```bash
echo -n "jdbc:postgresql:///wizard_db" | \
  gcloud secrets create wizard-db-url \
  --replication-policy="automatic" \
  --data-file=-
```

Agregar una versión (ejemplo password):

```bash
echo -n "super-secret" | \
  gcloud secrets create wizard-db-password \
  --replication-policy="automatic" \
  --data-file=-
```

Si el secreto ya existe y quieres agregar una nueva versión:

```bash
echo -n "super-secret-rotated" | \
  gcloud secrets versions add wizard-db-password --data-file=-
```

### 2.2 Listar secretos

```bash
gcloud secrets list
```

---

## 3) Otorgar acceso a secretos al service account de Workload Identity

El GSA que los Pods usan vía Workload Identity necesita `roles/secretmanager.secretAccessor`.

```bash
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$GKE_SA" \
  --role="roles/secretmanager.secretAccessor"
```

Verificación: revisa los IAM bindings, o intenta leer un secreto desde un Pod que use ese GSA vía Workload Identity.

---

## 4) Inyectar secretos en los Pods de GKE

Hay dos mecanismos habituales; elige uno por microservicio.

### 4.1 Opción A (recomendada): Secret Manager CSI driver

El [Secret Manager CSI driver](https://cloud.google.com/secret-manager/docs/secret-manager-managed-csi-component) monta los secretos directamente como archivos en el Pod, leyéndolos en vivo desde Secret Manager (no quedan copiados en un `Secret` de Kubernetes/`etcd`).

Habilita el addon en el cluster:

```bash
gcloud container clusters update "$CLUSTER_NAME" \
  --region="$REGION" \
  --update-addons=SecretManagerCsiDriver=ENABLED
```

En el manifiesto del `Deployment` (ver guía `04`), agrega un volumen tipo `csi` y móntalo en el contenedor; el Pod debe usar una `ServiceAccount` de Kubernetes asociada vía Workload Identity a `$GKE_SA` (detalle de la asociación, en la guía `04`, sección de Workload Identity). El patrón conceptual:

```yaml
volumes:
  - name: wizard-secrets
    csi:
      driver: secrets-store.csi.k8s.io
      readOnly: true
      volumeAttributes:
        secretProviderClass: "wizard-service-secrets"
```

El archivo montado se puede leer luego como variable de entorno vía un pequeño script de arranque, o directamente como archivo si tu configuración lo permite. En este trabajo **no se cambia código**; solo se define el patrón de configuración.

### 4.2 Opción B (más simple): `Secret` de Kubernetes sincronizado manualmente

Si prefieres no usar el CSI driver, crea un `Secret` de Kubernetes con el valor actual de Secret Manager:

```bash
kubectl create secret generic wizard-db-secrets \
  --from-literal=DB_URL="$(gcloud secrets versions access latest --secret=wizard-db-url)" \
  --from-literal=DB_USER="$(gcloud secrets versions access latest --secret=wizard-db-user)" \
  --from-literal=DB_PASSWORD="$(gcloud secrets versions access latest --secret=wizard-db-password)"
```

Y referencia el `Secret` en el `Deployment` con `envFrom`:

```yaml
envFrom:
  - secretRef:
      name: wizard-db-secrets
```

**Importante**: a diferencia de la Opción A, aquí el valor queda copiado en el cluster (`etcd`) en el momento de crear el `Secret`; si rotas la contraseña en Secret Manager, debes volver a ejecutar el `kubectl create secret ... --dry-run=client -o yaml | kubectl apply -f -` (o equivalente) y reiniciar los Pods para que tomen el nuevo valor.

### 4.3 Valores no sensibles como env vars normales

Defínelos directamente en el `Deployment` (ver guía `04`):

```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
```

---

## 5) Separación por ambientes

Dos enfoques comunes:

### Enfoque A (recomendado): un proyecto por ambiente

- `hogwarts-dev`
- `hogwarts-stg`
- `hogwarts-prod`

Pros: separa IAM/billing/recursos.
Contras: más proyectos que administrar.

### Enfoque B: un proyecto, secretos por ambiente

- secretos con sufijo `--dev`, `--prod`
- `SPRING_PROFILES_ACTIVE` (env var del Pod) para seleccionar

Pros: menos proyectos.
Contras: mayor riesgo de mezclar recursos y permisos.

---

## 6) Troubleshooting típico

### 6.1 "Permission denied" leyendo secretos

- Confirma que el Pod usa la `ServiceAccount` de Kubernetes esperada, asociada vía Workload Identity a `$GKE_SA`:

```bash
kubectl get pod <pod-name> -o jsonpath='{.spec.serviceAccountName}'
```

- Confirma el binding de IAM:

```bash
gcloud projects get-iam-policy "$PROJECT_ID" --flatten="bindings[].members" --format="table(bindings.role, bindings.members)" | grep "$GKE_SA"
```

### 6.2 “Secret not found”

- Verifica nombre exacto:

```bash
gcloud secrets list
```

