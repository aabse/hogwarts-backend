## 00 — Local prerequisites and tools

### Goal

Get your machine ready to run the commands in these guides, avoiding the typical failures caused by missing tools, permissions or `gcloud` configuration.

### Scope (important)

- This guide **does not modify the repository code**.
- It only prepares the local environment and access to GCP.

---

## 1) Repository requirements (context)

This repo is a **Gradle multi-module** project with Java 17 and 3 Spring Boot microservices:

- `wizard-service`
- `inventory-service`
- `duel-service`

Locally, the repo suggests running them with `./gradlew :<module>:bootRun`, passing `--server.port=...` (see the repo's `README.md`).

---

## 2) Tools you need on your machine

### 2.1 Google Cloud CLI (`gcloud`)

Install the Google Cloud CLI following the official documentation for your OS.

Check that it is installed:

```bash
gcloud version
```

Update components (if applicable):

```bash
gcloud components update
```

### 2.2 `kubectl` and the GKE authentication plugin

Since the microservices are deployed to **GKE**, you need `kubectl` to interact with the cluster.

Install `kubectl` as a `gcloud` component:

```bash
gcloud components install kubectl
```

> If you installed `gcloud` with Homebrew on macOS, use this instead: `brew install kubectl`. `gcloud` does **not** install `kubectl` automatically with the base CLI; it is a separate component that you must install explicitly with the command above.

`kubectl` no longer ships built-in GKE authentication support (it was removed from `client-go`), so you also need the `gke-gcloud-auth-plugin` plugin:

```bash
gcloud components install gke-gcloud-auth-plugin
```

> With Homebrew: `brew install gke-gcloud-auth-plugin`.

Export the variable that enables the plugin (add it to your `.bashrc`/`.zshrc` so it persists across sessions):

```bash
export USE_GKE_GCLOUD_AUTH_PLUGIN=True
```

Check both installations:

```bash
kubectl version --client
gke-gcloud-auth-plugin --version
```

### 2.3 Docker (optional, but recommended)

**When you need it**
- If you want to build and test images locally (with `pack` or Docker).
- If you want to run a temporary local Postgres to validate the connection (optional).

Check:

```bash
docker --version
docker ps
```

### 2.4 Pack CLI (optional; useful to test buildpacks locally)

Buildpacks build images without writing a Dockerfile. Cloud Build can also do it remotely, so `pack` is optional.

Check:

```bash
pack --version
```

### 2.5 Java 17 (local)

Only required if you are going to build/run locally.

Check Java:

```bash
java -version
```

### 2.6 Tools summary

| Tool | Required | What it is for |
|---|---|---|
| `gcloud` CLI | Yes | Create/manage all GCP resources (project, GKE, Cloud SQL, Secret Manager, etc.) |
| `kubectl` | Yes | Interact with the GKE cluster (`apply`, `get`, `logs`, `exec`) |
| `gke-gcloud-auth-plugin` | Yes | Lets `kubectl` authenticate against GKE |
| Docker | Optional | Build/test images locally, run a local Postgres |
| Pack CLI | Optional | Test buildpacks locally before using Cloud Build |
| Java 17 | Optional | Build/run the microservices locally |

---

## 3) `gcloud` authentication and initial setup

### 3.1 Login

Sign in:

```bash
gcloud auth login
```

If your organization uses SSO/federation, use the method indicated by your IdP (Workforce/Workload Identity Federation).

Check your active account:

```bash
gcloud auth list
```

### 3.2 Initialize the configuration (recommended)

```bash
gcloud init
```

This helps you select the default project, region and credentials.

### 3.3 Define working variables (recommended)

Throughout the guides you will see placeholders such as `PROJECT_ID`, `REGION`, `REPO_NAME`, etc.
A practical approach is to export them in your shell to avoid repeating them:

```bash
export PROJECT_ID="your-project-id"
export REGION="us-central1"
```

Then set the project in `gcloud`:

```bash
gcloud config set project "$PROJECT_ID"
```

---

## 4) Minimum GCP permissions you will need

Depending on whether you are an owner or a collaborator, you will generally need permissions to:

- Create/manage GKE clusters and their workloads (`Deployments`/`Services`)
- Create/manage Artifact Registry (Docker) repos
- Run builds on Cloud Build
- Create/manage Cloud SQL
- Create/read secrets in Secret Manager
- Configure Identity Platform
- (Optional) Create an API Gateway

**Tip**: in a "sandbox" project, `roles/owner` makes testing easier. For real environments, apply **least privilege**.

---

## 5) Quick GCP connectivity check

List projects (it should show at least one):

```bash
gcloud projects list
```

Show your current project:

```bash
gcloud config get-value project
```

---

## 6) Common troubleshooting

### 6.1 "Permission denied" / 403 on commands

- Check that you are on the correct `PROJECT_ID`:

```bash
gcloud config get-value project
```

- Check that your user has roles in that project.

### 6.2 "gcloud: command not found"

- Reinstall the Google Cloud CLI and restart the terminal.

### 6.3 Docker problems

- Make sure Docker Desktop/the daemon is running.
- On macOS, check the virtualization permissions.
