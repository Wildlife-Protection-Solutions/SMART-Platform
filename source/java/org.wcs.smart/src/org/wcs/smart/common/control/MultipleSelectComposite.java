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
package org.wcs.smart.common.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.internal.Messages;

/**
 * Composite to select multiple items.  This composite
 * consists of two lists: a list of items to select from
 * and a list of selected items.  Users move items from
 * one list to the other.
 * 
 * @author elitvin
 * @author Emily
 * @since 1.0.0
 */
public class MultipleSelectComposite<T> extends Composite {

	private Comparator<T> itemComparator;
	
	private ArrayList<T> allItems = null;
	private ArrayList<T> selectedItems = null;

	private TableViewer itemsListViewer;
	private TableViewer selectedItemsListViewer;
	
	private Label labelAll;
	private Label labelSelected;
	
	private Button btnAdd;
	private Button btnRemove;

	private List<IListChanged<T>> changeListeners = new ArrayList<IListChanged<T>>();

	/**
	 * Creates new compliste 
	 */
	public MultipleSelectComposite(Composite parent, int style) {
		super(parent, style);
		createControls();
	}

	/**
	 * Adds a listener that is fired when the list of selected
	 * items changes.
	 * 
	 * @param listener listener to add
	 */
	public void addSelectionChangedListener(IListChanged<T> listener) {
		changeListeners.add(listener);
	}

	/**
	 * Removes listener added
	 * @param listener
	 */
	public void removeSelectionChangedListener(IListChanged<T> listener) {
		changeListeners.remove(listener);
	}

	/*
	 * Fires change listeners
	 */
	protected void fireChangeListeners() {
		for (IListChanged<T> listener : changeListeners) {
			listener.listChanged(selectedItems);
		}
	}

	/*
	 * Creates items select composite
	 */
	private void createControls(){
		setLayout(new GridLayout(3, false));
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		labelAll = createFromSectionLabel(this);
		new Label(this, SWT.NONE);
		labelSelected = new Label(this, SWT.NONE);

		itemsListViewer = new TableViewer(this, SWT.MULTI | SWT.BORDER);
		itemsListViewer.setContentProvider(ArrayContentProvider.getInstance());
		itemsListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		itemsListViewer.setInput(allItems);
		((GridData)itemsListViewer.getControl().getLayoutData()).widthHint = 100;
		((GridData)itemsListViewer.getControl().getLayoutData()).heightHint = 150;
		itemsListViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addItems();
			}
		});
		itemsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsState();
			}
		});
		
		Composite btnComposite = new Composite(this, SWT.NONE);
		btnComposite.setLayout(new GridLayout(1, false));
		btnComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));

		btnAdd = new Button(btnComposite, SWT.PUSH);
		btnAdd.setText(Messages.MultipleSelectComposite_Button_Add);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addItems();
				updateButtonsState();
			}

		});
		btnRemove = new Button(btnComposite, SWT.PUSH);
		btnRemove.setText(Messages.MultipleSelectComposite_Button_Remove);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeItems();
				updateButtonsState();
			}
		});

		selectedItemsListViewer = new TableViewer(this, SWT.MULTI | SWT.BORDER);
		selectedItemsListViewer.setContentProvider(ArrayContentProvider.getInstance());
		selectedItemsListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		selectedItemsListViewer.setInput(selectedItems);
		((GridData)selectedItemsListViewer.getControl().getLayoutData()).widthHint = 100;
		((GridData)selectedItemsListViewer.getControl().getLayoutData()).heightHint = 150;
		selectedItemsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsState();
			}
		});
		selectedItemsListViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				removeItems();
			}
		});
		updateButtonsState();
	}

	private Label createFromSectionLabel(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label label = new Label(container, SWT.NONE);

		Composite contribution = new Composite(container, SWT.NONE);
		GridLayout cLayout = new GridLayout(1, false);
		cLayout.horizontalSpacing = 0;
		cLayout.verticalSpacing = 0;
		cLayout.marginWidth = 0;
		cLayout.marginHeight = 0;
		contribution.setLayout(cLayout);
		contribution.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
		contributeToFromLabelSection(contribution);
		
		return label;
	}
	
	protected void contributeToFromLabelSection(Composite parent) {
		// nothing by default
	}

	private void updateButtonsState() {
		btnAdd.setEnabled(!itemsListViewer.getSelection().isEmpty());
		btnRemove.setEnabled(!selectedItemsListViewer.getSelection().isEmpty());
	}
	
	/**
	 * Moves items from the list of all to the list of selected.
	 */
	private void addItems() {
		Iterator<?> items = ((IStructuredSelection)itemsListViewer.getSelection()).iterator();
		while(items.hasNext()){
			@SuppressWarnings("unchecked")
			T next = (T) items.next();
			allItems.remove(next);
			selectedItems.add(next);
		}
		sortList(selectedItems);
		itemsListViewer.refresh();
		selectedItemsListViewer.refresh();

		fireChangeListeners();
	}
	/**
	 * Moves items from the list of selected to the list of all.
	 */
	private void removeItems() {
		Iterator<?> items = ((IStructuredSelection)selectedItemsListViewer.getSelection()).iterator();
		while(items.hasNext()){
			@SuppressWarnings("unchecked")
			T next = (T)items.next();
			allItems.add(next);
			selectedItems.remove(next);
		}
		sortList(allItems);
		itemsListViewer.refresh();
		selectedItemsListViewer.refresh();

		fireChangeListeners();
	}
	
	/**
	 * Initialises the items lists
	 * @param all the list of items to select from
	 * @param current the list items selected by default
	 */
	public void setItemsData(List<T> all, List<T> current){

		this.allItems = new ArrayList<T>();
		this.allItems.addAll(all);
		this.allItems.removeAll(current);
		sortList(this.allItems);


		this.selectedItems = new ArrayList<T>();
		this.selectedItems.addAll(current);
		sortList(this.selectedItems);

		itemsListViewer.setInput(this.allItems);
		itemsListViewer.refresh();

		selectedItemsListViewer.setInput(this.selectedItems);
		selectedItemsListViewer.refresh();

		fireChangeListeners();
	}

	/**
	 * Sorts the items lists
	 */
	private void sortList(ArrayList<T> list) {
		if (itemComparator != null) {
			Collections.sort(list, itemComparator);
		}
	}
	
	public void setItemComparator(Comparator<T> itemComparator) {
		this.itemComparator = itemComparator;
	}
	
	public void setLabelProvider(LabelProvider labelProvider) {
		itemsListViewer.setLabelProvider(labelProvider);
		selectedItemsListViewer.setLabelProvider(labelProvider);
	}
	
	public void setLabelAllText(String text) {
		labelAll.setText(text);
	}

	public void setLabelSelectedText(String text) {
		labelSelected.setText(text);
	}
	
	public List<T> getSelectedItems() {
		List<T> list = (List<T>) this.selectedItemsListViewer.getInput();
		return list != null ? list : Collections.emptyList();
	}
	
	/**
	 * Creates a list out of the selected items
	 * @return
	 */
	public List<T> getSelectedItemsAsList() {
		ArrayList<T> items = new ArrayList<T>();
		items.addAll(getSelectedItems());
		return items;
	}

	/**
	 * Change listener fired when the list of
	 * selected items changes
	 * 
	 */
	public interface IListChanged<T> {
		/**
		 * Fired when the list of selected items is changes
		 * @param items the new list of selected items
		 */
		public void listChanged(List<T> items);
	}
}
