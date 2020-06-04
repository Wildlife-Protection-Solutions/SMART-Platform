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
package org.wcs.smart.incident.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.incident.IIncidentProvider;
import org.wcs.smart.incident.IncidentManager;

/**
 * Composite with required controls for string filtering. Used inside filter dialogs
 * 
 * @author elitvin
 * @author Emily
 * @since 1.0.0
 */
public class SourceFilterComposite extends Composite {

	private CheckboxTableViewer viewer;

	
	/**
	 * @param parent
	 * @param style
	 */
	public SourceFilterComposite(Composite parent, int style) {
		super(parent, style);
		createControls();
	}
	
	

	private void createControls() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)getLayout()).marginWidth = 0;
		
		viewer = CheckboxTableViewer.newCheckList(this, SWT.V_SCROLL | SWT.BORDER);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IIncidentProvider)element).getName();
			}
		});
		viewer.setInput(IncidentManager.getInstance().getIncidentProviders());
		
	}

	

	/**
	 * Updates the controls to the given values 
	 * @param comparison comparison operator
	 * @param text search value
	 * @param field field to search, can be <code>null</code> if only one field to search
	 */
	public void applyState(Set<IIncidentProvider> selected) {
		viewer.setAllChecked(false);
		for (IIncidentProvider p : selected) {
			viewer.setChecked(p, true);
		}
	}
	

	/**
	 * 
	 * @return the string comparison operator
	 */
	public Set<IIncidentProvider> getIncidentFilter() {
		Set<IIncidentProvider> selected = new HashSet<>();
		for (Object o : viewer.getCheckedElements()) {
			selected.add((IIncidentProvider)o);
		}
		return selected;
	}

	
}
