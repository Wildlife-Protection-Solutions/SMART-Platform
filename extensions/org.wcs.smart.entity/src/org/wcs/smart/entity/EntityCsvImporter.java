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
package org.wcs.smart.entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.Entity.Status;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;

import au.com.bytecode.opencsv.CSVReader;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Tool for importing entities.
 * 
 * @author Emily
 *
 */
public class EntityCsvImporter {

	private EntityType entityType;
	private File importFile;
	private CsvImporterConfig configuration;
	
	/**
	 * Creates a new importer for a given entity type.
	 * @param et
	 */
	public EntityCsvImporter(EntityType et){
		configuration = new CsvImporterConfig();
		this.entityType = et;
	}
	
	/**
	 * Entity type associated with the csv importer
	 * @return
	 */
	public EntityType getEntityType(){
		return this.entityType;
	}
	
	/**
	 * the associated configuration
	 * @return
	 */
	public CsvImporterConfig getConfiguration(){
		return this.configuration;
	}
	
	/**
	 * Sets the import file.
	 * 
	 * @param importFile
	 */
	public void setFile(File importFile){
		this.importFile = importFile;
	}
	
	/**
	 * Reads the first line of the csv and returns
	 * an array of the column names.
	 * 
	 * @return
	 * @throws Exception
	 */
	public String[] getFileHeaders() throws Exception{
		return readHeaders();
	}
	
	/*
	 * Reads the headers from the csv file
	 */
	private String[] readHeaders() throws Exception{
	
		InputStreamReader reader = new InputStreamReader(new FileInputStream(importFile), "UTF-8"); //$NON-NLS-1$
		try{
			CSVReader csvReader = new CSVReader(reader, configuration.getDelimiter());
			return csvReader.readNext();
		}finally{
			reader.close();
		}
	}
	
