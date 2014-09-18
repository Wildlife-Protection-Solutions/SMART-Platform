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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.entity.ccca.EntityTypeCcaaManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.config.ICsvExportDialogConfig;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Exporter for exporting entities to a csv file.
 * 
 * @author Emily
 *
 */
public class EntityCsvExporter implements ICsvDataExporter {

	private EntityType entityType;
	private boolean activeOnly;
	private EntityDialogConfig config;
	
	public EntityCsvExporter(EntityType et){
		this.entityType = et;
		this.config = new EntityDialogConfig();
	}
	
	public EntityDialogConfig getDialogConfiguration(){
		return this.config;
	}
	
	/**
	 * 
	 * @return the entity type being exported
	 */
	public EntityType getEntityType(){
		return this.entityType;
	}
	
	public void setActiveOnly(boolean activeOnly){
		this.activeOnly = activeOnly;
		
	}
	
	@Override
	public boolean exportCsvFile(File file, char delimiter, ConservationArea ca,
			boolean headers, IProgressMonitor monitor, Session session)
			throws Exception {
		
		
		
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(
					new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), //$NON-NLS-1$ 
					delimiter, '"',SmartUtils.LINE_SEPARATOR); 
			
			List<Entity> stations = getEntities(session, activeOnly);

			// WriteHeaders
			int extra = 2;
			if (entityType.getType().equals(EntityType.Type.FIXED)){
				extra = 4;
			}
			if (SmartDB.isMultipleAnalysis()){
				extra++;
			}
			
			String[] columns = new String[entityType.getAttributes().size() + extra];
			columns[0] = Entity.ID_FIELD_NAME;
			columns[1] = Entity.STATUS_FIELD_NAME;
			if (entityType.getType().equals(EntityType.Type.FIXED)){
				columns[2] = Entity.X_FIELD_NAME;
				columns[3] = Entity.Y_FIELD_NAME;
			}
			if (SmartDB.isMultipleAnalysis()){
				columns[extra-1] = Entity.CA_FIELD_NAME;
			}
			int i = 0;
			for (EntityAttribute ea : entityType.getAttributes()){
				columns[i + extra] = ea.getName();
				i++;
			}
			writer.writeNext(columns);
			
			//for each station write one record
			for (Entity entity : stations) {
				if (monitor.isCanceled()) return false;
				
				// entry in string array (csv_out) of names
				i = 0;
				String csvout[] = new String[columns.length];
				csvout[0] = entity.getId();
				csvout[1] = entity.getStatus().getGuiName();
				if (entityType.getType().equals(EntityType.Type.FIXED)){
					csvout[2] = String.valueOf(entity.getX());
					csvout[3] = String.valueOf(entity.getY());
				}
				if (SmartDB.isMultipleAnalysis()){
					csvout[extra-1] = entity.getEntityType().getConservationArea().getId();
				}
				for (EntityAttribute ea : entityType.getAttributes()){
					for (EntityAttributeValue v : entity.getAttributes()){
						if (v.getEntityAttribute().getKeyId().equals(ea.getKeyId())){
							csvout[i + extra] = v.getValueAsString();
							break;
						}
					}
					i++;
				}
				
				writer.writeNext(csvout);
			}
			writer.close();
			return true;
		} catch (IOException ex) {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				return false;
			}
			throw ex;
		}
	}

	
	
	@SuppressWarnings("unchecked")
	private List<Entity> getEntities(Session session, boolean onlyActive) {
		if (SmartDB.isMultipleAnalysis()){
			return EntityTypeCcaaManager.getInstance().getEntities(entityType.getKeyId(), session);
		}
		
		session.beginTransaction();
		try{
			Criteria c = session.createCriteria(Entity.class).add(Restrictions.eq("entityType", entityType)); //$NON-NLS-1$
			if (onlyActive){
				c = c.add(Restrictions.eq("status", Entity.Status.ACTIVE)); //$NON-NLS-1$
			}
			return c.list();
		}finally{
			session.getTransaction().rollback();
		}
	}

	
	class EntityDialogConfig implements ICsvExportDialogConfig{

		@Override
		public boolean includeHasHeader() {
			return false;
		}

		@Override
		public String getDefaultFileName(){
			return SmartDB.getCurrentConservationArea().getId() + "_entities"; //$NON-NLS-1$
		}
		
		@Override
		public String getHasHeaderText() {
			return null;
		}

		@Override
		public String getInfo() {
			return null;
		}

		@Override
		public String getTitle() {
			return Messages.EntityCsvExporter_ExporterTitle;
		}

		@Override
		public String getMessage() {
			return Messages.EntityCsvExporter_ExporterMessage;
		}

		@Override
		public String getSuccessMessage() {
			return Messages.EntityCsvExporter_ExportOk;
		}

		@Override
		public String getFailMessage() {
			return Messages.EntityCsvExporter_ExportFile;
		}

		@Override
		public String getActionButtonText() {
			return DialogConstants.EXPORT_BUTTON_TEXT;
		}

		@Override
		public int getFileDialogStyle() {
			return SWT.SAVE;
		}

		@Override
		public boolean appendFileExtension() {
			return true;
		}

		@Override
		public ICsvDataExporter getExporter() {
			return EntityCsvExporter.this;
		}
		
	}
}