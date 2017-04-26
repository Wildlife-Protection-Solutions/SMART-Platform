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
package org.wcs.smart.i2.entity.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
import org.wcs.smart.i2.ui.AttributeValueLabelProvider;
import org.wcs.smart.util.UuidUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Exports entities and relationships to csv files 
 * @author Emily
 *
 */
public class EntityRelationshipExporter {

	
	public EntityRelationshipExporter(){
		
	}
	
	@SuppressWarnings("unchecked")
	public boolean exportEntity(IntelEntity entity, int degrees, Path outputDirectory, IProgressMonitor monitor) throws Exception{
		if (monitor == null) monitor = new NullProgressMonitor();
		
		monitor.beginTask(Messages.EntityRelationshipExporter_ProgressMsg, 4);
		AttributeValueLabelProvider provider = new AttributeValueLabelProvider();
		Session s = HibernateManager.openSession();
		try{
			entity = (IntelEntity)s.get(IntelEntity.class,  entity.getUuid());
			
			HashSet<IntelEntity> entitiesToExport = new HashSet<>();
			HashSet<IntelEntityRelationship> relationshipsToExport = new HashSet<>();
			entitiesToExport.add(entity);
			
			int degree = 1;
			Set<IntelEntity> toSearch = new HashSet<>();
			toSearch.add(entity);
			
			
			monitor.setTaskName(Messages.EntityRelationshipExporter_TaskMsg);
			while(degree <= degrees && !toSearch.isEmpty()){
				degree++;
				if (monitor.isCanceled()) return false;
				entitiesToExport.addAll(toSearch);
				//find all relationships whose source or target entity 
				//is in tosearch
				
				String hql = "From IntelEntityRelationship WHERE sourceEntity IN (:toSearch) or targetEntity IN (:toSearch)"; //$NON-NLS-1$
				Query q = s.createQuery(hql);
				q.setParameterList("toSearch", toSearch); //$NON-NLS-1$
				
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
			
			if (monitor.isCanceled()) return false;
			monitor.worked(1);
			monitor.setTaskName(Messages.EntityRelationshipExporter_TaskMsg2);
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
			if (monitor.isCanceled()) return false;
			monitor.worked(1);
			monitor.setTaskName(Messages.EntityRelationshipExporter_TaskMsg3);
			
			Path entities = getEntityFile(outputDirectory, entity.getIdAttributeAsText());
			Path relationships = getRelationshipFile(outputDirectory, entity.getIdAttributeAsText());
			
			try(CSVWriter writer = new CSVWriter(Files.newBufferedWriter(entities))){
				String[] data = new String[entityAttributes.size() + 3];
				int i = 0;
				data[i++] = Messages.EntityRelationshipExporter_UuidColumnName;
				data[i++] = Messages.EntityRelationshipExporter_EntityColumnName;
				data[i++] = Messages.EntityRelationshipExporter_TypeColumnName;
				for (IntelAttribute ia : entityAttributes){
					data[i++] = ia.getName();
				}
				writer.writeNext(data);
				
				for(IntelEntity e : entitiesToExport){
					i = 0;
					data = new String[entityAttributes.size() + 3];
					data[i++] = UuidUtils.uuidToString(e.getUuid());
					data[i++] = e.getIdAttributeAsText();
					data[i++] = e.getEntityType().getName();
					for (IntelAttribute ia : entityAttributes){
						IntelEntityAttributeValue v = e.findAttributeValue(ia);
						if (v != null){
							data[i++] = provider.getText(v);
						}else{
							data[i++] = ""; //$NON-NLS-1$
						}
					}
					writer.writeNext(data);
					if (monitor.isCanceled()) return false;
				}
			}
			
			monitor.worked(1);
			monitor.setTaskName(Messages.EntityRelationshipExporter_TaskMsg4);
			
			try(CSVWriter writer = new CSVWriter(Files.newBufferedWriter(relationships))){
				String[] data = new String[relationshipAttributes.size() + 6];
				int i = 0;
				data[i++] = Messages.EntityRelationshipExporter_RelationshipUuidColumnName;
				data[i++] = "Relationship Type";
				data[i++] = Messages.EntityRelationshipExporter_SrcEntityUuid;
				data[i++] = Messages.EntityRelationshipExporter_SrcEntityId;
				data[i++] = Messages.EntityRelationshipExporter_TargetEntityUuid;
				data[i++] = Messages.EntityRelationshipExporter_TargetEntityId;
				
				for (IntelAttribute ia : relationshipAttributes){
					data[i++] = ia.getName();
				}
				writer.writeNext(data);
				
				for(IntelEntityRelationship e : relationshipsToExport){
					data = new String[data.length];
					i=0;
					data[i++] = UuidUtils.uuidToString(e.getUuid());
					data[i++] = e.getRelationshipType().getName();
					data[i++] = UuidUtils.uuidToString(e.getSourceEntity().getUuid());
					data[i++] = e.getSourceEntity().getIdAttributeAsText();
					data[i++] = UuidUtils.uuidToString(e.getTargetEntity().getUuid());
					data[i++] = e.getTargetEntity().getIdAttributeAsText();
					for (IntelAttribute ia : relationshipAttributes){
						IntelEntityRelationshipAttributeValue v = e.findAttributeValue(ia);
						if (v != null){
							data[i++] = provider.getText(v);
						}else{
							data[i++] = ""; //$NON-NLS-1$
						}
					}
					writer.writeNext(data);
					
					if (monitor.isCanceled()) return false;
				}
			}
			monitor.worked(1);
			monitor.done();
			
			
		}finally{
			try{
				s.close();
			}catch (Exception ex){
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
			try{
				provider.dispose();
			}catch (Exception ex){
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
		}
		
		return true;
	}
	
	private static String cleanFilename(String typeName) {
		StringBuffer fix = new StringBuffer(typeName);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fix.length(); i++) {
			char c = fix.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	
	/**
	 * Determines the entity file name by cleaning the provided name
	 * then adding _relationships.csv to it
	 * 
	 * @param dir
	 * @param name
	 * @return
	 */
	public static Path getEntityFile(Path dir, String name){
		name = cleanFilename(name);
		return dir.resolve(name + "_entities.csv"); //$NON-NLS-1$
	}
	
	/**
	 * Determines the relationship file name by cleaning the provided name
	 * then adding _relationships.csv to it
	 * 
	 * @param dir
	 * @param name
	 * @return
	 */
	public static  Path getRelationshipFile(Path dir, String name){
		name = cleanFilename(name);
		return dir.resolve(name + "_relationships.csv"); //$NON-NLS-1$
	}
}
