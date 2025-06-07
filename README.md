# Telepass Backend

## Configuration

The Telegram bot token is provided via the `telegram.bot-token` property.
`src/main/resources/application.yml` maps this to the `TELEGRAM_BOT_TOKEN`
environment variable.

Example:

```bash
export TELEGRAM_BOT_TOKEN=<your-token>
./gradlew bootRun
```

