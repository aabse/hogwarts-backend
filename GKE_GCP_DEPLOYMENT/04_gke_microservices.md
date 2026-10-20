## 04 — Microservices on GKE (deploy, run and test)

### Goal

Deploy the 3 microservices (`wizard-service`, `inventory-service`, `duel-service`) to **Google Kubernetes Engine (GKE)** using images published to **Artifact Registry**, and validate that the APIs work (smoke tests with `curl` and/or Postman).

### Scope (important)

- This guide describes steps in GCP + `gcloud` and `kubectl` commands.
- It does not modify the repository code.
- The Kubernetes manifests (`Deployment`, `Service`) are created **outside the repo**, in a local working directory (e.g. `~/hogwarts-gcp/k8s`), as infrastructure configuration files, not as part of the source code.
- It assumes you have already built and published images to Artifact Registry (see `03_artifact_registry_and_builds.md`).

### The 3 microservices to deploy

| Service | Image (Artifact Registry) | Container port | Manifest |
|---|---|---|---|
| `wizard-service` | `.../wizard-service` | 8080 | section 4 |
| `inventory-service` | `.../inventory-service` | 8080 | section 5 |
| `duel-service` | `.../duel-service` | 8080 | section 6 |

---

## 0) Base variables

```bash
export PROJECT_ID="hogwarts-dev-123"
export REGION="us-central1"
export ZONE="us-central1-a"
export AR_REPO="hogwarts-docker"

export CLUSTER_NAME="hogwarts-cluster-gke"

export WIZARD_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO/wizard-service"
export INVENTORY_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO/inventory-service"
export DUEL_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO/duel-service"

# Version of each service to deploy (semantic tag, see guide 03 §4).
# Run FROM THE REPO ROOT, before the `cd` below:
export WIZARD_VERSION=$(grep '^version=' wizard-service/gradle.properties | cut -d= -f2)
export INVENTORY_VERSION=$(grep '^version=' inventory-service/gradle.properties | cut -d= -f2)
export DUEL_VERSION=$(grep '^version=' duel-service/gradle.properties | cut -d= -f2)

mkdir -p ~/hogwarts-gcp/k8s
cd ~/hogwarts-gcp/k8s
```

The manifests below reference each image with its version tag (e.g. `wizard-service:1.4.0`), never `:latest`. That version must be published to Artifact Registry (guide 03 §4.7) and cannot be a `-SNAPSHOT`: if `gradle.properties` is on a SNAPSHOT, export the last published version by hand (e.g. `export WIZARD_VERSION=1.4.0`).

If you created the service account for Workload Identity (recommended, see guide `02`), we will use it later:

```bash
export GKE_SA="hogwarts-gke-sa@$PROJECT_ID.iam.gserviceaccount.com"
```

---

## 1) GKE concepts you need

Before creating anything, it helps to be clear on how the pieces used in this guide relate to each other. From "more infrastructure" to "more application", the hierarchy is:

**Cluster → Nodes → Pods → Deployment → Service**

