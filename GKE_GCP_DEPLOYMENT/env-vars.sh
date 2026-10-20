#!/usr/bin/env bash
# Environment variables used by the GKE_GCP_DEPLOYMENT guides
# Source: see each guide (00 to 12)

# Project and region
export PROJECT_ID="hogwarts-backend-dev"
export REGION="us-central1"
export ZONE="us-central1-a"

# GKE
export CLUSTER_NAME="hogwarts-cluster-gke"

# Workload Identity service account (used by the Pods)
export GKE_SA="hogwarts-gke-sa@$PROJECT_ID.iam.gserviceaccount.com"

# Artifact Registry
export AR_REPO="hogwarts-docker"
export WIZARD_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO/wizard-service"
export INVENTORY_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO/inventory-service"
export DUEL_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO/duel-service"

# Semantic version of each service (image tag), read from <service>/gradle.properties.
# See 03_artifact_registry_and_builds.md §4. Reload this file after changing a version.
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"
if [ -n "$REPO_ROOT" ]; then
  export WIZARD_VERSION="$(grep '^version=' "$REPO_ROOT/wizard-service/gradle.properties" | cut -d= -f2)"
  export INVENTORY_VERSION="$(grep '^version=' "$REPO_ROOT/inventory-service/gradle.properties" | cut -d= -f2)"
  export DUEL_VERSION="$(grep '^version=' "$REPO_ROOT/duel-service/gradle.properties" | cut -d= -f2)"
  export GIT_SHA="$(git -C "$REPO_ROOT" rev-parse --short HEAD)"
fi

# Cloud SQL
#export INSTANCE_ID="hogwarts-postgres"
#export INSTANCE_CONNECTION_NAME="$(gcloud sql instances describe "$INSTANCE_ID" --format='value(connectionName)')"

# Free alternative to Cloud SQL: PostgreSQL on an Always Free VM (see 07_free_postgres_vm_alternative.md)
#export VM_NAME="hogwarts-postgres-vm"
#export VM_EXTERNAL_IP="$(gcloud compute addresses describe hogwarts-postgres-ip --region="$REGION" --format='value(address)')"

# Identity Platform / API Gateway
export API_KEY="YOUR_API_KEY"
export EMAIL=""
export PASSWORD=""
export ID_TOKEN=""

export API_ID="hogwarts-api"
export API_CONFIG_ID="hogwarts-api-config-v1"
export GATEWAY_ID="hogwarts-gateway"
export OPENAPI_SPEC_PATH="$HOME/hogwarts-gcp/openapi-hogwarts.yaml"
export GATEWAY_HOSTNAME=""
export GATEWAY_URL=""

# Ingress HTTPS endpoint used as the API Gateway backend (see 09_api_security_identity_platform.md)
#export INGRESS_IP="$(gcloud compute addresses describe hogwarts-ingress-ip --global --format='value(address)')"
#export BACKEND_URL="https://$INGRESS_IP"

# External IPs of the GKE Services (direct smoke tests, see 04_gke_microservices.md)
#export WIZARD_IP="$(kubectl get service wizard-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
#export INVENTORY_IP="$(kubectl get service inventory-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
#export DUEL_IP="$(kubectl get service duel-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"

# Correlation ID used for observability
export CID="$(uuidgen)"

# Notes:
# - Set PROJECT_ID, API_KEY, EMAIL, PASSWORD before running.
# - If you restart the terminal, reload with: source ./env-vars.sh
# - To regenerate the Identity Platform token, run the corresponding login command.

# Real values (API_KEY, EMAIL, PASSWORD, ID_TOKEN, DB_PASSWORD...) go in env-vars.local.sh,
# which is in .gitignore and overrides the values defined above.
ENV_VARS_LOCAL="$(dirname "${BASH_SOURCE[0]:-$0}")/env-vars.local.sh"
[ -f "$ENV_VARS_LOCAL" ] && source "$ENV_VARS_LOCAL"
