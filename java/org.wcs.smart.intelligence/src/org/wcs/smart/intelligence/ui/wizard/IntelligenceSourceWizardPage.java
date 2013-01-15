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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceSourceType;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Intelligence Wizard page for collecting the intelligence source information
 * 
 * @author elitvin
 *
 */
public class IntelligenceSourceWizardPage extends IntelligenceWizardPage {

	private static final String ERROR_SOURCE_REQUIRED = Messages.IntelligenceSourceWizardPage_Error_SourceRequired;
	private static final String ERROR_PATROL_ID_REQUIRED = Messages.IntelligenceSourceWizardPage_Error_PatrolIdRequired;
	
    private ComboViewer sourceType = null;
    
    private Label patrolLabel = null;
    private ComboViewer patrolId = null;
    
	/*
	 * job to load all patrol ids
	 */
	private Job loadPatrolIdJob = new LoadPatrolIdJob();
    
    /**
     * @param pageName
     */
    public IntelligenceSourceWizardPage() {
        super(Messages.IntelligenceSourceWizardPage_PageTitle);
        setPageComplete(false);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        Composite center = new Composite(parent, SWT.NONE);
        center.setLayout(new GridLayout(2, false));
        center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        
        Label sourceLabel = new Label(center, SWT.NONE);
        sourceLabel.setText(Messages.IntelligenceSourceWizardPage_IntelligenceSource_Label);
        sourceLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        sourceType = new ComboViewer(center, SWT.READ_ONLY);
        sourceType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        sourceType.setContentProvider(ArrayContentProvider.getInstance());
        sourceType.setLabelProvider(new IntelligenceSourceLabelProvider());
 
        sourceType.setInput(IntelligenceSourceType.values());
        sourceType.setSelection(new StructuredSelection(IntelligenceSourceType.PATROL));
        sourceType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean isPatrolSelected = IntelligenceSourceType.PATROL.equals(getSelectedSourceType());
				patrolLabel.setVisible(isPatrolSelected);
				patrolId.getControl().setVisible(isPatrolSelected);
				setPageComplete(isPageValid());
			}
		});
        
        patrolLabel = new Label(center, SWT.NONE);
        patrolLabel.setText(Messages.IntelligenceSourceWizardPage_PatrolId_Label);
        patrolLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        //NOTE: data for patrolId is filled with loadPatrolIdJob
        patrolId = new ComboViewer(center, SWT.READ_ONLY);
        patrolId.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        patrolId.setContentProvider(ArrayContentProvider.getInstance());
        patrolId.setLabelProvider(new PatrolIDLabelProvider());
        patrolId.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setPageComplete(isPageValid());
			}
		});
        
        setControl(center);
        setMessage(Messages.IntelligenceSourceWizardPage_Message);
        
        loadPatrolIdJob.schedule();

    }

    /* (non-Javadoc)
     * @see org.wcs.smart.intelligence.ui.wizard.IntelligenceWizardPage#updateModel(org.wcs.smart.intelligence.model.Intelligence)
     */
    @Override
    protected boolean updateModel(Intelligence intelligence) {
    	IntelligenceSourceType source = getSelectedSourceType();
    	if (source == null) {
    		IntelligencePlugIn.displayLog(ERROR_SOURCE_REQUIRED, null);
    		return false;
    	}
    	intelligence.setSource(source);
    	if (IntelligenceSourceType.PATROL.equals(source)) {
    		Patrol patrol = getSelectedPatrol();
    		if (patrol == null) {
    			IntelligencePlugIn.displayLog(ERROR_PATROL_ID_REQUIRED, null);
    			return false;
    		}
    		intelligence.setPatrol(patrol);
    	} else {
    		intelligence.setPatrol(null);
    	}
    	return true;
    }

    @Override
    public boolean isPageValid() {
    	IntelligenceSourceType source = getSelectedSourceType();
		if (source == null) {
			return false;
		}
		if (IntelligenceSourceType.PATROL.equals(source)) {
			return getSelectedPatrol() != null;
		}
		return true;
    }
    
    private IntelligenceSourceType getSelectedSourceType() {
		ISelection sourceSelection = sourceType.getSelection();
		if (sourceSelection instanceof IStructuredSelection) {
			return (IntelligenceSourceType)((IStructuredSelection)sourceSelection).getFirstElement();
		}
		return null;
    }

    private Patrol getSelectedPatrol() {
		ISelection patrolSelection = patrolId.getSelection();
		if (patrolSelection instanceof IStructuredSelection) {
			return (Patrol)((IStructuredSelection)patrolSelection).getFirstElement();
		}
		return null;
    }
    
    /**
     * Job is used to fill some list viewer with data
     * 
     * @author elitvin
     *
     */
    private class LoadPatrolIdJob extends Job {
        
        public LoadPatrolIdJob() {
            super(Messages.LoadPatrolIdJob_Name);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (patrolId == null || patrolId.getControl().isDisposed()){
                return Status.OK_STATUS;
            }
            final List<Patrol> data = IntelligenceHibernateManager.getPatrols();
            Display.getDefault().asyncExec(new Runnable(){
                @Override
                public void run() {
                    if (patrolId.getControl().isDisposed()){
                        return ;
                    }
                    for (Patrol id : data){
                    	patrolId.add(id);
                    }               
                }});
            return Status.OK_STATUS;
        }
    }

    /**
     * LabelProvider used to display enum values from {@link IntelligenceSourceType}
     * 
     * @author elitvin
     *
     */
    private class IntelligenceSourceLabelProvider extends LabelProvider {
    	@Override
    	public String getText(Object element) {
    		if (element instanceof IntelligenceSourceType) {
    			return ((IntelligenceSourceType)element).getName();
    		}
    		return super.getText(element);
    	}
    }

    /**
     * LabelProvider used to display {@link Patrol} IDs values
     * 
     * @author elitvin
     *
     */
    private class PatrolIDLabelProvider extends LabelProvider {
    	@Override
    	public String getText(Object element) {
    		if (element instanceof Patrol) {
    			return ((Patrol)element).getId();
    		}
    		return super.getText(element);
    	}
    }    
}
