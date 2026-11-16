# AlgoArena deployment

## Render deployment

The backend is configured as a Render Docker web service through [`render.yaml`](./render.yaml).
Create a Blueprint from the repository, then provide the prompted secret values:

- `DB_URL`: Prisma's **direct** PostgreSQL URL, for example
  `postgres://USER:PASSWORD@db.prisma.io:5432/postgres?sslmode=require`.
- `APP_JWT_SECRET`: a newly generated random value of at least 32 bytes.
- `ADMIN_BOOTSTRAP_ENABLED`
- `ADMIN_USERNAME`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`

Render supplies `PORT` automatically. The Blueprint keeps one instance because live
presence/SSE state is held in memory and the import recovery flow assumes a single
application instance. The Docker image listens on port `8080`, uses the `prod` Spring
profile, and exposes `/actuator/health` for health checks. Keep
`ADMIN_BOOTSTRAP_ENABLED=false` after the first admin account has been created. The
`ADMIN_*` values can then be removed from the Render service.

In the Cloudflare frontend's build settings, set:

```text
NEXT_PUBLIC_API_BASE_URL=https://YOUR_RENDER_SERVICE.onrender.com/api
```

Rebuild the frontend after changing this value. The default production CORS origin is
`https://algoarena.open-next.workers.dev`; set `APP_CORS_ALLOWED_ORIGIN_PATTERNS` in
Render if the frontend uses a different custom domain.

Use Prisma's direct TCP URL rather than `prisma+postgres://` or the pooled URL. The
backend converts the standard Prisma URL to JDBC internally and requires SSL.

## Google Cloud Run deployment

Frontend origin:

- `https://algoarena.open-next.workers.dev`

Backend stack:

- Cloud Run service: `algoarena-api`
- Prisma Postgres database
- Artifact Registry repository: `algoarena`
- Region: `asia-south1` (Mumbai)
- Runtime service account: `algoarena-runner`
- Google Secret Manager for Prisma, JWT, and one-time admin credentials

## Database compatibility

The backend remains Spring Data JPA/Hibernate. Prisma Postgres is used only as the managed PostgreSQL service; Prisma ORM is not added to this Java project.

In Prisma Console, open the database and generate both connection strings. They look like:

```text
Pooled: postgres://USER:PASSWORD@pooled.db.prisma.io:5432/postgres?sslmode=require
Direct: postgres://USER:PASSWORD@db.prisma.io:5432/postgres?sslmode=require
```

Use the **direct TCP** credential for the current deployment. Hibernate still initializes and updates the schema at application startup, which requires the session continuity of a direct connection. Cloud Run is limited to one instance and Hikari to five connections to avoid exhausting Prisma Postgres.

Do not use a legacy `prisma+postgres://` Accelerate URL; PostgreSQL JDBC cannot use it. Once versioned database migrations replace `ddl-auto=update`, switch application traffic to Prisma’s pooled host.

## Production safety

The `prod` profile intentionally disables:

- user code execution (`app.judge.enabled=false`);
- Nation awards (`nation.award-enabled=false`);
- automated problem imports (`app.problem-import.enabled=false`).

The current judge executes user Java directly and is not sandboxed. Do not enable it or Nation awards publicly. The production image also uses a JRE instead of a JDK, so it cannot compile submitted Java.

The single-instance limit is also required because presence state is currently stored in memory.

## 1. Create Google Cloud resources

Replace `YOUR_PROJECT_ID` in the commands below.

```bash
gcloud config set project YOUR_PROJECT_ID

gcloud services enable \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  secretmanager.googleapis.com
```

Create the Docker repository and runtime identity:

```bash
gcloud artifacts repositories create algoarena \
  --repository-format=docker \
  --location=asia-south1

gcloud iam service-accounts create algoarena-runner \
  --display-name="AlgoArena Cloud Run"
```

Grant the runtime identity access to Secret Manager:

```bash
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:algoarena-runner@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

No Cloud SQL API, Cloud SQL instance, VPC connector, or Cloud SQL IAM role is required. Cloud Run connects to Prisma Postgres over encrypted outbound TCP on port `5432`.

## 2. Create Prisma and application secrets

The deployment follows the backend environment contract:

- `DB_URL`
- `ADMIN_BOOTSTRAP_ENABLED`
- `ADMIN_USERNAME`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`

