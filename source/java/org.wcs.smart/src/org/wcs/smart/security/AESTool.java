package org.wcs.smart.security;
/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
 
public final class AESTool {
	
    private static final int pswdIterations = 65536;
    private static final int keySize = 128;
 
    private final SecretKeySpec createKeySpec(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException { 
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); //$NON-NLS-1$
        PBEKeySpec spec = new PBEKeySpec(
                password,
                salt,
                pswdIterations,
                keySize
                );
 
        SecretKey secretKey = factory.generateSecret(spec);
        return new SecretKeySpec(secretKey.getEncoded(), "AES"); //$NON-NLS-1$
    }

    public final EncryptedData encrypt(Object data, char[] password) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, IllegalBlockSizeException, BadPaddingException {
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	try (ObjectOutput out = new ObjectOutputStream(bos)) {
    		out.writeObject(data);
    		byte[] bytes = bos.toByteArray();
    		return encrypt(bytes, password);
    	}
    }

    public final Object decrypt(EncryptedData encryptedData, char[] password) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException, ClassNotFoundException {
    	if (encryptedData == null || encryptedData.isEmpty()) {
    		return null;
    	}
    	byte[] bytes = decryptBytes(encryptedData, password);
    	if (bytes == null) {
    		return null;
    	}
    	ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    	try (ObjectInput in = new ObjectInputStream(bis)) {
    		Object o = in.readObject(); 
    		return o;
    	}
    }
    
    private final EncryptedData encrypt(byte[] data, char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidParameterSpecException, IllegalBlockSizeException, BadPaddingException {
    	byte[] salt = generateSalt(); 
    	SecretKeySpec secret = createKeySpec(password, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); //$NON-NLS-1$
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
        byte[] encryptedBytes = cipher.doFinal(data);
        return new EncryptedData(salt, ivBytes, encryptedBytes);
    }

    private final byte[] decryptBytes(EncryptedData encryptedData, char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
    	SecretKeySpec secret = createKeySpec(password, encryptedData.getSalt());
        // Decrypt the message
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); //$NON-NLS-1$
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(encryptedData.getIv()));

        byte[] decryptedTextBytes = cipher.doFinal(encryptedData.getData());
    	return decryptedTextBytes;
    }

    private final byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[20];
        random.nextBytes(bytes);
        return bytes;
    }
}