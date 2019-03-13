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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.record.importer.RecordImportConfig.Column;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import au.com.bytecode.opencsv.CSVReader;

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
		SubMonitor monitor = SubMonitor.convert(pMonitor, Messages.RecordImportEngine_TaskName, 4);
		
		//ensure the file exists
		if (!Files.exists(config.getFile())) throw new FileNotFoundException(config.getFile().toString());
		
		CoordinateReferenceSystem fromCrs = ReprojectUtils.stringToCrs(config.getProjection().getDefinition());
		CoordinateReferenceSystem toCrs = GeometryUtils.SMART_CRS;
		MathTransform transform = CRS.findMathTransform(fromCrs, toCrs);
		
		
		monitor.subTask(Messages.RecordImportEngine_LoadingSubTaskName);
//		//load the attributes 
		Set<IntelRecordSource> sources = new HashSet<>();
		Collection<Employee> allEmployees = new ArrayList<>();
		try(Session s = HibernateManager.openSession()){
			sources.addAll( QueryFactory.buildQuery(s, IntelRecordSource.class, "conservationArea", SmartDB.getCurrentConservationArea()).getResultList() ); //$NON-NLS-1$
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
				allEmployees.addAll( QueryFactory.buildQuery(s, Employee.class, "conservationArea", SmartDB.getCurrentConservationArea()).list() ); //$NON-NLS-1$
				allEmployees.forEach(e->{e.getUuid(); SmartLabelProvider.getFullLabel(e);});
			});
			
		}
		monitor.worked(1);
		monitor.checkCanceled();

		List<IntelRecord> addedItems = new ArrayList<>();
		List<String> warnings = new ArrayList<String>();
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(config.getDateFormatString());
		
		monitor.subTask(Messages.RecordImportEngine_ReadingSubTask);
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
		monitor.checkCanceled();
		
		SubMonitor kidMonitor = monitor.split(1);
		kidMonitor.setWorkRemaining(lineCnt);
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(config.getFile()), config.getDelimiter())){
			kidMonitor.split(1);
			
			int line = 0;
			if (config.skipFirstLine()){
				reader.readNext();
				line++;
			}
			
			String[] data = null;

			while((data=reader.readNext())!=null ){
				line++;
				if (data.length < numcols){
					warnings.add(MessageFormat.format(Messages.RecordImportEngine_InvalidLine, line, data.length, (numcols+1)));
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
				//record date
				Integer primaryDateColumn = config.getMappedColumn(Column.PRIMARY_DATE);
				if (primaryDateColumn != null && !data[primaryDateColumn].trim().isEmpty()){
					try{
						record.setPrimaryDate( Date.from(LocalDate.parse(data[primaryDateColumn], dateFormatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
					}catch (Exception ex){
						warnings.add(MessageFormat.format(Messages.RecordImportEngine_DateParseError, data[primaryDateColumn], record.getTitle()));
					}
					
				}
				if (record.getPrimaryDate() == null) {
					record.setPrimaryDate(new Date());
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
								attributeValue = convertValue(value, value2, ia.getAttribute(), dateFormatter, transform, allEmployees);
							}catch (Exception ex){
								attributeValue = null;
								warnings.add(MessageFormat.format(Messages.RecordImportEngine_AttributeConversionError, value, ia.getAttribute().getName(), ex.getMessage()));
							}
						}else if (ia.getEntityType() != null){
							//search entity type
							try{
								attributeValue = convertValue(value, ia);
							}catch (Exception ex){
								attributeValue = null;
								warnings.add(MessageFormat.format(Messages.RecordImportEngine_EntityConversionError, value, ex.getMessage()));
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
		
		
		kidMonitor = monitor.split(1);
		kidMonitor.setWorkRemaining(addedItems.size()+1);
		//save change
		List<IntelRecord> toSave = new ArrayList<>();
		List<IntelRecord> toDelete = new ArrayList<>();
		List<IntelEntity> modifiedEntities = new ArrayList<>();
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			s.beginTransaction();
			try{
				kidMonitor.split(1);
				//check for duplicate names
				for (IntelRecord i : addedItems){
					Long cnt = QueryFactory.buildCountQuery(s, IntelRecord.class, 
							new Object[] {"conservationArea", i.getConservationArea()}, //$NON-NLS-1$
							new Object[] {"title", i.getTitle()}); //$NON-NLS-1$
					int action = confirmDuplicate(cnt, i.getTitle());
					if (action == 2) {
						//delete
						IntelRecord delete= QueryFactory.buildQuery(s, IntelRecord.class, 
								new Object[] {"conservationArea", i.getConservationArea()},  //$NON-NLS-1$
								new Object[] {"title", i.getTitle()}).uniqueResult();  //$NON-NLS-1$
						toDelete.add(delete);
						toSave.add(i);
					}else if (action == 1) {
						//save
						toSave.add(i);
					}else if (action == 0) {
						//skip
					}
				}
				
				kidMonitor.setWorkRemaining(toDelete.size() + toSave.size());
				for (IntelRecord i : toDelete) {
					kidMonitor.split(1);
					for (IntelEntityRecord e : i.getEntities()){
						modifiedEntities.add(e.getEntity());
					}
					RecordManager.INSTANCE.deleteRecord(i, s);
				}
				//save new records
				for (IntelRecord i : toSave){
					kidMonitor.split(1);
					s.save(i);
				}
				s.getTransaction().commit();
			}catch (OperationCanceledException ex) {
				s.getTransaction().rollback();
				return null;
			}catch (Exception ex){
				s.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(Messages.RecordImportEngine_SaveError + ex.getMessage(), ex);
				return null;
			}
		}
		if (!modifiedEntities.isEmpty()) eventBroker.send(IntelEvents.ENTITY_MODIFIED, modifiedEntities);
		if (!toDelete.isEmpty()) eventBroker.send(IntelEvents.RECORD_DELETE, toDelete);
		eventBroker.send(IntelEvents.RECORD_NEW, toSave);		
		return toSave.size();
	}


	private IntelRecordAttributeValue convertValue(String strvalue, IntelRecordSourceAttribute attribute) throws Exception{
		if (strvalue.isEmpty()) return null;
		
		IntelRecordAttributeValue value = new IntelRecordAttributeValue();
		
		//search entities for name strvalue
		try(Session s = HibernateManager.openSession()){

			IntelEntityType type = (IntelEntityType) s.get(IntelEntityType.class, attribute.getEntityType().getUuid());
			IntelAttribute ia = type.getIdAttribute();
			
			String hql = "SELECT v.id.entity FROM IntelEntityAttributeValue v join v.id.entity e WHERE v.id.attribute = :ia and e.entityType = :type and lower(v.stringValue) like :value "; //$NON-NLS-1$
			Query<IntelEntity> query = s.createQuery(hql, IntelEntity.class);
			query.setParameter("ia", ia); //$NON-NLS-1$
			query.setParameter("type", type); //$NON-NLS-1$
			query.setParameter("value", "%" + strvalue.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			List<IntelEntity> entities = query.list();
			if (entities.size() > 1){
				throw new Exception(MessageFormat.format(Messages.RecordImportEngine_MultipleEntitiesFound, type.getName(), strvalue));
			}else if (entities.size() == 0){
				throw new Exception(MessageFormat.format(Messages.RecordImportEngine_NoEntitiesFound, type.getName(), strvalue));
			}
			
			IntelRecordAttributeValueList item = new IntelRecordAttributeValueList();
			item.getId().setValue(value);
			item.getId().setElementUuid(entities.get(0).getUuid());
			value.setAttributeListItems(Collections.singletonList(item));
			return value;
		}
		
	}
	
	
	private IntelRecordAttributeValue convertValue(String strvalue, String strvalue2, IntelAttribute attribute, DateTimeFormatter dFormatter, MathTransform transform, Collection<Employee> allEmployees) throws Exception{
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
				throw new Exception(MessageFormat.format(Messages.RecordImportEngine_InvalidBoolean, strvalue));
			}
		case DATE:
			try{
				Date d = Date.from(LocalDate.parse(strvalue, dFormatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
				value.setDateValue(d);
				return value;
			}catch (Exception ex){
				throw new Exception(MessageFormat.format(Messages.RecordImportEngine_InvalidDate, strvalue));
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
			throw new Exception(MessageFormat.format(Messages.RecordImportEngine_InvalidListItem, strvalue, attribute.getName()));
		case EMPLOYEE:
			for (Employee e : allEmployees) {
				if(strvalue.equalsIgnoreCase(SmartLabelProvider.getFullLabel(e))) {
					IntelRecordAttributeValueList list = new IntelRecordAttributeValueList();
					list.getId().setElementUuid(e.getUuid());
					list.getId().setValue(value);
					value.setAttributeListItems(Collections.singletonList(list));
					return value;
				}
			}
			throw new Exception(MessageFormat.format(Messages.RecordImportEngine_EmployeeNotFound, strvalue, attribute.getName()));
		case NUMERIC:
			try{
				Double dvalue = Double.parseDouble(strvalue);
				value.setNumberValue(dvalue);
				return value;
			}catch (Exception ex){
				throw new Exception(MessageFormat.format(Messages.RecordImportEngine_InvalidNumber, strvalue));
			}
		case POSITION:
			if (strvalue2 == null) throw new Exception(Messages.RecordImportEngine_MissingYValue);
			
			Double x = null;
			Double y = null;
			try{
				x = Double.parseDouble(strvalue);
			}catch (Exception ex){
				throw new Exception(MessageFormat.format(Messages.RecordImportEngine_InvalidX, strvalue));
			}
			try{
				y = Double.parseDouble(strvalue2);
			}catch (Exception ex){
				throw new Exception(MessageFormat.format(Messages.RecordImportEngine_InvalidY, strvalue2));
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
			WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), Messages.RecordImportEngine_WarningTitle, Messages.RecordImportEngine_WarningMessage, warnings,
					new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);	
			if (wd.open() == 0){
				r[0] = true;
			}
		});
		return r[0];	
	}

	/**
	 * 
	 * @param cnt number of duplicate records
	 * @param title title
	 * @return 2 - overwrite existing record, 1 - save without changing, 0 - skip
	 */
	public static int confirmDuplicate(long cnt, String title){
		if (cnt == 0) return 1;
		
		int r[] = new int[]{1};
		Display.getDefault().syncExec(()->{
			if (cnt == 1) {
				MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(), Messages.RecordImportEngine_DuplicateTitle, null, 
						 MessageFormat.format(Messages.RecordImportEngine_SingleRecordExists, title),
						MessageDialog.QUESTION, 1, new String[] {Messages.RecordImportEngine_OverwriteBtn, DialogConstants.SAVE_TEXT, Messages.RecordImportEngine_SkipBtn});
				int open = md.open();
				if (open == 0) {
					r[0] = 2;
				}else if (open == 1) {
					r[0] = 1;
				}else {
					r[0] = 0;
				}
			}else {
				MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(), Messages.RecordImportEngine_DuplicateTitle, null, 
						 MessageFormat.format(Messages.RecordImportEngine_MultiRecordsExist, cnt, title),
						MessageDialog.QUESTION, 0, new String[] {DialogConstants.SAVE_TEXT, Messages.RecordImportEngine_SkipBtn});
				if (md.open() == 0) {
					r[0] = 1;
					return;
				}
				r[0] = 0;
				return;
			}
		});
		return r[0];	
	}

}
