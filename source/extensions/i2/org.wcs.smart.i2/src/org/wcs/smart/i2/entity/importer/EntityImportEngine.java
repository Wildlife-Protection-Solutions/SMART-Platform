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
package org.wcs.smart.i2.entity.importer;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Import entity engine.
 * 
 * @author Emily
 *
 */
public enum EntityImportEngine {
	
	INSTANCE;
	
	/**
	 * 
	 * @param config
	 * @param pMonitor
	 * @return the number of entities imported or null if nothing imported
	 * @throws Exception
	 */
	public Integer importEntities(EntityImportConfig config, IEventBroker eventBroker, IProgressMonitor pMonitor) throws Exception{
		SubMonitor monitor = SubMonitor.convert(pMonitor);
		
		monitor.beginTask("Importing Entities", 5);
		//ensure the file exists
		if (!Files.exists(config.getFile())) throw new FileNotFoundException(config.getFile().toString());
		
		monitor.subTask("Loading attribute information");
		//load the attributes 
		Set<IntelAttribute> attributes = new HashSet<>();
		Session s = HibernateManager.openSession();
		try{
			for (IntelAttribute a : config.getMappedAttributes()){
				IntelAttribute attribute = (IntelAttribute) s.get(IntelAttribute.class, a.getUuid());
				attribute.getType();
				attribute.getName();
				if (attribute.getAttributeList() != null){
					attribute.getAttributeList().forEach(l -> l.getName());
				}
				attributes.add(attribute);
			}
		}finally{
			s.close();
		}
		monitor.worked(1);
		if (monitor.isCanceled()) return null;
		
		List<IntelEntity> newEntities = new ArrayList<>();
		List<IntelAttributeListItem> addedItems = new ArrayList<>();
		List<String> warnings = new ArrayList<String>();
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(config.getDateFormatString());
		
		monitor.subTask("Reading csv file");
		int lineCnt = 0;
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(config.getFile()), config.getDelimiter())){
			lineCnt = reader.readAll().size();
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
			int numcols = config.getMaxColumn();
			while((data=reader.readNext())!=null ){
				line++;
				if (data.length < numcols+1){
					warnings.add(MessageFormat.format("Invalid line {0}. The number of columns ({1}) is less than the required number of columns ({2}).  Line will be ignored.", line, data.length, (numcols+1)));
					continue;
				}
				
				IntelEntity entity = new IntelEntity();
				entity.setConservationArea(SmartDB.getCurrentConservationArea());
				entity.setEntityType(config.getType());
				entity.setAttributes(new ArrayList<IntelEntityAttributeValue>());
				newEntities.add(entity);
				
				
				for (IntelAttribute a : attributes){
					IntelEntityAttributeValue avalue = new IntelEntityAttributeValue();
					avalue.setAttribute(a);
					avalue.setEntity(entity);
					
					int columnIndex = config.getColumn(a);
					boolean add = false;
					String value = data[columnIndex];
					if (value.trim().isEmpty()) continue;	//skip empty lines
					switch(a.getType()){
					case BOOLEAN:
						Boolean bvalue = null;
						if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")){
							bvalue = true;
						}else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")){
							bvalue = false;
						}else{
							try{
								Integer i = Integer.parseInt(value);
								if (i == 0){
									bvalue = false;
								}else if (i == 1){
									bvalue = true;
								}
							}catch (Exception ex){
								
							}
							
						}
						if (bvalue != null){
							avalue.setNumberValue(bvalue ? 1d : 0d);
							add = true;
							break;
						}
						warnings.add(MessageFormat.format("Cannot convert the value {0} to valid boolean.  Attribute will not be imported. (Line {1} Attribute {2})", value, line, a.getName()));
						break;
					case DATE:
						try{
							Date d = Date.from(LocalDate.parse(value, dateFormatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
							avalue.setDateValue(d);
							add = true;
						}catch (Exception ex){
							ex.printStackTrace();
							warnings.add(MessageFormat.format("Cannot convert the value {0} to valid date.  Attribute will not be imported. (Line {1} Attribute {2})", value, line, a.getName()));	
						}
						break;
					case LIST:
						//search names
						for (IntelAttributeListItem i : a.getAttributeList()){
							if (i.getName().equalsIgnoreCase(value)){
								avalue.setAttributeListItem(i);
								add = true;
								break;
							}
						}
						//search keys
						if (avalue.getAttributeListItem() == null){
							for (IntelAttributeListItem i : a.getAttributeList()){
								if (i.getKeyId().equalsIgnoreCase(value)){
									avalue.setAttributeListItem(i);
									add = true;
									break;
								}
							}
						}
						//not found ask the user if they want to add it
						if (avalue.getAttributeListItem() == null){
							IntelAttributeListItem addedItem = confirmAddListItem(value, a);
							if (addedItem != null){
								avalue.setAttributeListItem(addedItem);
								addedItems.add(addedItem);
								add = true;
								break;	
							}
							warnings.add(MessageFormat.format("Cannot find a list value for value ''{0}''.  Attribute will not be imported. (Line {1} Attribute {2})", value, line, a.getName()));
						}
						
						break;
					case NUMERIC:
						try{
							Double d = Double.parseDouble(value);
							avalue.setNumberValue(d);
							add = true;
						}catch (NumberFormatException e ){
							//cannot parse a value; skip this attribute
							warnings.add(MessageFormat.format("Cannot convert the value {0} to a number. Attribute will not be imported. (Line {1} Attribute {2})", value, line, a.getName()));
						}
						break;
					case TEXT:
						avalue.setStringValue(value);
						add=true;
						break;

					}
					
					if (add){
						entity.getAttributes().add(avalue);
					}
				}
				
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
		kidMonitor.setWorkRemaining(newEntities.size());
		//save change
		s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			//save new list items
			for (IntelAttributeListItem i : addedItems){
				s.save(i);
				s.saveOrUpdate(i.getAttribute());
			}
			
			//save new entities
			for (IntelEntity e : newEntities){
				s.save(e);
				kidMonitor.worked(1);
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Error saving imported entities to database. " + ex.getMessage(), ex);
			return null;
		}finally{
			s.close();
		}
		
		eventBroker.send(IntelEvents.ENTITY_NEW, newEntities);
		return newEntities.size();
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
