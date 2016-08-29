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
package org.wcs.smart.er.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.wcs.smart.common.filter.IUpdatableView;
import org.wcs.smart.er.EcologicalRecordsPlugIn;

/**
 * Combo viewer with a filter button that can filter what is displayed in the 
 * drop down.
 * 
 * @author Emily
 *
 * @param <T>
 */
public abstract class FilteredComboViewer<T> extends Composite implements IUpdatableView {

    protected ComboViewer viewer;
    protected Button btnFilter;

    protected T currentSelection;

	private List<ISelectionChangedListener> changeListeners = new ArrayList<ISelectionChangedListener>();
	
	public FilteredComboViewer(Composite parent) {
		super(parent, SWT.NONE);
		createControls();
	}

	@Override
	public void setEnabled(boolean enabled){
		super.setEnabled(enabled);
		viewer.getControl().setEnabled(enabled);
		btnFilter.setEnabled(enabled);
	}
	
	/**
	 * The tooltip to display on filter button
	 * @return
	 */
	protected abstract String getTooltip();
	
	/**
	 * Combo label provider
	 * @return
	 */
	protected abstract ILabelProvider getLabelProvider();
	
	/**
	 * Displays filter dialog
	 */
	protected abstract void showFilterDialog();
	
	/**
	 * Load combo viewer items
	 */
	protected abstract void loadListItems();
	
	@Override
	public abstract void updateContent();
	
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
		viewer.setLabelProvider(getLabelProvider());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners(event);
			}
		});

		btnFilter = new Button(this, SWT.PUSH);
		Image image = EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.FILTER_ICON);		
		btnFilter.setImage(image);
		btnFilter.setToolTipText(getTooltip());
		btnFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnFilter.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showFilterDialog();
			}

		});
		
        loadListItems();
	}

	public Control getControl() {
		return viewer.getControl();
	}

	public void setControlsEnabled(boolean enabled) {
		viewer.getControl().setEnabled(enabled);
		btnFilter.setEnabled(enabled);
	}
	
	public void setSelection(T currentSelection) {
    	this.currentSelection = currentSelection;
    	viewer.setSelection(new StructuredSelection(currentSelection));
	}    

	@SuppressWarnings("unchecked")
	public T getSelection() {
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			return (T)((IStructuredSelection)selection).getFirstElement();
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
		
}
