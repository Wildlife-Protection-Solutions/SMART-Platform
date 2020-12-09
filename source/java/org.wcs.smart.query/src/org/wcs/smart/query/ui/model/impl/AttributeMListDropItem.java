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
package org.wcs.smart.query.ui.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryFilterConfigManager;
import org.wcs.smart.query.QueryFilterConfigManager.IConfigurationChangeListener;
import org.wcs.smart.query.common.model.QueryFilterConfiguration;
import org.wcs.smart.query.common.ui.OperatorLabelProvider;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.ui.CheckBoxDropDown;

/**
 * Attribute multi-list type drop item.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeMListDropItem extends DropItem implements IFilterDropItem{
	
	protected String text;
	protected String key;
	protected Label lblAttribute;
	protected CheckBoxDropDown listViewer;
	protected ComboViewer opViewer;

	private Font smallerFont;
	protected Attribute attribute = null;
	
	protected Collection<ListItem> currentSelection = null;
	protected Operator currentOp = null;
	
	/*
	 * Job to load the attribute list options
	 */
	protected Job loadItemsJobs = new Job(Messages.AttributeListDropItem_LoadingJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			final ArrayList<ListItem> items = new ArrayList<ListItem>();
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try{
					boolean showInactive = QueryFilterConfigManager.getInstance().isShowInactiveItems();
					List<AttributeListItem> litems = QueryDataModelManager.getInstance().getAttributeListItems(attribute, s, !showInactive);
					for (AttributeListItem item : litems){
						items.add(new ListItem(item.getUuid(), item.getName(), item.getKeyId(), item.getIsActive()));
					}
					//add the any item
					//items.add(0, BasicDropItemFactory.ANY_OPTION);				
					
				}finally{
					s.getTransaction().rollback();
				}
			}
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					if (listViewer == null || listViewer.isDisposed()) return;
					
					if (currentSelection != null) {
						for (ListItem current: currentSelection) {
							if (!items.contains(current)) items.add(current);
						}
					}
					
					listViewer.setInput(items);
					if (currentSelection != null) listViewer.setValue(currentSelection);
					
					getTargetPanel().redraw();
				}});
			return Status.OK_STATUS;
		}
	};

	private IConfigurationChangeListener queryConfChangeListener = new IConfigurationChangeListener() {
		@Override
		public void configurationChanged(QueryFilterConfiguration config) {
			loadItemsJobs.cancel();
			loadItemsJobs.schedule();
		}
	};
		
	/**
	 * Creates a new attribute list drop item
	 * 
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the category attribute to make up the drop item
	 */
	public AttributeMListDropItem(CategoryAttribute att) {
		//super(parent, panel);
		this(att.getAttribute().getName() + " (" + att.getCategory().getFullCategoryName() + ")",  //$NON-NLS-1$ //$NON-NLS-2$
				"category:" + att.getCategory().getHkey() + ":attribute:" + att.getAttribute().getType().typeKey + ":" + att.getAttribute().getKeyId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.attribute = att.getAttribute();
	}

	
	/**
	 * Creates a new attribute list drop item
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the attribute to make up the drop item
	 */
	public AttributeMListDropItem(Attribute att) {
		//super(parent, panel);
		this(att.getName(), "attribute:" + att.getType().typeKey + ":" + att.getKeyId()); //$NON-NLS-1$ //$NON-NLS-2$
		this.attribute = att;
	}
	
	protected AttributeMListDropItem(String text, String key) {
		this.text = text;
		this.key = key;
	}
	
	/**
	 * 
	 * The expected input for data is an array of two elements.  The first
	 * element is the operator, the second element is a list of ListItem
	 * representing the selected options.
	 * 
	 * @param data - a listItem 
	 */
	@SuppressWarnings("unchecked")
	public void initializeData(Object data){
		Object[] datas = (Object[])data;
	
		currentOp = (Operator) datas[0];
		currentSelection = (Collection<ListItem>)datas[1];
	}
	
	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		QueryFilterConfigManager.getInstance().removeChangeListener(queryConfChangeListener);
		super.dispose();
		loadItemsJobs.cancel();
		if (smallerFont != null){
			smallerFont.dispose();
		}
		
		lblAttribute = null;
		listViewer = null;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		
		return this.text + ((Operator)opViewer.getStructuredSelection().getFirstElement()).asSmartValue() + listViewer.getText();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder query = new StringBuilder(this.key);
		query.append(" "); //$NON-NLS-1$
		query.append(((Operator)opViewer.getStructuredSelection().getFirstElement()).asSmartValue());
		query.append(" "); //$NON-NLS-1$
		Collection<ListItem> it = null;
		if (currentSelection != null){
			it = currentSelection;
		}else{
			it = new ArrayList<>();
			for(Object x : listViewer.getCheckObjects()) {
				it.add((ListItem)x);
			}
		}
		if (it == null || it.isEmpty()) return query.toString();
		
		for (ListItem li : it) {
			query.append(li.getKey());
			query.append(AttributeFilter.MLIST_SEPERATOR);
		}
		
		return query.substring(0, query.length() - 1);
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);

		opViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		opViewer.setContentProvider(ArrayContentProvider.getInstance());
		opViewer.setLabelProvider(new OperatorLabelProvider());
		opViewer.setInput(AttributeFilter.MULTI_LIST_OPERATORS);
		opViewer.setSelection(new StructuredSelection(AttributeFilter.MULTI_LIST_OPERATORS[0]));
		if (currentOp != null) {
			opViewer.setSelection(new StructuredSelection(currentOp));
		}
		opViewer.addSelectionChangedListener(e->{
			currentOp = (Operator) opViewer.getStructuredSelection().getFirstElement();
			queryChanged();	
		});

		Composite color = new Composite(main, SWT.NONE);
		color.setLayout(new GridLayout());
		((GridLayout)color.getLayout()).marginWidth = 0;
		((GridLayout)color.getLayout()).marginHeight = 0;
		color.setBackground(color.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		listViewer = new CheckBoxDropDown(color);
		listViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)listViewer.getLayoutData()).widthHint = 200;
		
		FontData fd = (listViewer.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		listViewer.setFont(smallerFont);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.setLabelProvider(ListItem.createLabelProvider());
		
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
				List<ListItem> selection = new ArrayList<>();
				for (Object x : listViewer.getCheckObjects()) {
					selection.add((ListItem)x);
				}
				
				Collection<ListItem> lastSelection = currentSelection;
				
				currentSelection = selection;
				
				if (!(lastSelection != null && selection.size() == lastSelection.size()
						&& selection.containsAll(lastSelection))){
					queryChanged();	
				}
			}
		});
		listViewer.setInput(Collections.singleton(new ListItem(Messages.AttributeListDropItem_LoadingLabel)));
		
		initDrag(main);
		initDrag(lblAttribute);
		
		lblAttribute.setText(formatStringForLabel(this.text));
		loadItemsJobs.schedule();
		QueryFilterConfigManager.getInstance().addChangeListener(queryConfChangeListener);
	}

}