	/**
	 * Imports all entities from the configured file.
	 * 
	 * The file must be set {@link #setFile(File)}, and the 
	 * configuration {@link #getConfiguration()} initialized.
	 * 	
	 * @param s
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	public boolean importEntities(Session s, IProgressMonitor monitor) throws Exception{
		final ArrayList<String> warnings = new ArrayList<String>();
		ArrayList<Entity> addedEntities = new ArrayList<Entity>();
		
		entityType = (EntityType) s.load(EntityType.class, entityType.getUuid());
		
		int totalCount = -1;
		// read all to get the total count
		InputStreamReader reader = new InputStreamReader(new FileInputStream(importFile), "UTF-8"); //$NON-NLS-1$
		try{
			CSVReader csvReader = new CSVReader(reader, configuration.getDelimiter());
			totalCount = csvReader.readAll().size();
			csvReader.close();
		}finally{
			reader.close();
		}
		
		List<EntityAttributeSelfReference> selfReferenceItems = new ArrayList<EntityAttributeSelfReference>();
		reader = new InputStreamReader(new FileInputStream(importFile), "UTF-8"); //$NON-NLS-1$
		try{
			int lineCount = 0;
			CSVReader csvReader = new CSVReader(reader, configuration.getDelimiter()); 
			if (configuration.getSkipHeader()){
				csvReader.readNext();
				lineCount ++;
			}
			
			if (configuration.getIdColumn() == null){
				throw new Exception(Messages.EntityCsvImporter_NoIdColumn);
			}
			
			if (configuration.getStatusColumn() == null){
				warnings.add(Messages.EntityCsvImporter_NoStatusColumn);
			}
			
			for (EntityAttribute ea : entityType.getAttributes()){
				Integer x = configuration.getColumn(ea);
				if (x == null){
					warnings.add(MessageFormat.format(Messages.EntityCsvImporter_NotAttributeColumn, ea.getName()));	
				}
			}
			
			String[] data = csvReader.readNext();
			lineCount ++;
			
			
			while(data != null){
				int progress = (int)(((lineCount+ 0.0) / totalCount) * 100.0);
				int lastprogress = (int)(((lineCount - 1.0) / totalCount) * 100.0);
				monitor.worked(progress - lastprogress);
				
				Entity entity = new Entity();
				entity.setEntityType(entityType);
				
				// ID Column
				if (configuration.getIdColumn() != null){
					String id = data[configuration.getIdColumn()].trim();
					if (id.length() == 0){
						throw new Exception(MessageFormat.format(Messages.EntityCsvImporter_IdFieldRequired,new Object[]{lineCount}));
					}else if (!SmartUtils.isSimpleString(id, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Entity.ID_MAX_LENGTH, 1)){
						throw new Exception(MessageFormat.format(Messages.EntityCsvImporter_InvalidId, lineCount,Entity.ID_MAX_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc));
					}
					entity.setId(id);
				}
				
				//Status Column
				entity.setStatus(Status.ACTIVE);
				if (configuration.getStatusColumn() != null){
					Entity.Status status = parseStatus(data[configuration.getStatusColumn()]);
					if (status == null){
						warnings.add(
								MessageFormat.format(Messages.EntityCsvImporter_StatusNotSupported, new Object[]{data[configuration.getStatusColumn()], lineCount}));
					}else{
						entity.setStatus(status);
					}
				}
				
				//X & Y Column
				if (configuration.getXColumn() != null){
					//get x,y, projection
					String x = data[configuration.getXColumn()].trim();
					String y = data[configuration.getYColumn()].trim();
					
					if (x.length() == 0){
						throw new Exception(MessageFormat.format(Messages.EntityCsvImporter_XRequired,new Object[]{lineCount}));
					}
					
					if (y.length() == 0){
						throw new Exception(MessageFormat.format(Messages.EntityCsvImporter_YRequired,new Object[]{lineCount}));
					}
					Double cx = null;
					Double cy = null;
					
					try{
						cx = Double.parseDouble(x);
					}catch (Exception ex){
						throw new Exception(MessageFormat.format(Messages.EntityCsvImporter_XCannotBeParsed, new Object[]{x,lineCount}));
					}
					
					try{
						cy = Double.parseDouble(y);
					}catch (Exception ex){
						throw new Exception(MessageFormat.format(Messages.EntityCsvImporter_YCannotBeParsed, new Object[]{y,lineCount}));
					}
					
					if (cx == null || cy == null){
						throw new Exception(MessageFormat.format(Messages.EntityCsvImporter_InvalidCoordinates, new Object[]{lineCount}));
					}
					Projection proj = configuration.getProjection();
					Coordinate dbC = null;
					try{
						dbC = ReprojectUtils.reproject(cx, cy, proj.getCrs(), SmartDB.DATABASE_CRS);
					}catch (Exception ex){
						throw new Exception(MessageFormat.format(Messages.EntityCsvImporter_CannotReproject + "\n\n" + ex.getMessage(), new Object[]{lineCount})); //$NON-NLS-1$
					}
					entity.setX(dbC.x);
					entity.setY(dbC.y);
					
				}
				
				//remaining columns
				entity.setAttributes(new ArrayList<EntityAttributeValue>());
				for (EntityAttribute ea : entityType.getAttributes()){
					//s.saveOrUpdate(ea.getDmAttribute());
					Integer x = configuration.getColumn(ea);
					if (x != null){
						Object value = parseString(ea, data[x], warnings, lineCount);
						if (value != null){
							if (value instanceof EntityAttributeSelfReference){
								((EntityAttributeSelfReference)value).entity = entity;
								selfReferenceItems.add((EntityAttributeSelfReference) value);
							}else{
								EntityAttributeValue eav = new EntityAttributeValue();
								eav.setEntityAttribute(ea);
								eav.setEntity(entity);
								eav.setValue(value);
								entity.getAttributes().add(eav);
							}
						}
					}
				}
				
				data = csvReader.readNext();
				lineCount ++;
				
				List<NamedKeyItem> tmpList = new ArrayList<NamedKeyItem>();
				tmpList.addAll(entityType.getDmAttribute().getAttributeList());
				for (Entity e : addedEntities){
					tmpList.add(e.getAttributeListItem());
				}
				
				AttributeListItem listItem = new AttributeListItem();
				listItem.setAttribute(entityType.getDmAttribute());
				listItem.setIsActive(entity.getStatus() == Status.ACTIVE);
				listItem.setKeyId(NamedKeyItem.generateKey(entity.getId(), tmpList));
				listItem.setListOrder(entityType.getDmAttribute().getAttributeList().size() + addedEntities.size());
				listItem.setName(entity.getId());
				listItem.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), entity.getId());
				entity.setAttributeListItem(listItem);
			
				//add the entity
				addedEntities.add(entity);
				
				if (monitor.isCanceled()){
					return false;
				}
			}
			csvReader.close();
		}finally{
			reader.close();
		}
		
		//update all self reference attributes
		for (EntityAttributeSelfReference r : selfReferenceItems){
			boolean found = false;
			for (Entity e : addedEntities){
				if (!e.equals(r.entity) && 
						e.getId().equals(r.value)){
					EntityAttributeValue eav = new EntityAttributeValue();
					eav.setEntityAttribute(r.ea);
					eav.setEntity(r.entity);
					eav.setValue(e.getAttributeListItem());
					r.entity.getAttributes().add(eav);
					found = true;
					break;
				}
			}
			
			if (!found){
				warnings.add(MessageFormat.format(Messages.EntityCsvImporter_InvalidValue,
						new Object[]{r.value, r.ea.getName()}));
			}
			
		}
		
		if (addedEntities.size() == 0){
			throw new Exception(Messages.EntityCsvImporter_NoEntitiesFound);
		}
		
		if (warnings.size() > 0){
			final boolean[] cancel = new boolean[]{false};
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					WarningDialog dialog = new WarningDialog(Display.getDefault().getActiveShell(), Messages.EntityCsvImporter_ImportDialogTitle, Messages.EntityCsvImporter_WarningsMessage, warnings);
					if (dialog.open() != WarningDialog.OK){
						//cancel
						cancel[0] = true;
					}
					
				}});
			if (cancel[0]){
				return false;
			}
			
		}
		if (monitor.isCanceled()){
			return false;
		}
		
		s.beginTransaction();
		try{
			entityType.getEntities().addAll(addedEntities);
			//added entity types
			for (Entity e : addedEntities){
				s.saveOrUpdate(e.getAttributeListItem());
				s.saveOrUpdate(e);
			}
			
			for (Entity e : addedEntities){
				//s.saveOrUpdate(e);
				entityType.getDmAttribute().getAttributeList().add(e.getAttributeListItem());
			}
			s.flush();
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			throw ex;
		}
		
		EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_MODIFIED, entityType);
		
		//fire events
		DataModelManager.getInstance().fireChangeListeners();
		
		return true;
	}
	
	/**
	 * Parses the status string
	 * @param value
	 * @return
	 */
	private Entity.Status parseStatus(String value){
		try{
			return Entity.Status.valueOf(value);
		}catch(Exception ex){
			
		}
		for (Entity.Status s : Entity.Status.values()){
			if (s.getGuiName().equalsIgnoreCase(value)){
				return s;
			}
		}
		return null;
	}
	
