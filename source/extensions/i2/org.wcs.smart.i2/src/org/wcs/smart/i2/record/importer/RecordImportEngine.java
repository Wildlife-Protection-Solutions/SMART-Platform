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
package org.wcs.smart.i2.record.importer;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.record.importer.RecordImportConfig.Column;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

import au.com.bytecode.opencsv.CSVReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Import record engine.
 * 
 * @author Emily
 *
 */
public enum RecordImportEngine {
	
	INSTANCE;
	
	/**
	 * 
	 * @param config
	 * @param pMonitor
	 * @return the number of entities imported or null if nothing imported
	 * @throws Exception
	 */
	public Integer importRecords(RecordImportConfig config, IEventBroker eventBroker, IProgressMonitor pMonitor) throws Exception{
		SubMonitor monitor = SubMonitor.convert(pMonitor);
		
		monitor.beginTask("Importing Records", 5);
		//ensure the file exists
		if (!Files.exists(config.getFile())) throw new FileNotFoundException(config.getFile().toString());
		
		CoordinateReferenceSystem fromCrs = ReprojectUtils.stringToCrs(config.getProjection().getDefinition());
		CoordinateReferenceSystem toCrs = GeometryUtils.SMART_CRS;
		MathTransform transform = CRS.findMathTransform(fromCrs, toCrs);
		
		
		monitor.subTask("Loading source and attribute information");
//		//load the attributes 
		Set<IntelRecordSource> sources = new HashSet<>();
		Session s = HibernateManager.openSession();
		try{
			sources.addAll(s.createCriteria(IntelRecordSource.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.list());
			sources.forEach(src -> {
				src.getNames().size();
				for (IntelRecordSourceAttribute sa : src.getAttributes()){
					if (sa.getAttribute() != null){
						sa.getAttribute().getName();
						if (sa.getAttribute().getAttributeList() != null){
							for (IntelAttributeListItem i : sa.getAttribute().getAttributeList()){
								i.getNames().size();
							}
						}
					}
				}
				
			});
			
		}finally{
			s.close();
		}
		monitor.worked(1);
		if (monitor.isCanceled()) return null;

		List<IntelRecord> addedItems = new ArrayList<>();
		List<String> warnings = new ArrayList<String>();
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(config.getDateFormatString());
		
		monitor.subTask("Reading csv file");
		int lineCnt = 0;
		int numcols = -1;
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(config.getFile()), config.getDelimiter())){
			lineCnt = reader.readAll().size();
		}
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(config.getFile()), config.getDelimiter())){
			String[] ldata = reader.readNext();
			numcols = ldata.length;
		}
		monitor.worked(1);
		
		SubMonitor kidMonitor = monitor.newChild(1, 0);
		kidMonitor.setWorkRemaining(lineCnt);
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(config.getFile()), config.getDelimiter())){
			kidMonitor.worked(1);
			if (monitor.isCanceled()) return null;
			int line = 0;
			if (config.skipFirstLine()){
				reader.readNext();
				line++;
			}
			
			String[] data = null;

			while((data=reader.readNext())!=null ){
				line++;
				if (data.length < numcols){
					warnings.add(MessageFormat.format("Invalid line {0}. The number of columns ({1}) is less than the required number of columns ({2}).  Line will be ignored.", line, data.length, (numcols+1)));
					continue;
				}

				IntelRecord record = new IntelRecord();
				record.setConservationArea(SmartDB.getCurrentConservationArea());
				record.setStatus(Status.NEW);
				
				//title
				Integer titleColumn = config.getMappedColumn(Column.TITLE);
				if (titleColumn != null && !data[titleColumn].trim().isEmpty()){
					record.setTitle(data[titleColumn].trim());
				}
				//narrative
				Integer narrativeColumn = config.getMappedColumn(Column.NARRATIVE);
				if (narrativeColumn != null && !data[narrativeColumn].trim().isEmpty()){
					record.setDescription(data[narrativeColumn].trim());
				}
				//scratchpad
				Integer scratchColumn = config.getMappedColumn(Column.SCRATCHPAD);
				if (scratchColumn != null && !data[scratchColumn].trim().isEmpty()){
					record.setComment(data[scratchColumn].trim());
				}
				
				//source
				Integer srcColumn = config.getMappedColumn(Column.SOURCE);
				if (srcColumn != null){
					String src = data[srcColumn].trim();
					if (!src.isEmpty()){
						//find the matching source
						for (IntelRecordSource r : sources){
							for (Label l : r.getNames()){
								if (l.getValue().toLowerCase().equals(src.toLowerCase())){
									record.setRecordSource(r);
									break;
								}
							}
							if (record.getRecordSource() != null) break;
						}
					}
				}
				
				if (record.getRecordSource() != null){
					record.setAttributes(new ArrayList<IntelRecordAttributeValue>());
					//assign attributes if possible
					for (IntelRecordSourceAttribute ia : record.getRecordSource().getAttributes()){
						Integer attCol = config.getMappedColumn(ia);
						if (attCol == null) continue;
						
						String value = data[attCol].trim();
						String value2 = null;
						
						IntelRecordAttributeValue attributeValue = null;
						if (ia.getAttribute() != null){
							if (ia.getAttribute().getType() == AttributeType.POSITION){
								Integer v2 = config.getYMappedColumn(ia);
								if (v2 != null && !data[v2].trim().isEmpty()) value2 = data[v2].trim();
							}
							try{
								attributeValue = convertValue(value, value2, ia.getAttribute(), dateFormatter, transform);
							}catch (Exception ex){
								attributeValue = null;
								warnings.add(MessageFormat.format("Error converting value ''{0}'' to valid value for attribute ''{1}'': {2}", value, ia.getAttribute().getName(), ex.getMessage()));
							}
						}else if (ia.getEntityType() != null){
							//search entity type
							try{
								attributeValue = convertValue(value, ia);
							}catch (Exception ex){
								attributeValue = null;
								warnings.add(MessageFormat.format("Error converting value ''{0}'' to valid entity: {1}", value, ex.getMessage()));
							}
						}
						
						if (attributeValue != null){
							attributeValue.setAttribute(ia);
							attributeValue.setRecord(record);
							record.getAttributes().add(attributeValue);
						}
						
					}
				}
				addedItems.add(record);
				
			}	//end of reading data
		}//close csv reader
		
		
		if (warnings.size() > 0){
			//confirm with user to continue
			if (!confirmContinue(warnings)){
				return null;
			}
		}
		monitor.worked(1);
		if (monitor.isCanceled()) return null;
		
		kidMonitor = monitor.newChild(1, 0);
		kidMonitor.setWorkRemaining(addedItems.size());
		//save change
		s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			//save new records
			for (IntelRecord i : addedItems){
				s.save(i);
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Error saving imported records to database. " + ex.getMessage(), ex);
			return null;
		}finally{
			s.close();
		}
		
		eventBroker.send(IntelEvents.RECORD_NEW, addedItems);
		return addedItems.size();
	}
	
	private IntelRecordAttributeValue convertValue(String strvalue, IntelRecordSourceAttribute attribute) throws Exception{
		if (strvalue.isEmpty()) return null;
		
		IntelRecordAttributeValue value = new IntelRecordAttributeValue();
		
		//search entities for name strvalue
		Session s = HibernateManager.openSession();
		try{
			IntelEntityType type = (IntelEntityType) s.get(IntelEntityType.class, attribute.getEntityType().getUuid());
			IntelAttribute ia = type.getIdAttribute();
			
			String hql = "SELECT v.id.entity FROM IntelEntityAttributeValue v join v.id.entity e WHERE v.id.attribute = :ia and e.entityType = :type and lower(v.stringValue) like :value ";
			Query query = s.createQuery(hql);
			query.setParameter("ia", ia);
			query.setParameter("type", type);
			query.setParameter("value", "%" + strvalue.toLowerCase() + "%");
			
			List<IntelEntity> entities = query.list();
			if (entities.size() > 1){
				throw new Exception(MessageFormat.format("Multiple entities of type {0} found with id {1}", type.getName(), strvalue));
			}else if (entities.size() == 0){
				throw new Exception(MessageFormat.format("No entities of type {0} found with id {1}", type.getName(), strvalue));
			}
			
			IntelRecordAttributeValueList item = new IntelRecordAttributeValueList();
			item.getId().setValue(value);
			item.getId().setElementUuid(entities.get(0).getUuid());
			value.setAttributeListItems(Collections.singletonList(item));
			return value;
		}finally{
			s.close();
		}
		
	}
	
	
	private IntelRecordAttributeValue convertValue(String strvalue, String strvalue2, IntelAttribute attribute, DateTimeFormatter dFormatter, MathTransform transform) throws Exception{
		if (strvalue.isEmpty()) return null;
		
		IntelRecordAttributeValue value = new IntelRecordAttributeValue();
		
		switch(attribute.getType()){
		case BOOLEAN:
			try{
				Boolean x = Boolean.parseBoolean(strvalue);
				if (x){
					value.setNumberValue(1d);
				}else{
					value.setNumberValue(0d);
				}
				return value;
			}catch (Exception ex){
				throw new Exception(MessageFormat.format("Invalid Boolean: {0}", strvalue));
			}
		case DATE:
			try{
				Date d = Date.from(LocalDate.parse(strvalue, dFormatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
				value.setDateValue(d);
				return value;
			}catch (Exception ex){
				throw new Exception(MessageFormat.format("Invalid Date: {0}", strvalue));
			}
		case LIST:
			for (IntelAttributeListItem i : attribute.getAttributeList()){
				for (Label l : i.getNames()){
					if (l.getValue().equalsIgnoreCase(strvalue)){
						IntelRecordAttributeValueList list = new IntelRecordAttributeValueList();
						list.getId().setElementUuid(i.getUuid());
						list.getId().setValue(value);
						value.setAttributeListItems(Collections.singletonList(list));
						return value;
					}
				}
			}
			throw new Exception(MessageFormat.format("Invalid list option {0} for attribute {1}", strvalue, attribute.getName()));
		case NUMERIC:
			try{
				Double dvalue = Double.parseDouble(strvalue);
				value.setNumberValue(dvalue);
				return value;
			}catch (Exception ex){
				throw new Exception(MessageFormat.format("Invalid Number: {0}", strvalue));
			}
		case POSITION:
			if (strvalue2 == null) throw new Exception("No Y value found for matching X value.");
			
			Double x = null;
			Double y = null;
			try{
				x = Double.parseDouble(strvalue);
			}catch (Exception ex){
				throw new Exception(MessageFormat.format("Invalid number for x position attribute: {0}", strvalue));
			}
			try{
				y = Double.parseDouble(strvalue2);
			}catch (Exception ex){
				throw new Exception(MessageFormat.format("Invalid number for y position  attribute: {0}", strvalue2));
			}
			
			Point p = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(x,y));
			p = (Point) JTS.transform(p, transform);
			value.setNumberValue(p.getX());
			value.setNumberValue2(p.getY());
			return value;
		case TEXT:
			value.setStringValue(strvalue);
			return value;
		}
		return null;
	}
	
	private boolean confirmContinue(List<String> warnings){
		boolean r[] = new boolean[]{false};
		Display.getDefault().syncExec(()->{
			WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), "Import Entitie", "The following warnings were generated while parsing data.  Do you want to continue with the import?", warnings,
					new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);	
			if (wd.open() == 0){
				r[0] = true;
			}
		});
		return r[0];	
	}
	
	private IntelAttributeListItem confirmAddListItem(String value, IntelAttribute attribute){
		IntelAttributeListItem[] item = new IntelAttributeListItem[]{null};
		Display.getDefault().syncExec(()->{
			
			if (MessageDialog.open(MessageDialog.QUESTION, Display.getDefault().getActiveShell(), "Attribute List Item", MessageFormat.format("No list item found with the value {0} for attribute {1}. Do you want to add this {2} to the list options?", value, attribute.getName(), value), SWT.NONE)){
				item[0] = new IntelAttributeListItem();
				item[0].setAttribute(attribute);
				item[0].setKeyId(DataModelManager.INSTANCE.generateKey(value, attribute.getAttributeList()));
				item[0].updateName(SmartDB.getCurrentLanguage(), value);
				item[0].updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), value);
				item[0].setName(value);
				attribute.getAttributeList().add(item[0]);
			}
		});
		return item[0];
	}
}
