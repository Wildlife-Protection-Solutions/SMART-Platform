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
package org.wcs.smart.patrol.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.views.IPatrolFilteringView;
import org.wcs.smart.patrol.internal.ui.views.PatrolFilterDialog;
import org.wcs.smart.patrol.internal.ui.views.PatrolViewFilter;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Composite that contains {@link ComboViewer} and "Filter" button which launches
 * dialog that filters {@link ComboViewer} input content.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolFilteredComboViewer extends Composite implements IPatrolFilteringView {

    private ComboViewer viewer;

    private PatrolViewFilter filter = new PatrolViewFilter();
	private LoadPatrolIdJob loadPatrolIdJob = new LoadPatrolIdJob();

	private boolean stopSelectionPropogation = false;
	private List<ISelectionChangedListener> changeListeners = new ArrayList<ISelectionChangedListener>();
	
	public PatrolFilteredComboViewer(Composite parent) {
		super(parent, SWT.NONE);
		createControls();
	}

	private void createControls() {
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		viewer = new ComboViewer(this, SWT.READ_ONLY);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new PatrolIDLabelProvider());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!isStopSelectionPropogation()) {
					fireChangeListeners(event);
				}
				setStopSelectionPropogation(false);
			}
		});

		Button btnFilter = new Button(this, SWT.PUSH);
		Image image = SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_FILTER_ICON);		
		btnFilter.setImage(image);
		btnFilter.setToolTipText(Messages.PatrolFilteredComboViewerComposite_Filter_Tooltip);
		btnFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnFilter.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PatrolFilterDialog pfd = new PatrolFilterDialog(getShell(), PatrolFilteredComboViewer.this);
				pfd.open();
			}

		});
		
        loadPatrolIdJob.schedule();
	}

	public Control getControl() {
		return viewer.getControl();
	}

	@Override
	public void updateContent() {
		loadPatrolIdJob.cancel();
		Patrol currentPatrol = getSelection();
		if (currentPatrol != null) {
			loadPatrolIdJob.setPreselectedPatrol(currentPatrol);
		}
		loadPatrolIdJob.schedule();		
	}

	@Override
	public PatrolViewFilter getFilter() {
		return filter;
	}
	
	public void setSelection(Patrol patrol) {
    	loadPatrolIdJob.setPreselectedPatrol(patrol);
    	viewer.setSelection(new StructuredSelection(patrol));
	}    

	public Patrol getSelection() {
		ISelection patrolSelection = viewer.getSelection();
		if (patrolSelection instanceof IStructuredSelection) {
			return (Patrol)((IStructuredSelection)patrolSelection).getFirstElement();
		}
		return null;
	}

	/**
	 * Adds a listener that is fired when the list of selected
	 * items changes.
	 * 
	 * @param listener listener to add
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		changeListeners.add(listener);
	}

	/**
	 * Removes listener added
	 * @param listener
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		changeListeners.remove(listener);
	}

	/*
	 * Fires change listeners
	 */
	private void fireChangeListeners(SelectionChangedEvent event) {
		for (ISelectionChangedListener listener : changeListeners) {
			listener.selectionChanged(event);
		}
	}
	
	private void setStopSelectionPropogation(boolean stopSelectionPropogation) {
		this.stopSelectionPropogation = stopSelectionPropogation;
	}
	
	private boolean isStopSelectionPropogation() {
		return stopSelectionPropogation;
	}
	
	/**
	 * Job is used to fill some list viewer with data
	 * 
	 * @author elitvin
	 *
	 */
	private class LoadPatrolIdJob extends Job {
 
        private Patrol preselectedPatrol;
    	
        public LoadPatrolIdJob() {
            super(Messages.LoadPatrolIdJob_Name);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (viewer == null || viewer.getControl().isDisposed()){
                return Status.OK_STATUS;
            }
            final List<Patrol> data = loadPatrolIds();
            getDisplay().asyncExec(new Runnable(){
                @Override
                public void run() {
                    if (viewer.getControl().isDisposed()){
                        return ;
                    }
                    viewer.setInput(data);
                    if (preselectedPatrol != null) {
                    	setStopSelectionPropogation(true);
                    	viewer.setSelection(new StructuredSelection(preselectedPatrol));
                    }
                }});
            return Status.OK_STATUS;
        }
 
        private List<Patrol> loadPatrolIds() {
        	Session s = PatrolHibernateManager.openSession();
        	try {
        		Query query = filter.buildQuery(s);
        		List<?> results = query.list();
        		List<Patrol> patrols = new ArrayList<Patrol>(results.size()+1);
        		boolean defaultPresent = preselectedPatrol == null; //indicated if default patrol id is in filtered list
        		for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
        			Object[] data = (Object[]) iterator.next();
        			Patrol p = new Patrol();
        			p.setUuid((UUID)data[0]);
        			p.setId((String)data[1]);
        			defaultPresent = defaultPresent || p.equals(preselectedPatrol);
        			patrols.add(p);
        		}
        		if (!defaultPresent) {
        			//we don't want to reset selection to null if previously selected patrol is not in filtered list
        			//this is why we add it to result list
        			patrols.add(preselectedPatrol);
        		}
        		return patrols;
        	} finally {
        		s.close();
        	}
        }
        
        public void setPreselectedPatrol(Patrol preselectedPatrol) {
			this.preselectedPatrol = preselectedPatrol;
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
