package org.wcs.smart.entity.ui.typelist.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

public class EntityTypeEntitiesPage extends EditorPart {
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private Form form;

	private TableViewer entityTable;
	private EntityTypeEditor parentEditor;
	private EntityAttributeComposite entityAttributes;
	
	private Composite entityDetails;
		
	/**
	 * Creates a new plan editor page
	 * @param editor
	 */
	public EntityTypeEntitiesPage(EntityTypeEditor editor){
		this.parentEditor = editor;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit.setBorderStyle(SWT.BORDER);
		form = toolkit.createForm(parent);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		

		GridLayout glayout = new GridLayout();
		glayout.verticalSpacing = 0;
		glayout.marginHeight = 0;
		form.getBody().setLayout(glayout);
		
		final Section entityList = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED );
		entityList.setLayout(new GridLayout(1, false));
		entityList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityList.setText("Entity List");
		
		Composite main = toolkit.createComposite(entityList);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityList.setClient(main);
		
		// --- attribute table list
		entityTable = new TableViewer(main, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		entityTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityTable.setContentProvider(ArrayContentProvider.getInstance());
		entityTable.getTable().setHeaderVisible(true);
		entityTable.getTable().setLinesVisible(true);
		entityTable.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editEntity();
			}
		});
		
		Composite buttonTableComp = toolkit.createComposite(main);
		buttonTableComp.setLayout(new GridLayout(4, false));
		
		Button btnAdd = toolkit.createButton(buttonTableComp, DialogConstants.ADD_BUTTON_TEXT, SWT.PUSH);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)btnAdd.getLayoutData()).widthHint = 100;
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addEntity();
			}
		});
		
		Button btnEdit = toolkit.createButton(buttonTableComp, DialogConstants.EDIT_BUTTON_TEXT, SWT.PUSH);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)btnEdit.getLayoutData()).widthHint = 100;
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editEntity();
			}
		});
		
		Button btnDelete = toolkit.createButton(buttonTableComp, DialogConstants.DELETE_BUTTON_TEXT, SWT.PUSH);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)btnDelete.getLayoutData()).widthHint = 100;
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteEntity();
			}
		});

		
		final Section entityDetailsSection = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		entityDetailsSection.setLayout(new GridLayout(1, false));
		entityDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityDetailsSection.setText("Entity Details");
		entityDetailsSection.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				if (entityDetailsSection.isExpanded()){
					entityDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));			
				}else{
					entityDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}
				entityDetailsSection.getParent().layout(true, true);
			}
		});
		entityDetails = toolkit.createComposite(entityDetailsSection);
		entityDetailsSection.setClient(entityDetails);
		entityDetails.setLayout(new GridLayout());
		entityDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	
		entityAttributes = new EntityAttributeComposite();
		
		entityTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Entity e = (Entity) ((IStructuredSelection)entityTable.getSelection()).getFirstElement();
				entityAttributes.setEntity(e);
				
			}
		});
	}
	
	
	private void addEntity(){
		NewEntityDialog dialog = new NewEntityDialog(getSite().getShell(), parentEditor.getEntityType(),null);
		if (dialog.open() == NewEntityDialog.OK){
			EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_ADDED, dialog.getEntity());
		}
		
	}
	
	
	private void deleteEntity(){
		//TODO:
	}
	
	private void editEntity(){
		//TODO:
	}
	
	/**
	 * Updates the widgets with the value from the plan.
	 */
	public void initValues() {
		//TODO: look at passing a current session to this function
		
		EntityType type = this.parentEditor.getEntityType();
		form.setText(getEditorInput().getName());
		
		Session s = HibernateManager.openSession();
		try{
			s.update(type);
			type.getNames().size();

			//create entity table columns
			for (TableColumn c : entityTable.getTable().getColumns()){
				c.dispose();
			}
			
			TableViewerColumn column = new TableViewerColumn(entityTable, SWT.NONE);
			column.getColumn().setText(Entity.ID_FIELD_NAME);
			column.getColumn().setWidth(160);
			column.setLabelProvider(new ColumnLabelProvider(){
				public String getText(Object element){
					if (element instanceof Entity){
						return ((Entity) element).getId();
					}
					return super.getText(element);
				}
			});
			
			column = new TableViewerColumn(entityTable, SWT.NONE);
			column.getColumn().setText(Entity.STATUS_FIELD_NAME);
			column.getColumn().setWidth(160);
			column.setLabelProvider(new ColumnLabelProvider(){
				public String getText(Object element){
					if (element instanceof Entity){
						return ((Entity) element).getStatus().getGuiName();
					}
					return super.getText(element);
				}
			});
			
			for (final EntityAttribute ea : type.getAttributes()){
				
				
				if (ea.getIsPrimary()){
					//only show primary columns in this table
					column = new TableViewerColumn(entityTable, SWT.NONE);
					column.getColumn().setText(ea.getName());
					column.getColumn().setWidth(160);
					column.setLabelProvider(new ColumnLabelProvider(){
						public String getText(Object element){
							if (element instanceof Entity){
								Entity e = (Entity)element;
								EntityAttributeValue value = e.findAttribute(ea);
								return value.getValueAsString();
							}
							return super.getText(element);
						}
					});
				}
			}
			entityTable.setInput(type.getEntities());
			
			for(Control c : entityDetails.getChildren()){
				c.dispose();
			}
			entityAttributes.setEntityType(type);
			entityAttributes.createComposite(entityDetails);
			entityDetails.layout(true);
		}finally{
			s.close();
		}
		
		
		
	}
	
	@Override
	public void setFocus() {
		entityTable.getTable().setFocus();
	}

}