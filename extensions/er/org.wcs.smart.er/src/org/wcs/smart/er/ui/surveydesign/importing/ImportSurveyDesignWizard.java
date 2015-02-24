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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.ui.handlers.EditSurveyElementHandler;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.er.xml.SurveyDesignFromXmlConverter;
import org.wcs.smart.er.xml.SurveyDesignXMLManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

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

	public ImportSurveyDesignWizard() {
		setWindowTitle(Messages.ImportSurveyDesignWizard_Title);
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
			importDesigns(designsPage.getDesigns());
		}
		return true;
	}

	private boolean designExists(Session session, String keyId) {
		List<?> existingDesigns = session.createCriteria(SurveyDesign.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))  //$NON-NLS-1$
				.add(Restrictions.eq("keyId", keyId)).list(); //$NON-NLS-1$
		return (existingDesigns.size() > 0);
	}
	
	private void importDesigns(final List<SurveyDesign> designs) {
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.SurveyDesignImportHandler_0, designs.size());
					SurveyDesign lastDesign = null;
					int successCount = 0;
					Session session = HibernateManager.openSession();
					SurveyDesignImporter importer = new SurveyDesignImporter();
					monitor.subTask(Messages.SurveyDesignImportHandler_3);
					for(SurveyDesign source : designs){
						source = (SurveyDesign)session.merge(source);
						SurveyDesign sd = importer.importSurveyDesign(session, source);
						List<SamplingUnit> newSamplingUnits = SurveyDesignImporter.importSamplingUnits(session, source, sd);
						if (processSave(session, sd, newSamplingUnits)) {
							lastDesign = sd;
							successCount++;
						}
						monitor.worked(1);
					}
					session.close();
					reportResult(successCount, designs.size(), lastDesign);
				}
			});
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		}
	}

	private void importFiles(final List<File> importFiles) {
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.SurveyDesignImportHandler_0, importFiles.size());

					org.wcs.smart.er.xml.model.surveydesign.SurveyDesign xmlsd = null;
					SurveyDesign lastDesign = null;
					int successCount = 0;
					Session session = HibernateManager.openSession();
					for(File file : importFiles){
						try{
							monitor.subTask(Messages.SurveyDesignImportHandler_1);
							FileInputStream fin = new FileInputStream(file);
							try{
								xmlsd = SurveyDesignXMLManager.readDataModel(fin);
							}finally{
								fin.close();
							}
						}catch (Exception ex){
							EcologicalRecordsPlugIn.displayLog(MessageFormat.format(Messages.SurveyDesignImportHandler_2, file.getName()), ex);
							continue; //continue importing other files
						}

						try{
							monitor.subTask(Messages.SurveyDesignImportHandler_3);
							SurveyDesign sd = SurveyDesignFromXmlConverter.fromXml(xmlsd, session);
							List<SamplingUnit> units =  SurveyDesignFromXmlConverter.getSamplingUnits(xmlsd, sd, session);
							if (processSave(session, sd, units)) {
								lastDesign = sd;
								successCount++;
							}
							monitor.worked(1);
						}catch(ParseException parse){
							EcologicalRecordsPlugIn.displayLog(parse.getMessage(), parse);
							continue; //continue importing other files;
						}
					}//end loop of each xml file.
					session.close();
					reportResult(successCount, importFiles.size(), lastDesign);
				}
			});
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		}
	}

	private boolean processSave(Session session, SurveyDesign sd, List<SamplingUnit> newSamplingUnits) {
		try{
			//	ensure key doesn't already exist
			if (designExists(session, sd.getKeyId())){
				final String msg = MessageFormat.format(Messages.SurveyDesignImportHandler_4, new Object[]{sd.getKeyId()});
				getShell().getDisplay().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openError(getShell(), Messages.EcologicalRecordsPlugIn_ErrorDialogTitle, msg);
					}
				});
				return false;
			}

			//	save to the database
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
			for (SamplingUnit su : newSamplingUnits){
				session.saveOrUpdate(su);
			}

			session.getTransaction().commit();

			SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_ADDED, sd);
		}catch (Exception ex){
			try{
				session.getTransaction().rollback();
			}catch (Exception ex2){
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex2);
			}
			EcologicalRecordsPlugIn.displayLog(Messages.ImportSurveyDesignWizard_Error + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			return false;
		}
		return true;
	}
	
	private void reportResult(int successCount, int totalCount, SurveyDesign lastDesign) {
		if (totalCount > 1) {
			final String message = MessageFormat.format(Messages.ImportSurveyDesignWizard_Report_Message, successCount, totalCount);
			getShell().getDisplay().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openInformation(getShell(), Messages.ImportSurveyDesignWizard_Report_Title, message);
				}				
			});
		} else if (lastDesign != null) {
			//open editor
			final SurveyDesignEditorInput input = new SurveyDesignEditorInput(lastDesign.getName(), lastDesign.getUuid(), lastDesign.getKeyId(), lastDesign.getState());
			getShell().getDisplay().syncExec(new Runnable(){
				@Override
				public void run() {		
					(new EditSurveyElementHandler()).execute(new StructuredSelection(input), getShell());		
				}
				
			});
			
		}
	}
	
}
