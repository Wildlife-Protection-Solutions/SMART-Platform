package org.wcs.smart.dataentry.dialog.composite;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

public class ListItemsConfigDialog extends AbstractPropertyJHeaderDialog {

	private TableViewer listViewer;
	private TableViewer names;

	private Session currentSession;
	private Attribute attribute;
	private ConfigurableModel editModel;

	private AttributeListItem currentNode;
	private CmAttributeListItem currentCmNode;
	
	protected ListItemsConfigDialog(Attribute attribute, ConfigurableModel editModel, Session currentSession) {
		super(Display.getDefault().getActiveShell(), Messages.ListItemsConfigDialog_Title);
		this.attribute = attribute;
		this.currentSession = currentSession;
		this.editModel = editModel;
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, true));

		listViewer = new TableViewer(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		listViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		listViewer.setLabelProvider(new NamedItemLabelProvider());
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		listViewer.setInput(attribute.getActiveListItems());
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((StructuredSelection)listViewer.getSelection()).getFirstElement();
				currentNode = null;
				currentCmNode = null;
				if (x instanceof AttributeListItem){
					currentNode = (AttributeListItem) x;
					currentCmNode = (CmAttributeListItem) currentSession.createCriteria(CmAttributeListItem.class).add(Restrictions.eq("listItem", currentNode)).uniqueResult();
				}
				names.refresh();
			}
		});

		names = new TableViewer(container, SWT.FULL_SELECTION | SWT.BORDER);
		names.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		names.setContentProvider(ArrayContentProvider.getInstance());
		names.getTable().setHeaderVisible(true);
		names.getTable().setLinesVisible(true);
		
		TableViewerColumn colLang = new TableViewerColumn(names, SWT.NONE);
		colLang.getColumn().setWidth(100);
		colLang.getColumn().setText("Language");
		colLang.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					return ((Language) element).getDisplayName();
				}
			  	return super.getText(element);
			}

		});
		
		TableViewerColumn colName = new TableViewerColumn(names, SWT.NONE);
		colName.getColumn().setWidth(150);
		colName.getColumn().setText("Configured Name");
		colName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					if (currentCmNode != null){
						String label = currentCmNode.findNameNull((Language) element);
						if (label != null){
							return label;
						}
					}
					if (currentNode != null){
						return currentNode.findName((Language) element);
					}
					return "";
				}
			  	return super.getText(element);
			}
			
			@Override
			public Color getForeground(Object element) {
				if (currentCmNode == null || currentCmNode.findNameNull((Language)element) == null){
					return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				}
				return null;
			}
		});
		
		colName.setEditingSupport(new EditingSupport(names) {
			private TextCellEditor editor =  new TextCellEditor(names.getTable());
		
			@Override
			protected void setValue(Object element, Object value) {
				Language lang = (Language)element;
				String newValue = (String)value;
				
				if (newValue.trim().length() == 0){
					
					if (currentCmNode != null){
						for (Label l : currentCmNode.getNames()){
							if (l.getLanguage().equals(lang)){
								currentCmNode.getNames().remove(l);
								l.setElement(null);
								l.setLanguage(null);
								currentSession.delete(l);
							}
						}
					}
				}else if(!currentNode.findName(lang).equals(newValue)){
					
					if (currentCmNode == null){
						currentCmNode = new CmAttributeListItem();
						currentCmNode.setConfigurableModel(editModel);
						currentCmNode.setListItem(currentNode);
						currentSession.save(currentCmNode);
					}
					currentCmNode.updateName(((Language)element), (String)value);
					for (Label l : currentCmNode.getNames()){
						currentSession.saveOrUpdate(l);
					}
					currentSession.saveOrUpdate(currentCmNode);
					currentSession.flush();
					
					
				}
				names.refresh();
				listViewer.refresh();
			}
			
			@Override
			protected Object getValue(Object element) {
				if (currentCmNode != null){
					String label = currentCmNode.findNameNull(((Language)element));
					if (label != null){
						return label;
					}
				}
				if (currentNode != null){
					return currentNode.findName(((Language)element));
				}
				return "";
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		TableViewerColumn dmName = new TableViewerColumn(names, SWT.NONE);
		dmName.getColumn().setWidth(150);
		dmName.getColumn().setText("Data Model Name");
		dmName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					if (currentNode != null){
						return currentNode.findName((Language) element);
					}else{
						return "";
					}
				}
			  	return super.getText(element);
			}
		});
		
		
		
		names.setInput(SmartDB.getCurrentConservationArea().getLanguages());
		
		setTitle(Messages.ListItemsConfigDialog_Title);
		setMessage(Messages.ListItemsConfigDialog_Message);

		setChangesMade(false);
		return container;
	
	}

	@Override
	protected boolean performSave() {
		// TODO Auto-generated method stub
		return false;
	}

}
