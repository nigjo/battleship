/*
 * Copyright 2023 nigjo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.nigjo.battleship.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;

//    KeyManager km = gamedata.get("keymanager.self", KeyManager.class);
//    String encoded = km.encode("Hallo Welt!");
//    System.out.println("encoded="+encoded);
//    String plain = km.decode(encoded);
//    System.out.println("plain="+plain);
/**
 * Verwaltet die Schlüsselpaare fuer das Ver- und Entschlüsseln der Spielstand-Records.
 * Die Sicherheitsanforderungen an diese Schlüssel sind nicht sehr hoch. Es ist ein
 * einfaches Spiel.
 *
 * @author nigjo
 */
public class KeyManager
{
  private PrivateKey own;
  private PublicKey playerKey;
  private static final int KEY_LENGTH = 1024; // enough for this game. No real

  /**
   * Erstellt einen neuen Schlüssel
   */
  public KeyManager(Path privateStore)
  {
    if(Files.exists(privateStore))
    {
      loadFromPrivate(privateStore);
    }
    else
    {
      generateNew(privateStore);
    }
  }

  private void loadFromPrivate(Path privateStore)
  {
    try(InputStream in = new BufferedInputStream(
        new FileInputStream(privateStore.toFile())
    ))
    {
      byte[] size = in.readNBytes(4);
      int keySize = ByteBuffer.wrap(size).getInt();
      byte[] key1data = in.readNBytes(keySize);

      size = in.readNBytes(4);
      keySize = ByteBuffer.wrap(size).getInt();
      byte[] key2data = in.readNBytes(keySize);

      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(key1data);
      own = keyFactory.generatePrivate(privKeySpec);
      EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(key2data);
      playerKey = keyFactory.generatePublic(pubKeySpec);
    }
    catch(IOException | GeneralSecurityException ex)
    {
      Logger.getLogger(KeyManager.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void generateNew(Path privateStore)
  {
    try
    {
      KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
      gen.initialize(KEY_LENGTH);
      KeyPair pair = gen.generateKeyPair();
      playerKey = pair.getPublic();
      own = pair.getPrivate();

      //String playerKey = Base64.getEncoder().encodeToString(encoded);
      try(OutputStream out = new BufferedOutputStream(
          Files.newOutputStream(privateStore)))
      {
        byte[] encoded;

        encoded = own.getEncoded();
        out.write(ByteBuffer.allocate(4).putInt(encoded.length).array());
        out.write(encoded);

        encoded = playerKey.getEncoded();
        out.write(ByteBuffer.allocate(4).putInt(encoded.length).array());
        out.write(encoded);
      }
      catch(IOException ex)
      {
        Logger.getLogger(KeyManager.class.getName()).log(Level.SEVERE, null, ex);
      }

    }
    catch(NoSuchAlgorithmException ex)
    {
      throw new IllegalStateException(ex);
    }
  }

  public KeyManager(String storedPlayerKey)
  {
  }

  public String getPublicKey()
  {
    byte[] data = own.getEncoded();
    return Base64.getEncoder().encodeToString(data);
  }

  public String decode(String data)
  {
    try
    {
      Cipher decryptCipher = Cipher.getInstance("RSA");
      decryptCipher.init(Cipher.DECRYPT_MODE, own);
      byte[] decoded = Base64.getDecoder().decode(data);
      byte[] message = decryptCipher.doFinal(decoded);
      return new String(message, StandardCharsets.UTF_8);
    }
    catch(GeneralSecurityException ex)
    {
      Logger.getLogger(KeyManager.class.getName()).log(Level.SEVERE, ex.toString(), ex);
      return null;
    }
  }

  public String encode(String message)
  {
    try
    {
      Cipher encryptCipher = Cipher.getInstance("RSA");
      encryptCipher.init(Cipher.ENCRYPT_MODE, playerKey);
      byte[] sourceMessage = message.getBytes(StandardCharsets.UTF_8);
      byte[] data = encryptCipher.doFinal(sourceMessage);
      return Base64.getEncoder().encodeToString(data);
    }
    catch(GeneralSecurityException ex)
    {
      Logger.getLogger(KeyManager.class.getName()).log(Level.SEVERE, null, ex);
      return message;
    }
  }
}
