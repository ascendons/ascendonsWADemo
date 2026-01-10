package com.divisha.controller;

import com.divisha.configuration.MetaV3Crypto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flows")
@RequiredArgsConstructor
public class HealthController {

  private final ObjectMapper mapper;

  // Load your RSA PRIVATE KEY (PKCS8 format)
  private PrivateKey loadPrivateKey() throws Exception {
    String privateKeyPem =
        """
                    -----BEGIN PRIVATE KEY-----
                    MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC3RfpGvfEyCqMB
                    UjwBiUTnj4wZgqJulVRfQcFY8HZOAJJqQb3HMSNufDSddy8TDrAqjf+AywGij5O2
                    ApMfbGnTvIwCkHlHgp0/Xxo1oM74S+VjbODb8QKhXQH35gVdvAkOwJimd1vlCXKb
                    Kf9aCaLZHdsEwCY369ycrhV3q8ulZ0aR1Cz2GoIegeuwaaIzj8AII+kQ0Y64wOCG
                    JvA5hahdBSGTAl5PvwIhg16Z0QOkXlqeqP7nbDIL0jZQoNg2J0A3+fAAvpapTA7I
                    mQrERSZ9H3wffKADG2pMDdyhmmKpz8Dr0KH2PA0VujYDUNwyDeOz7UIsiUpcpAoF
                    9paqRSspAgMBAAECggEAAfr0ix8Mj7n/bHIYQUFEaGEI7C9HfTcJw2TSuQ1Ad9DY
                    Cfs/ajVaq7NbmUZW6BpmY+dSnNkmJ2tJpYgMlzAyBLiKsXmsQLbIcwG1vkolZUY/
                    8FHzcgt+IKWXrCulDu0W+2tqg5Dd9qVzzj9+VSrz8RoAKlbWz/hUUw38fR+V9CP4
                    FMtDvQtrKoYSfzJv9NMNal6Dz5u9KEWywTUOKVNMpU4vn7igbsfRqdyf21KXEikO
                    68pAX8IslDscKnCRI5rOeU+O8G/R26tzrxpvRSrKlFfy2h+wNwdjQ8aJkuTWCyfv
                    tms2yLCT/9ZA9p93PzDnIT/RbJhhw6yLrhH3uyAAQQKBgQDd1PCoX4lUeBXweoXb
                    qNujwD6SQefF5k7TpHEEgXHgemzFMSbkZjPZadAv9n3dG92w7UJyFheWd9eABCa1
                    nRnjMYADs1vDASL2KkcaSv2rWh2EQ2/lu5JgpdVkjytfh5JBFMKLRMrvs9+Rei0p
                    L9wHfNvnMFpiz9Uo4NUuyxU6cQKBgQDTgKM9Ob/il2YKsOjt3lMjcNqkaBxVP8Xl
                    Hdsnm+CFsMRs2SGrQmaqod9KMqJkn22nbktLj6Y4aP1YhIJwGLwotiSlsbG6H/Bo
                    dbjaDlkStaPa4Sl6FO+NPxjGsWI76RRJAh4zhHeNum6B+URITkWYMh/A0KRMgbxU
                    sEJkRcGoOQKBgQCl6TV1OL8FpisCiSDEgS6E8qvZx+EJao9aS4sby5TYR7hCY9iq
                    yqXM3g8PUFQio0zTnyArI8rQhyFuZaxt+On7unH8UpE28AquAkDbsWq4VdXtrmJq
                    eDeqgV1wsIPIFyWT92rprrH5RZbYv4A3Zcwy7XerGccAUGElCcNoFAv0gQKBgEs9
                    TdTwAFUJauGOwa4tEwJemUk3SC0DfNDe6CGVEDA/DAF01Mdp3cAByb9Jd/+3v3h6
                    ggqlVYpnvwiISya5heYttafEKOd/iTR//HyF8iX3vAyXYPvvCrECqzuzyrBpYDAA
                    suG81BL1KW7PT/0w6w0Td7xN/MTCuk6eTWACeLvxAoGBAJKeE27lGpVVHvYgvXli
                    aCuuS0VP3B28i64yqwkmt/UGDunAP1u0Xg1kH6J9auk1tO04AyLDYSTHqRnT8W4N
                    AUkjQv4bZBNvSVLH8Hw/uGPj121cytjk0VXPMxNd3SFgWZRrcFD9oCsJMJMkVtuE
                    S3QR4TQbaJMzKmomsZNBFt2D
                    -----END PRIVATE KEY-----
                        """
            .replaceAll("-----\\w+ PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

    byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
  }

  @PostMapping(value = "/health", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> handleMeta(@RequestBody Map<String, Object> request)
      throws Exception {

    System.out.println("Incoming Request: " + mapper.writeValueAsString(request));

    String encryptedAesKey = (String) request.get("encrypted_aes_key");
    String encryptedFlowData = (String) request.get("encrypted_flow_data");
    String requestIvBase64 = (String) request.get("initial_vector");

    byte[] requestIv = Base64.getDecoder().decode(requestIvBase64);

    // 1. Load Private RSA Key
    PrivateKey privateKey = loadPrivateKey();

    // 2. Decrypt AES Payload Key
    byte[] aesKey = MetaV3Crypto.rsaDecryptAesKey(encryptedAesKey, privateKey);

    // 3. Decrypt request payload JSON (optional for health)
    String requestJson = MetaV3Crypto.decryptRequestPayload(aesKey, encryptedFlowData, requestIv);
    System.out.println("Decrypted Request JSON: " + requestJson);

    // 4. Your response JSON
    String responseJson = "{\"status\":\"active\"}";

    // 5. Encrypt response JSON using AES-GCM with inverted IV
    String encryptedResponseBase64 =
        MetaV3Crypto.encryptResponsePayload(aesKey, responseJson, requestIv);

    System.out.println("Returning encrypted response: " + encryptedResponseBase64);

    // 6. MUST return as plain text
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(encryptedResponseBase64);
  }
}
