/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * For managing signature types
 * 
 * @author Emily
 *
 */
public class SignatureTypeManager {

	public static SignatureTypeManager INSTANCE = new SignatureTypeManager();
	
	protected SignatureTypeManager() {
		
	}
	
	/**
	 * Gets all types for a given Conservation Area
	 * @param session
	 * @param ca
	 * @return
	 */
	public List<SignatureType> getTypes(Session session, ConservationArea ca){
		return QueryFactory.buildQuery(session, SignatureType.class,
				new Object[] {"conservationArea", ca}).list();  //$NON-NLS-1$
	}
	
	/**
	 * Find a signature type with a given keyid in the conservation area
	 * 
	 * @param keyId
	 * @param ca
	 * @param session
	 * @return
	 */
	public SignatureType findType(String keyId, ConservationArea ca, Session session) {
		SignatureType stype = QueryFactory.buildQuery(session, SignatureType.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", keyId}).uniqueResult(); //$NON-NLS-1$
		return stype;

	}
	
}
