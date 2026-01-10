# WhatsApp Flow API Documentation

## Overview

This document describes the API endpoints for the WhatsApp Flow appointment booking system. The system uses encrypted communication with WhatsApp to provide a secure, interactive appointment booking experience.

## Architecture

```
WhatsApp Flow (User Device)
    ↓ (Encrypted Request)
Flow Data Exchange Controller
    ↓
Appointment Flow Service
    ↓
Repository Layer (MongoDB)
```

## Endpoints

### 1. Data Exchange Endpoint

**Endpoint:** `POST /meta/flow/data-exchange`

**Description:** Handles dynamic data requests from WhatsApp Flow screens. This endpoint is called when users interact with dropdown fields to fetch options dynamically.

**Content-Type:** `application/json`

**Request Format:**

The request arrives encrypted from WhatsApp. After decryption, the payload structure is:

```json
{
  "version": "3.0",
  "action": "data_exchange",
  "screen": "APPOINTMENT_SELECTION",
  "data": {
    "doctor": "DOC001",
    "location": "LOC001",
    "date": "",
    "time_slot": ""
  },
  "flow_token": "unique-session-token"
}
```

**Request Fields:**

| Field | Type | Description |
|-------|------|-------------|
| version | string | Flow version (always "3.0") |
| action | string | Action type (always "data_exchange") |
| screen | string | Current screen ID |
| data | object | Current form field values |
| flow_token | string | Unique session identifier |

**Response Format:**

The response is encrypted before sending back to WhatsApp. The decrypted structure:

