#!/bin/bash

# WhatsApp Flow Message Sender
# Flow: Doctor Appointment
# Flow ID: 863931629459268
# Recipient: 919771186585

# Meta API Configuration
API_URL="https://graph.facebook.com/v22.0"
PHONE_NUMBER_ID="956231507564148"
ACCESS_TOKEN="EAAQWD70jeScBQInMck1nR5u4RZCBQw8Wllpr3BslnmclVqQEjlaROQZC4RLg7SVqkcwknRWJMt0KZCpHCQxZA4h9fAS5D6R74mZA9jMFtjhdbcGLZBdrmmq3xkPFIDoIq4Gn9GCY9NdJWZA5QZA6QNwrCgklIMUwcNVGBJkHr3SDuwl5QnE7OZAQ9p4Xilo64JgZDZD"

# Message Configuration
RECIPIENT="919771186585"
FLOW_ID="863931629459268"

# Generate flow token
TIMESTAMP=$(date +%s)
FLOW_TOKEN="FLOW_${RECIPIENT}_${TIMESTAMP}"

# Send Flow Message
echo "Sending Doctor Appointment Flow to $RECIPIENT..."

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
        "text": "Schedule your doctor appointment using our booking system. Click the button below to get started."
      },
      "footer": {
        "text": "Powered by Divisha Healthcare"
      },
      "action": {
        "name": "flow",
        "parameters": {
          "flow_message_version": "3",
          "flow_token": "'"${FLOW_TOKEN}"'",
          "flow_id": "'"${FLOW_ID}"'",
          "flow_cta": "Book Appointment",
          "flow_action": "navigate",
          "flow_action_payload": {
            "screen": "APPOINTMENT"
          }
        }
      }
    }
  }'

echo ""
echo "Flow message sent successfully!"
