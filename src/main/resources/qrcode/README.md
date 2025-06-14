# QR Code Storage Directory

This directory stores QR code images generated for user registrations.

## File Naming Convention

- Format: `user_{userId}_{uuid}.png`
- Example: `user_123e4567-e89b-12d3-a456-426614174000_abc123def456.png`

## QR Code Content Format

Each QR code contains the following data separated by pipes (|):

```
{eventId}|{userId}|{registrationToken}
```

## Storage Path in Database

The relative path stored in the User entity's `qrCode` field:

```
qrcode/user_{userId}_{uuid}.png
```

## Usage

- QR codes are generated during user registration completion
- Users can scan these codes to check in to events
- Each QR code is unique and contains verification data
