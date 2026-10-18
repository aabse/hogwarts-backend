## 11 — Checklist de producción y costos

### Objetivo

Tener un checklist práctico para pasar de "funciona" a "operable en producción" en GCP, y evitar sorpresas de costos.

### Alcance (importante)

- Esta guía es un checklist de operación.
- No modifica código del repositorio.

### Resumen por categoría

| Categoría | Puntos clave |
|---|---|
| Seguridad e IAM | MFA, least privilege, `NetworkPolicy`, Workload Identity, Secret Manager |
| Identity Platform / Gateway | Providers, validación de tokens, CORS |
| GKE (operación) | Región, `requests`/`limits`, HPA, node pools |
| Cloud SQL | Backups, HA, usuarios, migraciones |
| Observabilidad | Dashboards, alertas, Correlation ID |
| Costos | Budgets, right-sizing, limpieza de recursos |

---

## 1) Seguridad e IAM

- [ ] **MFA** habilitado para cuentas con permisos altos.
- [ ] Principio de **least privilege** aplicado (evitar `Owner` en producción).
- [ ] Services internos de GKE como **`ClusterIP`** (no `LoadBalancer` público) salvo que sea explícitamente requerido.
- [ ] `NetworkPolicy` definidas restringiendo qué Pods pueden llamarse entre sí (ver guía `08`).
- [ ] **Workload Identity** configurado para los Pods (sin usar la service account default de los nodos).
- [ ] Secret Manager:
  - [ ] secretos no expuestos en env vars en CI logs
  - [ ] el GSA de Workload Identity tiene `roles/secretmanager.secretAccessor` únicamente donde corresponde
- [ ] Rotación de credenciales (DB/users) definida.

---

## 2) Identity Platform / API Gateway (si aplica)

- [ ] Provider(s) habilitados (email/password u otros).
- [ ] Tokens validados correctamente:
  - [ ] sin token → 401/403
  - [ ] token inválido → 401/403
  - [ ] token válido → 200
- [ ] Gateway enruta a los 3 servicios.
- [ ] Restricción de CORS (si habrá frontend browser).

---

## 3) GKE (operación)

- [ ] Región del cluster correcta (cerca de usuarios y Cloud SQL).
- [ ] `requests`/`limits` de CPU/memoria adecuados (sin OOM ni throttling en carga normal).
- [ ] `HorizontalPodAutoscaler` configurado si la carga es variable.
- [ ] Node pools dimensionados conscientemente (Standard) o límites de recursos del namespace (Autopilot).
- [ ] Liveness/Readiness probes configuradas.
- [ ] Logs revisados (sin credenciales).
- [ ] Variables de entorno y secretos documentados por servicio.

---

## 4) Cloud SQL (Postgres)

- [ ] Backups habilitados.
- [ ] Retención definida.
- [ ] (Si aplica) HA habilitado.
- [ ] Usuarios por microservicio (no usar superuser para apps).
- [ ] Monitoreo de conexiones y límites de pooling.
- [ ] Plan de migraciones (Flyway/Liquibase) definido.

---

## 5) Observabilidad

- [ ] Dashboard(s) básicos:
  - requests/min
  - latencia p95
  - 4xx/5xx
- [ ] Alertas mínimas:
  - 5xx > umbral
  - latencia p95 > umbral
- [ ] Correlation ID (`X-Correlation-Id`) presente en requests y trazable en logs.

---

## 6) Costos: control y prevención

### 6.1 Budgets y alertas

En Cloud Console:

1. Billing → Budgets & alerts
2. Crea un budget mensual (ej. USD 20/50/100)
3. Configura alertas al 50%, 90%, 100%

### 6.2 Recomendaciones para no gastar de más

- GKE:
  - en Autopilot, ajusta `requests` al uso real — pagas por lo que reservan los Pods, no por lo que realmente usan
  - en Standard, dimensiona el node pool y habilita autoscaling de nodos para no pagar VMs idle
  - limita el `HorizontalPodAutoscaler` (`maxReplicas`) para controlar picos de costo
- Cloud SQL:
  - elige tier acorde a dev/stg/prod
  - apaga/borra recursos de ambientes temporales
- Artifact Registry:
  - limpia imágenes viejas (retention policy)

---

## 7) Checklist final (go/no-go)

- [ ] Acceso público deshabilitado en servicios internos.
- [ ] Gateway funcionando con auth.
- [ ] DB con backups y credenciales seguras.
- [ ] Alertas activas y probadas (al menos una).
- [ ] Presupuesto configurado.
- [ ] Procedimiento de rollback (re-deploy versión anterior) definido.

