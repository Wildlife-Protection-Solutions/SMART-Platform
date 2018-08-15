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
package org.wcs.smart.i2.ui.views.entity.search;

import java.text.Collator;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.search.AdvancedEntitySearch;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.views.query.dropitem.DateDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.ErrorDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.OptionDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem.InputType;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextOperatorDropItem;
import org.wcs.smart.ui.SmartShellDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SharedUtils;

/**
 * Advanced search composite 
 * 
 * @author Emily
 *
 */
public abstract class EntitySearchPanel extends Composite {

	private enum FilterOption{
		ENTITY_TYPE(Messages.AdvancedEntitySearchPanel_EntityTypeFilterLabel),
		ENTITY_ATTRIBUTE_TYPE(Messages.AdvancedEntitySearchPanel_AttributeFilterLabel),
		NOT(Operator.NOT.getLabel(Locale.getDefault())),
		BRACKET(Messages.AdvancedEntitySearchPanel_bracketsFilterLabel);
		
		public String guiName;
		
		FilterOption(String name){
			this.guiName = name;
		}
	}
	
	
	private EntitySearchDropPanel searchPanel;
	private FilterOptionShell optionShell;
	private Button btnAddFilter;
	private Button btnSearch;
	private FormToolkit toolkit;
	private ToolBar toolbar;
	
	public EntitySearchPanel(Composite parent) {
		super(parent, SWT.NONE);
		toolkit = new FormToolkit(parent.getDisplay());
		addListener(SWT.Dispose, e->toolkit.dispose());
		createContent();
	}

	/**
	 * Save the search to the database
	 */
	public abstract void saveSearch();
	
	/**
	 * Run the search and display the results
	 */
	public abstract void doSearch();
	
	public ToolBar getToolbar() {
		return this.toolbar;
	}
	
	void clearPanel() {
		searchPanel.clear();
	}
	
	private void createContent() {
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		Composite top = new Composite(this, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnAddFilter = toolkit.createButton(top, Messages.AdvancedEntitySearchPanel_addFilterBtn, SWT.PUSH);
		btnAddFilter.addListener(SWT.Selection, e-> showFilterMenu(e));
		btnAddFilter.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		
		toolbar = new ToolBar(top, SWT.FLAT);
		toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		ToolItem clear = new ToolItem(toolbar, SWT.PUSH);
		clear.setToolTipText(Messages.AdvancedEntitySearchPanel_Cleartooltip);
		clear.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CLEAR));
		clear.addListener(SWT.Selection, (event)->clearPanel());
		clear.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		
		searchPanel = new EntitySearchDropPanel(){
			public String validate(){
				String validate = super.validate();
				
				btnSearch.setEnabled(validate == null);
				if (validate != null) btnSearch.setToolTipText(validate);
				return validate;
			}
		};
		searchPanel.addQueryModifiedListener((event)->searchPanel.validate());
		Composite c = searchPanel.createComposite(this);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(c);
		