	/**
	 * Converts a string to the associated attribute entity value.
	 * 
	 * @param ea
	 * @param value
	 * @param warnings
	 * @param lineNumber
	 * @return
	 */
	private Object parseString(EntityAttribute ea, String value, List<String> warnings, int lineNumber){
		if (value == null || value.length() == 0){
			return null;
		}
		
		if (ea.getDmAttribute().getType() == AttributeType.TEXT){
			return value;
		}
		
		if (ea.getDmAttribute().getType() == AttributeType.NUMERIC){
			try{
				return Double.parseDouble(value);
			}catch (Exception ex){
				warnings.add(MessageFormat.format(Messages.EntityCsvImporter_ValueNotSet, new Object[]{value, ea.getName(), lineNumber}));
			}
			return null;
		}
		if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN){
				try{
					return Double.parseDouble(value);
				}catch (Exception ex){}
				if (value.equalsIgnoreCase(Attribute.BOOLEAN_FALSE_LABEL)){
					return Boolean.FALSE;
				}else if (value.equals(Attribute.BOOLEAN_TRUE_LABEL)){
					return Boolean.TRUE;
				}
				
				warnings.add(MessageFormat.format(Messages.EntityCsvImporter_ValueNotSet, new Object[]{value, ea.getName(), lineNumber}));
				return null;
			}
		
