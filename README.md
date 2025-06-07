# Telepass Backend

## Configuration

The application reads the Telegram bot token from the `telegram.bot-token` property.
Provide this value via the environment variable `TELEGRAM_BOT_TOKEN` referenced in
`src/main/resources/application.yml`.

Example:

```bash
export TELEGRAM_BOT_TOKEN=<your-token>
```

