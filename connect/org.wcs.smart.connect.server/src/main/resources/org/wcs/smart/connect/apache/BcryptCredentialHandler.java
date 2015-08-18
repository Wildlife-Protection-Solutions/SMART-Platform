/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.apache;

import org.mindrot.jbcrypt.BCrypt;

public class BcryptCredentialHandler implements org.apache.catalina.CredentialHandler {

	/**
	 * This interface is used by the {@link Realm} to compare the user provided
	 * credentials with the credentials stored in the {@link Realm} for that user.
	 */
	@Override
	public boolean matches(String inputCredentials, String storedCredentials) {
		if (inputCredentials == null || storedCredentials == null) return false;
		return BCrypt.checkpw(inputCredentials, storedCredentials);
	}

	/**
     * Generates the equivalent stored credentials for the given input
     * credentials.
     *
     * @param inputCredentials  User provided credentials
     *
     * @return  The equivalent stored credentials for the given input
     *          credentials
     */

	@Override
	public String mutate(String inputCredentials) {
		return hashPassword(inputCredentials);
	}

	public static String hashPassword(String password){
		return BCrypt.hashpw(password, BCrypt.gensalt(12));
	}
	
	public static void main(String args[]){
		System.out.println(BCrypt.hashpw("smart", BCrypt.gensalt(12))); //$NON-NLS-1$
		System.out.println(BCrypt.hashpw("smart", BCrypt.gensalt(12))); //$NON-NLS-1$
		String x = BCrypt.hashpw("smart", BCrypt.gensalt(12)); //$NON-NLS-1$
		
		System.out.println(BCrypt.checkpw("smart1", x)); //$NON-NLS-1$
	}
}
