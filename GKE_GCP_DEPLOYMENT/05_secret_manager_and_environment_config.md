## 05 — Secret Manager and per-environment configuration

### Goal

Define a simple and secure strategy to handle sensitive variables (DB credentials, internal URLs, keys) using **Secret Manager**, and configure the **GKE Pods** to consume them per environment (dev/stg/prod).

### Scope (important)

- This guide describes configuration in GCP and how to "inject" secrets into the GKE Pods.
- It does not modify the repository code.

### Summary: secrets per microservice

| Secret | Consumed by | Injection mechanism |
|---|---|---|
| `wizard-db-url` / `-user` / `-password` | `wizard-service` | Secret Manager CSI driver or Kubernetes `Secret` → env var |
| `inventory-db-url` / `-user` / `-password` | `inventory-service` | same |
| `duel-db-url` / `-user` / `-password` | `duel-service` | same |

---

## 0) Recommended principles

- **Do not hardcode secrets** in code or in the repository.
- **Do not use** `.env` files in production.
- On GKE:
  - non-sensitive values as regular env vars in the `Deployment`
  - sensitive values synced from Secret Manager into the Pod (CSI driver, or a Kubernetes `Secret`)
- Minimum roles (least privilege): only the Workload Identity service account (`GKE_SA`) should read the secrets it needs.

---

## 1) Secret naming convention

Recommended names:

- `wizard-db-url`
- `wizard-db-user`
- `wizard-db-password`
- `inventory-db-url`
- `inventory-db-user`
- `inventory-db-password`
- `duel-db-url`
- `duel-db-user`
- `duel-db-password`

Optionally, per environment:

- `wizard-db-password--dev`
- `wizard-db-password--prod`

The exact convention depends on your governance; what matters is consistency.

---

## 2) Create secrets (CLI)

Define variables:

```bash
export PROJECT_ID="hogwarts-dev-123"
export REGION="us-central1"
export GKE_SA="hogwarts-gke-sa@$PROJECT_ID.iam.gserviceaccount.com"
```

### 2.1 Create a secret (example)

```bash
echo -n "jdbc:postgresql:///wizard_db" | \
  gcloud secrets create wizard-db-url \
  --replication-policy="automatic" \
  --data-file=-
```

Add a version (password example):

```bash
echo -n "super-secret" | \
  gcloud secrets create wizard-db-password \
  --replication-policy="automatic" \
  --data-file=-
```

If the secret already exists and you want to add a new version:

```bash
echo -n "super-secret-rotated" | \
  gcloud secrets versions add wizard-db-password --data-file=-
```

### 2.2 List secrets

```bash
gcloud secrets list
```

---

## 3) Grant secret access to the Workload Identity service account

The GSA that the Pods use via Workload Identity needs `roles/secretmanager.secretAccessor`.

```bash
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$GKE_SA" \
  --role="roles/secretmanager.secretAccessor"
```

Verification: review the IAM bindings, or try to read a secret from a Pod that uses that GSA via Workload Identity.

---

## 4) Inject secrets into the GKE Pods

There are two common mechanisms; pick one per microservice.

### 4.1 Option A (recommended): Secret Manager CSI driver

The [Secret Manager CSI driver](https://cloud.google.com/secret-manager/docs/secret-manager-managed-csi-component) mounts the secrets directly as files in the Pod, reading them live from Secret Manager (they are not copied into a Kubernetes `Secret`/`etcd`).

Enable the add-on on the cluster:

```bash
gcloud container clusters update "$CLUSTER_NAME" \
  --region="$REGION" \
  --update-addons=SecretManagerCsiDriver=ENABLED
```

In the `Deployment` manifest (see guide `04`), add a `csi` volume and mount it in the container; the Pod must use a Kubernetes `ServiceAccount` bound via Workload Identity to `$GKE_SA` (details of the binding in guide `04`, Workload Identity section). The conceptual pattern:

```yaml
volumes:
  - name: wizard-secrets
    csi:
      driver: secrets-store.csi.k8s.io
      readOnly: true
      volumeAttributes:
        secretProviderClass: "wizard-service-secrets"
```

The mounted file can then be read as an environment variable through a small startup script, or directly as a file if your configuration allows it. This work **does not change code**; it only defines the configuration pattern.

### 4.2 Option B (simpler): manually synced Kubernetes `Secret`

If you prefer not to use the CSI driver, create a Kubernetes `Secret` with the current value from Secret Manager:

```bash
kubectl create secret generic wizard-db-secrets \
  --from-literal=DB_URL="$(gcloud secrets versions access latest --secret=wizard-db-url)" \
  --from-literal=DB_USER="$(gcloud secrets versions access latest --secret=wizard-db-user)" \
  --from-literal=DB_PASSWORD="$(gcloud secrets versions access latest --secret=wizard-db-password)"
```

And reference the `Secret` in the `Deployment` with `envFrom`:

```yaml
envFrom:
  - secretRef:
      name: wizard-db-secrets
```

**Important**: unlike Option A, here the value is copied into the cluster (`etcd`) when the `Secret` is created; if you rotate the password in Secret Manager, you must run `kubectl create secret ... --dry-run=client -o yaml | kubectl apply -f -` (or equivalent) again and restart the Pods so they pick up the new value.

### 4.3 Non-sensitive values as regular env vars

Define them directly in the `Deployment` (see guide `04`):

```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
```

---

## 5) Separation by environment

Two common approaches:

### Approach A (recommended): one project per environment

- `hogwarts-dev`
- `hogwarts-stg`
- `hogwarts-prod`

Pros: separates IAM/billing/resources.
Cons: more projects to manage.

### Approach B: one project, secrets per environment

- secrets with a `--dev`, `--prod` suffix
- `SPRING_PROFILES_ACTIVE` (Pod env var) to select

Pros: fewer projects.
Cons: higher risk of mixing resources and permissions.

---

## 6) Common troubleshooting

### 6.1 "Permission denied" when reading secrets

- Confirm that the Pod uses the expected Kubernetes `ServiceAccount`, bound via Workload Identity to `$GKE_SA`:

```bash
kubectl get pod <pod-name> -o jsonpath='{.spec.serviceAccountName}'
```

- Confirm the IAM binding:

```bash
gcloud projects get-iam-policy "$PROJECT_ID" --flatten="bindings[].members" --format="table(bindings.role, bindings.members)" | grep "$GKE_SA"
```

### 6.2 "Secret not found"

- Check the exact name:

```bash
gcloud secrets list
```
