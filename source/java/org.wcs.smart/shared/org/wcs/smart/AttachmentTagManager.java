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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.AttachmentTag;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * For managing signature types
 * 
 * @author Emily
 *
 */
public class AttachmentTagManager {

	public static AttachmentTagManager INSTANCE = new AttachmentTagManager();
	
	protected AttachmentTagManager() {
		
	}
	
	/**
	 * Gets all types for a given Conservation Area.
	 * @param session
	 * @param ca
	 * @param ccaas - list of conservation areas if the ca is the ccaa can be null
	 * @return
	 */
	public List<AttachmentTag> getTags(Session session,  Collection<ConservationArea> ccaas){
		
		List<AttachmentTag> tags = session.createQuery("FROM AttachmentTag WHERE conservationArea in (:ccaa)", AttachmentTag.class) //$NON-NLS-1$
				.setParameter("ccaa", ccaas) //$NON-NLS-1$
				.list();
		
		HashMap<String, AttachmentTag> key2tag = new HashMap<>();
		for (AttachmentTag tag : tags) {
			AttachmentTag current = key2tag.get(tag.getKeyId());
			if (current == null) {
				current = new AttachmentTag();
				current.setKeyId(tag.getKeyId());
				current.setName(tag.getName());
				current.setConservationArea(null);
				current.setNames(new HashSet<>());
				
				key2tag.put(current.getKeyId(), current);
			}
			//merge labels
			for(Label l : tag.getNames()) {
				boolean skip = false;
				for (Label k : current.getNames()) {
					if (l.getLanguage().isSame(k.getLanguage())) {
						skip = true;
						break;
					}
				}
				if (!skip) {
					current.getNames().add(l);
				}
			}
		}
		ArrayList<AttachmentTag> merged = new ArrayList<>();
		merged.addAll(key2tag.values());
		return merged;
	}
	
	public List<AttachmentTag> getTags(Session session, ConservationArea ca){
		if ( (session.get(ConservationArea.class, ca.getUuid()).getIsCcaa())) {
			//this should not happen - if in CCAA should call get Tags(session, Collection<ConservationArea>)
			throw new RuntimeException("getTags (session, ca) not supported for CCAA"); //$NON-NLS-1$
		}else {
			return QueryFactory.buildQuery(session, AttachmentTag.class,
				new Object[] {"conservationArea", ca}).list();  //$NON-NLS-1$
		}
	}
	
	/**
	 * Find a signature type with a given keyid in the conservation area
	 * 
	 * @param keyId
	 * @param ca
	 * @param session
	 * @return
	 */
	public AttachmentTag findTag(String keyId, ConservationArea ca, Session session) {
		AttachmentTag stype = QueryFactory.buildQuery(session, AttachmentTag.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", keyId}).uniqueResult(); //$NON-NLS-1$
		return stype;

	}

}
