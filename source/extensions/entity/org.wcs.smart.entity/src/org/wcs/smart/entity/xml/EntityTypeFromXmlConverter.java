package org.wcs.smart.entity.xml;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.Status;
import org.wcs.smart.entity.xml.model.AggregationType;
import org.wcs.smart.entity.xml.model.DataModelAttribute;
import org.wcs.smart.entity.xml.model.LabelType;
import org.wcs.smart.entity.xml.model.ListNode;
import org.wcs.smart.entity.xml.model.TreeNodeType;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Converts an xml entity type schema to an SMART EntityType object.
 * 
 * @author Emily
 *
 */
public class EntityTypeFromXmlConverter {

	
	public static EntityType fromXml(org.wcs.smart.entity.xml.model.EntityType xml, Session session) throws ParseException{
		
		EntityType entityType = new EntityType();
		entityType.setAttributes(new ArrayList<EntityAttribute>());
		entityType.setConservationArea(SmartDB.getCurrentConservationArea());
		entityType.setCreator(SmartDB.getCurrentEmployee());
		entityType.setDateCreated(new Date());
		
		entityType.setEntities(new ArrayList<Entity>());
		entityType.setKeyId(xml.getKeyid());
		importNames(xml.getNames(), entityType, session, true);
		entityType.setStatus(Status.valueOf(xml.getStatus()));
		entityType.setType(EntityType.Type.valueOf(xml.getType()));
		
		//this should not exist; instead it should be created when
		//the entitytype is created

		Attribute dmAttribute = findAttribute(xml.getDmAttribute().getKey(), session);
		if (dmAttribute != null){
			throw new ParseException(
					MessageFormat.format(Messages.EntityTypeFromXmlConverter_AttributeAlreadyExists, new Object[]{xml.getDmAttribute().getKey()}), 0); 
		}
		dmAttribute = createAttribute(xml.getDmAttribute(), session);
		dmAttribute.getAttributeList().clear();	//we don't want to import any entity references
		entityType.setDmAttribute(dmAttribute);
		
		
		//all of the attributes should exist
		int order = 1;
		HashMap<String, Attribute> newAttributes = new HashMap<String, Attribute>();
		
		for (org.wcs.smart.entity.xml.model.EntityAttribute xmlAtt : xml.getAttributes()){
			EntityAttribute ea = new EntityAttribute();

			ea.setEntityType(entityType);
			ea.setIsPrimary(xmlAtt.isIsPrimary());
			ea.setIsRequired(xmlAtt.isIsRequired());
			ea.setKeyId(xmlAtt.getKeyid());
			importNames(xmlAtt.getAliases(), ea, session, false);
			ea.setOrder(order++);
			
			entityType.getAttributes().add(ea);	

			if (xmlAtt.getDmAttribute().getKey().equals(xml.getDmAttribute().getKey())){
				//this attribute is the same as the entity attribute
				//so we want to use that one
				ea.setDmAttribute(dmAttribute);	
			}else{
				//look up attribute in data model
				Attribute a = findAttribute(xmlAtt.getDmAttribute().getKey(), session);
				if (a != null){
					//verify that the type is the same
					Attribute.AttributeType xmlType = Attribute.decodeAttributeType(xmlAtt.getDmAttribute().getType());
					if (xmlType == null){
						throw new ParseException(MessageFormat.format(Messages.EntityTypeFromXmlConverter_AttributeTypeNoSupported, new Object[]{xmlAtt.getDmAttribute().getType()}), 0);
					}
					if (!xmlType.equals(a.getType())){
						//the attribute types are different so we cannot import this schema
						throw new ParseException(
							MessageFormat.format(Messages.EntityTypeFromXmlConverter_AttributeTypesDifferent,
							new Object[]{a.getName() + " [" + a.getKeyId() + "]", a.getType().name(), xmlType.name()}),0); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}else{
					//attribute not found in dm
					
					//see if we've already created it
					Attribute existingNew = newAttributes.get(xmlAtt.getDmAttribute().getKey());
					if (existingNew != null){
						a = existingNew;
					}else{
						//not already created so need to create a new one
						a = createAttribute(xmlAtt.getDmAttribute(), session);
						newAttributes.put(a.getKeyId(), a);
					}
				}
				ea.setDmAttribute(a);
			}
		}
		
		return entityType;
	}
	
	private static Attribute createAttribute(DataModelAttribute xml, Session session) throws ParseException{
		Attribute a = new Attribute();
		a.setAggregations(new ArrayList<Aggregation>());
		
		a.setConservationArea(SmartDB.getCurrentConservationArea());
		a.setIsRequired(xml.isIsrequired());
		a.setKeyId(xml.getKey());
		
		importNames(xml.getNames(),a, session, true);
		
		if (xml.getQaMinmax() != null){
			if (xml.getQaMinmax().getMinValue() != null){
				a.setMinValue(xml.getQaMinmax().getMinValue());	
			}
			if (xml.getQaMinmax().getMaxValue() != null){
				a.setMaxValue(xml.getQaMinmax().getMaxValue());
			}
		}	
		if (xml.getQaRegex() != null){
			a.setRegex(xml.getQaRegex());
		}
		
		/* Attribute List */
		a.setAttributeList(new ArrayList<AttributeListItem>());
		if (xml.getValues() != null){
			List<ListNode> items = xml.getValues();
			for (int i = 0; i < items.size(); i ++){
				ListNode item = items.get(i);
				AttributeListItem newItem = new AttributeListItem();
				newItem.setKeyId(item.getKey());
				newItem.setListOrder(i);
				importNames(item.getNames(), newItem, session, true);
				newItem.setUuid(null);
				newItem.setAttribute(a);
				newItem.setIsActive(item.isIsactive());
				a.getAttributeList().add(newItem);
				
			}
		}
		
		/* Tree List */
		List<TreeNodeType> rootNodes = xml.getTree();
		if (rootNodes != null && rootNodes.size() > 0){
			a.setTree(new ArrayList<AttributeTreeNode>());
			for (int i = 0; i < rootNodes.size(); i ++){
				AttributeTreeNode newNode = processAttributeTreeNode(null, a, rootNodes.get(i), session);
				newNode.setNodeOrder(i);
				a.getTree().add(newNode);
			}
		}
		
		
		/* Aggregations */
		List<AggregationType> aggs = xml.getAggregations();
		if (aggs != null && aggs.size() > 0){
			a.setAggregations(new ArrayList<Aggregation>());
			for (AggregationType agg : aggs){
				a.getAggregations().add(lookUpAggregation(agg.getAggregation()));
			}
		}
		
		Attribute.AttributeType newtype = Attribute.decodeAttributeType(xml.getType());
		if (newtype == null){
			throw new ParseException(MessageFormat.format(Messages.EntityTypeFromXmlConverter_AttributeTypeNoSupported, new Object[]{xml.getType()}), 0);
		}
		a.setType(newtype);
		
		return a;
	}
	
	/*
	 * Looks up aggregation from list
	 */
	private static Aggregation lookUpAggregation(String name) throws ParseException{
		for (Aggregation agg : DataModel.getAggregations()){
			if (agg.getName().equals(name)){
				return agg;
			}
		}
		throw new ParseException(MessageFormat.format(Messages.EntityTypeFromXmlConverter_AggregationNotFound, new Object[]{name}), 0);
	}
	
	private  static Attribute findAttribute(String keyId, Session s){
		List<?> attributes = s.createCriteria(Attribute.class).add(Restrictions.eq("keyId", keyId)).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$ //$NON-NLS-2$
		if (attributes == null || attributes.size() == 0){
			//attribute not found we will have to create it
			return null;
		}else{
			return (Attribute) attributes.get(0);
		}
	}
	
	private static AttributeTreeNode processAttributeTreeNode(AttributeTreeNode parent, 
			Attribute parentAttribute, 
			TreeNodeType xmlNode, Session session) {		
		AttributeTreeNode newAttributeTreeNode = new AttributeTreeNode();
		newAttributeTreeNode.setAttribute(parentAttribute);
		newAttributeTreeNode.setKeyId(xmlNode.getKey());
		importNames(xmlNode.getNames(), newAttributeTreeNode, session, true);
		newAttributeTreeNode.setParent(parent);

		newAttributeTreeNode.setIsActive(xmlNode.isIsactive());
		if (xmlNode.getChildren() != null && xmlNode.getChildren().size() > 0) {
			newAttributeTreeNode.setChildren(new ArrayList<AttributeTreeNode>());
			for (int i = 0; i < xmlNode.getChildren().size(); i ++){			
				AttributeTreeNode newChild = processAttributeTreeNode(newAttributeTreeNode, parentAttribute, xmlNode.getChildren().get(i), session);
				newChild.setNodeOrder(i);
				newAttributeTreeNode.getChildren().add(newChild);
			}
		}
		newAttributeTreeNode.updateHkey();
		return newAttributeTreeNode;
	}
	
	/**
	 * Imports the query names from the xml query type to the SMART query object.
	 * @param query
	 * @param qt
	 * @throws Exception
	 */
	private static void importNames(List<LabelType> names, 
			NamedItem toUpdate, 
			Session session, boolean required) {
		String xmlDefaultName = null;
		for (LabelType label : names){
			if (label.isIsDefault()){
				xmlDefaultName = label.getValue();
			}
			List<?> values = session.createCriteria(Language.class).
				add(Restrictions.eq("ca", SmartDB.getCurrentConservationArea())). //$NON-NLS-1$ 
				add(Restrictions.eq("code",label.getLanguageCode())).list(); //$NON-NLS-1$
				
			if (values.size() > 0){
				for (Object l : values){
					toUpdate.updateName((Language)l, label.getValue());
				}
			}
		}
			
		//ensure a deafult exists
		String defaultName = toUpdate.findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		if (defaultName == null){
			if (xmlDefaultName != null){
				toUpdate.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(),
						xmlDefaultName);
			}else{
				if (required){
					toUpdate.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), 
						names.get(0).getValue());
				}
			}
		}
		//update chached name
		String name = toUpdate.findNameNull(SmartDB.getCurrentLanguage());
		if (name == null){
			name = toUpdate.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		}
		toUpdate.setName(name);
		
	}
}
