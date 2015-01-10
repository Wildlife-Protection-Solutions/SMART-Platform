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

import java.util.HashMap;
import java.util.Map;

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
	
	private transient char[] password = "superSecret1".toCharArray(); //TODO
	private transient final Map<Informant, Map<InformantDataKey, Object>> data = new HashMap<Informant, Map<InformantDataKey,Object>>();
	
	private InformantAesManager() {
		//nothing
	}
	
	public static final InformantAesManager getInstance() {
		return manager;
	}

	public final void set(Informant informant, InformantDataKey key, Object value) {
		if (data == null)
			return;
		Map<InformantDataKey, Object> info = data.get(informant);
		if (info == null) {
			//TODO: try decrypt first
			info = new HashMap<InformantDataKey, Object>();
			data.put(informant, info);
		}
		info.put(key, value);
		EncryptedData encryptedData = aesTool.encrypt(info, password);
		informant.setEncryptedData(encryptedData);
	}

	public final Object get(Informant informant, InformantDataKey key) {
		if (data == null)
			return null; //data should never be null
		Map<InformantDataKey, Object> info = data.get(informant);
		if (info == null) {
			//TODO: ClassCast????
			Object obj = aesTool.decrypt(informant.getEncryptedData(), password);
			info = (Map<InformantDataKey, Object>) obj;
			data.put(informant, info);
		}
		return info != null ? info.get(key) : null;
	}
	
	public final void clear() {
		if (password != null) {
			for (int i = 0; i < password.length; i++) {
				password[i] = '?';
			}
		}
		if (data != null) {
			//TODO: is there a safer way to remove objects from memory
			data.clear();
		}
	}
	
}
