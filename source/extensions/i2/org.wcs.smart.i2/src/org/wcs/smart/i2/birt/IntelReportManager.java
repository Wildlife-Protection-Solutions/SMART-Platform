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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.birt.ColumnBindingFixer;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.entity.EntityDataset;
import org.wcs.smart.i2.birt.entity.attachment.EntityAttachmentDataset;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDataset;
import org.wcs.smart.i2.birt.entity.records.EntityRecordDataset;
import org.wcs.smart.i2.birt.entity.relation.EntityRelationDataset;
import org.wcs.smart.i2.birt.record.RecordDataset;
import org.wcs.smart.i2.birt.record.attachment.RecordAttachmentDataset;
import org.wcs.smart.i2.birt.record.entities.RecordEntityDataset;
import org.wcs.smart.i2.birt.record.location.RecordLocationDataset;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.util.E3Utils;
import org.wcs.smart.util.UuidUtils;


/**
 * BIRT utilities to support entity and intelligence record exports.
 */
public enum IntelReportManager {

	INSTANCE;

	/**
	 * The BIRT template for intelligence records for the given conservation area.
	 * 
	 * @param ca
	 * @return 
	 */
	public Path getRecordTemplate(ConservationArea ca){
		return FileSystems.getDefault().getPath(
				ca.getFileDataStoreLocation(),
				IntelAttachment.INTELLIGENCE_FS_DIR,
				"record" + IReportEditorContants.DESIGN_FILE_EXTENTION);
	}
	

	/**
	 * The BIRT template for a given entity type.
	 * 
	 * @param ca
	 * @return 
	 */
	public Path getEntityTemplate(IntelEntityType entityType){
		if (entityType.getBirtTemplate() == null) return null;
		return FileSystems.getDefault().getPath(
				entityType.getConservationArea().getFileDataStoreLocation(),
				IntelAttachment.INTELLIGENCE_FS_DIR,
				"entitytypes",
				entityType.getBirtTemplate());
	}
	
	/**
	 * Temporary directory for intelligence files.  This directory is emptied
	 * when the application is started up.
	 * 
	 * @return
	 */
	public Path getTemporaryDirectory(){
		return FileSystems.getDefault().getPath(
				SmartDB.getCurrentConservationArea().getFileDataStoreLocation(),
				IntelAttachment.INTELLIGENCE_FS_DIR + "_TEMP");
	}
	
	/**
	 * Exports an entity to the given format.
	 * 
	 * @param entity the entity to export 
	 * @param dFilter the date filter to use
	 * @param format the export format
	 */
	public void exportEntity(IntelEntity entity, Date[] dFilter, EmitterInfo format){
		EntityExportReportJob job = new EntityExportReportJob(entity, dFilter, format);
		job.schedule();
	}
	
	/**
	 * Export the intelligence record to the specified format
	 * @param record
	 * @param format
	 */
	public void exportRecord(IntelRecord record, EmitterInfo format){
		RecordExportJob job = new RecordExportJob(record, format);
		job.schedule();
	}
	
