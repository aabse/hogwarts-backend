## 11 — Production checklist and costs

### Goal

Have a practical checklist to go from "it works" to "operable in production" on GCP, and avoid cost surprises.

### Scope (important)

- This guide is an operations checklist.
- It does not modify the repository code.

### Summary by category

| Category | Key points |
|---|---|
| Security and IAM | MFA, least privilege, `NetworkPolicy`, Workload Identity, Secret Manager |
| Identity Platform / Gateway | Providers, token validation, CORS |
| GKE (operations) | Region, `requests`/`limits`, HPA, node pools |
| Cloud SQL | Backups, HA, users, migrations |
| Observability | Dashboards, alerts, Correlation ID |
| Costs | Budgets, right-sizing, resource cleanup |

---

## 1) Security and IAM

- [ ] **MFA** enabled for accounts with high privileges.
- [ ] **Least privilege** principle applied (avoid `Owner` in production).
- [ ] Internal GKE Services as **`ClusterIP`** (not a public `LoadBalancer`) unless explicitly required.
- [ ] `NetworkPolicy` objects defined, restricting which Pods can call each other (see guide `08`).
- [ ] **Workload Identity** configured for the Pods (without using the nodes' default service account).
- [ ] Secret Manager:
  - [ ] secrets not exposed in env vars in CI logs
  - [ ] the Workload Identity GSA has `roles/secretmanager.secretAccessor` only where appropriate
- [ ] Credential rotation (DB/users) defined.

---

## 2) Identity Platform / API Gateway (if applicable)

- [ ] Provider(s) enabled (email/password or others).
- [ ] Tokens validated correctly:
  - [ ] no token → 401/403
  - [ ] invalid token → 401/403
  - [ ] valid token → 200
- [ ] The Gateway routes to all 3 services.
- [ ] CORS restriction (if there will be a browser frontend).

---

## 3) GKE (operations)

- [ ] Correct cluster region (close to the users and to Cloud SQL).
- [ ] Appropriate CPU/memory `requests`/`limits` (no OOM or throttling under normal load).
- [ ] `HorizontalPodAutoscaler` configured if the load is variable.
- [ ] Node pools sized deliberately (Standard) or namespace resource limits (Autopilot).
- [ ] Liveness/Readiness probes configured.
- [ ] Logs reviewed (no credentials).
- [ ] Environment variables and secrets documented per service.

---

## 4) Cloud SQL (Postgres)

- [ ] Backups enabled.
- [ ] Retention defined.
- [ ] (If applicable) HA enabled.
- [ ] Users per microservice (do not use the superuser for apps).
- [ ] Monitoring of connections and pooling limits.
- [ ] Migration plan (Flyway/Liquibase) defined.

---

## 5) Observability

- [ ] Basic dashboard(s):
  - requests/min
  - p95 latency
  - 4xx/5xx
- [ ] Minimum alerts:
  - 5xx > threshold
  - p95 latency > threshold
- [ ] Correlation ID (`X-Correlation-Id`) present in requests and traceable in the logs.

---

## 6) Costs: control and prevention

### 6.1 Budgets and alerts

In Cloud Console:

1. Billing → Budgets & alerts
2. Create a monthly budget (e.g. USD 20/50/100)
3. Set alerts at 50%, 90%, 100%

### 6.2 Recommendations to avoid overspending

- GKE:
  - on Autopilot, tune `requests` to the real usage — you pay for what the Pods reserve, not for what they actually use
  - on Standard, size the node pool and enable node autoscaling so you don't pay for idle VMs
  - cap the `HorizontalPodAutoscaler` (`maxReplicas`) to control cost spikes
- Cloud SQL:
  - pick a tier that matches dev/stg/prod
  - stop/delete the resources of temporary environments
- Artifact Registry:
  - clean up old images (retention policy)

---

## 7) Final checklist (go/no-go)

- [ ] Public access disabled on internal services.
- [ ] Gateway working with auth.
- [ ] DB with backups and secure credentials.
- [ ] Alerts active and tested (at least one).
- [ ] Budget configured.
- [ ] Rollback procedure (redeploy the previous version) defined.
