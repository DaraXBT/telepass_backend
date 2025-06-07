# Telepass Backend

This project uses Spring Boot. Sensitive configuration values are provided via environment variables.

## Required environment variables

| Variable | Description |
| -------- | ----------- |
| `MAIL_USERNAME` | Username for sending email via SMTP |
| `MAIL_PASSWORD` | Password or app token for the SMTP user |
| `DB_USERNAME`   | Database username for the Postgres datasource |
| `DB_PASSWORD`   | Database password for the Postgres datasource |

These variables can be exported in your shell or provided in a `.env` file that Spring Boot reads using the standard property placeholders.
