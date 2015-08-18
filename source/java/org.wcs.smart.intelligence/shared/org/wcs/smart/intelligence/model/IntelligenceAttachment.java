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
package org.wcs.smart.intelligence.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.wcs.smart.common.attachment.ISmartAttachment;

/**
 * Link between intelligence and associated attachment
 * 
 * @author elitvin
 * @since 1.0.0
 */
@Entity
@Table(name="smart.intelligence_attachment")
public class IntelligenceAttachment extends ISmartAttachment {
	
	private Intelligence intelligence;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="intelligence_uuid", referencedColumnName="uuid")
	public Intelligence getIntelligence(){
		return this.intelligence;
	}

	public void setIntelligence(Intelligence intelligence){
		this.intelligence = intelligence;
		super.attachmentFile = null;
	}
	
	@Transient
	@Override
	public String getDatastoreFolderPath(Session session) throws Exception {
		return intelligence.getDatastoreLocation();
	}
	

}
