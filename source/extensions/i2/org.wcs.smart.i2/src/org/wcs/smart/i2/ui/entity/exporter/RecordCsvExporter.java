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
package org.wcs.smart.i2.ui.entity.exporter;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.config.ICsvExportDialogConfig;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.GeometryUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Exports a set of records to a csv file. 
 * 
 * @author Emily
 *
 */
public class RecordCsvExporter implements ICsvDataExporter {

	private static final String LAST_FILE_KEY = "org.wcs.smart.i2.ui.entity.exporter.RecordCsvExport.LAST_FILE"; //$NON-NLS-1$
	
	private List<UUID> uuids = null;
	
	public RecordCsvExporter(List<UUID> recordsToExport){
		this.uuids = recordsToExport;
	}
	
	@Override
	public boolean exportCsvFile(Path file, char delimiter,
			ConservationArea ca, boolean writeHeaders, Charset cs, IProgressMonitor monitor,
			Session session) throws Exception {
		
		//update last file name
		String lastFile = file.getFileName().toString();
		if (lastFile.lastIndexOf('.') > 0){
			lastFile = lastFile.substring(0, lastFile.lastIndexOf('.'));
		}
		Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(LAST_FILE_KEY, lastFile);
		
		
		monitor.beginTask(Messages.RecordCsvExporter_TaskName, uuids.size() + 2);
		monitor.worked(1);
		
		monitor.subTask(Messages.RecordCsvExporter_SubTask1);
		String hql = "SELECT distinct atts from IntelRecordSource s join s.attributes atts WHERE s.conservationArea = :ca"; //$NON-NLS-1$
		Query<IntelRecordSourceAttribute> q = session.createQuery(hql, IntelRecordSourceAttribute.class);
		q.setParameter("ca", ca); //$NON-NLS-1$
		
		List<IntelRecordSourceAttribute> attributes = q.list();
		
		
		List<String> headers = new ArrayList<>();

		headers.add(Messages.RecordCsvExporter_TitleColumn);
		headers.add(Messages.RecordCsvExporter_PrimaryDateColumn);
		headers.add(Messages.RecordCsvExporter_DateCreatedColumn);
		headers.add(Messages.RecordCsvExporter_DateModifiedColumn);
		headers.add(Messages.RecordCsvExporter_StatusColumn);
		headers.add(Messages.RecordCsvExporter_Sourcecolumn);
		headers.add(Messages.RecordCsvExporter_ProfileColumn);
		for (IntelRecordSourceAttribute ia : attributes){
			
			String name = ia.getName();
			
			if (ia.getAttribute() != null && ia.getAttribute().getType() == AttributeType.POSITION) {
				if (name == null) name = ia.getAttribute().getName();
				headers.add(MessageFormat.format("{0} X ({1})", name, ia.getSource().getName())); //$NON-NLS-1$
				headers.add(MessageFormat.format("{0} Y ({1})", name, ia.getSource().getName())); //$NON-NLS-1$
				headers.add(MessageFormat.format("{0} Geometry ({1})", name, ia.getSource().getName())); //$NON-NLS-1$
			}else {
				if (name == null){
					if (ia.getAttribute() != null){
						name = ia.getAttribute().getName();
					}else if (ia.getEntityType() != null){
						name = ia.getEntityType().getName();
					}
				}
				headers.add(MessageFormat.format("{0}({1})", name, ia.getSource().getName())); //$NON-NLS-1$
			}
			
			

		}
		
		IIntelligenceLabelProvider ll = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class);
		monitor.worked(1);
		monitor.subTask(Messages.RecordCsvExporter_SubTask2);
		try(CSVWriter writer = new CSVWriter(Files.newBufferedWriter(file, cs), delimiter)){
			
			writer.writeNext(headers.toArray(new String[headers.size()]));
			
			for (UUID uuid : uuids){
				IntelRecord r = (IntelRecord)session.get(IntelRecord.class, uuid);
				if (r == null) continue;
				headers.clear();

				headers.add(r.getTitle());
				headers.add(DateFormat.getDateInstance().format(r.getPrimaryDate()));
				headers.add(DateFormat.getDateInstance().format(r.getDateCreated()));
				headers.add(DateFormat.getDateInstance().format(r.getDateModified()));
				headers.add(ll.getLabel(r.getStatus(), Locale.getDefault()));
				headers.add(r.getRecordSource() == null ? "" : r.getRecordSource().getName()); //$NON-NLS-1$
				headers.add(r.getProfile().getName());
				
				for (IntelRecordSourceAttribute ia : attributes){
					IntelRecordAttributeValue found = null;
					for (IntelRecordAttributeValue v : r.getAttributes()){
						if (v.getAttribute().equals(ia)){
							found = v;
							break;
						}
					}
					
					if (found == null) {
						headers.add(null);
						if (ia.getAttribute() != null && ia.getAttribute().getType() == AttributeType.POSITION) {
							headers.add(null);
							headers.add(null);
						}
					}else {
						if (ia.isListAttribute()){
							StringBuilder sb = new StringBuilder();
							sb.append("("); //$NON-NLS-1$
							sb.append(found.getAttributeValueAsString(Locale.getDefault(), GeometryUtils.SMART_CRS));
							sb.append(") "); //$NON-NLS-1$
								
							for ( IntelRecordAttributeValueList  listItem : found.getAttributeListItems()){
								if (ia.getAttribute() != null){
									if (ia.getAttribute().getType() == AttributeType.LIST) {
										IntelAttributeListItem list = (IntelAttributeListItem) session.get(IntelAttributeListItem.class, listItem.getId().getElementUuid());
										sb.append(list.getName());
									}else if (ia.getAttribute().getType() == AttributeType.EMPLOYEE) {
										Employee e = (Employee) session.get(Employee.class, listItem.getId().getElementUuid());
										sb.append(SmartLabelProvider.getFullLabel(e));
									}
								}else if (ia.getEntityType() != null){
									IntelEntity list = (IntelEntity) session.get(IntelEntity.class, listItem.getId().getElementUuid());
									sb.append(list.getIdAttributeAsText());
								}
								sb.append(","); //$NON-NLS-1$
							}
							sb.deleteCharAt(sb.length() - 1);
							headers.add(sb.toString());
						}else if(ia.getAttribute() != null && ia.getAttribute().getType() == AttributeType.POSITION) {
							headers.add(found.getNumberValue().toString());
							headers.add(found.getNumberValue2().toString());
							headers.add(found.getAttributeValueAsString(Locale.getDefault(), GeometryUtils.SMART_CRS));
						}else{
							headers.add(found.getAttributeValueAsString(Locale.getDefault(), GeometryUtils.SMART_CRS));
						}
					}
				}
				writer.writeNext(headers.toArray(new String[headers.size()]));
				monitor.worked(1);
			}	
		}
		monitor.done();
		return true;
	}

	public ICsvExportDialogConfig createExportConfiguration(){
		return new ICsvExportDialogConfig() {
			
			@Override
			public boolean includeHasHeader() {
				return false;
			}
			
			@Override
			public String getTitle() {
				return Messages.RecordCsvExporter_ConfigTitle;
			}
			
			@Override
			public String getSuccessMessage() {
				return Messages.RecordCsvExporter_SuccessMsg;
			}
			
			@Override
			public String getMessage() {
				return Messages.RecordCsvExporter_ProgressMsg;
			}
			
			@Override
			public String getInfo() {
				return null;
			}
			
			@Override
			public String getHasHeaderText() {
				return null;
			}
			
			@Override
			public int getFileDialogStyle() {
				return SWT.SAVE;
			}
			
			@Override
			public String getFailMessage() {
				return Messages.RecordCsvExporter_ErrorMsg;
			}
			
			@Override
			public String getDefaultFileName() {
				String lastFile = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(LAST_FILE_KEY);
				if (lastFile == null || lastFile.isEmpty()) return "records"; //$NON-NLS-1$
				return lastFile;
			}
			
			@Override
			public String getActionButtonText() {
				return IDialogConstants.OK_LABEL;
			}
			
			@Override
			public boolean appendFileExtension() {
				return true;
			}
			
			@Override
			public ICsvDataExporter getExporter() {
				return RecordCsvExporter.this;
			}
		};
	
	}
}
