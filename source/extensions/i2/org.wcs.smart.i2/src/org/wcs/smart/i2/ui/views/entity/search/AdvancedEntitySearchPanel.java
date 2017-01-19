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
package org.wcs.smart.i2.ui.views.entity.search;

import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.search.AdvancedEntitySearch;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.SmartShellDialog;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.i2.ui.views.query.dropitem.DateDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.OptionDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem.InputType;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Advanced search panel for entity searches
 * 
 * @author Emily
 *
 */
public class AdvancedEntitySearchPanel extends Composite {

	private enum FilterOption{
		ENTITY_TYPE("Entity Type Filter..."),
		ENTITY_ATTRIBUTE_TYPE("Entity Attribute Filter..."),
		NOT(Operator.NOT.getLabel(Locale.getDefault())),
		BRACKET(" ( Brackets ) ");
		
		public String guiName;
		
		FilterOption(String name){
			this.guiName = name;
		}
	}
	
	private EntitySearchPanel searchPanel;
	
	private Button btnAddFilter;
	private FilterOptionShell optionShell;
	private Button btnSearch;
	
	private EntitySearchView view;
	private AdvancedEntitySearch search = null;
	
	public AdvancedEntitySearchPanel(Composite parent, EntitySearchView view, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.view = view;
		createContents(toolkit);
	}
	
