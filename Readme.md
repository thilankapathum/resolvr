# Resolvr

Internal complaint tracking system for telecom engineering teams.

---

## Tech Stack

| Layer     | Technology                      |
|-----------|---------------------------------|
| Backend   | Spring Boot 4.0, Java 21        |
| Frontend  | Angular 19, DaisyUI, Tailwind   |
| Database  | PostgreSQL 17 (Alpine)          |
| Migration | Flyway                          |
| Auth      | JWT (access + refresh tokens)   |
| Deploy    | Docker Compose + Nginx Proxy Mgr|

---

## Project Structure

```
resolvr/
├── backend/           # Spring Boot API
├── frontend/          # Angular SPA
├── docker-compose.yml            # Local dev
├── docker-compose.prod.yml       # Production overrides
├── .env.example                  # Template — copy to .env
└── .gitignore
```

---

## Local Development

### Prerequisites
- Docker Desktop (Windows)
- Git

### 1. Clone & configure

```bash
git clone https://github.com/your-org/resolvr.git
cd resolvr
# No .env needed for local dev — defaults are in docker-compose.yml
```

### 2. Start everything

```bash
docker compose up --build
```

This starts:
- **PostgreSQL** on `localhost:5432`
- **Spring Boot API** on `localhost:8080/api`
- **Angular Frontend** on `localhost:4200`

Flyway runs automatically on startup and:
1. Creates all tables (`V1__init_schema.sql`)
2. Seeds 25 Sri Lanka districts (`V2__seed_districts.sql`)
3. Creates the default admin user (`V3__seed_admin_user.sql`)

### 3. First login

| Field    | Value                  |
|----------|------------------------|
| URL      | http://localhost:4200  |
| Email    | admin@resolvr.local    |
| Password | Admin@1234             |

> ⚠️ **Change the admin password immediately after first login.**

---

## Production Deployment (Oracle Cloud ARM)

### Prerequisites on server
- Docker & Docker Compose installed
- Nginx Proxy Manager running with a known Docker network (e.g. `npm_network`)

### 1. Clone repo on server

```bash
git clone https://github.com/your-org/resolvr.git /opt/resolvr
cd /opt/resolvr
```

### 2. Create .env

```bash
cp .env.example .env
nano .env   # Fill in all values
```

Required values:
```env
DB_NAME=resolvr
DB_USER=resolvr
DB_PASSWORD=<strong-password>
JWT_SECRET=<base64-secret-min-32-chars>
CORS_ORIGINS=https://resolvr.yourdomain.com
FRONTEND_URL=https://resolvr.yourdomain.com
API_URL=https://resolvr.yourdomain.com/api
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=<gmail-app-password>
```

Generate a strong JWT secret:
```bash
openssl rand -base64 48
```

### 3. Identify your NPM Docker network

```bash
docker network ls | grep npm
# Note the network name (commonly 'npm_network' or 'nginx-proxy-manager_default')
```

Update `docker-compose.prod.yml` if the network name differs.

### 4. Deploy

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

### 5. Configure Nginx Proxy Manager

Add a **Proxy Host** in NPM:
- **Domain:** `resolvr.yourdomain.com`
- **Scheme:** `http`
- **Forward Hostname:** `resolvr-frontend`
- **Forward Port:** `80`
- **SSL:** Enable Let's Encrypt

No separate proxy for the API is needed — the Angular production build uses `/api` as a relative path, served through the same nginx that proxies the frontend.

> **Wait** — the Angular nginx conf only serves static files. For the API, you need NPM (or a secondary nginx rule) to proxy `/api/*` → `http://resolvr-backend:8080/api/*`. Add this in NPM as an **Advanced** custom nginx config for the same proxy host:
>
> ```nginx
> location /api/ {
>     proxy_pass http://resolvr-backend:8080/api/;
>     proxy_set_header Host $host;
>     proxy_set_header X-Real-IP $remote_addr;
>     proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
>     proxy_set_header X-Forwarded-Proto $scheme;
> }
> ```

### 6. Update deployments

```bash
cd /opt/resolvr
git pull origin main
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

---

## User Roles

| Role              | Can Do                                                      |
|-------------------|-------------------------------------------------------------|
| Technical Officer | Log complaints, start, analyse, solve, escalate to engineer |
| Engineer          | All TO actions + receive escalations                         |
| Manager           | Create complaints, close/reopen, oversee region             |
| Head              | Create complaints, assign; read-only oversight              |
| Admin             | Manage users, regions, districts, all settings              |

## Complaint Statuses

```
NOT_ASSIGNED → NOT_STARTED → IN_PROGRESS ──→ ESCALATED_TO_ENGINEER
                                          ↘
                                           RESOLVED → CLOSED
                                                    ↙ (Manager reopen)
                                            IN_PROGRESS
```

## Complaint Reference Format

```
{DISTRICT_CODE}-{YYYYMM}-{XXXX}
Example: CMB-202506-0042
```

---

## Development Tips

### Backend — run without Docker

```bash
cd backend
# Requires a local PostgreSQL instance
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend — run without Docker

```bash
cd frontend
npm install
npm start   # Proxies /api to localhost:8080
```

### Database access (local)

```
Host:     localhost:5432
Database: resolvr
User:     resolvr
Password: resolvr
```

### Flyway migrations

Add new migrations in `backend/src/main/resources/db/migration/`:
```
V4__add_something.sql
V5__another_change.sql
```
Flyway runs them automatically on next startup. **Never edit existing V* files.**

---

## Security Notes

- JWT access tokens expire in 15 minutes; refresh tokens in 7 days
- Refresh tokens rotate on every use
- All refresh tokens are revoked on logout, password change, or admin reset
- Passwords are BCrypt-hashed with cost 12
- Accounts must be email-verified **and** admin-activated before login
- All status transitions are server-side validated; client cannot bypass them

---

## Future Roadmap (not yet implemented)

- [ ] Performance dashboard & stats per user/manager/head
- [ ] GPS map picker (Leaflet)
- [ ] Email notifications on assignment / escalation
- [ ] Complaint export (PDF/CSV)
- [ ] Full-text search across complaints
- [ ] Mobile-responsive improvements