`APP_JWT_SECRET` is additionally required so production never uses the repository's development signing key.

Create these secrets:

```bash
gcloud secrets create algoarena-prisma-db-url --replication-policy=automatic
gcloud secrets create algoarena-jwt-secret --replication-policy=automatic
gcloud secrets create algoarena-admin-password --replication-policy=automatic
```

Add each value without placing it in source code or shell history:

```bash
gcloud secrets versions add algoarena-prisma-db-url --data-file=-
gcloud secrets versions add algoarena-jwt-secret --data-file=-
gcloud secrets versions add algoarena-admin-password --data-file=-
```

| Secret | Value |
|---|---|
| `algoarena-prisma-db-url` | Complete Prisma **direct** URL: `postgres://USER:PASSWORD@db.prisma.io:5432/postgres?sslmode=require` |
| `algoarena-jwt-secret` | New Base64-encoded random value of at least 32 bytes |
| `algoarena-admin-password` | Rotated admin password for one-time bootstrap |

The backend converts the standard Prisma URL to JDBC internally, including URL-decoded credentials. Do not convert it to a `jdbc:` URL and do not use `prisma+postgres://`.

## 3. Grant Cloud Build deployment access

Get the Cloud Build service account:

```bash
gcloud builds get-default-service-account
```

Grant that identity:

- `roles/artifactregistry.writer`
- `roles/run.admin`
- permission to act as `algoarena-runner` via `roles/iam.serviceAccountUser`

Use the smallest IAM scope your organization permits. Do not grant owner/editor to deployment automation.

## 4. Build and deploy

`cloudbuild.yaml` builds `backend/Dockerfile`, pushes the image to Artifact Registry, and deploys the service with the `prod` profile.

```bash
gcloud builds submit --config cloudbuild.yaml .
```

Cloud Run receives `DB_URL` and `APP_JWT_SECRET` from Secret Manager. Normal deployments set `ADMIN_BOOTSTRAP_ENABLED=false`; the remaining admin variables are attached only for the one-time bootstrap.

Get the backend URL:

```bash
gcloud run services describe algoarena-api \
  --region=asia-south1 \
  --format="value(status.url)"
```

Verify:

```text
https://YOUR_CLOUD_RUN_URL/actuator/health
```

If startup fails, verify that the Prisma credential is active, uses the direct TCP endpoint, allows SSL, and that outbound TCP port `5432` is not restricted by an organization policy.

## 5. Create the first admin

The application creates an admin only when no admin exists. Enable bootstrap for one revision:

```bash
gcloud run services update algoarena-api \
  --region=asia-south1 \
  --update-env-vars=ADMIN_BOOTSTRAP_ENABLED=true,ADMIN_USERNAME=YOUR_ADMIN_USERNAME,ADMIN_EMAIL=YOUR_ADMIN_EMAIL \
  --update-secrets=ADMIN_PASSWORD=algoarena-admin-password:latest
```

Confirm in Cloud Run logs that `ruler_of_algoarena` was created. Then immediately detach bootstrap credentials:

```bash
gcloud run services update algoarena-api \
  --region=asia-south1 \
  --update-env-vars=ADMIN_BOOTSTRAP_ENABLED=false \
  --remove-env-vars=ADMIN_USERNAME,ADMIN_EMAIL \
  --remove-secrets=ADMIN_PASSWORD
```

The database stores only the BCrypt password hash. Future Cloud Build deployments keep bootstrap disabled.

## 6. Connect the Cloudflare frontend

In Cloudflare/OpenNext build settings, set:

```text
NEXT_PUBLIC_API_BASE_URL=https://YOUR_CLOUD_RUN_URL/api
```

This is a public build-time value, not a secret. Rebuild the frontend after setting it. Backend CORS permits only:

```text
https://algoarena.open-next.workers.dev
```

Test registration, player login, admin login, profiles, problem listing, and health. Submission and custom execution intentionally return HTTP `503` until a secure judge exists.

## 7. Validation

```bash
cd backend
./mvnw clean verify
```

```bash
cd frontend
npm run build
```

The local default profile still uses local PostgreSQL. The `prod` profile consumes one standard `DB_URL` (`postgres://` or `postgresql://`), so it works with Prisma Postgres or another standard PostgreSQL provider without provider-specific code.
