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
| `TELEGRAM_BOT_TOKEN` | Telegram bot token for sending notifications |

These variables can be exported in your shell or provided in a `.env` file that Spring Boot reads using the standard property placeholders.

## Telegram configuration

The bot token is supplied via the `telegram.bot-token` property. `src/main/resources/application.yml` maps this property to the `TELEGRAM_BOT_TOKEN` environment variable.

Example:

```bash
export TELEGRAM_BOT_TOKEN=<your-token>
./gradlew bootRun
```