```json
{
  "version": "3.0",
  "screen": "APPOINTMENT_SELECTION",
  "data": {
    "doctors": [
      {
        "id": "DOC001",
        "title": "Dr. Sarah Johnson",
        "description": "Cardiologist"
      },
      {
        "id": "DOC002",
        "title": "Dr. Michael Chen",
        "description": "Pediatrician"
      }
    ],
    "locations": [
      {
        "id": "LOC001",
        "title": "Downtown Medical Center",
        "description": "123 Main Street, Suite 100"
      }
    ],
    "dates": [
      {
        "id": "2025-12-15",
        "title": "Mon, Dec 15, 2025",
        "description": ""
      }
    ],
    "time_slots": [
      {
        "id": "09:00",
        "title": "09:00 AM",
        "description": "Available"
      },
      {
        "id": "09:30",
        "title": "09:30 AM",
        "description": "Available"
      }
    ]
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| version | string | Flow version (matches request) |
| screen | string | Screen to display (usually same as request) |
| data | object | Dynamic data to populate dropdowns |
| error_message | string | Optional error message if request fails |

**Data Loading Behavior:**

The endpoint intelligently loads data based on user selections:

1. **Initial Load** (`doctor` is empty):
   - Returns list of all active doctors

2. **Doctor Selected** (`doctor` filled, `location` empty):
   - Returns doctors list (to preserve selection)
   - Returns locations where selected doctor practices

3. **Location Selected** (`location` filled, `date` empty):
   - Returns doctors and locations lists
   - Returns available dates for next 14 days

4. **Date Selected** (`date` filled, `time_slot` empty):
   - Returns doctors, locations, and dates lists
   - Returns available time slots for selected date

### 2. Flow Completion Webhook

**Endpoint:** `POST /meta/flow/webhook`

**Description:** Receives the final appointment data when user completes the booking flow. This endpoint is called by WhatsApp when the flow reaches a terminal screen with `action: "complete"`.

**Request Format:**

The request arrives in WhatsApp webhook format with encrypted flow data:

```json
{
  "entry": [
    {
      "changes": [
        {
          "value": {
            "messages": [
              {
                "from": "919876543210",
                "type": "interactive",
                "interactive": {
                  "type": "nfm_reply",
                  "nfm_reply": {
                    "response_json": "{\"encrypted_flow_data\":\"...\",\"encrypted_aes_key\":\"...\",\"initial_vector\":\"...\",\"flow_token\":\"...\"}"
                  }
                }
              }
            ]
          }
        }
      ]
    }
  ]
}
```

**Decrypted Flow Data:**

```json
{
  "flow_token": "unique-session-token",
  "action": "complete",
  "screen": "CONFIRMATION",
  "data": {
    "doctor": "DOC001",
    "location": "LOC001",
    "date": "2025-12-15",
    "time_slot": "09:00"
  },
  "version": "3.0",
  "flow_id": "1234567890"
}
```

**Processing:**

1. Extracts and decrypts flow completion data
2. Creates appointment record in database
3. Generates unique booking ID
4. Sends confirmation message to user via WhatsApp

**Response:**

```json
{
  "status": "success"
}
```

### 3. Health Check Endpoint

**Endpoint:** `GET /meta/flow/data-exchange/health`

**Description:** Health check endpoint for monitoring service availability.

**Response:**

```json
{
  "status": "healthy",
  "service": "flow-data-exchange"
}
```

## Data Models

### Doctor

```typescript
{
  id: string,              // Unique doctor ID (e.g., "DOC001")
  name: string,            // Doctor's full name
  specialization: string,  // Medical specialization
  gender: "MALE" | "FEMALE" | "OTHER",
  status: "active" | "inactive",
  createdAt: number       // Timestamp
}
```

### Location

```typescript
{
  id: string,       // Unique location ID (e.g., "LOC001")
  name: string,     // Location name
  address: string,  // Full address
  status: "active" | "inactive"
}
```

### DoctorSchedule

```typescript
{
  id: string,
  doctorId: string,
  doctorName: string,
  locationId: string,
  startTime: string,                    // "HH:mm" format (e.g., "09:00")
  endTime: string,                      // "HH:mm" format (e.g., "17:00")
  unavailableDaysOfWeek: string[],     // ["SATURDAY", "SUNDAY"]
  unavailableDates: string[],          // ["2025-12-25", "2026-01-01"]
  customDateSlots: CustomDateSlot[]    // Override schedule for specific dates
}
```

### Appointment

```typescript
{
  id: string,
  doctorId: string,
  locationId: string,
  patientId: string,
  bookingId: string,           // User-facing booking reference (e.g., "APT-1234567890-123")
  date: string,                // "yyyyMMdd" format (e.g., "20251215")
  time: string,                // "HH:mm" format (e.g., "09:00")
  phone: string,               // Patient phone number
  status: "confirmed" | "cancelled" | "completed",
  createdTs: number            // Timestamp
}
```

## Error Handling

### Common Error Scenarios

1. **Missing Doctor Schedule**
   - When: Location or date options requested for doctor without schedule
   - Response: Empty array in respective field
   - Logging: Warning logged with doctor ID

2. **No Available Slots**
   - When: All time slots for selected date are booked
   - Response: Empty `time_slots` array
   - User Experience: Dropdown shows no options

3. **Decryption Failure**
   - When: Invalid encryption keys or corrupted data
   - Response: HTTP 500 with encrypted error message
   - Logging: Full stack trace logged

4. **Missing Required Fields**
   - When: Flow completion data missing required fields
   - Response: Error message sent to user via WhatsApp
   - Logging: Warning logged with missing field names

## Security

### Encryption

All communication uses end-to-end encryption:

1. **Request Decryption:**
   - RSA decryption of AES key using private key
   - AES-256-CBC decryption of payload using decrypted AES key and IV

2. **Response Encryption:**
   - AES-256-CBC encryption of response using same AES key and IV
   - Base64 encoding of encrypted response

### Private Key Management

- Private key stored at path configured in `meta.flow.private.key.path`
- Key must be in PKCS#8 format
- Generated via Meta Business Manager during Flow setup

### Webhook Verification

- Webhook verification token validated on GET request
- Configured in `meta.api.webhook.token`

## Sample Data

The system includes a sample data initializer that creates:

- **4 Doctors:**
  - Dr. Sarah Johnson (Cardiologist)
  - Dr. Michael Chen (Pediatrician)
  - Dr. Emily Rodriguez (Dermatologist)
  - Dr. James Williams (Orthopedic Surgeon)

- **3 Locations:**
  - Downtown Medical Center
  - Northside Clinic
  - Westside Hospital

- **7 Doctor Schedules:** Various schedules across locations and days

## Testing

### Testing Data Exchange (Local)

Since the endpoint expects encrypted data, local testing requires:

1. Use the Flow Builder in Meta Business Manager
2. Configure webhook URL to point to your local server (use ngrok for local testing)
3. Test the flow in the Flow Builder preview mode

### Postman Testing

See `postman_collection.json` for example requests with sample encrypted payloads.

## Logging

The application uses SLF4J with structured logging:

- `📊` - Data exchange processing
- `📥` - Incoming requests
- `📤` - Outgoing responses
- `✅` - Successful operations
- `❌` - Errors
- `⚠️` - Warnings

Example log output:

```
2025-12-11 10:30:15 INFO  📊 Processing data_exchange for screen: APPOINTMENT_SELECTION, data: {doctor=DOC001, location=, date=, time_slot=}
2025-12-11 10:30:15 INFO  Found 4 active doctors
2025-12-11 10:30:15 INFO  Found 2 locations for doctor: DOC001
2025-12-11 10:30:15 INFO  📤 Response JSON: {"version":"3.0","screen":"APPOINTMENT_SELECTION",...}
```

## Performance Considerations

1. **Database Queries:**
   - Doctor schedules are cached in service layer
   - Location lookups use indexed queries
   - Appointment checks use compound index on (doctorId, locationId, date)

2. **Response Time:**
   - Target: < 500ms for data_exchange requests
   - Encryption overhead: ~50-100ms
   - Database queries: ~50-200ms

3. **Scalability:**
   - Stateless design allows horizontal scaling
   - MongoDB connection pooling for concurrent requests
   - Consider caching doctor/location data in Redis for high traffic

## Support

For issues or questions:
- Check application logs for detailed error messages
- Verify WhatsApp Flow configuration in Meta Business Manager
- Ensure private key is correctly configured
- Test encryption/decryption separately if issues persist
