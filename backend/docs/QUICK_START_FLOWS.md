# WhatsApp Flows - Quick Start Guide

## 5-Minute Setup

### 1. Generate RSA Keys

```bash
cd backend
mkdir -p keys
cd keys

# Generate private key
openssl genpkey -algorithm RSA -out private-key.pem -pkeyopt rsa_keygen_bits:2048

# Generate public key
openssl rsa -pubout -in private-key.pem -out public-key.pem

echo "Keys generated successfully!"
```

### 2. Update Configuration

Edit `src/main/resources/application.properties`:

```properties
# Point to your private key
meta.flow.private.key.path=./keys/private-key.pem

# You'll add Flow IDs after creating flows in Meta
meta.flow.patient.registration.id=YOUR_FLOW_ID_HERE
```

### 3. Create Flow in Meta

1. Go to https://business.facebook.com
2. Navigate to **WhatsApp Manager → Flows**
3. Click **Create Flow**
4. Name it: "Patient Registration"
5. Click **JSON** tab and paste:

```json
{
  "version": "3.0",
  "screens": [
    {
      "id": "PATIENT_REG_SCREEN",
      "title": "Register",
      "terminal": true,
      "layout": {
        "type": "SingleColumnLayout",
        "children": [
          {
            "type": "Form",
            "name": "form",
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
                "input-type": "number",
                "required": true
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
                "label": "Register",
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

6. Click **Settings → Endpoint** and upload `public-key.pem`
7. Click **Publish**
8. Copy the **Flow ID** (looks like `123456789012345`)
9. Update `application.properties` with this Flow ID

### 4. Configure Webhook

In Meta Business Manager:

1. Go to **WhatsApp → Configuration → Webhooks**
2. Set Callback URL: `https://your-domain.com/meta/flow/webhook`
3. Set Verify Token: `DIVISHA_META_WEBHOOK` (matches your config)
4. Subscribe to: `messages`

### 5. Send Your First Flow

Create a simple test handler or use existing handlers:

```java
@Autowired
private MetaMessageBuilder messageBuilder;

@Autowired
private MetaMessageSender messageSender;

@Value("${meta.flow.patient.registration.id}")
private String flowId;

public void sendTestFlow(String phoneNumber) {
    MetaFlowMessage flowMsg = messageBuilder.patientRegistrationFlow(flowId);
    messageSender.sendFlow(phoneNumber, flowMsg);
}
```

Or trigger it from your existing conversation handlers!

### 6. Test It

1. Start your application: `./mvnw spring-boot:run`
2. Send a test message to your WhatsApp bot
3. When the flow message appears, tap "Register Now"
4. Fill the form and submit
5. Check logs for:
   ```
   📩 Incoming Flow webhook payload: ...
   🔐 Decrypting flow data for user: ...
   ✅ Decrypted flow response: ...
   ```

## Integration with Existing Handlers

### Example: Replace Patient Name/Age/Gender Flow

**Before:** Multiple conversation states (ASK_NAME → ASK_AGE → ASK_GENDER)

**After:** Single flow!

```java
// In your StartHandler or wherever new patients are detected
if (isNewPatient) {
    // OLD WAY - 3 separate messages
    // contextStore.setState(phone, ConversationState.ASK_NAME);
    // messageSender.text(phone, "Please enter your name:");

    // NEW WAY - 1 flow
    MetaFlowMessage flow = messageBuilder.patientRegistrationFlow(flowId);
    messageSender.sendFlow(phone, flow);
}
```

The `FlowService` will automatically handle the response and populate:
- `contextStore.setName(phone, name)`
- `contextStore.setAge(phone, age)`
- `contextStore.setGender(phone, gender)`

Then transition to the next state (location selection, etc.)

## Common Use Cases

### Use Case 1: New Patient Registration

```java
// Send flow
messageSender.sendFlow(phone, messageBuilder.patientRegistrationFlow(flowId));

// FlowService automatically processes response
// Patient data saved to context and database
// Conversation continues to next step
```

### Use Case 2: Pre-fill Appointment Data

```java
Map<String, Object> prefill = Map.of(
    "location", contextStore.getLocation(phone),
    "doctor", contextStore.getDoctor(phone)
);

MetaFlowMessage flow = messageBuilder.appointmentBookingFlow(flowId, prefill);
messageSender.sendFlow(phone, flow);
```

### Use Case 3: Custom Flow

```java
MetaFlowMessage customFlow = messageBuilder.customFlow(
    "YOUR_FLOW_ID",
    "Feedback Form",
    "Please rate your experience",
    "Thank you!",
    "Submit Feedback",
    "FEEDBACK_SCREEN"
);
messageSender.sendFlow(phone, customFlow);
```

## Debugging Tips

### Check if private key is loaded correctly

```bash
# Should show RSA key details
openssl rsa -in keys/private-key.pem -text -noout
```

### Test webhook verification

```bash
curl -X GET "http://localhost:8090/meta/flow/webhook?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=DIVISHA_META_WEBHOOK"
# Should return: test123
```

### View decrypted flow data

Check application logs after submitting a flow:

```bash
tail -f logs/application.log | grep "Decrypted flow"
```

## Next Steps

1. ✅ Create additional flows (appointment booking, reschedule, cancel)
2. ✅ Update existing handlers to use flows instead of multi-step conversations
3. ✅ Add custom validation in `FlowService`
4. ✅ Implement error handling for invalid flow data
5. ✅ Monitor flow completion rates in Meta Analytics

## Need Help?

- **Full Documentation:** See `docs/WHATSAPP_FLOWS_INTEGRATION.md`
- **Sample Flows:** Check `docs/flows/` directory
- **Meta Docs:** https://developers.facebook.com/docs/whatsapp/flows

Happy flowing! 🚀
