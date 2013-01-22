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
package org.wcs.smart.intelligence.ui.wizard;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.panel.IntelligenceCompositeFactory.PanelType;

/**
 * Wizard to create new intelligence.
 * 
 * @author elitvin
 *
 */
public class NewIntelligenceWizard extends Wizard implements IPageChangingListener {
    
    private Session session = null;

    private Intelligence intelligence = null;

    private IWizardPage lastPage = null;
    
    SaveIntelligenceJob saveIntelligenceJob = new SaveIntelligenceJob();    

    public NewIntelligenceWizard() {
        super();
        setWindowTitle(Messages.IntelligenceWizard_Title);
        intelligence = new Intelligence();
        intelligence.setConservationArea(SmartDB.getCurrentConservationArea());
        
    }
    
    @Override
    public void addPages() {
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    	addPage(new TypedIntelligenceWizardPage(PanelType.RECIEVED));
    	addPage(new TypedIntelligenceWizardPage(PanelType.SOURCE));
    	addPage(new TypedIntelligenceWizardPage(PanelType.DATES));
    	addPage(new TypedIntelligenceWizardPage(PanelType.DESCRIPTION));
    	addPage(new TypedIntelligenceWizardPage(PanelType.LOCATION));
    	addPage(new TypedIntelligenceWizardPage(PanelType.ATTACHMENTS));


//    	addPage(new IntelligenceReceivedWizardPage());
//    	addPage(new IntelligenceSourceWizardPage());
//    	addPage(new IntelligenceDatesWizardPage());
//    	addPage(new IntelligenceDescWizardPage());
//    	addPage(new IntelligenceLocationWizardPage());
//    	addPage(new IntelligenceAttachmentsWizardPage());

    	super.addPages();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        if (lastPage instanceof IntelligenceWizardPage) {
            if (!((IntelligenceWizardPage) lastPage).updateModel(intelligence)) {
                return false;
            }
        }
    	saveIntelligenceJob.schedule();
    	try {
			saveIntelligenceJob.join(); //we don't want to close wizard if save failed
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	//IntelligenceHibernateManager.saveIntelligence(intelligence);
    	if (Status.OK_STATUS.equals(saveIntelligenceJob.getResult())) {
        	IntelligenceEventManager.getInstance().intelligenceAdded(intelligence);
        	return true;
    	}
		return false;
    }

    /**
     * Creates a new session and attaches the current conservation area.
     * 
     * @return
     */
    public Session getSession() {
    	if (session == null || !session.isOpen()) {
    		session = SmartHibernateManager.openSession(new AttachmentInterceptor());
    		session.update(intelligence.getConservationArea());
    	}
    	return session;
    }

    @Override
    public void handlePageChanging(PageChangingEvent event) {
        if (event.getCurrentPage() instanceof IntelligenceWizardPage) {
            if (!((IntelligenceWizardPage) event.getCurrentPage()).updateModel(intelligence)) {
                event.doit = false;
                return;
            }
        }

        if (event.doit) {
            lastPage = (IWizardPage) event.getTargetPage();
        }
    }

	/**
	 * Closes the active session
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (session != null && session.isOpen()) {
			session.close();
		}
	}

	/**
     * Job is used to save intelligence object
     * 
     * @author elitvin
     *
     */
    private class SaveIntelligenceJob extends Job {
  
        public SaveIntelligenceJob() {
            super(Messages.NewIntelligenceWizard_SaveIntelligenceJob_Title);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (IntelligenceHibernateManager.saveIntelligence(intelligence)) {
            	return Status.OK_STATUS;
            }
            //no need to use other status as hibernate manager will report error in case something is wrong
            return Status.CANCEL_STATUS;
        }
    }

}
