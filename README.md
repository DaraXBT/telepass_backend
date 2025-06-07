# Telepass Backend

## Configuration

The application reads the Telegram bot token from the `telegram.bot-token` property. This property is configured in `src/main/resources/application.yml` to use the `TELEGRAM_BOT_TOKEN` environment variable.

Example:

```bash
export TELEGRAM_BOT_TOKEN=<your-token>
./gradlew bootRun
```

