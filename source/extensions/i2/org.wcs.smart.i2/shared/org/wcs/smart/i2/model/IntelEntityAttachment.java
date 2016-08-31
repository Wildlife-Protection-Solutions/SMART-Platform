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
