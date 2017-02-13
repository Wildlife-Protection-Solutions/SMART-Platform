package org.wcs.smart.i2.entity.exporter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.util.UuidUtils;

import au.com.bytecode.opencsv.CSVWriter;

public class EntityRelationshipExporter {

	
	public EntityRelationshipExporter(){
		
	}
	
	public void exportEntity(IntelEntity entity, int degrees, Path outputDirectory){
		Session s = HibernateManager.openSession();
		try{
			entity = (IntelEntity)s.get(IntelEntity.class,  entity.getUuid());
			
			HashSet<IntelEntity> entitiesToExport = new HashSet<>();
			HashSet<IntelEntityRelationship> relationshipsToExport = new HashSet<>();
			entitiesToExport.add(entity);
			
			int degree = 1;
			Set<IntelEntity> toSearch = new HashSet<>();
			toSearch.add(entity);
			
			while(degree <= degrees && !toSearch.isEmpty()){
				entitiesToExport.addAll(toSearch);
				//find all relationships whose source or target entity 
				//is in tosearch
				
				String hql = "From IntelEntityRelationship WHERE sourceEntity IN (:toSearch) or targetEntity IN (:toSearch)";
				Query q = s.createQuery(hql);
				q.setParameterList("toSearch", toSearch);
				
				List<IntelEntityRelationship> relationships = q.list();
				
				Set<IntelEntity> search = new HashSet<IntelEntity>();
				for (IntelEntityRelationship r : relationships){
					boolean src = toSearch.contains(r.getSourceEntity());
					boolean trg = toSearch.contains(r.getTargetEntity());
					
					relationshipsToExport.add(r);
					
					if (src && !trg){
						search.add(r.getTargetEntity());
					}
					if (trg && !src){
						search.add(r.getSourceEntity());
					}
				}
				toSearch = search;
			}
			entitiesToExport.addAll(toSearch);
			
			
			//now we have entities and relationships to export lets compile a list of
			//all valid attributes
			Set<IntelEntityType> types = entitiesToExport.stream().map(r->r.getEntityType()).distinct().collect(Collectors.toSet());
			List<IntelAttribute> entityAttributes = new ArrayList<IntelAttribute>();
			for (IntelEntityType t : types){
				for (IntelEntityTypeAttribute a : t.getAttributes()){
					if (!entityAttributes.contains(a.getAttribute())) entityAttributes.add(a.getAttribute());
				}
			}
			
			Set<IntelRelationshipType> rtypes = relationshipsToExport.stream().map(r->r.getRelationshipType()).distinct().collect(Collectors.toSet());
			List<IntelAttribute> relationshipAttributes = new ArrayList<IntelAttribute>();
			for (IntelRelationshipType t : rtypes){
				for (IntelRelationshipTypeAttribute a : t.getAttributes()){
					if (!relationshipAttributes.contains(a.getAttribute())) relationshipAttributes.add(a.getAttribute());
				}
			}
			
			entityAttributes.sort((a,b)-> Collator.getInstance().compare(a.getName(), b.getName()));
			relationshipAttributes.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			
			//now we write out all entities to one csv file
			//and all relationships to another csv file
			String name = entity.getIdAttributeAsText();
			name = URLUtils.cleanFilename(name);
			Path entities = outputDirectory.resolve(name + "_entities.csv");
			Path relationships = outputDirectory.resolve(name + "+relationships.csv");
			
			
			
			AttributeLabelProvider provider = new AttributeLabelProvider();
			
			try(CSVWriter writer = new CSVWriter(Files.newBufferedWriter(entities))){
				String[] data = new String[entityAttributes.size() + 2];
				data[0] = "UUID";
				data[1] = "Entity Name";
				int i = 2;
				for (IntelAttribute ia : entityAttributes){
					data[i++] = ia.getName();
				}
				writer.writeNext(data);
				
				for(IntelEntity e : entitiesToExport){
					data = new String[entityAttributes.size() + 2];
					data[0] = UuidUtils.uuidToString(e.getUuid());
					data[1] = e.getIdAttributeAsText();
					i = 2;
					for (IntelAttribute ia : entityAttributes){
						IntelEntityAttributeValue v = e.findAttributeValue(ia);
						if (v != null){
							data[i++] = provider.getText(v);
						}else{
							data[i++] = "";
						}
					}
					writer.writeNext(data);
				}
			} catch (IOException e) {
				
			}
			
			try(CSVWriter writer = new CSVWriter(Files.newBufferedWriter(relationships))){
				String[] data = new String[relationshipAttributes.size() + 5];
				int i = 0;
				data[i++] = "UUID";
				data[i++] = "Source Entity UUID";
				data[i++] = "Source Entity ID";
				data[i++] = "Target Entity UUID";
				data[i++] = "Target Entity ID";
				
				for (IntelAttribute ia : entityAttributes){
					data[i++] = ia.getName();
				}
				writer.writeNext(data);
				
				for(IntelEntityRelationship e : relationshipsToExport){
					data = new String[data.length];
					data[i++] = UuidUtils.uuidToString(e.getUuid());
					data[i++] = UuidUtils.uuidToString(e.getSourceEntity().getUuid());
					data[i++] = e.getSourceEntity().getIdAttributeAsText();
					data[i++] = UuidUtils.uuidToString(e.getTargetEntity().getUuid());
					data[i++] = e.getTargetEntity().getIdAttributeAsText();
					i = 2;
					for (IntelAttribute ia : entityAttributes){
						IntelEntityRelationshipAttributeValue v = e.findAttributeValue(ia);
						if (v != null){
							data[i++] = provider.getText(v);
						}else{
							data[i++] = "";
						}
					}
					writer.writeNext(data);
				}
			} catch (IOException e) {
				
			}
			provider.dispose();
			
			
		}finally{
			s.close();
		}
	}
}