	/**
	 * Returns the dataset name for the given entity type and dataset
	 * id.
	 * 
	 * @param entityType optional but should be supplied for entity related datasets
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
		}else if (dataSetId.equals(RecordDataset.DATASET_TYPE)){
			return "Record Details";
		}else if (dataSetId.equals(RecordEntityDataset.DATASET_TYPE)){
			return "Record Entities";
		}else if (dataSetId.equals(RecordLocationDataset.DATASET_TYPE)){
			return "Record Locations";
		}else if (dataSetId.equals(RecordAttachmentDataset.DATASET_TYPE)){
			return "Record Attachments";
		}
		return dataSetId;
	}
	
	/**
	 * Call from display thread.  Resets the template for the given entity type
	 * 
	 * @param entityType
	 * @param service
	 */
	public void resetTemplate(IntelEntityType entityType, EPartService service){
		if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Reset", 
				MessageFormat.format("Are you sure you want to reset the printing template for entity type {0}?  You will loose any changes you have made to this template.", entityType.getName()))){
			return;
		}
		Session s = HibernateManager.openSession();
		try{
			entityType = (IntelEntityType) s.get(IntelEntityType.class, entityType.getUuid());
		}finally{
			s.close();
		}
			
		if (entityType.getBirtTemplate() != null){
			for (MPart p : service.getParts()){
				Object x = E3Utils.getSourceObject(p);
				if (x instanceof IEditorPart && 
						((IEditorPart)x).getEditorInput() instanceof IntelEntityTypeEditorInput &&
						((IntelEntityTypeEditorInput)((IEditorPart)x).getEditorInput()).getEntityType().equals(entityType)){
					service.hidePart(p, true);
				}
			}
			
			
			Path p = getEntityTemplate(entityType);
			if (Files.exists(p)){
				try {
					Files.delete(p);
				} catch (IOException e) {
					Intelligence2PlugIn.log("Unable to delete current template file. " + e.getMessage(), e);
					return;
				}
			}
			entityType.setBirtTemplate(null);
		}
		editTemplate(entityType);
	}
	
	/**
	 * Call from display thread.  Resets the intelligence record template 
	 * 
	 * @param entityType
	 * @param service
	 */
	public void resetRecordTemplate(EPartService service){
		if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Reset", 
				"Are you sure you want to reset the printing template for intelligence records?  You will loose any changes you have made to this template.")){
			return;
		}
		
		if (getRecordTemplate(SmartDB.getCurrentConservationArea()) != null){
			for (MPart p : service.getParts()){
				Object x = E3Utils.getSourceObject(p);
				if (x instanceof IEditorPart && 
						((IEditorPart)x).getEditorInput() instanceof IntelRecordTemplateEditorInput){
					service.hidePart(p, true);
				}
			}
			
			Path p = getRecordTemplate(SmartDB.getCurrentConservationArea());
			if (Files.exists(p)){
				try {
					Files.delete(p);
				} catch (IOException e) {
					Intelligence2PlugIn.log("Unable to delete current template file. " + e.getMessage(), e);
					return;
				}
			}
		}
		editRecordTemplate();
	}
	
	/**
	 * Edits the plan template
	 * @param event
	 */
	public void editTemplate(IntelEntityType entityType){
		try{
			if (entityType.getBirtTemplate() == null){
				//create a new template for entity type
				String file = entityType.getKeyId() + "." + UuidUtils.uuidToString(entityType.getUuid()) +  IReportEditorContants.DESIGN_FILE_EXTENTION;
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
			IntelEntityTypeEditorInput input = new IntelEntityTypeEditorInput(p.toFile(), entityType);
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, IReportEditorContants.DESIGN_EDITOR_ID);
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error opening entity printing template." + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return;
		}
	}


	/**
	 * Refresh bindings in the template for the given entity type
	 * @param type
	 * @param pService
	 * @throws Exception
	 */
	public void refreshReportDataset(IntelEntityType type, EPartService pService) throws Exception{
		if (type.getBirtTemplate() == null) return;
		Path file = getEntityTemplate(type);
		if (!Files.exists(file)) return;
		
		//see if any other parts are editing this template
		//if no we want to re-open after save
		//if yes we want to prompt to save or discard changes then re-open after save
		MPart toOpen = null;
		for (MPart p : pService.getParts()){
			Object x = E3Utils.getSourceObject(p);
			if (x instanceof IEditorPart){
				IEditorPart editor = (IEditorPart) x;
			
				if (editor.getEditorInput() instanceof IntelEntityTypeEditorInput
						&& ((IntelEntityTypeEditorInput)editor.getEditorInput()).getEntityType().equals(type)){
					toOpen = p;
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							if (editor.isDirty()){
								if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Entity Templates", 
										MessageFormat.format("Do you want to save the changes you made to the {0} entity type template?", type.getName()))){
									editor.doSave(new NullProgressMonitor());
								}
							}
							pService.hidePart(p, true);
						}
					});
				}
			}
		}
		
		SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();
		ReportDesignHandle rdh = session.openDesign( file.toString() );
		try{
			refreshReportDataset(rdh);
			rdh.save();
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(ex.getMessage(),ex);
		}finally{
			rdh.close();
		}	
		if (toOpen != null){
			final MPart fopen = toOpen;
			pService.showPart(fopen, PartState.CREATE);
		}
	}
	
	/**
	 * Refresh bindings for the given report design handle.
	 * 
	 * @param rdh
	 * @throws Exception
	 */
	public void refreshReportDataset(ReportDesignHandle rdh) throws Exception{
		for (Object x : rdh.getAllDataSets()){
			if (x instanceof OdaDataSetHandle){
				OdaDataSetHandle h = (OdaDataSetHandle)x;
				if (h.getExtensionID().equalsIgnoreCase(EntityDataset.DATASET_TYPE) || 
						h.getExtensionID().equalsIgnoreCase(EntityRelationDataset.DATASET_TYPE)){
					ColumnBindingFixer.fixBindings(h);
				}
			}
		}
		
	}
	
	
	/**
	 * Edits the intelligence record template
	 * @param event
	 */
	public void editRecordTemplate(){
		try{
			Path p = getRecordTemplate(SmartDB.getCurrentConservationArea());
			if (!Files.exists(p.getParent())){
				Files.createDirectory(p.getParent());
			}
			if (!Files.exists(p)){
				RecordReportGenerator.INSTANCE.generateReport(p);
			}
	
			PlatformUI.getWorkbench().showPerspective(IntelEntityReportPerspective.ID, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			IntelRecordTemplateEditorInput input = new IntelRecordTemplateEditorInput(p.toFile());
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, IReportEditorContants.DESIGN_EDITOR_ID);
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error opening intelligence record printing template." + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return;
		}
	}
}
