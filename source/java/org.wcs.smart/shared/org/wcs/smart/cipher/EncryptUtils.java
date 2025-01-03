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

package org.wcs.smart.cipher;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.wcs.smart.SmartContext;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.util.UuidUtils;

/**
 * Tools for encrypting and decrypting files.
 * 
 * @author Emily
 *
 */
public class EncryptUtils {

	/**
	 * Default location for placing decrypted files while in use
	 */
	public static final String TEMP_DIR = "temporaryfiles"; //$NON-NLS-1$

	private static final String ENCRYPTION_TRANSFORM = "AES/CBC/PKCS5Padding"; //$NON-NLS-1$
	private static final String ENCRYPTION_TYPE = "AES"; //$NON-NLS-1$

	/**
	 * You should not use this function.  It is provided for the upgrade script only.
	 * @param inputFile
	 * @param outputFile
	 * @param caUuid
	 * @return
	 * @throws Exception
	 */
	public static Path encryptFile(Path inputFile, Path outputFile, UUID caUuid) throws Exception {
		SecureRandom srandom = new SecureRandom();
		byte[] iv = new byte[128/8];
		srandom.nextBytes(iv);
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		
		SecretKeySpec key = new SecretKeySpec(UuidUtils.uuidToByte(caUuid), ENCRYPTION_TYPE);
		try(OutputStream out = Files.newOutputStream(outputFile)){
			out.write(iv);
			Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORM);
			cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);
			try (InputStream in = Files.newInputStream(inputFile)){
			    processFile(cipher, in, out);
			}	
		}
		return outputFile;
	}
	
	/**
	 * Encrypts a SMART attachment using the specified an input file and output file
	 * 
	 * @param inputFile
	 * @param outputFile
	 * @param attachment 
	 * @return
	 * @throws Exception
	 */
	public static Path encryptFile(Path inputFile, Path outputFile, ISmartAttachment attachment) throws Exception {
		SecureRandom srandom = new SecureRandom();
		byte[] iv = new byte[128/8];
		srandom.nextBytes(iv);
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		
		SecretKeySpec key = new SecretKeySpec(UuidUtils.uuidToByte(attachment.getConservationArea().getUuid()), ENCRYPTION_TYPE);
		try(OutputStream out = Files.newOutputStream(outputFile)){
			out.write(iv);
			Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORM);
			cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);
			try (InputStream in = Files.newInputStream(inputFile)){
			    processFile(cipher, in, out);
			}	
		}
		return outputFile;
	}

	/**
	 * Decrypts attachment to provided file.  Will override outputFile if
	 * it already exists
	 * 
	 * @param attachment
	 * @param outputFile
	 * @return
	 * @throws Exception
	 */
	public static Path decryptAttachment(ISmartAttachment attachment, Path outputFile) throws Exception {
		SecretKeySpec key = new SecretKeySpec(UuidUtils.uuidToByte(attachment.getConservationArea().getUuid()), ENCRYPTION_TYPE);
		
		try (InputStream in = Files.newInputStream(attachment.getAttachmentFile())){
			byte[] iv = new byte[128/8];
			in.read(iv);
			IvParameterSpec ivspec = new IvParameterSpec(iv);
			
			Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORM);
			cipher.init(Cipher.DECRYPT_MODE, key, ivspec);
			try(OutputStream out = Files.newOutputStream(outputFile)){
			    processFile(cipher, in, out);
			}
		}catch (Exception ex) {
			try {
				Files.delete(outputFile);
			}catch (Exception ex2) {
			}
			throw ex;
		}
		return outputFile;
	}
	
	/**
	 * Decrypts attachment to local temporary directory and returns the location
	 * of the file.  Will not overwrite files already decrypted; instead it will create
	 * a new file.
	 * 
	 * @param attachment
	 * @return
	 * @throws Exception
	 */
	public static Path decryptAttachment(ISmartAttachment attachment) throws Exception {
		Path core = Paths.get(SmartContext.INSTANCE.getFilestoreLocation()).resolve(TEMP_DIR);
		Path outputFile = uniqueFile(core.resolve(attachment.getFilename()));
		
		if (!Files.exists(outputFile.getParent())) Files.createDirectories(outputFile.getParent());
		decryptAttachment(attachment, outputFile);
		return outputFile;
	}
	
	/**
	 * Decrypts file to output stream
	 * @param attachment
	 * @param outStream
	 * @throws Exception
	 */
	public static void decryptAttachment(ISmartAttachment attachment, OutputStream outStream) throws Exception {
		SecretKeySpec key = new SecretKeySpec(UuidUtils.uuidToByte(attachment.getConservationArea().getUuid()), ENCRYPTION_TYPE);
		
		try (InputStream in = Files.newInputStream(attachment.getAttachmentFile())){
			byte[] iv = new byte[128/8];
			in.read(iv);
			IvParameterSpec ivspec = new IvParameterSpec(iv);
			
			Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORM);
			cipher.init(Cipher.DECRYPT_MODE, key, ivspec);
			
			processFile(cipher, in, outStream);
			
		}
	}
	
	static private void processFile(Cipher ci, InputStream in, OutputStream out)
			throws javax.crypto.IllegalBlockSizeException, javax.crypto.BadPaddingException, java.io.IOException {
		byte[] ibuf = new byte[1024];
		int len;
		while ((len = in.read(ibuf)) != -1) {
			byte[] obuf = ci.update(ibuf, 0, len);
			if (obuf != null)
				out.write(obuf);
		}
		byte[] obuf = ci.doFinal();
		if (obuf != null)
			out.write(obuf);
	}
	
	/**
	 * Compute a unique file name from the path.  If the file
	 * name exists it will add a number to the file name until it 
	 * doesn't exist
	 * @param path
	 * @return
	 */
	public static Path uniqueFile(Path path) {
		String filename = path.getFileName().toString();
		int index = filename.lastIndexOf('.');
		String prefix = filename;
		String suffix = ""; //$NON-NLS-1$
		if (index > 0) {
			suffix = prefix.substring(index + 1);
			prefix = prefix.substring(0,index);
		}
		int ctr = 1;
		Path outputFile = path;
		while(Files.exists(outputFile)) {
			outputFile = path.getParent().resolve(prefix + "_" + ctr + "." + suffix); //$NON-NLS-1$ //$NON-NLS-2$
			ctr++;
		}
		return outputFile;
	}
	
}
