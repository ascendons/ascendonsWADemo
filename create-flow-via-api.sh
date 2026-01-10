#!/bin/bash

# WhatsApp Flow Creation Script
# This script creates a Flow using Meta's Graph API

# ======================================
# CONFIGURATION - UPDATE THESE VALUES
# ======================================

# Your Meta Access Token (get from Meta Business Manager)
ACCESS_TOKEN="YOUR_ACCESS_TOKEN_HERE"

# Your WhatsApp Business Account ID
WABA_ID="YOUR_WABA_ID_HERE"

# Your backend endpoint URL
ENDPOINT_URL="https://your-ngrok-url.ngrok.io/meta/flow/data-exchange"

# ======================================
# CREATE FLOW
# ======================================

echo "🚀 Creating WhatsApp Flow..."

# Read the Flow JSON
FLOW_JSON=$(cat appointment-flow-minimal.json | jq -c '.')

# Create Flow via API
RESPONSE=$(curl -s -X POST \
  "https://graph.facebook.com/v18.0/${WABA_ID}/flows" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Doctor Appointment Booking\",
    \"categories\": [\"APPOINTMENT_BOOKING\"],
    \"endpoint_uri\": \"${ENDPOINT_URL}\",
    \"json_version\": \"7.2\"
  }")

echo "📋 Response: $RESPONSE"

# Extract Flow ID
FLOW_ID=$(echo $RESPONSE | jq -r '.id')

if [ "$FLOW_ID" != "null" ] && [ -n "$FLOW_ID" ]; then
  echo "✅ Flow created successfully!"
  echo "📝 Flow ID: $FLOW_ID"

  echo ""
  echo "🔧 Now publishing Flow..."

  # Publish the Flow
  PUBLISH_RESPONSE=$(curl -s -X POST \
    "https://graph.facebook.com/v18.0/${FLOW_ID}/publish" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")

  echo "📋 Publish Response: $PUBLISH_RESPONSE"

  echo ""
  echo "🎉 Flow setup complete!"
  echo "📝 Flow ID: $FLOW_ID"
  echo ""
  echo "📌 Next steps:"
  echo "1. Download private key from Meta Business Manager"
  echo "2. Configure private key path in application.properties"
  echo "3. Send Flow to users using Flow ID: $FLOW_ID"

else
  echo "❌ Failed to create Flow"
  echo "Error: $RESPONSE"
  echo ""
  echo "Common issues:"
  echo "- Invalid ACCESS_TOKEN"
  echo "- Invalid WABA_ID"
  echo "- Insufficient permissions"
  echo ""
  echo "To get your tokens:"
  echo "1. Access Token: Meta Business Settings → System Users → Generate Token"
  echo "2. WABA ID: Meta Business Settings → WhatsApp Accounts"
fi
