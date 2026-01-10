# API Testing Examples

This document provides curl commands and test examples for the WhatsApp Flow Appointment Booking API.

## Table of Contents

1. [Health Check](#health-check)
2. [Testing with Flow Builder](#testing-with-flow-builder)
3. [Direct MongoDB Queries](#direct-mongodb-queries)
4. [Sample Test Scenarios](#sample-test-scenarios)

## Health Check

### Test Data Exchange Health Endpoint

```bash
curl -X GET http://localhost:8080/meta/flow/data-exchange/health
```

**Expected Response:**
```json
{
  "status": "healthy",
  "service": "flow-data-exchange"
}
```

## Testing with Flow Builder

The recommended way to test the data_exchange endpoint is through Meta's Flow Builder, as it handles encryption automatically.

### Setup for Testing

1. **Start your local server:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Start ngrok:**
   ```bash
   ngrok http 8080
   ```

3. **Configure Flow Builder:**
   - Open Flow in Meta Business Manager
   - Go to Settings
   - Set Data Exchange URL: `https://your-ngrok-url.ngrok.io/meta/flow/data-exchange`

4. **Test in Preview Mode:**
   - Click Preview in Flow Builder
   - Interact with the dropdowns
   - Monitor server logs

### Expected Log Output

When testing, you should see logs like:

```
2025-12-11 10:30:15 INFO  📥 Received data_exchange request
2025-12-11 10:30:15 INFO  📄 Decrypted request: {"version":"3.0","action":"data_exchange","screen":"APPOINTMENT_SELECTION",...}
2025-12-11 10:30:15 INFO  📊 Processing data_exchange for screen: APPOINTMENT_SELECTION, data: {doctor=, location=, date=, time_slot=}
2025-12-11 10:30:15 INFO  Found 4 active doctors
2025-12-11 10:30:15 INFO  📤 Response JSON: {"version":"3.0","screen":"APPOINTMENT_SELECTION","data":{...}}
```

## Direct MongoDB Queries

### Verify Sample Data

```bash
# Connect to MongoDB
mongosh divisha-whatsapp

# Check doctors
db.doctors.find().pretty()

# Expected output:
{
  "_id": "DOC001",
  "name": "Dr. Sarah Johnson",
  "specialization": "Cardiologist",
  "status": "active",
  ...
}

# Check locations
db.locations.find().pretty()

# Check doctor schedules
db.doctor_schedule.find().pretty()

# Check appointments
db.appointments.find().pretty()
```

### Query Specific Data

```bash
# Find all active doctors
db.doctors.find({status: "active"})

# Find schedules for a specific doctor
db.doctor_schedule.find({doctorId: "DOC001"})

# Find appointments for a specific date
db.appointments.find({date: "20251215"})

# Find available time slots (appointments not booked)
db.appointments.find({
  doctorId: "DOC001",
  locationId: "LOC001",
  date: "20251215",
  status: {$ne: "cancelled"}
})
```

## Sample Test Scenarios

### Scenario 1: Complete Booking Flow

**Steps:**

1. **Start Flow**
   - Open Flow in WhatsApp or Flow Builder
   - Flow loads APPOINTMENT_SELECTION screen
   - Doctor dropdown is populated

2. **Select Doctor**
   - User selects "Dr. Sarah Johnson"
   - Triggers data_exchange request
   - Location dropdown populates with doctor's locations

3. **Select Location**
   - User selects "Downtown Medical Center"
   - Triggers data_exchange request
   - Date dropdown populates with next 14 days

4. **Select Date**
   - User selects a future date
   - Triggers data_exchange request
   - Time slot dropdown populates with available slots

5. **Select Time Slot**
   - User selects "09:00 AM"
   - Click Continue
   - Navigate to CONFIRMATION screen

6. **Confirm Booking**
   - Review details
   - Click "Confirm Booking"
   - Flow completes
   - Webhook receives completion data
   - Appointment created in database

**Verify in MongoDB:**

```bash
db.appointments.find().sort({createdTs: -1}).limit(1).pretty()
```

Expected:
```json
{
  "_id": "...",
  "doctorId": "DOC001",
  "locationId": "LOC001",
  "date": "20251215",
  "time": "09:00",
  "phone": "919876543210",
  "status": "confirmed",
  "bookingId": "APT-1234567890-123",
  "createdTs": 1702297200000
}
```

### Scenario 2: Test Unavailable Dates

**Setup:**

```bash
# Update a doctor schedule to mark certain dates as unavailable
mongosh divisha-whatsapp

db.doctor_schedule.updateOne(
  {doctorId: "DOC001", locationId: "LOC001"},
  {$set: {unavailableDates: ["2025-12-25", "2026-01-01"]}}
)
```

**Test:**
- Select doctor and location
- Verify December 25 and January 1 do NOT appear in dates dropdown

### Scenario 3: Test Fully Booked Slots

**Setup:**

```javascript
// Book all slots for a specific date
mongosh divisha-whatsapp

// Create appointments for all slots
const date = "20251220";
const times = ["09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30"];

times.forEach(time => {
  db.appointments.insertOne({
    doctorId: "DOC001",
    locationId: "LOC001",
    date: date,
    time: time,
    phone: "919999999999",
    status: "confirmed",
    bookingId: "TEST-" + Date.now(),
    createdTs: Date.now()
  });
});
```

**Test:**
- Select doctor, location, and December 20
- Verify time slot dropdown is empty or shows "No available slots"

### Scenario 4: Test Weekend Unavailability

**Default Configuration:**
- Most doctors have SATURDAY and SUNDAY in `unavailableDaysOfWeek`

**Test:**
- Select any doctor
- Select a location
- Verify weekend dates do not appear in dates dropdown

### Scenario 5: Test Multiple Locations

**Test Dr. Sarah Johnson (practices at 2 locations):**

1. Select "Dr. Sarah Johnson"
2. Verify locations dropdown shows:
   - Downtown Medical Center
   - Westside Hospital

3. Select "Downtown Medical Center"
4. Verify dates show Monday-Friday (no weekends)

5. Go back and select "Westside Hospital"
6. Verify dates show Monday, Wednesday, Friday only

### Scenario 6: Test Custom Date Slots

**Setup:**

```javascript
// Add custom slot for Christmas (if doctor works half day)
mongosh divisha-whatsapp

db.doctor_schedule.updateOne(
  {doctorId: "DOC002", locationId: "LOC002"},
  {
    $push: {
      customDateSlots: {
        date: "2025-12-25",
        startTime: "10:00",
        endTime: "14:00",
        slotDurationMinutes: 30
      }
    },
    $pull: {unavailableDates: "2025-12-25"}
  }
)
```

**Test:**
- Select Dr. Michael Chen
- Select Northside Clinic
- Select December 25
- Verify time slots show 10:00 AM to 2:00 PM only

## Testing Error Scenarios

### Error 1: Missing Private Key

**Simulate:**
```bash
# Rename or move private key file
mv /path/to/private-key.pem /path/to/private-key.pem.bak

# Restart application
```

**Expected:**
- Application fails to start or logs error on first request
- Error: "FileNotFoundException: private-key.pem"

**Fix:**
```bash
mv /path/to/private-key.pem.bak /path/to/private-key.pem
```

### Error 2: MongoDB Connection Failure

**Simulate:**
```bash
# Stop MongoDB
brew services stop mongodb-community
# or
sudo systemctl stop mongod
```

**Expected:**
- Application logs connection errors
- Data exchange requests fail
- Error response sent to WhatsApp

**Fix:**
```bash
brew services start mongodb-community
# or
sudo systemctl start mongod
```

### Error 3: Invalid Flow Data

**Simulate:**
Send a request with missing required fields (can only test via Flow Builder or manual encryption)

**Expected:**
- Application logs warning about missing fields
- Returns error message in response
- No appointment created

## Performance Testing

### Load Test Script

```bash
#!/bin/bash
# Simple load test for health endpoint

echo "Running load test..."
for i in {1..100}; do
  curl -s http://localhost:8080/meta/flow/data-exchange/health > /dev/null &
done
wait
echo "Load test complete"
```

### Expected Performance

- Health endpoint: < 50ms
- Data exchange (doctors list): < 200ms
- Data exchange (time slots): < 300ms
- Flow completion: < 500ms

### Monitor with Logs

```bash
# Enable timing logs
tail -f logs/spring.log | grep "Processing time"
```

## Automated Testing

### Unit Test Examples

```java
@SpringBootTest
class AppointmentFlowServiceTest {

    @Autowired
    private AppointmentFlowService service;

    @Test
    void testGetDoctorOptions() {
        List<DropdownOption> doctors = service.getDoctorOptions();
        assertFalse(doctors.isEmpty());
        assertTrue(doctors.size() >= 4); // Sample data has 4 doctors
    }

    @Test
    void testGetLocationOptionsForDoctor() {
        List<DropdownOption> locations = service.getLocationOptionsForDoctor("DOC001");
        assertFalse(locations.isEmpty());
        assertEquals(2, locations.size()); // Dr. Sarah Johnson has 2 locations
    }

    @Test
    void testGetAvailableDates() {
        List<DropdownOption> dates = service.getAvailableDates("DOC001", "LOC001");
        assertFalse(dates.isEmpty());
        // Should have dates for next 14 days (excluding weekends)
        assertTrue(dates.size() >= 10);
    }
}
```

### Integration Test

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FlowDataExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/meta/flow/data-exchange/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("healthy"));
    }
}
```

## Debugging Tips

### Enable Request/Response Logging

```properties
# application.properties
logging.level.org.springframework.web=DEBUG
logging.level.com.divisha.flow.controller=TRACE
```

### Monitor Network Traffic

```bash
# Use tcpdump to monitor requests (requires sudo)
sudo tcpdump -i any -n port 8080 -A
```

### Test Encryption Separately

```java
// Standalone test
public static void main(String[] args) throws Exception {
    String privateKeyPath = "/path/to/private-key.pem";
    String testPayload = "{\"version\":\"3.0\",\"action\":\"data_exchange\"}";

    // Generate test keys
    byte[] aesKey = new byte[32];
    byte[] iv = new byte[16];
    new SecureRandom().nextBytes(aesKey);
    new SecureRandom().nextBytes(iv);

    // Encrypt
    String encrypted = MetaV3Crypto.encryptResponsePayload(aesKey, testPayload, iv);
    System.out.println("Encrypted: " + encrypted);

    // Decrypt
    String decrypted = MetaV3Crypto.decryptRequestPayload(aesKey, encrypted, iv);
    System.out.println("Decrypted: " + decrypted);

    assert testPayload.equals(decrypted);
}
```

## Cleanup Test Data

### Reset Database

```bash
mongosh divisha-whatsapp

# Remove all test appointments
db.appointments.deleteMany({bookingId: {$regex: /^TEST-/}})

# Or reset entire database
db.dropDatabase()

# Restart application to re-initialize sample data
```

### Clear Specific Appointments

```bash
# Remove appointments for testing date
db.appointments.deleteMany({date: "20251220"})
```

## Continuous Integration

### GitHub Actions Example

```yaml
# .github/workflows/test.yml
name: Test WhatsApp Flow API

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      mongodb:
        image: mongo:4.4
        ports:
          - 27017:27017

    steps:
      - uses: actions/checkout@v2

      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
          distribution: 'adopt'

      - name: Build with Maven
        run: cd backend && mvn clean install

      - name: Run tests
        run: cd backend && mvn test

      - name: Check health endpoint
        run: |
          cd backend && mvn spring-boot:run &
          sleep 30
          curl -f http://localhost:8080/meta/flow/data-exchange/health || exit 1
```

## Support

If tests fail:

1. Check application logs for detailed errors
2. Verify MongoDB is running and accessible
3. Ensure sample data was initialized
4. Test individual components (encryption, database, API) separately
5. Compare with working examples in this document

---

**Happy Testing!** 🧪
