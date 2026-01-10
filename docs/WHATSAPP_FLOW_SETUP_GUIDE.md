# WhatsApp Flow Appointment Booking - Complete Setup Guide

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Backend Setup](#backend-setup)
5. [WhatsApp Flow Configuration](#whatsapp-flow-configuration)
6. [Testing](#testing)
7. [Deployment](#deployment)
8. [Troubleshooting](#troubleshooting)

## Overview

This is a complete WhatsApp Flow implementation for a doctor appointment booking system. The system allows users to:

- Select a doctor from a dropdown list
- Choose a location where the doctor practices (dynamically loaded)
- Pick an available date (dynamically loaded based on doctor's schedule)
- Select a time slot (dynamically loaded showing only available slots)
- Review and confirm the appointment
- Receive a booking confirmation with appointment ID

### Features

✅ **Dynamic Dropdown Loading** - Each dropdown is populated based on previous selections
✅ **Real-time Availability** - Shows only available time slots
✅ **Multi-location Support** - Doctors can practice at multiple locations
✅ **Flexible Scheduling** - Supports custom schedules and unavailable dates
✅ **Secure Communication** - End-to-end encryption with WhatsApp
✅ **Production Ready** - Proper error handling, logging, and validation

## Prerequisites

### Required Software

- **Java 17** or higher
- **Maven 3.6+**
- **MongoDB 4.4+** (running locally or cloud instance)
- **ngrok** (for local testing with WhatsApp webhook)
- **WhatsApp Business Account** with access to Meta Business Manager

### Required Accounts

1. **Meta Business Manager Account**
   - Create at https://business.facebook.com
   - Must be admin of a WhatsApp Business Account

2. **WhatsApp Business API Access**
   - Applied through Meta Business Manager
   - Access to Flow Builder

## Project Structure

```
divisha-whatsapp-bot/
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/divisha/
│   │       │       ├── flow/
│   │       │       │   ├── config/
│   │       │       │   │   └── SampleDataInitializer.java
│   │       │       │   ├── controller/
│   │       │       │   │   └── FlowDataExchangeController.java
│   │       │       │   ├── dto/
│   │       │       │   │   ├── AppointmentConfirmationDTO.java
│   │       │       │   │   ├── DropdownOption.java
│   │       │       │   │   ├── FlowDataExchangeRequest.java
│   │       │       │   │   └── FlowDataExchangeResponse.java
│   │       │       │   ├── model/
│   │       │       │   │   ├── Appointment.java (uses existing)
│   │       │       │   │   ├── Doctor.java (uses existing)
│   │       │       │   │   ├── DoctorSchedule.java (uses existing)
│   │       │       │   │   └── Location.java (uses existing)
│   │       │       │   └── service/
│   │       │       │       └── AppointmentFlowService.java
│   │       │       ├── controller/
│   │       │       │   └── FlowWebhookController.java (existing)
│   │       │       ├── service/
│   │       │       │   └── FlowService.java (enhanced)
│   │       │       └── repository/
│   │       │           ├── AppointmentRepository.java (enhanced)
│   │       │           ├── DoctorRepository.java (existing)
│   │       │           ├── DoctorScheduleRepository.java (enhanced)
│   │       │           └── LocationRepository.java (existing)
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
├── appointment-flow.json
└── docs/
    ├── FLOW_API_DOCUMENTATION.md
    └── WHATSAPP_FLOW_SETUP_GUIDE.md (this file)
```

## Backend Setup

### Step 1: Clone and Configure

1. **Navigate to the project:**
   ```bash
   cd /Users/pankajthakur/IdeaProjects/divisha-whatsapp-bot
   ```

2. **Verify MongoDB is running:**
   ```bash
   # For local MongoDB
   mongosh --eval "db.version()"

   # Should output MongoDB version
   ```

3. **Configure application.properties:**

   Update `backend/src/main/resources/application.properties`:

   ```properties
   # Server Configuration
   server.port=8080

   # MongoDB Configuration
   spring.data.mongodb.uri=mongodb://localhost:27017/divisha-whatsapp

   # Or for MongoDB Atlas:
   # spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/divisha-whatsapp

   # Meta/WhatsApp Configuration
   meta.flow.private.key.path=/path/to/your/private-key.pem
   meta.api.webhook.token=YOUR_WEBHOOK_VERIFY_TOKEN

   # Logging
   logging.level.com.divisha.flow=DEBUG
   logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n
   ```

### Step 2: Generate Private Key

The private key is required for encrypting/decrypting WhatsApp Flow data.

1. **Generate via Meta Business Manager:**
   - Go to https://business.facebook.com
   - Navigate to WhatsApp > API Setup
   - Create a new Flow
   - In Flow Settings, download the private key
   - Save as `private-key.pem`

2. **Update configuration:**
   ```properties
   meta.flow.private.key.path=/absolute/path/to/private-key.pem
   ```

### Step 3: Build and Run

1. **Build the project:**
   ```bash
   cd backend
   mvn clean install -DskipTests
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Verify startup:**

   You should see logs indicating:
   ```
   🔧 Initializing sample data for WhatsApp Flow appointment system...
   ✓ Created 3 locations
   ✓ Created 4 doctors
   ✓ Created 7 doctor schedules
   ✅ Sample data initialization complete!
   ```

4. **Test health endpoint:**
   ```bash
   curl http://localhost:8080/meta/flow/data-exchange/health
   ```

   Expected response:
   ```json
   {
     "status": "healthy",
     "service": "flow-data-exchange"
   }
   ```

### Step 4: Expose Local Server (for Testing)

WhatsApp needs to reach your local server. Use ngrok:

1. **Install ngrok:**
   ```bash
   # macOS
   brew install ngrok

   # Or download from https://ngrok.com
   ```

2. **Start ngrok:**
   ```bash
   ngrok http 8080
   ```

3. **Note the HTTPS URL:**
   ```
   Forwarding: https://abc123.ngrok.io -> http://localhost:8080
   ```

4. **Your webhook URLs will be:**
   - Data Exchange: `https://abc123.ngrok.io/meta/flow/data-exchange`
   - Flow Completion: `https://abc123.ngrok.io/meta/flow/webhook`

## WhatsApp Flow Configuration

### Step 1: Access Flow Builder

1. Go to https://business.facebook.com
2. Select your Business Account
3. Navigate to **WhatsApp > Flows**
4. Click **Create Flow**

### Step 2: Upload Flow JSON

1. In Flow Builder, click **⋯** (three dots) → **Open JSON Editor**
2. Copy the contents of `appointment-flow.json`
3. Paste into the JSON editor
4. Click **Save**

### Step 3: Configure Endpoints

1. **Click on Flow Settings (gear icon)**

2. **Set Data Exchange Endpoint:**
   ```
   https://your-ngrok-url.ngrok.io/meta/flow/data-exchange
   ```

3. **Set Completion Webhook:**
   ```
   https://your-ngrok-url.ngrok.io/meta/flow/webhook
   ```

4. **Download and Configure Private Key:**
   - Download the private key from Flow settings
   - Place it in your project directory
   - Update `application.properties` with the correct path

### Step 4: Configure Webhook Verification

1. **In Meta Business Manager:**
   - Go to WhatsApp > Configuration
   - Find Webhooks section
   - Click **Configure**

2. **Add Webhook URL:**
   ```
   https://your-ngrok-url.ngrok.io/meta/flow/webhook
   ```

3. **Verify Token:**
   - Use the token from your `application.properties`
   - This is the value of `meta.api.webhook.token`

4. **Subscribe to Events:**
   - Select `messages`
   - Click **Subscribe**

### Step 5: Test in Flow Builder

1. **Click Preview button** in Flow Builder

2. **Test the flow:**
   - Select a doctor → Locations should load
   - Select a location → Dates should load
   - Select a date → Time slots should load
   - Complete the booking

3. **Check your logs:**
   ```bash
   # In your terminal running Spring Boot
   📊 Processing data_exchange for screen: APPOINTMENT_SELECTION
   Found 4 active doctors
   Found 2 locations for doctor: DOC001
   ```

### Step 6: Publish Flow

1. **Click Publish** in Flow Builder
2. **Get Flow ID** - you'll need this to send the flow to users
3. **Flow is now live!**

## Testing

### Test Data Exchange Endpoint

The data exchange endpoint requires encrypted requests from WhatsApp. Test using:

1. **Flow Builder Preview** (Recommended)
   - Most accurate test environment
   - Uses real encryption
   - Tests actual user experience

2. **Manual Testing** (Advanced)
   - See `FLOW_API_DOCUMENTATION.md` for encryption details
   - Requires implementing encryption logic
   - Useful for automated testing

### Test Flow Completion

1. **Complete a booking in Flow Builder**
2. **Check application logs:**
   ```
   📅 Processing appointment booking flow for 919876543210
   ✅ Appointment created with ID: APT-1234567890-123
   ```

3. **Verify in MongoDB:**
   ```bash
   mongosh divisha-whatsapp
   db.appointments.find().pretty()
   ```

### Test Sample Data

Verify sample data was created:

```bash
# Check doctors
curl http://localhost:8080/api/doctors

# Check locations
curl http://localhost:8080/api/locations

# Or in MongoDB:
mongosh divisha-whatsapp
db.doctors.find()
db.locations.find()
db.doctor_schedule.find()
```

## Deployment

### Production Deployment

1. **Update application.properties:**
   ```properties
   # Use production profile
   spring.profiles.active=prod

   # Production MongoDB
   spring.data.mongodb.uri=mongodb+srv://user:pass@cluster.mongodb.net/divisha-prod

   # Production webhook URL
   server.url=https://your-production-domain.com
   ```

2. **Disable sample data:**
   - Sample data initializer uses `@Profile("!prod")`
   - Will not run in production

3. **Build production JAR:**
   ```bash
   mvn clean package -DskipTests
   ```

4. **Deploy to your server:**
   ```bash
   # Copy JAR to server
   scp target/divisha-whatsapp-bot-0.0.1-SNAPSHOT.jar user@server:/app/

   # Run on server
   java -jar divisha-whatsapp-bot-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```

### Environment Variables

For production, use environment variables:

```bash
export SPRING_DATA_MONGODB_URI="mongodb+srv://..."
export META_FLOW_PRIVATE_KEY_PATH="/secure/path/private-key.pem"
export META_API_WEBHOOK_TOKEN="your-secure-token"
```

### Docker Deployment (Optional)

Create `Dockerfile`:

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/divisha-whatsapp-bot-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:

```bash
docker build -t divisha-whatsapp-bot .
docker run -p 8080:8080 \
  -e SPRING_DATA_MONGODB_URI="..." \
  -e META_FLOW_PRIVATE_KEY_PATH="/app/private-key.pem" \
  divisha-whatsapp-bot
```

## Troubleshooting

### Common Issues

#### 1. "Decryption error" in logs

**Cause:** Private key mismatch or incorrect format

**Solution:**
- Ensure private key is from the same Flow in Meta Business Manager
- Verify key file has correct permissions (`chmod 600 private-key.pem`)
- Check that key path in `application.properties` is absolute

#### 2. "No schedules found for doctor"

**Cause:** Sample data not loaded or database connection issue

**Solution:**
```bash
# Check if MongoDB is running
mongosh divisha-whatsapp

# Verify data exists
db.doctors.count()
db.doctor_schedule.count()

# If zero, restart application to trigger sample data init
```

#### 3. Empty dropdowns in Flow

**Cause:** Data exchange endpoint not being called or returning errors

**Solution:**
- Check ngrok is running and URL is correct in Flow settings
- Verify logs show data_exchange requests: `📊 Processing data_exchange`
- Test data exchange health endpoint
- Check that response is being encrypted properly

#### 4. "Webhook verification failed"

**Cause:** Webhook token mismatch

**Solution:**
```properties
# Ensure token in application.properties matches Meta configuration
meta.api.webhook.token=EXACT_SAME_TOKEN_IN_META
```

#### 5. Flow not loading doctor names in confirmation

**Cause:** Data mapping issue in Flow JSON

**Solution:**
- The confirmation screen receives `doctor` (ID) but needs `doctor_name`
- This mapping should happen in the Flow JSON or backend
- Current implementation: IDs are sent, names should be resolved in backend

### Debug Mode

Enable detailed logging:

```properties
# application.properties
logging.level.com.divisha.flow=TRACE
logging.level.org.springframework.data.mongodb=DEBUG
logging.level.com.divisha.configuration.MetaV3Crypto=TRACE
```

### Verify Encryption

Test encryption/decryption separately:

```java
// In a test class
@Test
void testEncryption() throws Exception {
    String original = "{\"test\":\"data\"}";
    byte[] aesKey = new byte[32];
    byte[] iv = new byte[16];
    new SecureRandom().nextBytes(aesKey);
    new SecureRandom().nextBytes(iv);

    String encrypted = MetaV3Crypto.encryptResponsePayload(aesKey, original, iv);
    String decrypted = MetaV3Crypto.decryptRequestPayload(aesKey, encrypted, iv);

    assertEquals(original, decrypted);
}
```

## Additional Resources

- **WhatsApp Flow Documentation:** https://developers.facebook.com/docs/whatsapp/flows
- **Meta Business Manager:** https://business.facebook.com
- **MongoDB Documentation:** https://docs.mongodb.com
- **Spring Boot Documentation:** https://spring.io/projects/spring-boot

## Support

For issues specific to this implementation:

1. Check application logs for detailed error messages
2. Review `FLOW_API_DOCUMENTATION.md` for API details
3. Verify WhatsApp Flow JSON matches expected format
4. Test endpoints individually using curl/Postman

## Next Steps

After successful setup:

1. **Customize the UI:**
   - Modify `appointment-flow.json` to match your branding
   - Update text, colors, and layout

2. **Add Features:**
   - Email notifications
   - SMS reminders
   - Calendar integration
   - Payment integration

3. **Enhance Security:**
   - Implement rate limiting
   - Add request validation
   - Set up monitoring and alerts

4. **Scale:**
   - Add Redis caching for frequently accessed data
   - Implement connection pooling
   - Set up load balancing

---

**Congratulations!** 🎉 You now have a fully functional WhatsApp Flow appointment booking system.
