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

import java.util.HashMap;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.samplingunit.load.ImportAttributes;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Import sampling unit wizard.
 * 
 * @author Emily
 *
 */
public class ImportAttributeWizard extends Wizard implements IPageChangingListener{

	private Session session;
	
	private SurveyDesign surveyDesign;
	
	private boolean canFinish = false;
	
	private FileWizardPage filePage;
	private AttributePage attributePage;
	
	/**
	 * Creates a new wizard
	 */
	public ImportAttributeWizard(SurveyDesign surveyDesign){
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
		ImportAttributes ia = new ImportAttributes(
				filePage.getFile(), 
				filePage.getDelimiter(), 
				attributePage.getIdField(),
				attributePage.getAttributeFields(),
				surveyDesign);
		
		try {
			getContainer().run(true, false, ia);
		} catch (Exception e) {
			EcologicalRecordsPlugIn.displayLog(Messages.ImportAttributeWizard_ImportError + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
			return false;
		}
		return  !ia.hasError();
	}

	
	/**
     * The <code>Wizard</code> implementation of this <code>IWizard</code>
     * method does nothing. Subclasses should extend if extra pages need to be
     * added before the wizard opens. New pages should be added by calling
     * <code>addPage</code>.
     */
	public void addPages() {
    	setWindowTitle(Messages.ImportAttributeWizard_WizardTitle);
    	
    	filePage = new FileWizardPage(false);
    	attributePage = new AttributePage(true, surveyDesign, HibernateManager.getCaProjectionList(session));
    	
    	super.addPage(filePage);
    	super.addPage(attributePage);
    	
    	((WizardDialog) getContainer()).addPageChangingListener(this);
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