		if (ea.getDmAttribute().getType() == AttributeType.DATE){
			try{
				SimpleDateFormat sdf = new SimpleDateFormat(configuration.getDateFormatString());
				return sdf.parse(value);
			}catch (Exception ex){
				warnings.add(MessageFormat.format(Messages.EntityCsvImporter_ValueNotSet, new Object[]{value, ea.getName(), lineNumber}));
			}
			return null;
		}
		
		if (ea.getDmAttribute().getType() == AttributeType.LIST){
			//we need to look up the list value
			//first by key; then by name??
			
			//first search keys; generally this won't work
			for (AttributeListItem item : ea.getDmAttribute().getAttributeList()){
				if (item.getKeyId().equalsIgnoreCase(value)){
					return item;
				}
			}
			
			//search names in all languages
			for (AttributeListItem item : ea.getDmAttribute().getAttributeList()){
				for (Label l : item.getNames()){
					if (l.getValue().equalsIgnoreCase(value)){
						return item;
					}
				}
			}
			if (ea.getDmAttribute().equals(entityType.getDmAttribute())){
				
				//this attribute is the same one referenced by the
				//type so we
				//need to search for new items
				//but we should do this after we've imported all the
				//entities incase the reference is later in the csv file
				//(for self-reference entity attributes (parent mother etc.)
				return new EntityAttributeSelfReference(ea, value);
			}else{
				warnings.add(MessageFormat.format(Messages.EntityCsvImporter_ValueNotSet, new Object[]{value, ea.getName(), lineNumber}));
			}
			return null;
		}

		if (ea.getDmAttribute().getType() == AttributeType.TREE){
			//similar to lists; search all items; keys first then labels
			List<AttributeTreeNode> toProcess = new ArrayList<AttributeTreeNode>();
			toProcess.addAll(ea.getDmAttribute().getTree());
			while(toProcess.size() > 0){
				AttributeTreeNode node = toProcess.remove(0);
				if (node.getHkey().equalsIgnoreCase(value)){
					return node;
				}
				toProcess.addAll(node.getChildren());
			}
			
			toProcess.addAll(ea.getDmAttribute().getTree());
			while(toProcess.size() > 0){
				AttributeTreeNode node = toProcess.remove(0);
				for (Label l : node.getNames()){
					if (l.getValue().equalsIgnoreCase(value)){
						return node;
					}
				}
				toProcess.addAll(node.getChildren());
			}
			warnings.add(MessageFormat.format(Messages.EntityCsvImporter_ValueNotSet, new Object[]{value, ea.getName(), lineNumber}));
			return null;
		}
		
		warnings.add(MessageFormat.format(Messages.EntityCsvImporter_AttributeNotSupported, new Object[]{ea.getDmAttribute().getType()}));
		return null;
	}
	
	
	class EntityAttributeSelfReference{
		public Entity entity;
		public EntityAttribute ea;
		public String value;
		
		public EntityAttributeSelfReference(Entity entity, EntityAttribute ea, String value){
			this.entity = entity;
			this.ea = ea;
			this.value = value;
		}
		public EntityAttributeSelfReference(EntityAttribute ea, String value){
			this.ea = ea;
			this.value = value;
		}
	}
	
}

