# WhatsApp Flows API Integration Guide

## Overview

This guide explains how to use the WhatsApp Flows API integration in the Divisha WhatsApp Bot. WhatsApp Flows allow you to create rich, form-based interactions that replace multi-step conversations with a single interactive form.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Creating Flows in Meta](#creating-flows-in-meta)
5. [Sending Flow Messages](#sending-flow-messages)
6. [Processing Flow Responses](#processing-flow-responses)
7. [Sample Flows](#sample-flows)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### 1. Meta Business Account Setup

- WhatsApp Business Account with API access
- Access to Meta Flow Builder
- Valid Meta Access Token with `whatsapp_business_messaging` permission

### 2. Generate RSA Key Pair

WhatsApp Flows v3 requires RSA encryption for secure data exchange:

```bash
# Generate private key (PKCS8 format)
openssl genpkey -algorithm RSA -out private-key.pem -pkeyopt rsa_keygen_bits:2048

# Generate public key
openssl rsa -pubout -in private-key.pem -out public-key.pem
```

**Important:**
- Store `private-key.pem` securely on your server
- Upload `public-key.pem` to Meta Flow Builder when creating flows

---

## Architecture

### Components

1. **FlowWebhookController** (`/meta/flow/webhook`)
   - Receives flow completion events from Meta
   - Decrypts encrypted flow data using RSA + AES-GCM
   - Forwards to FlowService for processing

2. **FlowService**
   - Routes flow responses to appropriate business logic
   - Handles patient registration, appointment booking, etc.
   - Updates conversation state

3. **MetaFlowMessage**
   - Model for flow-triggering messages
   - Contains flow ID, CTA button, screen navigation

4. **MetaMessageSender**
   - Sends flow messages via Meta Cloud API
   - Method: `sendFlow(phoneNumber, flowMessage)`

5. **MetaMessageBuilder**
   - Helper methods to build flow messages
   - Pre-configured flows: patient registration, appointment booking

6. **ConversationContextStore**
   - Tracks active flow sessions
   - Stores flow tokens and user data

7. **MetaV3Crypto**
   - Decrypts incoming flow data
   - Uses RSA for AES key decryption
   - Uses AES-GCM for payload decryption

---

## Configuration

### application.properties

Add the following configuration:

```properties
# Path to RSA private key for flow data decryption
meta.flow.private.key.path=/path/to/private-key.pem

# Flow IDs from Meta Flow Builder
meta.flow.patient.registration.id=123456789012345
meta.flow.appointment.booking.id=234567890123456
meta.flow.reschedule.id=345678901234567
meta.flow.cancel.id=456789012345678
```

### Environment Variables (Recommended)

For production, use environment variables:

```bash
export META_FLOW_PRIVATE_KEY_PATH=/secure/path/private-key.pem
export META_FLOW_PATIENT_REG_ID=your_flow_id
export META_FLOW_APPOINTMENT_BOOKING_ID=your_flow_id
```

---

## Creating Flows in Meta

### Step 1: Access Flow Builder

1. Go to Meta Business Suite
2. Navigate to WhatsApp Manager → Flows
3. Click "Create Flow"

### Step 2: Define Flow Structure

Use the Flow Builder UI or upload JSON:

**Example: Patient Registration Flow**

```json
{
  "version": "3.0",
  "screens": [
    {
      "id": "PATIENT_REG_SCREEN",
      "title": "Patient Registration",
      "terminal": true,
      "layout": {
        "type": "SingleColumnLayout",
        "children": [
          {
            "type": "Form",
            "name": "patient_form",
            "children": [
              {
                "type": "TextInput",
                "name": "name",
                "label": "Full Name",
                "required": true
              },
              {
                "type": "TextInput",
                "name": "age",
                "label": "Age",
                "required": true,
                "input-type": "number"
              },
              {
                "type": "RadioButtonsGroup",
                "name": "gender",
                "label": "Gender",
                "required": true,
                "data-source": [
                  {"id": "male", "title": "Male"},
                  {"id": "female", "title": "Female"},
                  {"id": "other", "title": "Other"}
                ]
              },
              {
                "type": "Footer",
                "label": "Submit",
                "on-click-action": {
                  "name": "complete",
                  "payload": {
                    "name": "${form.name}",
                    "age": "${form.age}",
                    "gender": "${form.gender}"
                  }
                }
              }
            ]
          }
        ]
      }
    }
  ]
}
```

### Step 3: Upload Public Key

- In Flow settings, upload your `public-key.pem`
- This allows Meta to encrypt response data

### Step 4: Publish Flow

1. Test the flow in preview mode
2. Click "Publish"
3. Copy the Flow ID (e.g., `123456789012345`)
4. Add to `application.properties`

---

## Sending Flow Messages

### Using MetaMessageBuilder (Recommended)

```java
@Autowired
private MetaMessageBuilder messageBuilder;

@Autowired
private MetaMessageSender messageSender;

@Value("${meta.flow.patient.registration.id}")
private String patientRegFlowId;

// Send patient registration flow
public void sendPatientRegistrationFlow(String phoneNumber) {
    MetaFlowMessage flowMessage = messageBuilder.patientRegistrationFlow(patientRegFlowId);
    messageSender.sendFlow(phoneNumber, flowMessage);
}

// Send appointment booking flow with initial data
public void sendAppointmentBookingFlow(String phoneNumber, String locationId) {
    Map<String, Object> initialData = Map.of("preselected_location", locationId);
    MetaFlowMessage flowMessage = messageBuilder.appointmentBookingFlow(
        appointmentBookingFlowId,
        initialData
    );
    messageSender.sendFlow(phoneNumber, flowMessage);
}
```

### Manual Flow Message Creation

```java
MetaFlowMessage flowMessage = MetaFlowMessage.builder()
    .header("Patient Registration")
    .body("Please fill in your details to register")
    .footer("Your data is secure")
    .flowId("123456789012345")
    .flowCta("Register Now")
    .flowAction("navigate")
    .screenId("PATIENT_REG_SCREEN")
    .build();

messageSender.sendFlow("919876543210", flowMessage);
```

---

## Processing Flow Responses

### Flow Completion Webhook

When a user submits a flow, Meta sends an encrypted webhook to `/meta/flow/webhook`:

**Webhook Payload Structure:**

```json
{
  "entry": [{
    "changes": [{
      "value": {
        "messages": [{
          "from": "919876543210",
          "type": "interactive",
          "interactive": {
            "type": "nfm_reply",
            "nfm_reply": {
              "body": "encrypted_base64_data",
              "response_json": "{\"encrypted_aes_key\":\"...\",\"initial_vector\":\"...\"}"
            }
          }
        }]
      }
    }]
  }]
}
```

### Automatic Decryption

The `FlowWebhookController` automatically:

1. Extracts encrypted data from webhook
2. Decrypts AES key using RSA private key
3. Decrypts flow data using AES-GCM
4. Parses JSON into `FlowResponseData`
5. Routes to `FlowService`

### Handling Flow Responses in FlowService

```java
@Override
public void processFlowCompletion(String phoneNumber, FlowResponseData flowResponse) {
    Map<String, Object> data = flowResponse.getData();
    String flowId = flowResponse.getFlowId();

    if (flowId.contains("patient_registration")) {
        String name = (String) data.get("name");
        String age = (String) data.get("age");
        String gender = (String) data.get("gender");

        // Save to database
        Patient patient = patientService.createPatient(name, Integer.parseInt(age), gender, phoneNumber);

        // Send confirmation
        messageSender.text(phoneNumber, "Registration successful! Your Patient ID: " + patient.getPatientId());
    }
}
```

---

## Sample Flows

### 1. Patient Registration Flow

**File:** `docs/flows/patient-registration-flow.json`

**Fields:**
- Full Name (text)
- Age (number)
- Gender (radio)
- Symptoms (textarea, optional)

**Response Data:**
```json
{
  "name": "John Doe",
  "age": "35",
  "gender": "male",
  "symptoms": "Joint pain"
}
```

### 2. Appointment Booking Flow

**File:** `docs/flows/appointment-booking-flow.json`

**Fields:**
- Location (dropdown)
- Doctor (dropdown)
- Date (date picker)
- Consent (checkbox)

**Response Data:**
```json
{
  "location": "loc1",
  "doctor": "doc2",
  "date": "2025-01-15",
  "consent": true
}
```

---

## Testing

### 1. Test Flow Webhook Verification

```bash
curl -X GET "http://localhost:8090/meta/flow/webhook?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=DIVISHA_META_WEBHOOK"
```

Expected: `test123` (challenge echoed back)

### 2. Test Flow Sending

```java
@SpringBootTest
class FlowIntegrationTest {

    @Test
    void testSendPatientRegistrationFlow() {
        messageSender.sendFlow("919876543210",
            messageBuilder.patientRegistrationFlow(flowId));
        // Check WhatsApp app for interactive flow message
    }
}
```

### 3. Test Flow Response Processing

Use Meta's Flow Tester tool to simulate responses:

1. Go to Flow Builder → Test tab
2. Fill form and submit
3. Check application logs for decrypted data

---

## Troubleshooting

### Issue: "Private key not configured"

**Solution:** Ensure `meta.flow.private.key.path` points to valid PEM file

```bash
ls -l /path/to/private-key.pem
```

### Issue: "Decryption failed"

**Causes:**
- Public key uploaded to Meta doesn't match private key
- Private key not in PKCS8 format

**Solution:**
```bash
# Convert private key to PKCS8
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in old-key.pem -out private-key.pem
```

### Issue: "Flow not appearing in WhatsApp"

**Checks:**
1. Flow is published in Meta
2. Flow ID is correct in `application.properties`
3. Meta access token has correct permissions
4. Flow CTA button text is valid

### Issue: "Webhook not receiving flow responses"

**Solution:**
1. Configure webhook URL in Meta: `https://your-domain.com/meta/flow/webhook`
2. Ensure webhook token matches `meta.api.webhook.token`
3. Check webhook subscription includes `messages` field

---

## Best Practices

### 1. Security

- Store private key securely (never commit to Git)
- Use environment variables for production
- Implement webhook signature verification
- Validate flow response data before processing

### 2. User Experience

- Keep forms short (max 5-7 fields)
- Use appropriate input types (number, date, email)
- Provide helpful helper-text for fields
- Add clear success/error messages

### 3. Error Handling

```java
try {
    FlowResponseData response = decryptFlowData(...);
    processFlowCompletion(phone, response);
} catch (Exception e) {
    log.error("Flow processing failed", e);
    messageSender.text(phone, "Sorry, something went wrong. Please try again.");
}
```

### 4. Testing

- Test flows in Meta preview mode before publishing
- Use staging environment for webhook testing
- Monitor logs for decryption errors
- Validate all field data types

---

## API Reference

### MetaFlowMessage

```java
MetaFlowMessage.builder()
    .header(String)           // Optional header text
    .body(String)            // Required message body
    .footer(String)          // Optional footer text
    .flowId(String)          // WhatsApp Flow ID
    .flowCta(String)         // Button text (e.g., "Register")
    .flowAction(String)      // "navigate" or "data_exchange"
    .screenId(String)        // Screen to navigate to
    .flowActionPayload(Map)  // Optional initial data
    .build()
```

### FlowResponseData

```java
FlowResponseData {
    String flowToken;        // Unique flow session token
    String action;           // "complete", "back", "navigate"
    String screen;           // Screen where flow completed
    Map<String, Object> data; // Form field values
    String version;          // Flow version ("3.0")
    String flowId;           // WhatsApp Flow ID
}
```

### MetaMessageBuilder Methods

| Method | Description |
|--------|-------------|
| `patientRegistrationFlow(flowId)` | Patient registration form |
| `appointmentBookingFlow(flowId, data)` | Appointment booking form |
| `rescheduleAppointmentFlow(flowId, aptId)` | Reschedule form |
| `cancelAppointmentFlow(flowId, aptId)` | Cancellation form |
| `customFlow(flowId, header, body, ...)` | Custom flow message |

---

## Additional Resources

- [Meta WhatsApp Flows Documentation](https://developers.facebook.com/docs/whatsapp/flows)
- [Flow JSON Schema](https://developers.facebook.com/docs/whatsapp/flows/reference/flowjson)
- [Encryption Guide](https://developers.facebook.com/docs/whatsapp/flows/guides/implementingyourflowendpoint)

---

## Support

For issues or questions:
- Check logs: `tail -f logs/application.log`
- Review Meta webhook logs in Business Manager
- Contact: dev@divisha.com