- **Cluster**: the whole set you manage as a unit — it groups a *control plane* (the "brain" Google manages for you: Kubernetes API, scheduler, etcd, etc.) and one or more **nodes** where your containers actually run. When you run `gcloud container clusters create-auto`, you are creating this set.
- **Node**: a virtual machine (a Compute Engine VM underneath) that is part of the cluster and has CPU/memory capacity to host Pods. In **Standard** you decide how many nodes and of which type (`--num-nodes`, `--machine-type`); in **Autopilot** Google creates and sizes them automatically based on what your Pods request in `resources.requests`, and you never interact with them directly.
- **Pod**: the smallest deployable unit in Kubernetes. It contains one or more containers (in our case, always one: each microservice's container) that share networking and an internal IP. Pods are ephemeral — if one dies, it is not "repaired", it is replaced by a new one.
- **Deployment**: the object you declare (in the YAML) that tells Kubernetes "I want `N` replicas of this Pod, running this image, always". The Deployment creates and watches a **ReplicaSet**, which is what actually keeps the number of Pods alive and coordinates *rollouts* (progressive updates) when you change the image. That is why `kubectl rollout status deployment/...` and `kubectl set image deployment/...` operate on the Deployment, not on the Pods directly.
- **Service**: Pods are constantly created and destroyed (each with its own internal IP, which changes), so you don't connect to a Pod directly. The **Service** groups all Pods matching a label (`selector: app: wizard-service`) and gives them a stable network identity. This guide uses the `LoadBalancer` type, which also assigns an **external public IP** for the initial tests. (Other types, such as `ClusterIP`, are only reachable inside the cluster — they are used in guide `08` for internal communication between microservices, and are also mentioned in section [10](#10-minimum-hardening-recommendation) as hardening.)
- GKE has two main modes for managing nodes:
  - **Autopilot**: Google manages nodes, scaling and patching. You pay per Pod (for what each Pod reserves in `resources.requests`), not per VM. Simpler, and recommended to start.
  - **Standard**: you manage the *node pool* (size, machine type, number of nodes). More control, more operational work.
  - This guide uses **Autopilot** by default; notes for Standard are included where applicable.
- Spring Boot listens on port `8080` by default. In the Deployment, `containerPort: 8080` declares that port on the Pod; in the Service, `targetPort: 8080` points to that same Pod port while `port: 80` is the port exposed externally (that is why the URLs in this doc do not include `:80`).

The following diagram summarizes these relationships:

![GKE concepts: Cluster, Nodes, Pods, Deployment and Service](diagrams/gke_concepts.svg)

*Reading the diagram: the client calls the **Service** (gray arrow, `HTTP :80`); the Service routes that traffic (blue arrows, `selector: app=...`) to the **Pods**, which run inside the **Nodes** grouped in the **Cluster**. Separately, the **Deployment** declares and watches those Pod replicas (dashed purple arrows) — it takes no part in network traffic, only in their lifecycle.*

---

## 2) Enable APIs and create the cluster

This section enables the GKE service on the project and creates the **Cluster** described in the previous section — that is, it provisions the control plane and (in Standard mode) the nodes that will host the Pods. After this step, `kubectl` needs to know which cluster to connect to and with which credentials, and it is worth confirming that the connection really works before moving on.

### 2.1 Enable the GKE API

GCP APIs are disabled by default on a new project; without this step, any `gcloud container ...` command would fail with a permissions/API-not-enabled error. It is a one-time step per project (if you already enabled it, this command is a safe no-op).

```bash
gcloud services enable container.googleapis.com
```

### 2.2 Create the cluster (Autopilot, recommended)

This is the command that actually creates the Cluster: GCP provisions the managed control plane and, in Autopilot, leaves node management entirely to Google (nodes are created/adjusted based on the Pods you deploy later; there are no "empty" nodes waiting).

```bash
gcloud container clusters create-auto "$CLUSTER_NAME" \
  --region="$REGION" \
  --project="$PROJECT_ID"
```

Note: creating an Autopilot cluster can take several minutes.

### 2.2-bis Alternative: Standard cluster

Same goal (create the Cluster), but here you explicitly define the *node pool*: how many VMs (`--num-nodes`) and of what size (`--machine-type`) start up front. Use this only if you need that explicit control over nodes (skip this step if you already created the Autopilot cluster above — you cannot have both with the same `$CLUSTER_NAME`).

```bash
gcloud container clusters create "$CLUSTER_NAME" \
  --zone="$ZONE" \
  --num-nodes=2 \
  --machine-type=e2-small \
  --project="$PROJECT_ID"
```

### 2.3 Get `kubectl` credentials

Creating the cluster does not automatically configure your local `kubectl`. This command downloads the cluster's endpoint and credentials and stores them in `~/.kube/config`, so the `kubectl` commands in the following sections know which cluster to target (instead of failing or targeting another context you may have configured).

**Prerequisite: `kubectl` and `gke-gcloud-auth-plugin`**

If you already installed `kubectl` and `gke-gcloud-auth-plugin` following guide `00` (section 2.2), this step should work without problems. If not, and you see the error:

```
CRITICAL: ACTION REQUIRED: gke-gcloud-auth-plugin, which is needed for continued use of kubectl, was not found or is not executable.
```

The `kubeconfig entry generated for ...` that appears afterwards **was still generated** — the error is only a warning that this kubeconfig cannot authenticate yet. Install the plugin as described in `00_local_prerequisites_and_tools.md` and then run `get-credentials` again to regenerate the kubeconfig:

```bash
gcloud container clusters get-credentials "$CLUSTER_NAME" --region="$REGION" --project="$PROJECT_ID"
```

(If you used the Standard cluster with `--zone`, replace `--region` with `--zone` in the command above.)

### 2.4 Verify access to the cluster

A quick check that 2.2 and 2.3 worked before investing time in the deployments: `get nodes` confirms that there are nodes available and reachable (in Autopilot they may take a while to show up after creation), and `cluster-info` confirms that `kubectl` is talking to the right cluster.

```bash
kubectl get nodes
kubectl cluster-info
```

---

## 3) Allow GKE to read images from Artifact Registry

If the cluster and the Artifact Registry repository are in the same project, you usually need no extra configuration: GKE nodes use the Compute Engine service account, which already has read access.

If you use a dedicated service account for the Pods (Workload Identity) or Artifact Registry is in another project, grant the reader role:

```bash
gcloud artifacts repositories add-iam-policy-binding "$AR_REPO" \
  --location="$REGION" \
  --member="serviceAccount:$GKE_SA" \
  --role="roles/artifactregistry.reader"
```

---

## 4) Deploy `wizard-service`

### 4.1 `Deployment` + `Service` manifest

Create the `wizard-service.yaml` file:

```bash
cat > wizard-service.yaml <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wizard-service
  labels:
    app: wizard-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: wizard-service
  template:
    metadata:
      labels:
        app: wizard-service
    spec:
      containers:
        - name: wizard-service
          image: $WIZARD_IMAGE:$WIZARD_VERSION
          ports:
            - containerPort: 8080
          env:
            - name: PORT
              value: "8080"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
          resources:
            requests:
              cpu: "250m"
              memory: "512Mi"
            limits:
              cpu: "500m"
              memory: "1Gi"
---
apiVersion: v1
kind: Service
metadata:
  name: wizard-service
spec:
  type: LoadBalancer
  selector:
    app: wizard-service
  ports:
    - port: 80
      targetPort: 8080
EOF
```

Notes:

- Adjust or remove `readinessProbe` if the microservice does not expose Actuator (`/actuator/health`).
- The `resources` are example values; adjust them to the service's actual consumption.

### 4.2 Apply the manifest

```bash
kubectl apply -f wizard-service.yaml
```

### 4.3 Verify the rollout

```bash
kubectl rollout status deployment/wizard-service
kubectl get pods -l app=wizard-service
```

### 4.4 Get the service's external IP

Assigning the external IP can take 1-2 minutes:

```bash
kubectl get service wizard-service --watch
```

When `EXTERNAL-IP` no longer says `<pending>`, exit with `Ctrl+C` and save the URL:

```bash
export WIZARD_URL="http://$(kubectl get service wizard-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
echo "$WIZARD_URL"
```

### 4.5 Smoke tests with `curl`

List wizards (paginated):

```bash
curl -sS "$WIZARD_URL/api/wizards?page=0&size=20&sort=id,desc"
```

Create a wizard:

```bash
curl -sS -X POST "$WIZARD_URL/api/wizards" \
  -H "Content-Type: application/json" \
  -d '{"name":"Harry Potter","house":"Gryffindor"}'
```

Get by id (example `1`):

```bash
curl -sS "$WIZARD_URL/api/wizards/1"
```

---

## 5) Deploy `inventory-service`

### 5.1 Manifest

```bash
cat > inventory-service.yaml <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: inventory-service
  labels:
    app: inventory-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: inventory-service
  template:
    metadata:
      labels:
        app: inventory-service
    spec:
      containers:
        - name: inventory-service
          image: $INVENTORY_IMAGE:$INVENTORY_VERSION
          ports:
            - containerPort: 8080
          env:
            - name: PORT
              value: "8080"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
          resources:
            requests:
              cpu: "250m"
              memory: "512Mi"
            limits:
              cpu: "500m"
              memory: "1Gi"
---
apiVersion: v1
kind: Service
metadata:
  name: inventory-service
spec:
  type: LoadBalancer
  selector:
    app: inventory-service
  ports:
    - port: 80
      targetPort: 8080
EOF
```

### 5.2 Apply and verify

```bash
kubectl apply -f inventory-service.yaml
kubectl rollout status deployment/inventory-service
```

### 5.3 Get the URL

```bash
kubectl get service inventory-service --watch
```

```bash
export INVENTORY_URL="http://$(kubectl get service inventory-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
echo "$INVENTORY_URL"
```

### 5.4 Smoke tests

Create a wand:

```bash
curl -sS -X POST "$INVENTORY_URL/api/inventory/wands" \
  -H "Content-Type: application/json" \
  -d '{"wood":"Holly","core":"Phoenix feather","length":11.0,"description":"Example wand"}'
```

List wands:

```bash
curl -sS "$INVENTORY_URL/api/inventory/wands?page=0&size=20&sort=id,desc"
```

Assign a wand to a wizard (example wand `1` → wizard `1`):

```bash
curl -sS -X POST "$INVENTORY_URL/api/inventory/wands/1/assign" \
  -H "Content-Type: application/json" \
  -d '{"wizardId":1}'
```

---

## 6) Deploy `duel-service`

### 6.1 Manifest

```bash
cat > duel-service.yaml <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: duel-service
  labels:
    app: duel-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: duel-service
  template:
    metadata:
      labels:
        app: duel-service
    spec:
      containers:
        - name: duel-service
          image: $DUEL_IMAGE:$DUEL_VERSION
          ports:
            - containerPort: 8080
          env:
            - name: PORT
              value: "8080"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
          resources:
            requests:
              cpu: "250m"
              memory: "512Mi"
            limits:
              cpu: "500m"
              memory: "1Gi"
---
apiVersion: v1
kind: Service
metadata:
  name: duel-service
spec:
  type: LoadBalancer
  selector:
    app: duel-service
  ports:
    - port: 80
      targetPort: 8080
EOF
```

### 6.2 Apply and verify

```bash
kubectl apply -f duel-service.yaml
kubectl rollout status deployment/duel-service
```

### 6.3 Get the URL

```bash
kubectl get service duel-service --watch
```

```bash
export DUEL_URL="http://$(kubectl get service duel-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
echo "$DUEL_URL"
```

### 6.4 Smoke tests

Create a duel:

```bash
curl -sS -X POST "$DUEL_URL/api/duels" \
  -H "Content-Type: application/json" \
  -d '{"attackerId":1,"defenderId":2}'
```

Get a duel:

```bash
curl -sS "$DUEL_URL/api/duels/1"
```

---

## 7) Testing with Postman (recommended)

1. Import `Hogwarts_API.postman_collection.json`.
2. Define environment variables:
   - `wizardBaseUrl` = `WIZARD_URL`
   - `inventoryBaseUrl` = `INVENTORY_URL`
   - `duelBaseUrl` = `DUEL_URL`
3. Run the requests in order:
   - Create Wizard / List Wizards
   - Create Wand / Assign Wand
   - Create Duel / Get Duel

---

## 8) Logs and verification from Cloud Console / `kubectl`

Check the overall status:

```bash
kubectl get deployments
kubectl get pods
kubectl get services
```

View a Pod's logs (replace `<pod-name>` with the real name, e.g. `wizard-service-xxxxxxxxxx-yyyyy`):

```bash
kubectl get pods -l app=wizard-service
kubectl logs <pod-name>
```

Follow logs live:

```bash
kubectl logs -f <pod-name>
```

You can also view aggregated logs in Cloud Console → Logging → filtering by `resource.type="k8s_container"` and `resource.labels.cluster_name="$CLUSTER_NAME"`, or in Cloud Console → Kubernetes Engine → Workloads → (service) → Logs.

---

## 9) Update an already deployed image

When you publish a new version of the image (release flow in `03_artifact_registry_and_builds.md` §4.6), point the `Deployment` at its semantic tag:

```bash
export WIZARD_VERSION=1.4.0   # the version just published
kubectl set image deployment/wizard-service wizard-service="$WIZARD_IMAGE:$WIZARD_VERSION"
kubectl rollout status deployment/wizard-service
```

Since each version has its own tag, the tag change is what triggers the rollout: no `kubectl rollout restart` is needed. To go back to a previous version, use the same command with the previous tag (see guide 03 §4.8).

`kubectl set image` changes the `Deployment` in the cluster, but not your local `wizard-service.yaml`. If you apply that file again later, regenerate the manifest with the current version so you don't accidentally go back to an old image.

Repeat the same (with the corresponding name and version) for `inventory-service` and `duel-service`.

---

## 10) Minimum hardening (recommendation)

Once you have verified that they work:

- Replace the `LoadBalancer` `Service`s (one per microservice, each with its own public IP) with:
  - A single `Ingress` (with the GKE Ingress Controller) that exposes the required routes, or
  - `ClusterIP` `Service`s + an API Gateway in front (see guide `09_api_security_identity_platform.md`).
- Define `NetworkPolicy` objects to restrict which Pods can talk to each other.
- Use **Workload Identity** so that each Pod has the minimum IAM permissions it needs, instead of the nodes' default service account.
- Configure a `HorizontalPodAutoscaler` per microservice if you expect variable load.
- Set CPU and memory `requests`/`limits` based on real measurements (not this guide's example values).

---

## 11) Delete resources (to stop paying)

If this guide was only for testing, delete the resources when you are done:

```bash
kubectl delete -f wizard-service.yaml
kubectl delete -f inventory-service.yaml
kubectl delete -f duel-service.yaml

gcloud container clusters delete "$CLUSTER_NAME" --region="$REGION" --project="$PROJECT_ID"
```

**Important**: deleting the cluster removes all workloads running on it; make sure you don't need them before running the command above.
