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

import java.io.File;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.config.ICsvExportDialogConfig;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
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

	private List<UUID> uuids = null;
	
	public RecordCsvExporter(List<UUID> recordsToExport){
		this.uuids = recordsToExport;
	}
	
	
	@Override
	public boolean exportCsvFile(File file, char delimiter,
			ConservationArea ca, boolean headers, IProgressMonitor monitor,
			Session session) throws Exception {
		monitor.beginTask("Export records to csv file", uuids.size() + 2);
		monitor.worked(1);
		
		monitor.subTask("Loading record attributes...");
		String hql = "SELECT distinct atts from IntelRecordSource s join s.attributes atts WHERE s.conservationArea = :ca";
		Query q = session.createQuery(hql);
		q.setParameter("ca", ca);
		
		List<IntelRecordSourceAttribute> attributes = q.list();
		
		
		String[] data = new String[attributes.size() + 7];
		int i = 0;
		data[i++] = "Title";
		data[i++] = "Date Created";
		data[i++] = "Date Modified";
		data[i++] = "Created By";
		data[i++] = "Modified By";
		data[i++] = "Status";
		data[i++] = "Source";
		for (IntelRecordSourceAttribute ia : attributes){
			String name = ia.getName();
			if (name == null){
				if (ia.getAttribute() != null){
					name = ia.getAttribute().getName();
				}else if (ia.getEntityType() != null){
					name = ia.getEntityType().getName();
				}
			}
			
			data[i++] = name + "(" + ia.getSource().getName() + ")";
		}
		
		IIntelligenceLabelProvider ll = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class);
		monitor.worked(1);
		monitor.subTask("Exporting records...");
		try(CSVWriter writer = new CSVWriter(Files.newBufferedWriter(file.toPath()), delimiter)){
			writer.writeNext(data);
			
			data = new String[data.length];
			for (UUID uuid : uuids){
				IntelRecord r = (IntelRecord)session.get(IntelRecord.class, uuid);
				if (r == null) continue;
				i = 0;
				data[i++] = r.getTitle();
				data[i++] = DateFormat.getDateInstance().format(r.getDateCreated());
				data[i++] = DateFormat.getDateInstance().format(r.getDateModified());
				data[i++] = SmartLabelProvider.getShortLabel(r.getCreatedBy());
				data[i++] = SmartLabelProvider.getShortLabel(r.getLastModifiedBy());
				data[i++] = ll.getLabel(r.getStatus(), Locale.getDefault());
				data[i++] = r.getRecordSource() == null ? "" : r.getRecordSource().getName();
				
				for (IntelRecordSourceAttribute ia : attributes){
					data[i++] = null;
					for (IntelRecordAttributeValue v : r.getAttributes()){
						if (v.getAttribute().equals(ia)){
							if (ia.isListAttribute()){
								StringBuilder sb = new StringBuilder();
								sb.append("(");
								sb.append(v.getAttributeValueAsString(Locale.getDefault(), GeometryUtils.SMART_CRS));
								sb.append(") ");
								
								for ( IntelRecordAttributeValueList  listItem : v.getAttributeListItems()){
									if (ia.getAttribute() != null){
										IntelAttributeListItem list = (IntelAttributeListItem) session.get(IntelAttributeListItem.class, listItem.getId().getElementUuid());
										sb.append(list.getName());
									}else if (ia.getEntityType() != null){
										IntelEntity list = (IntelEntity) session.get(IntelEntity.class, listItem.getId().getElementUuid());
										sb.append(list.getIdAttributeAsText());
									}
									sb.append(",");
								}
								sb.deleteCharAt(sb.length() - 1);
								data[i-1] = sb.toString();
							}else{
								data[i-1] = v.getAttributeValueAsString(Locale.getDefault(), GeometryUtils.SMART_CRS);
							}
							break;
						}
					}
				}
				writer.writeNext(data);
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
				return "Export Records";
			}
			
			@Override
			public String getSuccessMessage() {
				return "Records exported successfully";
			}
			
			@Override
			public String getMessage() {
				return "Export intelligence records to csv file.";
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
				return "Failed to export records to file";
			}
			
			@Override
			public String getDefaultFileName() {
				return "records";
			}
			
			@Override
			public String getActionButtonText() {
				return "records";
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
