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
package org.wcs.smart.i2.birt;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.entity.EntityDataset;
import org.wcs.smart.i2.birt.entity.attachment.EntityAttachmentDataset;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDataset;
import org.wcs.smart.i2.birt.entity.records.EntityRecordDataset;
import org.wcs.smart.i2.birt.entity.relation.EntityRelationDataset;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.util.UuidUtils;


/**
 *
 */
public enum IntelReportManager {

	INSTANCE;
	
	public enum Format{
			DOC("DOCX", "org.eclipse.birt.report.engine.emitter.word"),
			PDF("PDF", "org.eclipse.birt.report.engine.emitter.pdf");
			
			public String id;
			public String emitter;
			
			Format(String id, String emitter){
				this.id = id;
				this.emitter = emitter;
			}
	}
	
	public Path getEntityTemplate(IntelEntityType entityType){
		return FileSystems.getDefault().getPath(
				entityType.getConservationArea().getFileDataStoreLocation(),
				IntelAttachment.INTELLIGENCE_FS_DIR,
				"entitytypes",
				entityType.getBirtTemplate());
	}
	public Path getTemporaryDirectory(){
		return FileSystems.getDefault().getPath(
				SmartDB.getCurrentConservationArea().getFileDataStoreLocation(),
				IntelAttachment.INTELLIGENCE_FS_DIR + "_TEMP");
	}
	
	public void exportEntity(IntelEntity entity, Date[] dFilter, EmitterInfo format){
		EntityExportReportJob job = new EntityExportReportJob(entity, dFilter, format);
		job.schedule();
	}
	
	/**
	 * Returns the dataset name for the given entity type and dataset
	 * id.
	 * 
	 * @param entityType
	 * @param dataSetId
	 * @return
	 */
	public String getName(IntelEntityType entityType, String dataSetId){
		if (dataSetId.equals(EntityDataset.DATASET_TYPE)){
			return entityType.getName();
		}else if (dataSetId.equals(EntityLocationDataset.DATASET_TYPE)){
			return MessageFormat.format("{0} - Locations", entityType.getName());
		}else if (dataSetId.equals(EntityRelationDataset.DATASET_TYPE)){
			return MessageFormat.format("{0} - Relationships", entityType.getName());
		}else if (dataSetId.equals(EntityAttachmentDataset.DATASET_TYPE)){
			return MessageFormat.format("{0} - Attachments", entityType.getName());
		}else if (dataSetId.equals(EntityRecordDataset.DATASET_TYPE)){
			return MessageFormat.format("{0} - Records", entityType.getName());
		}
		return null;
	}
	
	/**
	 * Edits the plan template
	 * @param event
	 */
	public void editTemplate(IntelEntityType entityType){
		try{
			if (entityType.getBirtTemplate() == null){
				//create a new template for entity type
				String file = entityType.getKeyId() + "." + UuidUtils.uuidToString(entityType.getUuid()) + ".rptdesign";
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					entityType = (IntelEntityType) s.get(IntelEntityType.class, entityType.getUuid());
					entityType.setBirtTemplate(file);
					s.getTransaction().commit();
				}catch (Exception ex){
					s.getTransaction().rollback();
					Intelligence2PlugIn.displayLog(MessageFormat.format("Unable to edit template for entity type {0}. {1}", entityType.getName(),  ex.getMessage()), ex);
				}finally{
					s.close();
				}
			}
			
			Path p = getEntityTemplate(entityType);
			if (!Files.exists(p.getParent())){
				Files.createDirectory(p.getParent());
			}
			if (!Files.exists(p)){
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					entityType = (IntelEntityType) s.get(IntelEntityType.class, entityType.getUuid());
					if (entityType.getAttributes() != null){
						entityType.getAttributes().forEach((a) ->a.getAttribute().getName());
					}
					s.getTransaction().commit();
				}catch (Exception ex){
					s.getTransaction().rollback();
					Intelligence2PlugIn.displayLog(MessageFormat.format("Unable to edit template for entity type {0}. {1}", entityType.getName(),  ex.getMessage()), ex);
				}finally{
					s.close();
				}
				EntityReportGenerator.INSTANCE.generateReport(entityType, p);
			}
			
			PlatformUI.getWorkbench().showPerspective(IntelEntityReportPerspective.ID, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			IntelEntityEditorInput input = new IntelEntityEditorInput(p.toFile(), entityType);
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, IReportEditorContants.DESIGN_EDITOR_ID);
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error opening entity printing template." + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return;
		}
	}


}
