/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.incident.ui;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.model.IncidentType;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Composite with required controls for incident type filtering. Used inside filter dialogs
 * 
 * @since 8.1.0
 */
public class IncidentTypeFilterComposite extends Composite {

	private CheckboxTableViewer viewer;
	private Collection<IncidentType> selected = null;
	
	/**
	 * @param parent
	 * @param style
	 */
	public IncidentTypeFilterComposite(Composite parent, int style) {
		super(parent, style);
		createControls();
	}
	
	

	private void createControls() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)getLayout()).marginWidth = 0;
		
		viewer = CheckboxTableViewer.newCheckList(this, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI);
		viewer.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(viewer));
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)viewer.getControl().getLayoutData()).heightHint = 100;
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new NamedItemLabelProvider());
		viewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
	
		Job loadJob = new Job("loading") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<IncidentType> types = null;
				try(Session session = HibernateManager.openSession()){
					types = IncidentManager.getInstance().getIncidentTypes(session, false);
				}
				final List<IncidentType> ftypes = types;
				Display.getDefault().asyncExec(()->{
					if (viewer.getControl().isDisposed()) return;
					viewer.setInput(ftypes);
					applyState(selected);
				});
				return Status.OK_STATUS;
			}
		};
		loadJob.schedule();
	}

	/**
	 * Updates the controls to the given values 
	 * @param comparison comparison operator
	 * @param text search value
	 * @param field field to search, can be <code>null</code> if only one field to search
	 */
	public void applyState(Collection<IncidentType> selected) {
		if (selected == null) return;
		this.selected = selected;
		viewer.setAllChecked(false);
		for (IncidentType p : selected) {
			viewer.setChecked(p, true);
		}
	}
	

	/**
	 * 
	 * @return the string comparison operator
	 */
	public Set<IncidentType> getIncidentFilter() {
		Set<IncidentType> selected = new HashSet<>();
		for (Object o : viewer.getCheckedElements()) {
			if (o instanceof IncidentType oo) selected.add(oo);
		}
		return selected;
	}

	
}
