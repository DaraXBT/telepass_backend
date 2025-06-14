# Event Roles Endpoint Fix

## Issue Identified

The `@GetMapping("/{eventId}/roles")` endpoint was returning `List<EventRole>` which caused several problems:

1. **Circular Reference Issues**: `EventRole` entity contains `Event` and `Event` contains `EventRole`, causing JSON serialization problems
2. **Lazy Loading Problems**: The `Admin user` field could cause lazy loading exceptions
3. **Unnecessary Data Exposure**: Returning full entities instead of relevant user information
4. **Performance Issues**: Loading full object graphs when only user information is needed

## Solution Implemented

### 1. Created EventRoleDTO

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRoleDTO {
    private UUID id;
    private UUID userId;
    private String username;
    private EventRoleType role;
}
```

### 2. Updated EventService Interface

Added method:

```java
List<EventRoleDTO> getEventRolesAsDTO(UUID eventId);
```

### 3. Updated EventServiceImpl

Added implementation:

```java
@Override
@Transactional(readOnly = true)
public List<EventRoleDTO> getEventRolesAsDTO(UUID eventId) {
    if (!eventRepository.existsById(eventId)) {
        throw new RuntimeException("Event not found");
    }

    List<EventRole> eventRoles = eventRoleRepository.findByEventId(eventId);
    return eventRoles.stream()
            .map(this::convertToEventRoleDTO)
            .collect(Collectors.toList());
}

private EventRoleDTO convertToEventRoleDTO(EventRole eventRole) {
    return EventRoleDTO.builder()
            .id(eventRole.getId())
            .userId(eventRole.getUser().getId())
            .username(eventRole.getUser().getUsername())
            .role(eventRole.getRole())
            .build();
}
```

### 4. Updated EventController

Changed endpoint return type:

```java
@GetMapping("/{eventId}/roles")
public List<EventRoleDTO> getEventRoles(@PathVariable UUID eventId) {
    return eventService.getEventRolesAsDTO(eventId);
}
```

## Benefits

1. **Clean JSON Response**: No circular references or serialization issues
2. **Better Performance**: Only loads necessary data
3. **User-Focused Data**: Returns user information (ID, username, role) as requested
4. **Maintainable**: Uses DTOs for API responses, keeping entities for internal use
5. **Consistent**: Follows the same pattern as other endpoints in the application

## API Response Example

**Before** (problematic):

```json
[
  {
    "id": "uuid",
    "event": {
      /* full event object with circular references */
    },
    "user": {
      /* full admin object with potential lazy loading issues */
    },
    "role": "OWNER"
  }
]
```

**After** (clean):

```json
[
  {
    "id": "uuid",
    "userId": "user-uuid",
    "username": "john.doe",
    "role": "OWNER"
  },
  {
    "id": "uuid",
    "userId": "user-uuid-2",
    "username": "jane.smith",
    "role": "ORGANIZER"
  }
]
```

## Usage

```
GET /api/v1/events/{eventId}/roles
```

Returns a list of users who have roles in the specified event, with their user information and assigned roles.
