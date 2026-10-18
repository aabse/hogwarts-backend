## 00 — Prerrequisitos locales y herramientas

### Objetivo

Dejar tu máquina lista para ejecutar los comandos de estas guías, evitando fallas típicas por falta de herramientas, permisos o configuración de `gcloud`.

### Alcance (importante)

- Esta guía **no modifica el código** del repositorio.
- Solo prepara el entorno local y el acceso a GCP.

---

## 1) Requisitos del repositorio (contexto)

Este repo es un proyecto **Gradle multi-módulo** con Java 17 y 3 microservicios Spring Boot:

- `wizard-service`
- `inventory-service`
- `duel-service`

En local, el repo sugiere correrlos con `./gradlew :<module>:bootRun` pasando `--server.port=...` (ver `README.md` del repo).

---

## 2) Herramientas necesarias en tu equipo

### 2.1 Google Cloud CLI (`gcloud`)

Instala el Google Cloud CLI siguiendo la documentación oficial de tu OS.

Valida que quedó instalado:

```bash
gcloud version
```

Actualiza componentes (si aplica):

```bash
gcloud components update
```

### 2.2 `kubectl` y el plugin de autenticación de GKE

Como el despliegue de los microservicios es en **GKE**, necesitas `kubectl` para interactuar con el cluster.

Instala `kubectl` como componente de `gcloud`:

```bash
gcloud components install kubectl
```

> Si instalaste `gcloud` con Homebrew en macOS, usa en cambio: `brew install kubectl`. `gcloud` **no** instala `kubectl` automáticamente junto con el CLI base; es un componente separado que debes instalar explícitamente con el comando anterior.

`kubectl` ya no trae integrado el soporte de autenticación contra GKE (fue retirado de `client-go`), por lo que además necesitas el plugin `gke-gcloud-auth-plugin`:

```bash
gcloud components install gke-gcloud-auth-plugin
```

> Con Homebrew: `brew install gke-gcloud-auth-plugin`.

Exporta la variable que activa el plugin (agrégala a tu `.bashrc`/`.zshrc` para que persista entre sesiones):

```bash
export USE_GKE_GCLOUD_AUTH_PLUGIN=True
```

Valida ambas instalaciones:

```bash
kubectl version --client
gke-gcloud-auth-plugin --version
```

### 2.3 Docker (opcional, pero recomendado)

**Cuándo lo necesitas**
- Si quieres construir y probar imágenes localmente (con `pack` o Docker).
- Si quieres correr un Postgres local temporal para validar conexión (opcional).

Valida:

```bash
docker --version
docker ps
```

### 2.4 Pack CLI (opcional; útil si quieres testear buildpacks localmente)

Buildpacks permiten construir imágenes sin escribir Dockerfile. Cloud Build también puede hacerlo remoto, por lo que `pack` es opcional.

Valida:

```bash
pack --version
```

### 2.5 Java 17 (local)

Solo es requerido si vas a compilar/ejecutar localmente.

Valida Java:

```bash
java -version
```

### 2.6 Resumen de herramientas

| Herramienta | Obligatoria | Para qué sirve |
|---|---|---|
| `gcloud` CLI | Sí | Crear/administrar todos los recursos de GCP (proyecto, GKE, Cloud SQL, Secret Manager, etc.) |
| `kubectl` | Sí | Interactuar con el cluster GKE (`apply`, `get`, `logs`, `exec`) |
| `gke-gcloud-auth-plugin` | Sí | Permite que `kubectl` se autentique contra GKE |
| Docker | Opcional | Construir/probar imágenes localmente, correr Postgres local |
| Pack CLI | Opcional | Probar buildpacks localmente antes de usar Cloud Build |
| Java 17 | Opcional | Compilar/ejecutar los microservicios en local |

---

## 3) Autenticación y configuración inicial de `gcloud`

### 3.1 Login

Inicia sesión:

```bash
gcloud auth login
```

Si tu organización usa SSO/federación, usa el método indicado por tu IdP (Workforce/Workload Identity Federation).

Valida tu cuenta activa:

```bash
gcloud auth list
```

### 3.2 Inicializa configuración (recomendado)

```bash
gcloud init
```

Esto ayuda a seleccionar proyecto por defecto, región, y credenciales.

### 3.3 Definir variables de trabajo (recomendado)

Durante las guías verás placeholders como `PROJECT_ID`, `REGION`, `REPO_NAME`, etc.
Una forma práctica es exportarlas en tu shell para evitar repetir:

```bash
export PROJECT_ID="tu-project-id"
export REGION="us-central1"
```

Luego, setear proyecto en `gcloud`:

```bash
gcloud config set project "$PROJECT_ID"
```

---

## 4) Permisos mínimos que vas a necesitar en GCP

Dependiendo de si eres owner o colaborador, en general necesitarás permisos para:

- Crear/administrar clusters de GKE y sus workloads (`Deployments`/`Services`)
- Crear/administrar Artifact Registry repos (Docker)
- Ejecutar builds en Cloud Build
- Crear/administrar Cloud SQL
- Crear/leer secretos en Secret Manager
- Configurar Identity Platform
- (Opcional) Crear API Gateway

**Sugerencia**: si estás en un proyecto “sandbox”, `roles/owner` facilita pruebas. Para entornos reales, aplica **least privilege**.

---

## 5) Verificación rápida de conectividad a GCP

Lista proyectos (debe mostrarte al menos uno):

```bash
gcloud projects list
```

Muestra tu proyecto actual:

```bash
gcloud config get-value project
```

---

## 6) Troubleshooting típico

### 6.1 “Permission denied” / 403 en comandos

- Verifica que estás en el `PROJECT_ID` correcto:

```bash
gcloud config get-value project
```

- Verifica que tu usuario tenga roles en ese proyecto.

### 6.2 “gcloud: command not found”

- Reinstala Google Cloud CLI y reinicia la terminal.

### 6.3 Problemas con Docker

- Asegúrate de que Docker Desktop/daemon esté corriendo.
- En macOS, revisa permisos de virtualización.

