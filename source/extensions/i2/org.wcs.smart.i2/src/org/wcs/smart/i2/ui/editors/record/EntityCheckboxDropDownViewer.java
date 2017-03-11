/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
import org.wcs.smart.ui.CheckBoxDropDown;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Entity selection dialog for selecting entities for record attributes
 * 
 * @author Emily
 *
 */
public class EntityCheckboxDropDownViewer extends CheckBoxDropDown{

	private FilterComposite txtSearch;
	private Pattern pattern = null;
	
	private IntelEntityType type;
	private Set<EntityItem> selected = new HashSet<EntityItem>();
	private boolean isLoading;
	private boolean isMulti;
	private boolean isInitializing = false;
	
	private ViewerFilter filter = new ViewerFilter() {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (pattern == null) return true;
			return pattern.matcher(labelProvider.getText(element).toLowerCase()).matches();
		}
	}; 
	
	public EntityCheckboxDropDownViewer(Composite parent, IntelEntityType type, boolean isMulti) {
		super(parent);
		this.type = type;
		this.isMulti = isMulti;
		
		setContentProvider(ArrayContentProvider.getInstance());
		setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof EntityItem){
					return ((EntityItem) element).name;
				}
				return super.getText(element);
			}
		});
	}

	public void initControl(final Collection<IntelRecordAttributeValueList> items){
		if (items == null || items.isEmpty()){
			List<EntityItem> eitems = new ArrayList<EntityItem>();
			selected.clear();		
			setValue(eitems);
			if (txtSearch != null) txtSearch.setText(""); //$NON-NLS-1$
			return;
		}
		
		Job j = new Job("initialize entity drop dwon"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<EntityItem> eitems = new ArrayList<EntityItem>();
				isLoading = true;
				try{
					Display.getDefault().syncExec(()->{
						setValue(null);
					});
					
					Session s = HibernateManager.openSession();
					try{
						for (IntelRecordAttributeValueList item : items){
							IntelEntity e = (IntelEntity) s.get(IntelEntity.class, item.getId().getElementUuid());
							if (e != null){
								eitems.add(new EntityItem(e.getUuid(), e.getIdAttributeAsText()));
							}
						}
					}finally{
						s.close();
					}
				}finally{
					isLoading = false;
				}
				selected.addAll(eitems);
				Display.getDefault().syncExec(()->{
					setValue(eitems);
				});
				
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
		
	}
	
	
	protected Shell createPopup(){
		// create shell and table
		Shell popup = new Shell(getShell(),  SWT.NO_TRIM | SWT.ON_TOP);
		popup.setLayout(new GridLayout());
		((GridLayout)popup.getLayout()).marginWidth = 1;
		((GridLayout)popup.getLayout()).marginHeight = 1;
		popup.addListener(SWT.Traverse, e-> {
	    	if (e.detail == SWT.TRAVERSE_ESCAPE) {
	    		e.doit = false;
	    	}
		});
		// create filter fields
		txtSearch = new FilterComposite(popup, SWT.NONE);
		txtSearch.setText(""); //$NON-NLS-1$
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSearch.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (txtSearch.getPatternFilter() == null || txtSearch.getPatternFilter().trim().isEmpty()){
					pattern = null;
					table.setFilters(new ViewerFilter[]{});
				}else{
					pattern = Pattern.compile(".*" + txtSearch.getPatternFilter().toLowerCase() + ".*"); //$NON-NLS-1$ //$NON-NLS-2$
					table.setFilters(new ViewerFilter[]{filter});
				}
				table.refresh();
				if (isMulti){
					((CheckboxTableViewer)table).setCheckedElements(selected.toArray());
				}else{
					if (selected.isEmpty()){
						table.setSelection(null);
					}else{
						table.setSelection(new StructuredSelection(selected.iterator().next()));
					}
				}
			}
		});
		
		// create table
		if (isMulti){
			Table ttable = new Table(popup, SWT.CHECK | SWT.V_SCROLL);
			table = new CheckboxTableViewer(ttable){
				@Override
				public Object[] getCheckedElements() {
					return selected.toArray();
				}
			};
			
			((CheckboxTableViewer)table).addCheckStateListener(new ICheckStateListener() {
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
					if (event.getElement() instanceof EntityItem){
						if (event.getChecked()){
							selected.add((EntityItem)event.getElement());
						}else{
							selected.remove(event.getElement());
						}
					}
					checkChanged = true;				
				}
			});
		}else{
			table = new ListViewer(popup, SWT.V_SCROLL);
			table.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (!isInitializing){
						selected.clear();
						Object element = ((IStructuredSelection)event.getSelection()).getFirstElement();
						if (element instanceof EntityItem){
							selected.clear();
							selected.add((EntityItem)element);
						}
						checkChanged = true;
					}
				}
			});
			table.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					dropDown(false);
				}
			});
		}
		
		table.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setContentProvider(contentProvider);
		table.setInput(new String[]{DialogConstants.LOADING_TEXT});
		popup.pack();		
		
		loadEntities();
		return popup;
	}
		
	@Override
	protected String getTextLabel(Collection<?> objects){
		if (isLoading) return DialogConstants.LOADING_TEXT;
		String value = super.getTextLabel(objects);
		if (!objects.isEmpty()){
			value = "(" + objects.size() + ") " + value; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return value;
	}
	
	public boolean updateValue(IntelRecordAttributeValue value){
		boolean add = false;
		if (value.getAttributeListItems() == null){
			value.setAttributeListItems(new ArrayList<>());
		}
		ArrayList<IntelRecordAttributeValueList> listValues = new ArrayList<IntelRecordAttributeValueList>();
		Collection<?> objects = getCheckObjects();
		for (Object item : objects) {					
			if (item instanceof EntityItem){
				IntelRecordAttributeValueList list = new IntelRecordAttributeValueList();
				list.getId().setElementUuid(((EntityItem) item).uuid);
				list.getId().setValue(value);
				listValues.add(list);
				add = true;
			}
		}
		List<IntelRecordAttributeValueList> delete = new ArrayList<IntelRecordAttributeValueList>();
		for (IntelRecordAttributeValueList existing : value.getAttributeListItems()){
			if (!listValues.contains(existing)) delete.add(existing);
		}
		value.getAttributeListItems().removeAll(delete);
		for (IntelRecordAttributeValueList newItem: listValues){
			if (!value.getAttributeListItems().contains(newItem)){
				value.getAttributeListItems().add(newItem);
			}
		}
		return add;
	}
	
	@Override
	protected Object[] getCheckedElements(){
		if (isMulti){
			return ((CheckboxTableViewer)table).getCheckedElements();
		}else{
			if (table.getSelection().isEmpty()){
				return new Object[]{};
			}else{
				Object x = ((StructuredSelection)table.getSelection()).getFirstElement();
				if (x instanceof EntityItem){
					return new Object[]{x};
				}else{
					return new Object[]{};
				}
			}
		}
	}
	@Override
	protected void setCheckedElements(Object[] elements){
		isInitializing = true;
		try{
			if (isMulti){
				((CheckboxTableViewer)table).setCheckedElements(elements);
			}else{
				if (elements.length == 0){
					table.setSelection(null);
				}else{
					table.setSelection(new StructuredSelection(elements[0]));
					((ListViewer)table).reveal(elements[0]);
				}
			}
		}finally{
			isInitializing = false;
		}
	}
	
	
	/**
	 * Called before the popup is made visible
	 */
	@Override
	protected void popupVisible(){
		table.setInput(new String[]{DialogConstants.LOADING_TEXT});
		loadEntities();
		txtSearch.setText(""); //$NON-NLS-1$
	}

	private void loadEntities(){
		Job j = new Job("loading entities"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<EntityItem> entities = new ArrayList<>();
				Session s = HibernateManager.openSession();
				try{
					ScrollableResults r = s.createCriteria(IntelEntity.class)
					.add(Restrictions.eq("entityType", type)) //$NON-NLS-1$
					.scroll();
					while(r.next()){
						IntelEntity e = (IntelEntity)r.get()[0];
						EntityItem i = new EntityItem(e.getUuid(), e.getIdAttributeAsText());
						entities.add(i);
					}
				}finally{
					s.close();
				}
				Display.getDefault().syncExec(()->{
					
					if (table.getControl().isDisposed()) return;
					txtSearch.setText(""); //$NON-NLS-1$
					table.setInput(entities);
					setCheckedElements(selected.toArray());
				});
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
	}
	
	public class EntityItem{
		String name;
		UUID uuid;
		
		public EntityItem(UUID uuid, String name){
			this.uuid = uuid;
			this.name = name;
		}
		
		@Override
		public int hashCode(){
			return uuid.hashCode();
		}
		
		@Override
		public boolean equals(Object other){
			if (this == other) return true;
			if (other == null) return false;
			if (getClass() != other.getClass()) return false;
			return uuid.equals(((EntityItem)other).uuid);
		}
	}
}
