#!/usr/bin/env bash
# Variables de entorno usadas en las guías de DESPLIEGUE_GKE_GCP
# Fuente: ver cada guía (00 a 12)

# Proyecto y región
export PROJECT_ID="hogwarts-backend-dev"
export REGION="us-central1"
export ZONE="us-central1-a"

# GKE
export CLUSTER_NAME="hogwarts-cluster-gke"

# Service account de Workload Identity (usado por los Pods)
export GKE_SA="hogwarts-gke-sa@$PROJECT_ID.iam.gserviceaccount.com"

# Artifact Registry
export AR_REPO="hogwarts-docker"
export WIZARD_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO/wizard-service"
export INVENTORY_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO/inventory-service"
export DUEL_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO/duel-service"

# Versiones semánticas de cada servicio (tag de imagen), leídas de <servicio>/gradle.properties.
# Ver 03_artifact_registry_y_builds.md §4. Vuelve a cargar este archivo después de cambiar una versión.
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

# Alternativa gratuita a Cloud SQL: PostgreSQL en VM Always Free (ver 07_alternativa_postgres_gratis_vm.md)
#export VM_NAME="hogwarts-postgres-vm"
#export VM_EXTERNAL_IP="$(gcloud compute addresses describe hogwarts-postgres-ip --region="$REGION" --format='value(address)')"

# Identity Platform / API Gateway
export API_KEY="TU_API_KEY"
export EMAIL=""
export PASSWORD=""
export ID_TOKEN=""

export API_ID="hogwarts-api"
export API_CONFIG_ID="hogwarts-api-config-v1"
export GATEWAY_ID="hogwarts-gateway"
export OPENAPI_SPEC_PATH="$HOME/hogwarts-gcp/openapi-hogwarts.yaml"
export GATEWAY_HOSTNAME=""
export GATEWAY_URL=""

# Endpoint HTTPS del Ingress usado como backend de API Gateway (ver 09_segurizacion_apis_identity_platform.md)
#export INGRESS_IP="$(gcloud compute addresses describe hogwarts-ingress-ip --global --format='value(address)')"
#export BACKEND_URL="https://$INGRESS_IP"

# IPs externas de los Services de GKE (smoke tests directos, ver 04_microservicios_gke.md)
#export WIZARD_IP="$(kubectl get service wizard-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
#export INVENTORY_IP="$(kubectl get service inventory-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
#export DUEL_IP="$(kubectl get service duel-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"

# Identificador de correlación usado en observabilidad
export CID="$(uuidgen)"

# Comentarios:
# - Ajusta PROJECT_ID, API_KEY, EMAIL, PASSWORD antes de ejecutar.
# - Si se reinicia la terminal, vuelve a cargar con: source ./env-vars.sh
# - Para regenerar el token de Identity Platform, ejecuta el comando de login correspondiente.

# Valores reales (API_KEY, EMAIL, PASSWORD, ID_TOKEN, DB_PASSWORD...) van en env-vars.local.sh,
# que está en .gitignore y sobrescribe lo definido arriba.
ENV_VARS_LOCAL="$(dirname "${BASH_SOURCE[0]:-$0}")/env-vars.local.sh"
[ -f "$ENV_VARS_LOCAL" ] && source "$ENV_VARS_LOCAL"
