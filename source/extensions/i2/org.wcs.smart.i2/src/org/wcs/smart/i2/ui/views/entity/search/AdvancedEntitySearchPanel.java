package org.wcs.smart.i2.ui.views.entity.search;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.ui.SmartShellDialog;
import org.wcs.smart.i2.ui.views.query.dropitem.DateDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.OptionDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem.InputType;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;
import org.wcs.smart.ui.properties.DialogConstants;

public class AdvancedEntitySearchPanel extends Composite {

	private enum FilterOption{
		ENTITY_TYPE("Entity Type Filter"),
		ENTITY_ATTRIBUTE_TYPE("Entity Attribute Filter");
		
		public String guiName;
		
		FilterOption(String name){
			this.guiName = name;
		}
	}
	
	private EntitySearchPanel searchPanel;
	
	private Button btnAddFilter;
	private FilterOptionShell optionShell;
	
	public AdvancedEntitySearchPanel(Composite parent) {
		super(parent, SWT.NONE);
		createContents();
	}
	
	private void createContents(){
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		btnAddFilter = new Button(this, SWT.PUSH);
		btnAddFilter.setText("Add Filter...");
		btnAddFilter.addListener(SWT.Selection, e-> showFilterMenu(e));
		
		searchPanel = new EntitySearchPanel();
		Composite c = searchPanel.createComposite(this);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
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
			TableViewer optionsTable = new TableViewer(parent, SWT.BORDER);
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
					// TODO Auto-generated method stub
					FilterOption op = (FilterOption) ((IStructuredSelection)optionsTable.getSelection()).getFirstElement();
					if (op == FilterOption.ENTITY_TYPE){
						createEntityTypeDropItem();
						FilterOptionShell.this.close();
					}
				}
			});
			
			optionsTable.addSelectionChangedListener(new ISelectionChangedListener(){
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					FilterOption op = (FilterOption) ((IStructuredSelection)optionsTable.getSelection()).getFirstElement();
					switch(op){
					
					case ENTITY_TYPE:
//						createEntityTypeDropItem();
//						FilterOptionShell.this.close();
						break;
					case ENTITY_ATTRIBUTE_TYPE:
						//show
						if (attributes != null){
							attributeTable.setInput(attributes);
						}else{
							attributeTable.setInput(new String[]{DialogConstants.LOADING_TEXT});
							Job j = new Job("loading attributes"){

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
									Display.getDefault().syncExec(()->attributeTable.setInput(attributes));
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
			});
			
			
			attributeTable = new TableViewer(parent, SWT.BORDER);
			attributeTable.setContentProvider(ArrayContentProvider.getInstance());
			attributeTable.setLabelProvider(new RelationshipTypeLabelProvider());
			attributeTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			attributeTable.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					createAttributeDropItem();		
				}
			});
		}
		
		private void createAttributeDropItem(){
			if (attributeTable.getSelection().isEmpty()) return;
			
			Object selection = ((IStructuredSelection)attributeTable.getSelection()).getFirstElement();
			if (!(selection instanceof IntelAttribute)) return;
			
			IntelAttribute a = (IntelAttribute)selection;
			
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
		private void createEntityTypeDropItem(){
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
			searchPanel.addItem(dropItem);
		}
		
	}
}
