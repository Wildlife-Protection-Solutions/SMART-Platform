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
package org.wcs.smart.intelligence.informant.aes;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.InformantDataKey;

/**
 * Class that manages secure data for informants
 *  
 * @author elitvin
 * @since 3.2.0
 */
public final class InformantAesManager {
	
	private static final InformantAesManager manager = new InformantAesManager();
	private static final AESTool aesTool = new AESTool();
	
	private transient char[] password = null;
	private transient final Map<Informant, Map<InformantDataKey, Object>> data = new HashMap<>();
	private transient final Set<Informant> invalidSet = new HashSet<>();
	
	private InformantAesManager() {
		//nothing
	}
	
	public static final InformantAesManager getInstance() {
		return manager;
	}
	
	public final void setPassword(char[] password) {
		clear();
		this.password = password;
	}

	public final boolean containsDecrypted() {
		return !data.isEmpty();
	}

	public final boolean containsInvalid() {
		return !invalidSet.isEmpty();
	}
	
	public final boolean isDecrypted(Informant informant) {
		return data.containsKey(informant);
	}
	
	public final boolean isPasswordSet() {
		return password != null;
	}
	
//	public final void set(Informant informant, InformantDataKey key, Object value) {
//		Map<InformantDataKey, Object> info = data.get(informant);
//		if (info == null) {
//			info = new HashMap<InformantDataKey, Object>();
//		}
//		info.put(key, value);
//		set(informant, info);
//	}

	public final void set(Informant informant, Map<InformantDataKey, Object> info) {
		if (data == null || informant == null)
			return;
		if (password == null) {
			return;
		}
		data.put(informant, info);
		try {
			EncryptedData encryptedData = !isEmpty(info) ? aesTool.encrypt(info, password) : new EncryptedData();
			informant.setEncryptedData(encryptedData);
		} catch (InvalidKeyException e) {
			IntelligencePlugIn.log("InvalidKeyException encrypting informant ", null); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			IntelligencePlugIn.log("NoSuchAlgorithmException encrypting informant ", null); //$NON-NLS-1$
		} catch (InvalidKeySpecException e) {
			IntelligencePlugIn.log("InvalidKeySpecException encrypting informant ", null); //$NON-NLS-1$
		} catch (NoSuchPaddingException e) {
			IntelligencePlugIn.log("NoSuchPaddingException encrypting informant ", null); //$NON-NLS-1$
		} catch (InvalidParameterSpecException e) {
			IntelligencePlugIn.log("InvalidParameterSpecException encrypting informant ", null); //$NON-NLS-1$
		} catch (IllegalBlockSizeException e) {
			IntelligencePlugIn.log("IllegalBlockSizeException encrypting informant ", null); //$NON-NLS-1$
		} catch (BadPaddingException e) {
			IntelligencePlugIn.log("BadPaddingException encrypting informant ", null); //$NON-NLS-1$
		} catch (IOException e) {
			IntelligencePlugIn.log("IOException encrypting informant ", null); //$NON-NLS-1$
		}
		
	}

	private boolean isEmpty(Map<InformantDataKey, Object> info) {
		if (info != null) {
			Set<InformantDataKey> keySet = info.keySet();
			for (InformantDataKey key : keySet) {
				Object value = info.get(key);
				if (value != null && !"".equals(value)) { //$NON-NLS-1$
					return false;
				}
			}
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public final Object get(Informant informant, InformantDataKey key) {
		if (data == null || informant == null)
			return null; //data should never be null
		EncryptedData encryptedData = informant.getEncryptedData();
		if (encryptedData == null || encryptedData.isEmpty()) {
			return ""; //$NON-NLS-1$
		}
		if (password == null) {
			return Messages.InformantAesManager_PasswordProtected;
		}
		if (invalidSet.contains(informant)) {
			return Messages.InformantAesManager_InvalidPassword;
		}
		Map<InformantDataKey, Object> info = data.get(informant);
		if (info == null) {
			try {
				Object obj = aesTool.decrypt(encryptedData, password);
				info = (Map<InformantDataKey, Object>) obj;
				if (info == null) {
					info = new HashMap<InformantDataKey, Object>();
				}
				data.put(informant, info);
			} catch (InvalidKeyException e) {
				IntelligencePlugIn.log("InvalidKeyException decrypting informant ", null); //$NON-NLS-1$
			} catch (NoSuchAlgorithmException e) {
				IntelligencePlugIn.log("NoSuchAlgorithmException decrypting informant ", null); //$NON-NLS-1$
			} catch (InvalidKeySpecException e) {
				IntelligencePlugIn.log("InvalidKeySpecException decrypting informant ", null); //$NON-NLS-1$
			} catch (NoSuchPaddingException e) {
				IntelligencePlugIn.log("NoSuchPaddingException decrypting informant ", null); //$NON-NLS-1$
			} catch (InvalidAlgorithmParameterException e) {
				IntelligencePlugIn.log("InvalidAlgorithmParameterException decrypting informant ", null); //$NON-NLS-1$
			} catch (IllegalBlockSizeException e) {
				IntelligencePlugIn.log("IllegalBlockSizeException decrypting informant ", null); //$NON-NLS-1$
			} catch (BadPaddingException e) {
				IntelligencePlugIn.log("BadPaddingException decrypting informant ", null); //$NON-NLS-1$
			} catch (IOException e) {
				IntelligencePlugIn.log("IOException decrypting informant ", null); //$NON-NLS-1$
			} catch (ClassNotFoundException e) {
				IntelligencePlugIn.log("ClassNotFoundException decrypting informant ", null); //$NON-NLS-1$
			} catch (Exception e) {
				IntelligencePlugIn.log("Exception decrypting informant: " + e.getClass().toString(), null); //$NON-NLS-1$
			}
		}
		if (info == null) {
			invalidSet.add(informant);
			return Messages.InformantAesManager_InvalidPassword;
		}
		return info.get(key);
	}
	
	public final void clear() {
		if (password != null) {
			for (int i = 0; i < password.length; i++) {
				password[i] = '?';
			}
		}
		password = null;
		if (data != null) {
			//TODO: is there a safer way to remove objects from memory
			data.clear();
		}
		invalidSet.clear();
	}
	
}
