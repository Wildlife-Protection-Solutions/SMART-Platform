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
package org.wcs.smart.er.ui.samplingunit.load.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.samplingunit.load.CsvSamplingUnitImporter;
import org.wcs.smart.er.ui.samplingunit.load.ISamplingUnitImporter;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Import sampling unit wizard.
 * 
 * @author Emily
 *
 */
public class ImportWizard extends Wizard implements IPageChangingListener{

	private Session session;
	
	private SurveyDesign surveyDesign;
	
	private boolean canFinish = false;
	
	private TypePage typePage;
	private FileWizardPage filePage;
	private AttributePage attributePage;
	private boolean finishOk = false;
	
	/**
	 * Creates a new wizard
	 */
	public ImportWizard(SurveyDesign surveyDesign){
		session = HibernateManager.openSession();
	
		setNeedsProgressMonitor(true);
		this.surveyDesign = (SurveyDesign) session.load(SurveyDesign.class, surveyDesign.getUuid());
	}
	
	@Override
	public void dispose(){
		if (session.isOpen()){
			session.close();
		}
		super.dispose();
	}
	
	@Override
	public boolean canFinish(){
		if (canFinish){
			return super.canFinish();
		}
		return false;
	}

	@Override
	public boolean performFinish() {
		//validate
		String msg = attributePage.validate();
		if (msg != null){
			MessageDialog.openError(getShell(), Messages.ImportWizard_ErrorTitle, msg);
			return false ;
		}
		
		//setup parameters
		final HashMap<Object, Object> params = new HashMap<Object, Object>();
		try{
			params.put(CsvSamplingUnitImporter.DELIMETER_KEY, filePage.getDelimiter());
		}catch (Exception ex){
			MessageDialog.openError(getShell(), Messages.ImportWizard_ErrorTitle, ex.getMessage());
			return false ;
		}
		params.put(ISamplingUnitImporter.TYPE_KEY, typePage.getType());
		params.put(ISamplingUnitImporter.PROJECTION_KEY, attributePage.getProjection());
		params.put(ISamplingUnitImporter.ID_FIELD_KEY, attributePage.getIdField());
		params.put(ISamplingUnitImporter.X1_FIELD_KEY, attributePage.getX1Field());
		params.put(ISamplingUnitImporter.Y1_FIELD_KEY, attributePage.getY1Field());
		params.put(ISamplingUnitImporter.X2_FIELD_KEY, attributePage.getX2Field());
		params.put(ISamplingUnitImporter.Y2_FIELD_KEY, attributePage.getY2Field());
		params.putAll(attributePage.getAttributeFields());
		
		//get importer
		final ISamplingUnitImporter importer = filePage.getImporter();
		
		final File file = filePage.getFile();
		
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					
					monitor.beginTask(Messages.ImportWizard_ProgressLabel1, 2);
					
					finishOk = false;

					List<SamplingUnit> existing = SurveyHibernateManager.getInstance().getSamplingUnits(surveyDesign, session, null);
					HashSet<String> existingIds = new HashSet<String>();
					for (SamplingUnit su : existing){
						existingIds.add(su.getId());
					}
					
					params.put(ISamplingUnitImporter.EXISTING_IDS_KEY, existingIds);
					//get units
					List<SamplingUnit> units = null;
					monitor.subTask(Messages.ImportWizard_ProgressLabel2);
					try {
						units = importer.importFile(file, params, new SubProgressMonitor(monitor, 1));
					}catch (Exception ex){
						EcologicalRecordsPlugIn.displayLog(ex.getMessage(), ex);
						return;
					}
					if (units == null || units.size() == 0){
						EcologicalRecordsPlugIn.log(Messages.ImportWizard_NoFeatures, null);
						return;
					}

					//validate ids unique within file
					HashSet<String> ids = new HashSet<String>();
					for(SamplingUnit su : units){
						if (ids.contains(su.getId())){
							EcologicalRecordsPlugIn.displayLog(MessageFormat.format(Messages.ImportWizard_IdNoUnique, new Object[]{su.getId()}), null);
							finishOk = false;
							return;
						}
						ids.add(su.getId());
					}
					
					//save units
					monitor.subTask(Messages.ImportWizard_ProgressLabel3);
					if (!session.isOpen()){
						session = HibernateManager.openSession();
					}
					session.beginTransaction();
					try{
						for (SamplingUnit su : units){
							su.setSurveyDesign(surveyDesign);
							session.save(su);
						}
						session.getTransaction().commit();
						finishOk = true;	
					}catch (Exception ex){
						EcologicalRecordsPlugIn.displayLog(Messages.ImportWizard_ImportError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
						session.getTransaction().rollback();
					}finally{
						//close session to event manager doesn't conflict
						session.close();	
					}
					
					//run event manager
					SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, surveyDesign);
				}
			});
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.displayLog(Messages.ImportWizard_ImportError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			return false;
		}
		
		return finishOk;
	}

	
	/**
     * The <code>Wizard</code> implementation of this <code>IWizard</code>
     * method does nothing. Subclasses should extend if extra pages need to be
     * added before the wizard opens. New pages should be added by calling
     * <code>addPage</code>.
     */
	public void addPages() {
    	setWindowTitle(Messages.ImportWizard_WindowTitle);
    	
    	typePage = new TypePage();
    	filePage = new FileWizardPage(true);
    	attributePage = new AttributePage(false, surveyDesign, HibernateManager.getCaProjectionList(session));
    	
    	super.addPage(typePage);
    	super.addPage(filePage);
    	super.addPage(attributePage);
    	
    	
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    }

    public GeometryType getSamplingUnitType(){
    	return typePage.getType();
    }

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() == filePage && 
				event.getTargetPage() == attributePage){
			
			HashMap<String, Object> options = new HashMap<String, Object>();
			String[] items = filePage.getFieldNames(options);
			if (items == null){
				event.doit = false;
				return;
			}
			
			attributePage.setFields(filePage.getImporter(), items);
		}
		if (event.getTargetPage() == attributePage){
			canFinish = true;
		}else{
			canFinish = false;
		}
		
	}

}
