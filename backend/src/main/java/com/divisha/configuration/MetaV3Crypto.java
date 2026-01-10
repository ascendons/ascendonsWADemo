package com.divisha.configuration;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

public class MetaV3Crypto {

  private static final int GCM_TAG_LENGTH_BITS = 128; // 16 bytes

  /** Decrypt AES key using RSA-OAEP SHA-256 (Meta spec). */
  public static byte[] rsaDecryptAesKey(String encryptedAesKeyBase64, PrivateKey privateKey)
      throws Exception {

    try {
      byte[] encryptedKey = Base64.getDecoder().decode(encryptedAesKeyBase64);

      Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

      OAEPParameterSpec oaepParams =
          new OAEPParameterSpec(
              "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

      cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);

      byte[] decryptedKey = cipher.doFinal(encryptedKey);

      if (decryptedKey.length > 16) {
        return Arrays.copyOf(decryptedKey, 16);
      }

      return decryptedKey;

    } catch (BadPaddingException e) {
      throw new Exception(
          "RSA decryption failed (BadPadding). Possible causes:\n"
              + "1. Private key does NOT match uploaded Meta public key\n"
              + "2. Flow is in DRAFT mode (Meta test keys)\n"
              + "3. Wrong private key format or file\n"
              + "Error: "
              + e.getMessage(),
          e);
    }
  }

  /** AES-GCM decrypt for encrypted_flow_data. */
  public static String decryptRequestPayload(
      byte[] aesKey, String encryptedFlowDataBase64, byte[] requestIv) throws Exception {

    byte[] cipherWithTag = Base64.getDecoder().decode(encryptedFlowDataBase64);

    SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
    GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, requestIv);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

    byte[] plain = cipher.doFinal(cipherWithTag);
    return new String(plain, StandardCharsets.UTF_8);
  }

  /** Encrypt response JSON using AES-GCM and inverted IV. */
  public static String encryptResponsePayload(byte[] aesKey, String responseJson, byte[] requestIv)
      throws Exception {

    byte[] plainBytes = responseJson.getBytes(StandardCharsets.UTF_8);
    byte[] responseIv = invert(requestIv);

    SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
    GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, responseIv);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

    byte[] cipherWithTag = cipher.doFinal(plainBytes);
    return Base64.getEncoder().encodeToString(cipherWithTag);
  }

  private static byte[] invert(byte[] in) {
    byte[] out = new byte[in.length];
    for (int i = 0; i < in.length; i++) out[i] = (byte) (~in[i]);
    return out;
  }
}
