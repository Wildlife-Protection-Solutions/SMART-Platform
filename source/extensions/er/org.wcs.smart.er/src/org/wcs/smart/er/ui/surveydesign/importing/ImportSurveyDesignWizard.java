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
package org.wcs.smart.er.ui.surveydesign.importing;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.ui.SurveyDesignListView;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditor;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.er.xml.SurveyDesignFromXmlConverter;
import org.wcs.smart.er.xml.SurveyDesignXMLManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ui.FieldDataPerspective;

/**
 * Wizard to import survey designs.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ImportSurveyDesignWizard  extends Wizard implements IPageChangingListener {

	private ImportSurveyDesignSourcePage sourcePage;
	private ImportSurveyDesignFilesPage filesPage;
	private ImportSurveyDesignCaPage caPage;
	private ImportSurveyDesignDesignsPage designsPage;

	private boolean importFile = true;

	private String errorMessage = null;
	private Exception exception = null;
	private SurveyDesign newDesign = null;
	
	public ImportSurveyDesignWizard() {
		setWindowTitle("Import Survey Design(s)");
		super.setNeedsProgressMonitor(true);
	}

	/**
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		
		sourcePage = new ImportSurveyDesignSourcePage();
		super.addPage(sourcePage);
		
		filesPage = new ImportSurveyDesignFilesPage();
		super.addPage(filesPage);
		
		caPage = new ImportSurveyDesignCaPage();
		super.addPage(caPage);
		
		designsPage = new ImportSurveyDesignDesignsPage();
		super.addPage(designsPage);
	}

    public boolean canFinish() {
    	IWizardPage curPage = getContainer().getCurrentPage();
		return (curPage == filesPage || curPage == designsPage) && curPage.isPageComplete();
    }

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getTargetPage() == designsPage){
			designsPage.setInput(caPage.getConservationArea());
		}else if (event.getTargetPage() == filesPage){
			importFile = true;
		}else if (event.getTargetPage() == caPage){
			importFile = false;
		}
	}

	@Override
	public boolean performFinish() {
		if (importFile) {
			importFiles(filesPage.getFiles());
		} else {
			importDesigns();
		}
		return true;
	}

	private void importDesigns() {
		// TODO Auto-generated method stub
		
	}

	private void importFiles(final List<File> importFiles) {
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.SurveyDesignImportHandler_0, importFiles.size());
					
					monitor.subTask(Messages.SurveyDesignImportHandler_1);
					org.wcs.smart.er.xml.model.surveydesign.SurveyDesign xmlsd = null;
					for(File file : importFiles){
						try{
							FileInputStream fin = new FileInputStream(file);
							try{
								xmlsd = SurveyDesignXMLManager.readDataModel(fin);
							}finally{
								fin.close();
							}
						}catch (Exception ex){
							errorMessage = Messages.SurveyDesignImportHandler_2;
							exception = ex;
							return;
						}
						monitor.worked(1);
					
					
						monitor.subTask(Messages.SurveyDesignImportHandler_3);
				
						Session session = HibernateManager.openSession();
						try{
							SurveyDesign sd = SurveyDesignFromXmlConverter.fromXml(xmlsd, session);
						
						//	ensure key doesn't already exist
							List<?> existingDesigns = session.createCriteria(SurveyDesign.class)
								.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))  //$NON-NLS-1$
								.add(Restrictions.eq("keyId", sd.getKeyId())).list(); //$NON-NLS-1$
							if (existingDesigns.size() > 0){
								errorMessage = MessageFormat.format(Messages.SurveyDesignImportHandler_4, new Object[]{sd.getKeyId()});
								return;
							}
						//	save to the database
							try{						
								session.beginTransaction();
												
								//update/add new mission properties	
								for( MissionProperty mp : sd.getMissionProperties()){
									session.saveOrUpdate(mp.getAttribute());
								}
							

								//update/add new sampling unit attributes
								for (SurveyDesignSamplingUnitAttribute sdua: sd.getSamplingUnitAttributes()){
									session.saveOrUpdate(sdua.getSamplingUnitAttribute());
								}
							
								//save survey design
								session.saveOrUpdate(sd);
							
								//save sampling Units
								List<SamplingUnit> units =  SurveyDesignFromXmlConverter.getSamplingUnits(xmlsd, sd, session);
								for (SamplingUnit su : units){
									session.saveOrUpdate(su);
								}
	
								session.getTransaction().commit();
								newDesign = sd;
							}catch (Exception ex){
								session.getTransaction().rollback();
								throw ex;
							}
						}catch(ParseException parse){
							errorMessage = parse.getMessage();
							exception = parse;
							return;
						}catch(Exception ex){
							errorMessage = "An Error occured when loading Survey Design: " + "\n\n" + ex.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
							exception = ex;
							return;
						}finally{
							session.close();
						}
					}//end loop of each xml file.
					
					
					if (newDesign != null){
						SurveyEventHandler.getInstance().fireEvent(SurveyEventHandler.EventType.SURVEY_DESIGN_ADDED, newDesign);
					}
				}
			});
			}catch (Exception ex){
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
			}
		
		
		if (errorMessage != null || exception != null){
			EcologicalRecordsPlugIn.displayLog(errorMessage != null ? errorMessage : exception.getMessage(), exception);
		}else if (newDesign != null){
			//open editor
			try {
				FieldDataPerspective.openPerspective(SurveyDesignListView.ID);
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new SurveyDesignEditorInput(newDesign.getName(), newDesign.getUuid(), newDesign.getKeyId(), newDesign.getState()), SurveyDesignEditor.ID);
			} catch (PartInitException e) {

			}
			
		}
	}
	
}
