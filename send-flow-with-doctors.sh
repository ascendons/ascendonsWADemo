#!/bin/bash

# Send WhatsApp Flow with Dynamic Doctor Data
# This script sends the appointment booking flow with a list of available doctors

# Meta API Configuration
API_URL="https://graph.facebook.com/v22.0"
PHONE_NUMBER_ID="956231507564148"
ACCESS_TOKEN="EAAQWD70jeScBQInMck1nR5u4RZCBQw8Wllpr3BslnmclVqQEjlaROQZC4RLg7SVqkcwknRWJMt0KZCpHCQxZA4h9fAS5D6R74mZA9jMFtjhdbcGLZBdrmmq3xkPFIDoIq4Gn9GCY9NdJWZA5QZA6QNwrCgklIMUwcNVGBJkHr3SDuwl5QnE7OZAQ9p4Xilo64JgZDZD"

# Message Configuration
RECIPIENT="${1:-919771186585}"  # Default or pass as argument
FLOW_ID="863931629459268"

# Generate flow token
TIMESTAMP=$(date +%s)
FLOW_TOKEN="FLOW_${RECIPIENT}_${TIMESTAMP}"

echo "📤 Sending Doctor Appointment Flow to $RECIPIENT..."
echo "🏥 Flow ID: $FLOW_ID"

# Doctor data from your database
# DOC001: Dr. Sarah Johnson - Cardiologist
# DOC002: Dr. Michael Chen - Pediatrician
# DOC003: Dr. Emily Rodriguez - Dermatologist
# DOC004: Dr. James Williams - Orthopedic Surgeon

# Send Flow Message with Initial Doctor Data
curl -X POST "${API_URL}/${PHONE_NUMBER_ID}/messages" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "messaging_product": "whatsapp",
    "recipient_type": "individual",
    "to": "'"${RECIPIENT}"'",
    "type": "interactive",
    "interactive": {
      "type": "flow",
      "header": {
        "type": "text",
        "text": "Book Your Appointment"
      },
      "body": {
        "text": "Welcome to Divisha Healthcare! Schedule your doctor appointment easily. We have 4 specialists available."
      },
      "footer": {
        "text": "Your health is our priority"
      },
      "action": {
        "name": "flow",
        "parameters": {
          "flow_message_version": "3",
          "flow_token": "'"${FLOW_TOKEN}"'",
          "flow_id": "'"${FLOW_ID}"'",
          "flow_cta": "Book Now",
          "flow_action": "navigate",
          "flow_action_payload": {
            "screen": "APPOINTMENT",
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
                },
                {
                  "id": "DOC003",
                  "title": "Dr. Emily Rodriguez",
                  "description": "Dermatologist"
                },
                {
                  "id": "DOC004",
                  "title": "Dr. James Williams",
                  "description": "Orthopedic Surgeon"
                }
              ],
              "locations": [],
              "dates": [],
              "time_slots": [],
              "doctor": "",
              "location": "",
              "date": "",
              "time_slot": ""
            }
          }
        }
      }
    }
  }' | jq '.'

echo ""
echo "✅ Flow message sent successfully!"
echo ""
echo "📋 Summary:"
echo "   - Recipient: $RECIPIENT"
echo "   - Doctors: 4 specialists"
echo "   - Flow ID: $FLOW_ID"
echo "   - Screen: APPOINTMENT"
echo ""
echo "💡 The flow will dynamically load locations, dates, and time slots"
echo "   based on the doctor selected by the user."
