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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	public final boolean validatePassword(char[] password) {
		return this.password != null && password != null && Arrays.equals(this.password, password);
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

	public final void set(Informant informant, Map<InformantDataKey, Object> info) throws Exception{
		if (data == null || informant == null)
			return;
		if (password == null) {
			return;
		}
		data.put(informant, info);
		
		EncryptedData encryptedData = !isEmpty(info) ? aesTool.encrypt(info, password) : new EncryptedData();
		informant.setEncryptedData(encryptedData);
		
	}

	private final boolean isEmpty(Map<InformantDataKey, Object> info) {
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
	public final Map<InformantDataKey, Object> get(Informant informant) throws Exception {
		if (data == null || informant == null)
			return null; //data should never be null
		EncryptedData encryptedData = informant.getEncryptedData();
		if (encryptedData == null || encryptedData.isEmpty()) {
			return null;
		}
		if (password == null) {
			return null;
		}
		if (invalidSet.contains(informant)) {
			return null;
		}
		Map<InformantDataKey, Object> info = data.get(informant);
		if (info == null) {
			
			Object obj = aesTool.decrypt(encryptedData, password);
			info = (Map<InformantDataKey, Object>) obj;
			if (info == null) {
				info = new HashMap<InformantDataKey, Object>();
			}
			data.put(informant, info);
		}
		return info;
	}

	public final Object get(Informant informant, InformantDataKey key) throws Exception {
		if (data == null || informant == null)
			return null; //data should never be null
		EncryptedData encryptedData = informant.getEncryptedData();
		if (encryptedData == null || encryptedData.isEmpty()) {
			return ""; //$NON-NLS-1$
		}
		if (password == null) {
			throw new PasswordProctectedException();
		}
		if (invalidSet.contains(informant)) {
			throw new InvalidPasswordException();
		}
		Map<InformantDataKey, Object> info = get(informant);
		if (info == null) {
			throw new InvalidPasswordException();
		}
		return info.get(key);
	}
	
	public final void clear() {
		clearPassword();
		if (data != null) {
			//TODO: is there a safer way to remove objects from memory
			data.clear();
		}
		invalidSet.clear();
	}

	private final void clearPassword() {
		if (password != null) {
			for (int i = 0; i < password.length; i++) {
				password[i] = '?';
			}
		}
		password = null;
	}
	
	public class InvalidPasswordException extends Exception{};
	public class PasswordProctectedException extends Exception{};
}
