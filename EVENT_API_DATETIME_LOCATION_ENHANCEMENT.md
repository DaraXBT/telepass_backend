# Event API Enhancement - DateTime and Location Fields

## Overview

Added three new fields to the Event entity and all related API endpoints to support event scheduling and location information.

## New Fields Added

### 1. **startDateTime** (LocalDateTime)

- **Type**: `LocalDateTime`
- **Database Column**: `start_date_time`
- **Purpose**: Stores the start date and time of the event
- **Format**: ISO 8601 datetime format (e.g., "2025-06-14T10:00:00")

### 2. **endDateTime** (LocalDateTime)

- **Type**: `LocalDateTime`
- **Database Column**: `end_date_time`
- **Purpose**: Stores the end date and time of the event
- **Format**: ISO 8601 datetime format (e.g., "2025-06-14T18:00:00")

### 3. **location** (String)

- **Type**: `String`
- **Database Column**: `location` (TEXT type)
- **Purpose**: Stores event location information (can be text address or URL)
- **Examples**:
  - Text: "Conference Hall A, Building 1, University Campus"
  - URL: "https://maps.google.com/..."

## Files Modified

### 1. **Event Entity** (`Event.java`)

```java
// New fields added to Event entity
@Column(name = "start_date_time")
private LocalDateTime startDateTime;

@Column(name = "end_date_time")
private LocalDateTime endDateTime;

@Column(name = "location", columnDefinition = "TEXT")
private String location; // Can be text or URL for event location
```

### 2. **EventRequest** (`EventRequest.java`)

```java
// New fields added to request DTO
private LocalDateTime startDateTime;
private LocalDateTime endDateTime;
private String location; // Can be text or URL for event location
```

### 3. **EventResponse** (`EventResponse.java`)

```java
// New fields added to response DTO
private LocalDateTime startDateTime;
private LocalDateTime endDateTime;
private String location; // Can be text or URL for event location
```

### 4. **EventServiceImpl** (`EventServiceImpl.java`)

- Updated `createEvent()` method to handle new fields
- Updated `updateEvent()` method to handle new fields
- Updated `convertToResponse()` method to include new fields

### 5. **EventController** (`EventController.java`)

- Updated create endpoint response to include new fields
- Updated debug endpoint to include new fields

## API Endpoints Updated

### 1. **GET /api/v1/events** - Get All Events

**Response includes new fields:**

```json
{
  "id": "uuid",
  "name": "Event Name",
  "description": "Event Description",
  "startDateTime": "2025-06-14T10:00:00",
  "endDateTime": "2025-06-14T18:00:00",
  "location": "Conference Hall A",
  "status": "Upcoming",
  "category": "Conference",
  "capacity": 100,
  "registered": 50,
  "qrCodePath": "storage/event_uuid.png",
  "eventImg": "path/to/image.jpg",
  "adminId": "admin-uuid"
}
```

### 2. **GET /api/v1/events/{id}** - Get Event By ID

**Response includes new fields** (same format as above)

### 3. **POST /api/v1/events** - Create Event

**Request Body:**

```json
{
  "name": "New Event",
  "description": "Event Description",
  "startDateTime": "2025-06-14T10:00:00",
  "endDateTime": "2025-06-14T18:00:00",
  "location": "Conference Hall A",
  "status": "Upcoming",
  "category": "Conference",
  "capacity": 100,
  "registered": 0,
  "eventImg": "path/to/image.jpg",
  "adminId": "admin-uuid"
}
```

**Response:** EventResponse with new fields included

### 4. **PUT /api/v1/events/{id}** - Update Event

**Request Body:** Same format as POST (includes new fields)
**Response:** EventResponse with updated new fields

### 5. **GET /api/v1/events/debug/{id}** - Debug Event Details

**Response includes new fields:**

```json
{
  "eventId": "uuid",
  "name": "Event Name",
  "startDateTime": "2025-06-14T10:00:00",
  "endDateTime": "2025-06-14T18:00:00",
  "location": "Conference Hall A",
  "...": "other fields"
}
```

## Database Impact

### Migration Required

The database schema will need to be updated to include the new columns:

