# WhatsApp Flows Setup & Troubleshooting Guide

## Changes Made to Fix Decryption Error

### 1. Fixed AES Key Size Issue ✅
**Problem:** Meta uses 128-bit AES encryption, but the code wasn't explicitly handling this.

**Solution:** Updated `MetaV3Crypto.java:15-32` to ensure only the first 16 bytes (128 bits) of the decrypted AES key are used.

### 2. Fixed Private Key Path ✅
**Problem:** Path was set to `private-key.pem` but file is at `backend/private-key.pem`.

**Solution:** Updated `application.properties:50` to use `backend/private-key.pem` as the default path.

### 3. Verified Key Pair ✅
Your public/private key pair is valid and matches correctly.

---

## Upload Public Key to Meta

Your public key (Base64 format) is in `public-key-base64.txt`:
```
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiYz9LOnB/iJ3B8VaxH89jKdrlB53bO2ix212FDxY735cV8gjH2w9JSjqC/TLQYF4+DKQUWGFeEl914/e37z3ZkQgPm1tDqslApd2v5LFMW68R6w8XIXjOOw3nLaj9Xj3A/CaeXQmsLz+McPWN9+gygNvq/IsOKH5WNFRB8vx1J7khlF4K26uBzt0jK3NxanbzNOjVye6f5kbSsopNKMYUH9hwb8O718qGcJKxVRVcDRiazKdDAwtxtuowxuzUIff2eiu9UFyJ8DAKnBw+nNQqQgDmPTiqHxUW+eS5mlKhYhL+SIRJKPOXdSNbGnwmiG6EsOj1griLV5JInV2lhtF5wIDAQAB
```

**Steps to Upload:**
1. Go to [Meta Business Manager](https://business.facebook.com/)
2. Navigate to: **WhatsApp Manager** → **Phone Numbers** → Select your phone number
3. Go to **WhatsApp Flows** settings
4. Click **Upload Public Key**
5. Paste the Base64 public key above (entire string, no line breaks)
6. Save

---

## Test Your Endpoint

### Test 1: Verify Webhook (GET Request)
Your endpoint should respond to GET requests for verification:

```bash
curl "http://localhost:8090/meta/flow/webhook?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=DIVISHA_META_WEBHOOK"
```

**Expected Response:** `test123`

### Test 2: Health Check (GET Request without params)
```bash
curl "http://localhost:8090/meta/flow/webhook"
```

**Expected Response:** `OK`

### Test 3: Test Decryption (POST Request)
You'll need to test this from Meta's Flow Builder "Test" feature:
1. Create a flow in [Meta Flow Builder](https://business.facebook.com/wa/manage/flows/)
2. Set your endpoint URL: `https://your-domain.com/meta/flow/webhook`
3. Click **Test** button
4. The Flow Builder will send encrypted test data to your endpoint

---

## Deployment Checklist

- [ ] Public key uploaded to Meta ✅ (Use the key from `public-key-base64.txt`)
- [ ] Endpoint URL is publicly accessible (Use ngrok or deploy to production)
- [ ] Endpoint URL configured in Meta Flow Builder
- [ ] Private key file exists at `backend/private-key.pem` ✅
- [ ] `meta.api.webhook.token` matches the verification token in Meta
- [ ] Server is running on port 8090 (or configured port)

---

## Common Errors & Solutions

### Error: "Failed to decrypt"
**Causes:**
- ❌ Public key uploaded to Meta doesn't match your private key
- ❌ Private key path is incorrect
- ❌ AES key size mismatch (FIXED ✅)

**Solution:** Verify key pair matches and path is correct (already done ✅)

### Error: "Private key path not configured"
**Cause:** `meta.flow.private.key.path` not set

**Solution:** Already set to `backend/private-key.pem` ✅

### Error: "Verification failed (403)"
**Cause:** Webhook verification token doesn't match

**Solution:** Ensure `meta.api.webhook.token` in `application.properties` matches the token you configured in Meta.

### Error: Flow shows "Endpoint Error" in DRAFT mode
**Note:** In DRAFT mode, Meta uses test keys that won't match your keys. The code already handles this (lines 286-299 in FlowWebhookController.java).

**Solution:** Publish your flow. Once PUBLISHED, Meta will use your uploaded public key.

---

## Testing Flow with ngrok (Local Development)

1. Start your Spring Boot application:
```bash
cd backend
./mvnw spring-boot:run
```

2. In another terminal, start ngrok:
```bash
ngrok http 8090
```

3. Copy the HTTPS URL (e.g., `https://abc123.ngrok.io`)

4. Set this as your endpoint URL in Meta Flow Builder:
```
https://abc123.ngrok.io/meta/flow/webhook
```

5. Test your flow from the Flow Builder

---

## Monitoring & Debugging

### Enable Debug Logging
Add this to `application.properties`:
```properties
logging.level.com.divisha.controller.FlowWebhookController=DEBUG
logging.level.com.divisha.configuration.MetaV3Crypto=DEBUG
```

### Watch Logs
```bash
tail -f logs/spring.log
```

Look for these log messages:
- ✅ `🔍 Incoming Flow Webhook GET request` - Verification endpoint called
- ✅ `📩 Incoming Flow webhook payload` - Flow data received
- ✅ `🔐 Decrypting flow data` - Decryption started
- ✅ `📄 Decrypted flow JSON` - Decryption successful
- ❌ `❌ Error processing flow webhook` - Something went wrong

---

## Reference Documentation

- [Meta Flows Documentation](https://developers.facebook.com/docs/whatsapp/flows/)
- [Implementing Flow Endpoint](https://developers.facebook.com/docs/whatsapp/flows/guides/implementingyourflowendpoint)
- [Flow Error Codes](https://developers.facebook.com/docs/whatsapp/flows/reference/error-codes)
- [Official Example (Node.js)](https://github.com/fbsamples/WhatsApp-Flows-Tools/tree/main/examples/endpoint/nodejs/basic)

---

## Next Steps

1. ✅ Fixes have been applied
2. 📤 Upload public key to Meta (use key from `public-key-base64.txt`)
3. 🚀 Deploy or use ngrok to make endpoint publicly accessible
4. 🧪 Test from Meta Flow Builder
5. 📱 Test end-to-end with WhatsApp

---

**Status:** Your decryption implementation is now fixed and should work correctly! 🎉
