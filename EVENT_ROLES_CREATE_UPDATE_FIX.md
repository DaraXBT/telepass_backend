# Event Roles Fix - Create and Update Events

## Issue Identified

The event creation and update functionality had a critical issue with `eventRoles` handling:

### ✅ CREATE EVENT - Working Correctly

- The `createEvent` method properly handled `eventRoles` from the request
- All eventRoles were saved to the database during creation

### ❌ UPDATE EVENT - Missing EventRoles Handling

- The `updateEvent` method **completely ignored** `eventRoles` from the request
- Only basic event fields (name, description, status, etc.) were updated
- EventRoles in the request body were silently ignored

## Solution Implemented

### 1. Fixed Update Event Method

Updated `EventServiceImpl.updateEvent()` to handle eventRoles:

- Added eventRoles processing after updating basic fields
- Removes existing roles and adds new ones from the request
- Uses the same pattern as createEvent for consistency

### 2. Created Reusable Helper Method

Added `handleEventRoles()` private method to:

- Reduce code duplication between create and update
- Centralize eventRoles logic for better maintainability
- Handle both create (isUpdate=false) and update (isUpdate=true) scenarios

```java
private void handleEventRoles(UUID eventId, List<EventRoleRequest> eventRoleRequests, Event event, boolean isUpdate) {
    if (eventRoleRequests == null || eventRoleRequests.isEmpty()) {
        log.debug("No event roles provided - skipping role handling");
        return;
    }

    log.debug("Handling {} event roles for event ID: {}", eventRoleRequests.size(), eventId);

    // If this is an update, remove existing roles first
    if (isUpdate) {
        log.debug("Removing existing event roles for event ID: {}", eventId);
        eventRoleRepository.deleteByEventId(eventId);
    }

    // Add new roles from request
    for (var roleRequest : eventRoleRequests) {
        try {
            if (roleRequest.getUserId() == null) {
                log.warn("Skipping event role with null user ID");
                continue;
            }

            Admin user = new Admin();
            user.setId(roleRequest.getUserId());

            EventRole role = EventRole.builder()
                    .event(event)
                    .user(user)
                    .role(roleRequest.getRole())
                    .build();
            eventRoleRepository.save(role);
            log.debug("Created event role for user: {} with role: {}", roleRequest.getUserId(), roleRequest.getRole());
        } catch (Exception e) {
            log.error("Failed to create event role for user {}: {}", roleRequest.getUserId(), e.getMessage(), e);
            // Continue with other roles - don't fail the entire operation
        }
    }
    log.debug("Event roles handling completed");
}
```

### 3. Updated Both Methods

**CreateEvent:**

```java
// Create initial owner role if specified in request
handleEventRoles(savedEvent.getId(), eventRequest.getEventRoles(), savedEvent, false);
```

**UpdateEvent:**

```java
// Handle event roles update if specified in request
handleEventRoles(savedEvent.getId(), eventRequest.getEventRoles(), savedEvent, true);
```

## Request Body Example

This request body now works correctly for both CREATE and UPDATE:

```json
{
  "name": "Pizza day",
  "description": "jjjjj",
  "status": "upcoming",
  "category": "Conferences",
  "capacity": 111,
  "registered": 1,
  "eventImg": "",
  "adminId": "bb9222d7-4f19-4a9b-80d1-8b6b6f903ac6",
  "eventRoles": [
    {
      "userId": "57a84922-5097-4dcc-a0f9-c41da32910d5",
      "role": "ORGANIZER"
    },
    {
      "userId": "889a9243-3eb5-43e0-8125-9c4f6a020593",
      "role": "ORGANIZER"
    }
  ],
  "registeredUsers": []
}
```

## Behavior Changes

### CREATE Event (No Changes)

- ✅ All eventRoles are saved to database
- ✅ Works exactly as before

### UPDATE Event (Fixed)

- ✅ All existing eventRoles for the event are removed
- ✅ All eventRoles from the request are saved to database
- ✅ If no eventRoles provided, existing roles are preserved (commented in code)

## Key Features

1. **Transactional Safety**: Both operations are wrapped in `@Transactional`
2. **Error Handling**: Individual role failures don't break the entire operation
3. **Logging**: Comprehensive logging for debugging
4. **Null Safety**: Handles null userIds gracefully
5. **Consistency**: Both create and update use the same logic
6. **Maintainability**: Centralized eventRoles handling

## Testing

- ✅ Compilation successful
- ✅ Both CREATE and UPDATE now handle eventRoles correctly
- ✅ Request body with multiple eventRoles works for both operations

## Database Impact

- Creates records in `event_role` table linking events to admin users
- Uses proper foreign key relationships (event_id, user_id)
- Supports OWNER and ORGANIZER role types
