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
