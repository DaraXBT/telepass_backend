# Telepass Backend

This project uses Spring Boot. Sensitive configuration values are provided via environment variables.

## Required environment variables

| Variable | Description |
| -------- | ----------- |
| `MAIL_USERNAME` | Username for sending email via SMTP |
| `MAIL_PASSWORD` | Password or app token for the SMTP user |
| `DB_USERNAME`   | Database username for the Postgres datasource |
| `DB_PASSWORD`   | Database password for the Postgres datasource |
| `ADMIN_USERNAME` | Default admin account username |
| `ADMIN_PASSWORD` | Default admin account password |
| `BASE_URL`      | Base URL used for building verification links |
| `WEB_BASE_URL`  | URL for the frontend to redirect after verification |

These variables can be exported in your shell or provided in a `.env` file that Spring Boot reads using the standard property placeholders.

## Quick start

1. Copy `.env.sample` to `.env` and update the values as needed.

```bash
cp .env.sample .env
```

2. Start a local PostgreSQL instance using Docker Compose:

```bash
docker compose up -d db
```

This exposes Postgres on `localhost:5432` with credentials taken from the
`DB_USERNAME` and `DB_PASSWORD` values in your `.env` file.

3. Build and run the application:

```bash
./gradlew bootRun
```