	private void createContents(FormToolkit toolkit){
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		btnAddFilter = toolkit.createButton(this, "Add Filter ...", SWT.PUSH);
		btnAddFilter.addListener(SWT.Selection, e-> showFilterMenu(e));
		
		searchPanel = new EntitySearchPanel(){
			public String validate(){
				String validate = super.validate();
				
				btnSearch.setEnabled(validate == null);
				if (validate != null) btnSearch.setToolTipText(validate);
				return validate;
			}
		};

		
		Composite c = searchPanel.createComposite(this);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(c);
		
		Composite bottom = toolkit.createComposite(this);
		bottom.setLayout(new GridLayout(2, false));
		((GridLayout)bottom.getLayout()).marginWidth = 0;
		((GridLayout)bottom.getLayout()).marginHeight = 0;
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnSearch = toolkit.createButton(bottom, "Search", SWT.PUSH);
		btnSearch.addListener(SWT.Selection, e->doSearch());
		
		Hyperlink saveSearch = toolkit.createHyperlink(bottom, "Save Search", SWT.NONE);
		saveSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		saveSearch.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				saveSearch();
			}
		});
	}
	
	private void saveSearch(){
		configureSearch();	
		view.saveSearch(search);
	}
	private void doSearch(){
		configureSearch();
		view.doAdvancedSearch(search, 0);
	}
	
	private void configureSearch(){
		String error = searchPanel.validate();
		if (error != null){
			MessageDialog.openError(getShell(), "Search", "Invalid search filter.");
			return ;
		}
		
		if (search == null){
			search = new AdvancedEntitySearch();
		}
		search.setSearchString(searchPanel.getQueryPart());
	}
	
	private void showFilterMenu(Event e){
		optionShell = new FilterOptionShell(getShell());
		Rectangle r = btnAddFilter.getBounds();
		optionShell.open(btnAddFilter.toDisplay(r.x + r.width, r.y));
	}

	
	
	private class FilterOptionShell extends SmartShellDialog {

		private TableViewer optionsTable;
		private TableViewer attributeTable;
		
		private List<IntelAttribute> attributes = null;
		
		public FilterOptionShell(Shell owner){
			super(owner);
		}
		
		@Override
		public void createContents(Composite parent){
			parent.setLayout(new GridLayout(2, true));
			optionsTable = new TableViewer(parent, SWT.BORDER);
			optionsTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			optionsTable.setContentProvider(ArrayContentProvider.getInstance());
			optionsTable.setLabelProvider(new LabelProvider(){
				public String getText(Object element){
					if (element instanceof FilterOption){
						return ((FilterOption) element).guiName;
					}
					return super.getText(element);
				}
			});
			
			optionsTable.setInput(FilterOption.values());
			optionsTable.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					FilterOption op = (FilterOption) ((IStructuredSelection)optionsTable.getSelection()).getFirstElement();
					if (op == FilterOption.ENTITY_TYPE){
						createEntityTypeDropItem(null);
						FilterOptionShell.this.close();
					}else if (op == FilterOption.NOT){
						createNotDropItem();
						FilterOptionShell.this.close();
					}else if (op == FilterOption.BRACKET){
						createBracketDropItem();
						FilterOptionShell.this.close();
					}
				}
			});
			
			optionsTable.addSelectionChangedListener(event-> processOptionSelection());
			
			
			attributeTable = new TableViewer(parent, SWT.BORDER);
			attributeTable.setContentProvider(ArrayContentProvider.getInstance());
			attributeTable.setLabelProvider(new LabelProvider(){
				private EntityTypeLabelProvider typeLabelProvider = new EntityTypeLabelProvider();
				private AttributeLabelProvider attributeProvider = new AttributeLabelProvider();
				
				@Override
				public void dispose(){
					typeLabelProvider.dispose();
					attributeProvider.dispose();
				}
				
				@Override
				public String getText(Object element){
					if (element instanceof IntelEntityType){
						return typeLabelProvider.getText(element);
					}
					if (element instanceof IntelAttribute){
						return attributeProvider.getText(element);
					}
					return super.getText(element);
				}
				
				@Override
				public Image getImage(Object element){
					if (element instanceof IntelEntityType){
						return typeLabelProvider.getImage(element);
					}
					if (element instanceof IntelAttribute){
						return attributeProvider.getImage(element);
					}
					return super.getImage(element);
				}
				
			});
			attributeTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			attributeTable.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					if (attributeTable.getSelection().isEmpty()) return;
					
					Object x =((IStructuredSelection) attributeTable.getSelection()).getFirstElement();
					if (x instanceof IntelEntityType){
						createEntityTypeDropItem((IntelEntityType)x);	
						FilterOptionShell.this.close();
					}else if (x instanceof IntelAttribute){
						createAttributeDropItem((IntelAttribute)x);	
						FilterOptionShell.this.close();
					}
				}
			});
		}
		
		private void processOptionSelection(){
			FilterOption op = (FilterOption) ((IStructuredSelection)optionsTable.getSelection()).getFirstElement();
			switch(op){
			case NOT:
			case BRACKET:
				attributeTable.setInput(null);
				break;
			case ENTITY_TYPE:
				attributeTable.setInput(DialogConstants.LOADING_TEXT);
				Job entityJob = new Job("loading entities"){

					private List<IntelEntityType> entities;
					@SuppressWarnings("unchecked")
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						Session s = HibernateManager.openSession();
						try{
							entities = s.createCriteria(IntelEntityType.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
							.list();
							entities.forEach(ent->ent.getName());
						}finally{
							s.close();
						}
						Display.getDefault().syncExec(()->{if (!attributeTable.getControl().isDisposed()) attributeTable.setInput(entities);});
						return Status.OK_STATUS;
					}
				};
				entityJob.schedule();
				break;
			case ENTITY_ATTRIBUTE_TYPE:
				//show
				if (attributes != null){
					attributeTable.setInput(attributes);
				}else{
					attributeTable.setInput(new String[]{DialogConstants.LOADING_TEXT});
					Job j = new Job("loading attributes"){

						@SuppressWarnings("unchecked")
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							Session s = HibernateManager.openSession();
							try{
								List<IntelAttribute> ats = s.createCriteria(IntelAttribute.class)
								.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
								.list();
								ats.forEach(a->{
									a.getName();
									if (a.getType() == IntelAttribute.AttributeType.LIST){
										a.getAttributeList().forEach(li -> li.getName());
									}
								});
								attributes = ats;
							}finally{
								s.close();
							}
							Display.getDefault().syncExec(()->{if (!attributeTable.getControl().isDisposed()) attributeTable.setInput(attributes);});
							return Status.OK_STATUS;
						}
					};
					j.schedule();
				}
				break;
			default:
				break;
			
			}
		}
		
		
		private void createAttributeDropItem(IntelAttribute a){
			DropItem di = null;
			String key = "a:" + a.getType().key + ":" + a.getKeyId();
			switch(a.getType()){
			case BOOLEAN:
				di = new TextDropItem(a.getName(), key);
				break;
			case DATE:
				di = new DateDropItem(a.getName(), key);
				break;
			case LIST:
				String[] names = new String[a.getAttributeList().size()];
				String[] keys = new String[a.getAttributeList().size()];
				for (int i = 0; i < a.getAttributeList().size(); i ++){
					names[i] = a.getAttributeList().get(i).getName();
					keys[i] = a.getAttributeList().get(i).getKeyId();
				}
				di = new OptionDropItem(a.getName(), key, names, keys);
				break;
			case NUMERIC:
				di = new TextBoxDropItem(a.getName(), key, InputType.NUMERIC);
				break;
			case TEXT:
				di = new TextBoxDropItem(a.getName(), key, InputType.TEXT);
				break;
			default:
				break;
			
			}
			if (di != null){
				searchPanel.addItem(di);
			}
			
		}
		@SuppressWarnings("unchecked")
		private void createEntityTypeDropItem(IntelEntityType type){
			//TODO: do this outside a display thread
			
			String[] names = null;
			String[] keys = null;
			
			Session s = HibernateManager.openSession();
			try{
				List<IntelEntityType> types = s.createCriteria(IntelEntityType.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
				
				names = new String[types.size()];
				keys = new String[types.size()];
				for (int i = 0;i < types.size(); i ++){
					names[i] = types.get(i).getName();
					keys[i] = types.get(i).getKeyId();
				}
			}finally{
				s.close();
			}
			
			OptionDropItem dropItem = new OptionDropItem("Entity Type", "et", names, keys);
			if (type != null){
				dropItem.setInitialValue(type.getKeyId());
			}
			searchPanel.addItem(dropItem);
		}
		
		private void createNotDropItem(){
			TextDropItem di = new TextDropItem(FilterOption.NOT.guiName, Operator.NOT.getKey());
			searchPanel.addItem(di);
		}
		
		private void createBracketDropItem(){
			searchPanel.addItem(new TextDropItem("(", "("));
			searchPanel.addItem(new TextDropItem(")", ")"));
		}
	}
}