		Composite bottom = toolkit.createComposite(this);
		bottom.setLayout(new GridLayout(2, false));
		((GridLayout)bottom.getLayout()).marginWidth = 0;
		((GridLayout)bottom.getLayout()).marginHeight = 0;
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnSearch = toolkit.createButton(bottom, Messages.AdvancedEntitySearchPanel_SaveButton, SWT.PUSH);
		btnSearch.addListener(SWT.Selection, e->doSearch());
		btnSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		
		Hyperlink saveSearch = toolkit.createHyperlink(bottom, Messages.AdvancedEntitySearchPanel_SaveSearchlink, SWT.NONE);
		saveSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		saveSearch.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				saveSearch();
			}
		});
		saveSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());

	}
	
	public void addQueryModifiedListener(Listener l) {
		searchPanel.addQueryModifiedListener(l);
	}
	
	private void showFilterMenu(Event e){
		optionShell = new FilterOptionShell(getShell());
		Rectangle r = btnAddFilter.getBounds();
		optionShell.open(btnAddFilter.toDisplay(r.x + r.width, r.y));
	}
	
	public String getQueryString() {
		return searchPanel.getQueryPart();
	}
	
	public String validate() {
		return searchPanel.validate();
	}
	
	public void initPanel(String searchString){
		searchPanel.clear();
		
		List<DropItem> toAdd = new ArrayList<DropItem>();
		
		String[] parts = searchString.split("\\|"); //$NON-NLS-1$
		
		for(String p : parts){
			if (p.equalsIgnoreCase(Operator.BRACKET_OPEN.getKey())){
				toAdd.add(createOpenBracketDropItem());
			}else if (p.equalsIgnoreCase(Operator.BRACKET_CLOSE.getKey())){
				toAdd.add(createCloseBracketDropItem());
			}else if (p.equalsIgnoreCase(Operator.NOT.getKey())){
				toAdd.add(createNotDropItem());
			}else if (p.equalsIgnoreCase(Operator.AND.getKey())){
				OptionDropItem di = OptionDropItem.createAndOrDropItem(true);
				di.setInitialValue(Operator.AND.getKey());
				toAdd.add(di);
			}else if (p.equalsIgnoreCase(Operator.OR.getKey())){
				OptionDropItem di = OptionDropItem.createAndOrDropItem(true);
				di.setInitialValue(Operator.OR.getKey());
				toAdd.add(di);
			}else if (p.startsWith(AdvancedEntitySearch.ENTITYTYPE_KEY)){
				String entityTypeKey = p.split("=")[1].trim(); //$NON-NLS-1$
				toAdd.add(createEntityTypeDropItem(entityTypeKey));				
			}else if (p.startsWith(AdvancedEntitySearch.ATTRIBUTE_KEY + ":")){ //$NON-NLS-1$
				String[] bits = p.split(" ")[0].split(":"); //$NON-NLS-1$ //$NON-NLS-2$
				String key = bits[2];
				IntelAttribute ia = null;
				try(Session session = HibernateManager.openSession()){
					ia = QueryFactory.buildQuery(session, IntelAttribute.class,
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
							new Object[] {"keyId", key}).uniqueResult(); //$NON-NLS-1$
					
					if (ia != null && ia.getType() == AttributeType.LIST){
						String listKey = p.split(" ")[2]; //$NON-NLS-1$
						boolean found = false;
						if (ia.getAttributeList() != null){
							for (IntelAttributeListItem listitem : ia.getAttributeList()){
								listitem.getName();
								if (listitem.getKeyId().equalsIgnoreCase(listKey)){
									found = true;
								}
							}
						}
						if (!found){
							toAdd.add(new ErrorDropItem(MessageFormat.format(Messages.AdvancedEntitySearchPanel_listitemnotfound, listKey, ia.getName())));
							continue;
						}
					}
				}
				if (ia == null){
					toAdd.add(new ErrorDropItem(MessageFormat.format(Messages.AdvancedEntitySearchPanel_attributenotfound, key)));
				}else{
					try{
						DropItem di = createAttributeDropItem(ia);
						if (ia.getType() == IntelAttribute.AttributeType.TEXT){
							String[] queryParts = p.split(" "); //$NON-NLS-1$
							Operator op = Operator.parse(queryParts[1]);
							int startIndex = p.indexOf(queryParts[2], queryParts[0].length());
							String strValue = p.substring(startIndex).trim();
							String value = SharedUtils.stripQuotes(strValue);
							((TextBoxDropItem)di).setInitialValue(op, value);
						}else if (ia.getType() == IntelAttribute.AttributeType.NUMERIC){
							String[] queryParts = p.split(" "); //$NON-NLS-1$
							Operator op = Operator.parse(queryParts[1]);
							String value = queryParts[2];
							((TextBoxDropItem)di).setInitialValue(op, value);
						}else if (ia.getType() == IntelAttribute.AttributeType.DATE){
							String[] queryParts = p.split(" "); //$NON-NLS-1$
							Operator op = Operator.parse(queryParts[1]);
							Date d1 = (new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR )).parse(queryParts[2]);
							Date d2 = (new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR )).parse(queryParts[4]);
							((DateDropItem)di).setInitialValue(op, d1, d2);
						}else if (ia.getType() == IntelAttribute.AttributeType.LIST){
							String listKey = p.split(" ")[2]; //$NON-NLS-1$
							((OptionDropItem)di).setInitialValue(listKey);
						}
						toAdd.add(di);
					}catch (Exception ex){
						toAdd.add(new ErrorDropItem(MessageFormat.format(Messages.AdvancedEntitySearchPanel_ParseError, ex.getMessage())));
					}
				}
					
			}
		}
		searchPanel.initializeItems(toAdd);
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
					List<DropItem> dis = null;
					if (op == FilterOption.ENTITY_TYPE){
						DropItem di = createEntityTypeDropItem(null);
						if (di != null) dis = Collections.singletonList(di);
					}else if (op == FilterOption.NOT){
						DropItem di = createNotDropItem();
						if (di != null) dis = Collections.singletonList(di);
					}else if (op == FilterOption.BRACKET){
						dis = createBracketDropItem();
					}
					if (dis != null){
						dis.forEach(di -> searchPanel.addItem(di));
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
					DropItem di = null;
					if (x instanceof IntelEntityType){
						di = createEntityTypeDropItem(((IntelEntityType)x).getKeyId());
					}else if (x instanceof IntelAttribute){
						di = createAttributeDropItem((IntelAttribute)x);
					}
					if (di != null){
						searchPanel.addItem(di);
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
				Job entityJob = new Job(Messages.AdvancedEntitySearchPanel_LoadingEntitiesJobName){

					private List<IntelEntityType> entities;
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try(Session s = HibernateManager.openSession()){
							entities = QueryFactory.buildQuery(s,IntelEntityType.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
							entities.forEach(ent->ent.getName());
						}
						entities.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
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
					Job j = new Job(Messages.AdvancedEntitySearchPanel_loadingAttributeJobName){
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try(Session s = HibernateManager.openSession()){
								List<IntelAttribute> ats = QueryFactory.buildQuery(s, IntelAttribute.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
								ats.forEach(a->{
									a.getName();
									if (a.getType() == IntelAttribute.AttributeType.LIST){
										a.getAttributeList().forEach(li -> li.getName());
									}
								});
								
								attributes = ats;
							}
							attributes.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));
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
	}
	
	
	private DropItem createAttributeDropItem(IntelAttribute a){
		DropItem di = null;
		String key = AdvancedEntitySearch.ATTRIBUTE_KEY + ":" + a.getType().key + ":" + a.getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$
		switch(a.getType()){
		case BOOLEAN:
			di = new TextDropItem(a.getName(), key);
			break;
		case DATE:
			di = new DateDropItem(a.getName(), key, true);
			break;
		case LIST:
			String[] names = new String[a.getAttributeList().size()];
			String[] keys = new String[a.getAttributeList().size()];
			for (int i = 0; i < a.getAttributeList().size(); i ++){
				names[i] = a.getAttributeList().get(i).getName();
				keys[i] = a.getAttributeList().get(i).getKeyId();
			}
			di = new OptionDropItem(a.getName(), key, names, keys, true);
			break;
		case NUMERIC:
			di = new TextBoxDropItem(a.getName(), key, InputType.NUMERIC, true);
			break;
		case TEXT:
			di = new TextBoxDropItem(a.getName(), key, InputType.TEXT, true);
			break;
		default:
			break;
		
		}
		return di;
	}
	
	private DropItem createEntityTypeDropItem(String entityTypeKey){
		String[][] values = new String[2][];
		Job j = new Job(Messages.AdvancedEntitySearchPanel_loadingEntityTypeJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try(Session s = HibernateManager.openSession()){
					List<IntelEntityType> types = QueryFactory.buildQuery(s, IntelEntityType.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$				
					values[0] = new String[types.size()];
					values[1] = new String[types.size()];
					for (int i = 0;i < types.size(); i ++){
						values[0][i] = types.get(i).getName();
						values[1][i] = types.get(i).getKeyId();
					}
				}
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			return new ErrorDropItem(e.getMessage());
		}
		
		OptionDropItem dropItem = new OptionDropItem(Messages.AdvancedEntitySearchPanel_EntityTypeOptionDropItemName, AdvancedEntitySearch.ENTITYTYPE_KEY, values[0], values[1], true);
		if (entityTypeKey != null){
			dropItem.setInitialValue(entityTypeKey);
		}
		return dropItem;
	}
	
	private TextOperatorDropItem createNotDropItem(){
		return new TextOperatorDropItem(Operator.NOT);
	}
	
	private List<DropItem> createBracketDropItem(){
		List<DropItem> newList = new ArrayList<DropItem>();
		newList.add(createOpenBracketDropItem());
		newList.add(createCloseBracketDropItem());
		return newList;
	}
	
	private TextOperatorDropItem createOpenBracketDropItem(){
		return new TextOperatorDropItem(Operator.BRACKET_OPEN);
	}
	private TextOperatorDropItem createCloseBracketDropItem(){
		return new TextOperatorDropItem(Operator.BRACKET_CLOSE);
	}
}
