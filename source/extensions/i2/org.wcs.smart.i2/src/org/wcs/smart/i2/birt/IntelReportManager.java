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

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.ScalarParameterHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.datasource.DataSourceParameter;
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
				//create a report files
				SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();
				ReportDesignHandle rdh = session.createDesign(p.toFile().getAbsolutePath());
				
				try {
					rdh.setTitle(entityType.getName());
				} catch (SemanticException e) {
					//lets just consume this 
				}
				
				//add parameters - entity types and dates
				ScalarParameterHandle shandler = rdh.getElementFactory().newScalarParameter(DataSourceParameter.ENTITY_UUID.getName());
				shandler.setValueType(DesignChoiceConstants.PARAM_VALUE_TYPE_STATIC);
				shandler.setIsRequired(true);
				shandler.setDataType(DesignChoiceConstants.PARAM_TYPE_STRING);
				shandler.setDistinct(true);
				rdh.getParameters().add(shandler);
				
				shandler = rdh.getElementFactory().newScalarParameter(DataSourceParameter.START_DATE.getName());
				shandler.setValueType(DesignChoiceConstants.PARAM_VALUE_TYPE_STATIC);
				shandler.setIsRequired(false);
				shandler.setDataType(DesignChoiceConstants.PARAM_TYPE_DATETIME);
				shandler.setDistinct(true);
				rdh.getParameters().add(shandler);
				
				shandler = rdh.getElementFactory().newScalarParameter(DataSourceParameter.END_DATE.getName());
				shandler.setValueType(DesignChoiceConstants.PARAM_VALUE_TYPE_STATIC);
				shandler.setIsRequired(false);
				shandler.setDataType(DesignChoiceConstants.PARAM_TYPE_DATETIME);
				shandler.setDistinct(true);
				rdh.getParameters().add(shandler);
				
				//TODO: see if we can include datasets by default
				
				rdh.save();
				rdh.close();
			}
			
			PlatformUI.getWorkbench().showPerspective(IntelEntityReportPerspective.ID, 
					PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			
			IntelEntityEditorInput input = new IntelEntityEditorInput(p.toFile(), entityType);
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, IReportEditorContants.DESIGN_EDITOR_ID);
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error opening entity printing template." + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return;
		}
	}


}
