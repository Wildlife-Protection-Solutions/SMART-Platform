package org.wcs.smart.i2.search;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;

public class SearchDataGenerator {

	private static int numOfAttributes = 1000;
	private static int numOfListItems = 50;
	private static int numberOfTypes = 100;
	
	private static int attributesPerType = 100;
	
	private static int numberOfEntities = 2000;
	
	private static int numberOfRelationshipGroups = 200;
	private static int numberOfRelationshipTypes = 200;
	
	public static void generateData2(Session session){
		List<IntelRelationshipGroup> groups = new ArrayList<>();
		
		List<IntelAttribute> attributes = session.createCriteria(IntelAttribute.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
		List<IntelEntityType> types = session.createCriteria(IntelEntityType.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
		for (int i = 0; i < numberOfRelationshipGroups; i ++){
			System.out.println("Generating Relationship Groups: " + i  + "/" + numberOfRelationshipGroups);
			IntelRelationshipGroup g = new IntelRelationshipGroup();
			g.setConservationArea(SmartDB.getCurrentConservationArea());
			g.setKeyId("relationshipgroup" + i);
			g.updateName(SmartDB.getCurrentLanguage(), "Relationship Group " + i);
			session.save(g);
			groups.add(g);
		}
		
		List<IntelRelationshipType> rtypes = new ArrayList<IntelRelationshipType>();
		for (int i = 0; i < numberOfRelationshipTypes; i ++){
			System.out.println("Generating Relationship Types : " + i  + "/" + numberOfRelationshipTypes);
			IntelRelationshipType type = new IntelRelationshipType();
			type.setConservationArea(SmartDB.getCurrentConservationArea());
			type.setKeyId("relationshiptype" + i);
			type.updateName(SmartDB.getCurrentLanguage(), "Relationship Type " + i);
			type.setAttributes(new ArrayList<IntelRelationshipTypeAttribute>());
			if (Math.random() > 0.2){
				type.setSourceEntityType(types.get( (int)Math.round(Math.random() * (types.size() - 1))));
			}
			if (Math.random() > 0.2){
				type.setTargetEntityType(types.get( (int)Math.round(Math.random() * (types.size() - 1))));
			}
			HashSet<IntelAttribute> usedAttributes = new HashSet<IntelAttribute>();
			for (int k = 0; k < Math.random() * attributesPerType; k ++){
				IntelAttribute ia = attributes.get(  (int)Math.round(Math.random() * (attributes.size()-1)));
				while(usedAttributes.contains(ia)){
					ia = attributes.get(  (int)Math.round(Math.random() * (attributes.size()-1)));
				}
				usedAttributes.add(ia);
				IntelRelationshipTypeAttribute a = new IntelRelationshipTypeAttribute();
				a.setAttribute(ia);
				a.setOrder(k);
				a.setRelationshipType(type);
				type.getAttributes().add(a);
			}
			session.save(type);
			rtypes.add(type);
		}
	}
	public static void generateData(Session session){
	
		List<IntelAttribute> attributes = new ArrayList<>();
		
		
		
		
		for (int i = 0; i < numOfAttributes; i ++){
			System.out.println("Generating Attribute: " + i  + "/" + numOfAttributes);
			IntelAttribute attribute = new IntelAttribute();
			attribute.setConservationArea(SmartDB.getCurrentConservationArea());
			attribute.setKeyId("attribute_" + i);
			attribute.updateName(SmartDB.getCurrentLanguage(), "Attribute " + i);
			attribute.setType(IAttributeType.values()[i % IAttributeType.values().length]);
			
			
			if (attribute.getType() == IAttributeType.LIST){
				attribute.setAttributeList(new ArrayList<>());
				for (int k = 0;  k < numOfListItems; k ++){
					IntelAttributeListItem li = new IntelAttributeListItem();
					li.setKeyId("list_" + i + "_" + k);
					li.updateName(SmartDB.getCurrentLanguage(), "List Item " + i + " " + k);
					li.setAttribute(attribute);
					attribute.getAttributeList().add(li);
				}
			}
			session.save(attribute);
			attributes.add(attribute);
		}
		session.flush();
		
		List<IntelEntityType> types = new ArrayList<>();
		
		for (int i = 0; i < numberOfTypes; i ++){
			System.out.println("Generating Type: " + i  + "/" + numberOfTypes);
			
			IntelEntityType type = new IntelEntityType();
			
			type.setConservationArea(SmartDB.getCurrentConservationArea());
			type.setKeyId("type" + i);
			type.updateName(SmartDB.getCurrentLanguage(), "Type " + i);
			type.setAttributes(new ArrayList<IntelEntityTypeAttribute>());
			
			Set<IntelAttribute> atts = new HashSet<>();
			for (int k = 0; k < attributesPerType; k ++){
				IntelEntityTypeAttribute ia = new IntelEntityTypeAttribute();
			
				IntelAttribute a = attributes.get((int)Math.round(Math.random() * (attributes.size()-1)));
				while(atts.contains(a)){
					a = attributes.get((int)Math.round(Math.random() * (attributes.size()-1)));
				}
				
				atts.add(a);
				ia.setAttribute(a);
				ia.setEntityType(type);
				type.getAttributes().add(ia);
				ia.setOrder(k);
				if (a.getType() == IAttributeType.TEXT)	ia.setInBasicSearch(true);
				
				if (a.getType() == IAttributeType.TEXT && type.getIdAttribute() == null){
					type.setIdAttribute(a);
				}
						
			}
			if (type.getIdAttribute() == null){
				type.setIdAttribute(type.getAttributes().get(0).getAttribute());
			}
			
			session.save(type);
			types.add(type);
		}
		session.flush();
		InputStream is = FuzzySearchTest.class.getClassLoader().getResourceAsStream("org/wcs/smart/i2/search/words.txt");
//		Set<String> phones = new HashSet<>();
//		Map<String, List<Object[]>> map = new HashMap<>();
		List<String> items = new ArrayList<String>();
		try{
			Scanner s = new Scanner(is).useDelimiter("\\n");
			while(s.hasNext()){
				String n = s.next().trim();
				items.add(n);
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		for (int i = 0; i < numberOfEntities; i ++){
			System.out.println("Generating Entities: " + i  + "/" + numberOfEntities);
		
			IntelEntity entity = new IntelEntity();
			entity.setAttributes(new ArrayList<IntelEntityAttributeValue>());
			entity.setConservationArea(SmartDB.getCurrentConservationArea());
			
			IntelEntityType type  = types.get((int)Math.round(Math.random() * (types.size()-1)));
			
			entity.setEntityType(type);
			
			for (IntelEntityTypeAttribute a : type.getAttributes()){
				IntelEntityAttributeValue av = generateValue(a, items);
				entity.getAttributes().add(av);
				av.setEntity(entity);
			}
			session.save(entity);
			if (i % 1000 == 0) session.flush();
		}
		session.flush();
	}
	
	private static IntelEntityAttributeValue generateValue(IntelEntityTypeAttribute a, List<String> strings){
		IntelEntityAttributeValue value = new IntelEntityAttributeValue();
		value.setAttribute(a.getAttribute());
		
		if (a.getAttribute().getType() == IAttributeType.BOOLEAN){
			value.setNumberValue( Math.random() > 0.5 ? 1.0 : 0.0 );
		}else if (a.getAttribute().getType() == IAttributeType.DATE){
			value.setDateValue(new Date());
		}else if (a.getAttribute().getType() == IAttributeType.LIST){
			int index = (int)Math.round((a.getAttribute().getAttributeList().size()-1) * Math.random());
			value.setAttributeListItem(a.getAttribute().getAttributeList().get(index));
		}else if (a.getAttribute().getType() == IAttributeType.NUMERIC){
			value.setNumberValue(Math.random() * 100 * Math.random() + Math.random());
		}else if (a.getAttribute().getType() == IAttributeType.TEXT){
			StringBuilder sb= new StringBuilder();
			for (int i = 0; i < Math.random() * 15; i ++){
				sb.append(strings.get(  (int)Math.round(Math.random() * (strings.size() - 1) )) + " ");
			}
			value.setStringValue(sb.toString());
		}
		
		return value;
	}
}
