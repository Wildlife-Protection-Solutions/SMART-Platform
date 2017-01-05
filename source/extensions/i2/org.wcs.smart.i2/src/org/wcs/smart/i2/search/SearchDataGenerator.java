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
package org.wcs.smart.i2.search;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
/**
 * Generates a large quantity of intelligence data for testing.
 * 
 * @author Emily
 *
 */
public class SearchDataGenerator {

	public static List<IntelRelationshipType> generateRelationshipTypes(int numberOfGroups, int numberOfRelationshipTypes, int numberOfAttributePerRelationship, List<IntelAttribute> attributes, List<IntelEntityType> types, IProgressMonitor monitor, Session session){
			
		List<IntelRelationshipGroup> groups = new ArrayList<>();
		List<IntelRelationshipType> rtypes= new ArrayList<>();
		
		monitor.beginTask("Generating Relationsihp Groups...", numberOfGroups + numberOfRelationshipTypes);
		
		
		for (int i = 0; i < numberOfGroups; i ++){
			monitor.subTask(i + "/" + numberOfGroups);
			
			IntelRelationshipGroup g = new IntelRelationshipGroup();
			g.setConservationArea(SmartDB.getCurrentConservationArea());
			g.setKeyId("relationshipgroup" + i);
			g.updateName(SmartDB.getCurrentLanguage(), "Relationship Group " + i);
			g.setRelationshipTypes(new ArrayList<IntelRelationshipType>());
			session.save(g);
			groups.add(g);
			monitor.worked(1);
		}
		
		
		for (int i = 0; i < numberOfRelationshipTypes; i ++){
			
			monitor.subTask(i + "/" + numberOfRelationshipTypes);
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
			
			if (Math.random() > 0.3){
				IntelRelationshipGroup g = groups.get((int)Math.round(Math.random() * (groups.size()-1)));
				type.setRelationshipGroup(g);
				g.getRelationshipTypes().add(type);
			}
			
			HashSet<IntelAttribute> usedAttributes = new HashSet<IntelAttribute>();
			for (int k = 0; k < Math.random() * numberOfAttributePerRelationship; k ++){
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
			monitor.worked(1);
		}
		return rtypes;
	}
	
	public static List<IntelAttribute> generateAttribute(int numberOfAttributes, int numberOfListItems, IProgressMonitor monitor, Session session){
	
		List<IntelAttribute> attributes = new ArrayList<>();
		
		monitor.beginTask("Generating Attributes", numberOfAttributes);
		for (int i = 0; i < numberOfAttributes; i ++){
			monitor.subTask(i + "/" + numberOfAttributes);
			IntelAttribute attribute = new IntelAttribute();
			attribute.setConservationArea(SmartDB.getCurrentConservationArea());
			attribute.setKeyId("attribute_" + i);
			attribute.updateName(SmartDB.getCurrentLanguage(), "Attribute " + i);
			attribute.setType(AttributeType.values()[i % AttributeType.values().length]);
			
			
			if (attribute.getType() == AttributeType.LIST){
				attribute.setAttributeList(new ArrayList<>());
				for (int k = 0;  k < numberOfListItems; k ++){
					IntelAttributeListItem li = new IntelAttributeListItem();
					li.setKeyId("list_" + i + "_" + k);
					li.updateName(SmartDB.getCurrentLanguage(), "List Item " + i + " " + k);
					li.setAttribute(attribute);
					attribute.getAttributeList().add(li);
				}
			}
			session.save(attribute);
			attributes.add(attribute);
			monitor.worked(1);
		}
		session.flush();	
		return attributes;
	}
	
	public static List<IntelEntityType> generateEntityTypes(int numberOfTypes, int numberOfAttributePerType, List<IntelAttribute> attributes, IProgressMonitor monitor, Session session){
		
		List<IntelEntityType> types = new ArrayList<>();
		
		monitor.beginTask("Generating Entity Types", numberOfTypes);
		for (int i = 0; i < numberOfTypes; i ++){
			monitor.subTask(i + "/" + numberOfTypes);
			
			IntelEntityType type = new IntelEntityType();
			
			type.setConservationArea(SmartDB.getCurrentConservationArea());
			type.setKeyId("type" + i);
			type.updateName(SmartDB.getCurrentLanguage(), "Type " + i);
			type.setAttributes(new ArrayList<IntelEntityTypeAttribute>());
			
			Set<IntelAttribute> atts = new HashSet<>();
			for (int k = 0; k < numberOfAttributePerType; k ++){
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
				if (a.getType() == AttributeType.TEXT && type.getIdAttribute() == null){
					type.setIdAttribute(a);
				}
						
			}
			if (type.getIdAttribute() == null){
				type.setIdAttribute(type.getAttributes().get(0).getAttribute());
			}
			
			session.save(type);
			types.add(type);
			monitor.worked(1);
		}
		session.flush();
		return types;
	}
	
	private static IntelEntityAttributeValue generateValue(IntelEntityTypeAttribute a, List<String> strings){
		IntelEntityAttributeValue value = new IntelEntityAttributeValue();
		value.setAttribute(a.getAttribute());
		
		if (a.getAttribute().getType() == AttributeType.BOOLEAN){
			value.setNumberValue( Math.random() > 0.5 ? 1.0 : 0.0 );
		}else if (a.getAttribute().getType() == AttributeType.DATE){
			value.setDateValue(new Date());
		}else if (a.getAttribute().getType() == AttributeType.LIST){
			int index = (int)Math.round((a.getAttribute().getAttributeList().size()-1) * Math.random());
			value.setAttributeListItem(a.getAttribute().getAttributeList().get(index));
		}else if (a.getAttribute().getType() == AttributeType.NUMERIC){
			value.setNumberValue(Math.random() * 100 * Math.random() + Math.random());
		}else if (a.getAttribute().getType() == AttributeType.TEXT){
			StringBuilder sb= new StringBuilder();
			int numWords = (int)Math.round(Math.random() * 15);
			if (a.getEntityType().getIdAttribute().equals(a.getAttribute())){
				if (numWords == 0) numWords = 1;
			}
			for (int i = 0; i < numWords; i ++){
				sb.append(strings.get(  (int)Math.round(Math.random() * (strings.size() - 1) )) + " ");
			}
			value.setStringValue(sb.toString());
//			System.out.println(sb.toString());
		}
		
		return value;
	}
	
	private static IntelEntityRelationshipAttributeValue generateValue(IntelRelationshipTypeAttribute a, List<String> strings){
		IntelEntityRelationshipAttributeValue value = new IntelEntityRelationshipAttributeValue();
		value.setAttribute(a.getAttribute());
		
		if (a.getAttribute().getType() == AttributeType.BOOLEAN){
			value.setNumberValue( Math.random() > 0.5 ? 1.0 : 0.0 );
		}else if (a.getAttribute().getType() == AttributeType.DATE){
			value.setDateValue(new Date());
		}else if (a.getAttribute().getType() == AttributeType.LIST){
			int index = (int)Math.round((a.getAttribute().getAttributeList().size()-1) * Math.random());
			value.setAttributeListItem(a.getAttribute().getAttributeList().get(index));
		}else if (a.getAttribute().getType() == AttributeType.NUMERIC){
			value.setNumberValue(Math.random() * 100 * Math.random() + Math.random());
		}else if (a.getAttribute().getType() == AttributeType.TEXT){
			StringBuilder sb= new StringBuilder();
			for (int i = 0; i < Math.random() * 15; i ++){
				sb.append(strings.get(  (int)Math.round(Math.random() * (strings.size() - 1) )) + " ");
			}
			value.setStringValue(sb.toString());
//			System.out.println(sb.toString());
		}
		
		return value;
	}
	
	public static void generateEntities(int numberOfEntities,  List<IntelEntityType> types, List<IntelRelationshipType> relationshipTypes, IProgressMonitor monitor, Session session){
		InputStream is = SearchDataGenerator.class.getClassLoader().getResourceAsStream("org/wcs/smart/i2/search/words.txt");
		
		List<String> items = new ArrayList<String>();
		try(Scanner s = new Scanner(is).useDelimiter("\\n")){
			while(s.hasNext()){
				String n = s.next().trim();
				items.add(n);
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		monitor.beginTask("Generating Entities...", numberOfEntities * 2 + types.size() + relationshipTypes.size());
		
		
		HashMap<IntelEntityType, List<IntelEntity>> typesToEntity = new HashMap<>();
		List<IntelEntity> entities = new ArrayList<IntelEntity>();
		int k = 0;
		
		for (IntelEntityType t : types){
			monitor.subTask("Loading type details..." + (k++) + " / " + types.size());
			
			if (t.getAttributes() != null){
				t.getAttributes().forEach(a ->{
					a.getAttribute();
					a.getAttribute().getType();
					if (a.getAttribute().getAttributeList() != null){
						a.getAttribute().getAttributeList().forEach(l -> l.getNames().size());
					}
				});
			}
			monitor.worked(1);
		}
		k = 0;
		for (IntelRelationshipType t : relationshipTypes){
			monitor.subTask("Loading relationship type details..." + (k++) + " / " + types.size());
			if (t.getAttributes() != null){
				t.getAttributes().forEach(a ->{
					a.getAttribute();
					a.getAttribute().getType();
					if (a.getAttribute().getAttributeList() != null){
						a.getAttribute().getAttributeList().forEach(l -> l.getNames().size());
					}
				});
			}
			monitor.worked(1);
		}
		for (int i = 0; i < numberOfEntities; i ++){
			monitor.subTask("entities:" + i + "/" + numberOfEntities);
			IntelEntity entity = new IntelEntity();
			entity.setAttributes(new ArrayList<IntelEntityAttributeValue>());
			entity.setConservationArea(SmartDB.getCurrentConservationArea());
			
			IntelEntityType type  = types.get((int)Math.round(Math.random() * (types.size()-1)));
			entity.setEntityType(type);
			
			session.save(entity);
			if (i % 10 == 0) {
				session.flush();
			}
			
			for (IntelEntityTypeAttribute a : type.getAttributes()){
				IntelEntityAttributeValue av = generateValue(a, items);
				//entity.getAttributes().add(av);
				av.setEntity(entity);
				session.save(av);
			}
			
			List<IntelEntity> le = typesToEntity.get(type);
			if (le == null){
				le = new ArrayList<>();
				typesToEntity.put(type, le);
			}
			le.add(entity);
			entities.add(entity);
			
			monitor.worked(1);
			
			if (i % 10 == 0) {
				session.clear();
				
			}
		}
		
		//generate relationships
		int cnt = 0;
		for (IntelEntity entity: entities){
			monitor.subTask("relations:" + (cnt++) + "/" + entities.size());
			//generate some relationships for this 
			int rcnt = 0;
			for (IntelRelationshipType t : relationshipTypes){
				IntelEntityRelationship relation = new IntelEntityRelationship();
				relation.setRelationshipType(t);
				if (t.getSourceEntityType() == null || t.getSourceEntityType().equals(entity.getEntityType())){
					relation.setSourceEntity(entity);
					if (t.getTargetEntityType() == null){
						//pick any random entity
						IntelEntity target = entities.get((int)Math.round((Math.random() * (entities.size()-1))));
						relation.setTargetEntity(target);
					}else{
						//pick from list
						List<IntelEntity> ops = typesToEntity.get(t.getTargetEntityType());
						if (ops == null || ops.isEmpty()){
							relation = null;
						}else{
							IntelEntity target = ops.get((int)Math.round((Math.random() * (ops.size()-1))));
							relation.setTargetEntity(target);
						}
					}
				}else{
					relation = null;
				}
				
				if (relation != null){
					//generate attributes
					relation.setAttributes(new ArrayList<IntelEntityRelationshipAttributeValue>());
					session.save(relation);
					rcnt++;
					
					for (IntelRelationshipTypeAttribute a : relation.getRelationshipType().getAttributes()){
						IntelEntityRelationshipAttributeValue value = generateValue(a, items);
						//relation.getAttributes().add(value);
						value.setRelationship(relation);
						session.save(value);
					}
					
				}
				if (rcnt > 20) break; //max out at 20 relationships
			}
			monitor.worked(1);
			
			if (cnt % 10 == 0) {
				session.flush();
				session.clear();
			}
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public static void generateRecords(int numberRecords, IProgressMonitor monitor, Session session){
		InputStream is = SearchDataGenerator.class.getClassLoader().getResourceAsStream("org/wcs/smart/i2/search/words.txt");
		
		List<String> items = new ArrayList<String>();
		try(Scanner s = new Scanner(is).useDelimiter("\\n")){
			while(s.hasNext()){
				String n = s.next().trim();
				items.add(n);
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		monitor.beginTask("Generating Records...", numberRecords);
		
		List<IntelEntity> entities = session.createCriteria(IntelEntity.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();

		for (int i = 0; i < numberRecords; i ++){
			monitor.subTask("Generated Records " + i + " / " + numberRecords);
			
			IntelRecord record = new IntelRecord();
//			record.setAttachments(attachments);
			record.setConservationArea(SmartDB.getCurrentConservationArea());		
			record.setEntities(new ArrayList<IntelEntityRecord>());
			record.setLocations(new ArrayList<IntelLocation>());
			
			int status = (int)Math.round(Math.random() * 2);
			record.setStatus(IntelRecord.Status.values()[status]);
			
			int wordsTitle = (int)Math.round(Math.random() * 6);
			if (wordsTitle == 0) wordsTitle = 1;
			StringBuilder title = new StringBuilder();
			for (int j = 0; j < wordsTitle; j ++){
				int index = (int)Math.round(Math.random() * (items.size()-1));
				title.append(items.get(index));
				title.append(" ");
			}
			title.deleteCharAt(title.length() - 1);
			record.setTitle(WordUtils.capitalize(title.toString()));
			
			
			int descTitle = (int)Math.round(Math.random() * 10000);
			if (descTitle == 0) descTitle = 1;
			StringBuilder desc = new StringBuilder();
			for (int j = 0; j < descTitle; j ++){
				int index = (int)Math.round(Math.random() * (items.size()-1));
				desc.append(items.get(index));
				desc.append(" ");
			}
			String v = desc.toString();
			if (v.length() > 32700) v = v.substring(0, 32700);
			record.setDescription(v.toString());
			
			
			int numEntities = (int)Math.round(Math.random() * 20);
			Set<IntelEntity> used = new HashSet<IntelEntity>();
			int j = 0;
			while( j < numEntities && used.size() < entities.size()){
				int index = (int)Math.round(Math.random() * (entities.size() - 1));
				IntelEntity e = entities.get(index);
				if (!used.contains(e)){
					IntelEntityRecord entity = new IntelEntityRecord();
					entity.setRecord(record);
					entity.setEntity(e);
					record.getEntities().add(entity);
					j++;
					used.add(e);
				}
			}
			session.save(record);
			session.flush();
			session.clear();
			monitor.worked(1);
		}
	}
}
