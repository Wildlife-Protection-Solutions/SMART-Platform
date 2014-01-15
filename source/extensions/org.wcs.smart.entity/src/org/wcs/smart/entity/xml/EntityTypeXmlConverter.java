package org.wcs.smart.entity.xml;

import org.wcs.smart.ca.Label;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.xml.model.LabelType;

public class EntityTypeXmlConverter {

	/**
	 * Converts a EntityType to the XML Representation
	 * of the xml type.
	 * 
	 * 
	 * @param entityType
	 * @return
	 */
	public static org.wcs.smart.entity.xml.model.EntityType toXml(EntityType entityType){
		org.wcs.smart.entity.xml.model.EntityType xml = new org.wcs.smart.entity.xml.model.EntityType();
		
		xml.setId(entityType.getId());
		xml.setKeyid(entityType.getKeyId());
		xml.setStatus(entityType.getStatus().name());
		xml.setType(entityType.getType().name());
		
		for (Label l : entityType.getNames()){
			LabelType lt = new LabelType();
			lt.setLanguageCode(l.getLanguage().getCode());
			lt.setValue(l.getValue());
			xml.getNames().add(lt);
		}
		
		for (EntityAttribute et : entityType.getAttributes()){
			org.wcs.smart.entity.xml.model.EntityAttribute xmlAtt = new org.wcs.smart.entity.xml.model.EntityAttribute();
			
			xmlAtt.setAttributeKeyid(et.getDmAttribute().getKeyId());
			xmlAtt.setIsPrimary(et.getIsPrimary());
			xmlAtt.setIsRequired(et.getIsRequired());
			xmlAtt.setKeyid(et.getKeyId());
			for (Label l : et.getNames()){
				LabelType lt = new LabelType();
				lt.setLanguageCode(l.getLanguage().getCode());
				lt.setValue(l.getValue());
				xmlAtt.getAliases().add(lt);
			}
			
			xml.getAttributes().add(xmlAtt);
		}
		
		return xml;
	}
}
