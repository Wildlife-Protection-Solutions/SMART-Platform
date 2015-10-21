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
package org.wcs.smart.connect.model;

import org.wcs.smart.security.AESTool;
import org.wcs.smart.security.EncryptedData;


/**
 * Class that manages encrypting and decrypting smart connect passwords.
 * Modeled after informant encrypting/decryption.
 *  
 * @author egouge
 */
public final class PasswordAesManager {
	
	private static final PasswordAesManager manager = new PasswordAesManager();
	private static final AESTool aesTool = new AESTool();
	
	private PasswordAesManager() {
		//nothing
	}
	
	public static final PasswordAesManager getInstance() {
		return manager;
	}
	
	public final String encryptPassword(String password, String key) throws Exception{
		EncryptedData data = aesTool.encrypt(password, key.toCharArray());
		return data.toString();
	}
	
	public final String decryptPassword(String password, String key) throws Exception{
		EncryptedData toDecrypt = EncryptedData.fromString(password);
		String value = (String)aesTool.decrypt(toDecrypt, key.toCharArray());
		return value;
	}
	
}