```sql
ALTER TABLE Event ADD COLUMN start_date_time TIMESTAMP;
ALTER TABLE Event ADD COLUMN end_date_time TIMESTAMP;
ALTER TABLE Event ADD COLUMN location TEXT;
```

**Note:** If using Spring Boot with JPA/Hibernate auto-DDL, these columns will be created automatically.

## Usage Examples

### Creating an Event with DateTime and Location

```json
POST /api/v1/events
{
  "name": "Tech Conference 2025",
  "description": "Annual technology conference",
  "startDateTime": "2025-06-14T09:00:00",
  "endDateTime": "2025-06-14T17:00:00",
  "location": "Convention Center, 123 Main St, City",
  "status": "Upcoming",
  "category": "Technology",
  "capacity": 500,
  "adminId": "admin-uuid-here"
}
```

### Updating Event Times and Location

```json
PUT /api/v1/events/{eventId}
{
  "startDateTime": "2025-06-15T10:00:00",
  "endDateTime": "2025-06-15T18:00:00",
  "location": "https://maps.google.com/venue-link",
  "...": "other fields"
}
```

## Validation Considerations

### Recommended Validations (to be implemented)

1. **Start before End**: `startDateTime` should be before `endDateTime`
2. **Future Events**: `startDateTime` should be in the future for new events
3. **Location Format**: Validate if location is URL format or text address
4. **Time Zone**: Consider time zone handling for international events

## Testing

- ✅ **Compilation**: Successful
- ✅ **Build**: Successful
- ✅ **Field Mapping**: All DTOs updated
- ✅ **API Endpoints**: All CRUD operations support new fields
- ✅ **Telegram Integration**: Enhanced welcome messages with datetime and location
- ✅ **Map Functionality**: Location handling with Google Maps integration
- ✅ **Helper Methods**: All Telegram helper methods implemented and tested

## Implementation Status

- ✅ Event entity fields added (`startDateTime`, `endDateTime`, `location`)
- ✅ Event request/response DTOs updated
- ✅ Event service implementation updated
- ✅ Event controller endpoints updated
- ✅ Telegram service welcome message enhanced
- ✅ Telegram location map functionality implemented
- ✅ All helper methods implemented:
  - `formatEventDateTime(EventResponse event)`
  - `formatEventLocation(EventResponse event)`
  - `sendLocationIfPossible(long chatId, String location)`
  - `isValidMapUrl(String location)`
  - `isCoordinateFormat(String location)`
  - `sendMapLink(long chatId, String mapUrl)`
  - `sendCoordinatesAsLocation(long chatId, String coordinates)`
- ✅ Full compilation and build successful
- ✅ No breaking changes to existing functionality

## Backward Compatibility

- ✅ **Existing Data**: New fields are nullable, existing events will have null values
- ✅ **API Calls**: Existing API calls without new fields will still work
- ✅ **Optional Fields**: New fields are optional in requests
- ✅ **Telegram Messages**: Gracefully handle events without datetime/location data

## Telegram Bot Enhancements

### Welcome Message Format

The Telegram bot now displays enhanced welcome messages including:

- 📅 Start datetime in Khmer (បើមាន)
- 🏁 End datetime in Khmer (បើមាន)
- 📍 Location information (បើមាន)
- 🗺️ Interactive map links when applicable

### Location Handling Features

- **Text locations**: Auto-generates Google Maps search links
- **Google Maps URLs**: Sends as clickable links
- **Coordinates (lat,lng)**: Converts to Google Maps viewing links
- **Invalid/empty locations**: Gracefully omitted from welcome message
- **Error handling**: Fallback to text display if map functionality fails

## Implementation Complete

All requested functionality has been successfully implemented:

1. ✅ Added startDateTime, endDateTime, and location fields to Event entity
2. ✅ Updated all API endpoints (create, update, get) to handle new fields
3. ✅ Enhanced TelegramServiceImpl to include datetime and location in welcome messages
4. ✅ Implemented map functionality for location display
5. ✅ All helper methods implemented and working
6. ✅ Full compilation and build successful
7. ✅ Comprehensive error handling and graceful fallbacks
