/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.model;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Link between entity and attachment
 */
@Entity
@Table(name = "smart.i_entity_attachment")
public class IntelEntityAttachment  {

	private EntityAttachmentPk id = new EntityAttachmentPk();	
	
	public IntelEntityAttachment(){
		
	}
	
	@EmbeddedId
	public EntityAttachmentPk getId(){
		return this.id;
	}
	public void setId(EntityAttachmentPk id){
		this.id = id;
	}
	
	@Transient
	public IntelEntity getEntity() {
		return id.getEntity();
	}

	public void setEntity(IntelEntity entity) {
		id.setEntity(entity);
	}
	
	@Transient
	public IntelAttachment getAttachment() {
		return id.getAttachment();
	}

	public void setAttachment(IntelAttachment attachment) {
		id.setAttachment(attachment);
	}
	

	/**
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o){
		if (o instanceof IntelEntityAttachment){
			return this.id.equals(((IntelEntityAttachment)o).id);
		}
		return false;
	}
	
	/**
	 * @return
	 */
	@Override
	public int hashCode(){
		return id.hashCode();
	}
	
	/**
	 * Primary key object for category attribute association 
	 * 
	 */
	@Embeddable
	private static class EntityAttachmentPk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private IntelEntity entity;
		private IntelAttachment attachment;
		

		public EntityAttachmentPk(){
			
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="entity_uuid")
		public IntelEntity getEntity() {
			return entity;
		}

		public void setEntity(IntelEntity entity) {
			this.entity = entity;
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="attachment_uuid")
		public IntelAttachment getAttachment() {
			return attachment;
		}

		public void setAttachment(IntelAttachment attachment) {
			this.attachment = attachment;
		}
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof EntityAttachmentPk)){
				return false;
			}
			EntityAttachmentPk p = (EntityAttachmentPk)key;
			
			if (p.entity == null || this.entity == null ||
				p.attachment == null || this.attachment == null ){
				
				if (p.entity == null && this.entity == null && 
					p.attachment == null && this.attachment == null){
						return true;
				}
				return false;
			}
			
			return p.entity.equals(this.entity) &&
					p.attachment.equals(this.attachment);
		}
		@Override
		public int hashCode() {
		    int code = 0;
		    if (entity != null) {code += entity.hashCode();}
		    code *= 31;
		    if (attachment != null) {code += attachment.hashCode(); }
		    return code;
		  }
	}

}
