package org.wcs.smart.i2.ui.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.IntelAttributeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.ImageComposite;
import org.wcs.smart.i2.ui.NamedItemViewerFilter;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

public class EntityTypeDialog extends TitleAreaDialog {

	private IntelEntityType type;
	private NameKeyComposite nameKeyInfo;
	private ImageComposite icon;
	private List<IntelEntityType> entityTypeSiblings;
	private TableViewer attributeList;
	private ComboViewer idAttribute;
	
	private ControlDecoration cdList;
	private ControlDecoration cdId;
	
	public EntityTypeDialog(Shell parentShell, IntelEntityType type) {
		super(parentShell);
		this.type = type;
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*1.4));
	}
	
	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	protected void okPressed() {
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.saveOrUpdate(type);
			s.getTransaction().commit();
		}catch (Exception ex){
			if (s.getTransaction().isActive())s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Unable to save changes: " +ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
		initFields();
	}
	
	private void modified(){
		boolean isError = false;
		if (nameKeyInfo.validate()){
			isError = true;
		}
		
		cdList.hide();
		if (type.getAttributes().isEmpty()){
			isError = true;
			cdList.setDescriptionText("At least one attribute must exist.");
			cdList.show();
		}
		cdId.hide();
		if (type.getIdAttribute() == null){
			isError = true;
			cdId.setDescriptionText("One attribute must be selected as the identifier for the attribute.");
			cdId.show();
		}
		
		getButton(IDialogConstants.OK_ID).setEnabled(!isError);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		nameKeyInfo = new NameKeyComposite();
		nameKeyInfo.createControls(parent, true, type.getUuid() == null, new IChangeListener() {
			@Override
			public void itemModified() {
				if (!nameKeyInfo.validate()){
					nameKeyInfo.updateFields(type);
				}
				modified();
			}
		});
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Icon:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		icon = new ImageComposite(parent);
		icon.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		icon.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				modified();
				type.setIcon(icon.getImage());
			}
		});
	
		l = new Label(parent, SWT.NONE);
		l.setText("Id Attribute:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		idAttribute = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		idAttribute.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		idAttribute.setContentProvider(ArrayContentProvider.getInstance());
		idAttribute.setLabelProvider(AttributeLabelProvider.INSTANCE);
		idAttribute.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)idAttribute.getSelection()).getFirstElement();
				if (x instanceof IntelEntityTypeAttribute){
					type.setIdAttribute(((IntelEntityTypeAttribute) x).getAttribute());
				}
				modified();
			}
		});
		cdId= createDecoration(idAttribute.getControl());
		
		l = new Label(parent, SWT.NONE);
		l.setText("Attributes:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		
		Composite attributeComp = new Composite(parent, SWT.NONE);
		attributeComp.setLayout(new GridLayout(2, false));
		attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
				
		attributeList = new TableViewer(attributeComp, SWT.BORDER | SWT.MULTI);
		attributeList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeList.setContentProvider(ArrayContentProvider.getInstance());
		attributeList.setLabelProvider(AttributeLabelProvider.INSTANCE);
		cdList = createDecoration(attributeList.getControl());
		
		Composite buttonComp = new Composite(attributeComp, SWT.NONE);
		buttonComp.setLayout(new GridLayout());
		buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnAdd = new Button(buttonComp, SWT.NONE);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAttribute();
			}
		});
		
		Button btnEdit = new Button(buttonComp, SWT.NONE);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editAttribute();
			}
		});
		
		Button btnDelete = new Button(buttonComp, SWT.NONE);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeAttributes();
			}
		});
		setTitle("Entity Type");
		getShell().setText("Entity Type");
		setMessage("Configure entity type");
		
		return parent;
	}
	
	private void addAttribute(){
		AttributeListDialog dialog = new AttributeListDialog(getShell(), type);
		if (dialog.open() == Window.OK){
			attributeList.refresh();
			idAttribute.refresh();
			modified();
		}
	}
	
	private void editAttribute(){
		Object x = ((IStructuredSelection)attributeList.getSelection()).getFirstElement();
		if (x instanceof IntelEntityTypeAttribute){
			IntelEntityTypeAttribute attribute = (IntelEntityTypeAttribute)x;
			
			AttributeDialog ad = new AttributeDialog(getShell(), attribute.getAttribute());
			ad.open();
			
			//refresh
			attributeList.refresh();
		}
	}
	
	private void removeAttributes(){
		
	}
	private void initFields(){
		if (type.getIcon() != null){
			icon.setImage(type.getIcon());
		}
		attributeList.setInput(type.getAttributes());
		List<IntelAttribute> idAttributes = new ArrayList<IntelAttribute>();
		for (IntelEntityTypeAttribute a : type.getAttributes()){
			idAttributes.add(a.getAttribute());
		}
		idAttribute.setInput(idAttributes);
		idAttribute.setSelection(new StructuredSelection(type.getIdAttribute()));
		siblingsJob.setSystem(true);
		siblingsJob.schedule(0);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
	private Job siblingsJob = new Job("get siblings"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try{
				entityTypeSiblings = EntityTypeManager.INSTANCE.getEntityTypes(s, SmartDB.getCurrentConservationArea());
				entityTypeSiblings.remove(type);
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					nameKeyInfo.initFields(type, entityTypeSiblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());					
					getButton(IDialogConstants.OK_ID).setEnabled(type.getUuid() == null);
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	
	
	private class AttributeListDialog extends TitleAreaDialog{
		private IntelEntityType type;
		private CheckboxTableViewer attributeList;
		private NamedItemViewerFilter filter;
		
		private Job loadAttributes = new Job("load attributes"){

			private List<IntelAttribute> attributes;
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					attributes = IntelAttributeManager.INSTANCE.getAttributes(s, type.getConservationArea());
				}finally{
					s.close();
				}
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						attributeList.setInput(attributes);
					}					
				});
				return Status.OK_STATUS;
			}
			
		};
		public AttributeListDialog(Shell parentShell, IntelEntityType type) {
			super(parentShell);
			this.type = type;
		}

		@Override
		protected Point getInitialSize() {
			Point p = super.getInitialSize();
			return new Point(p.x,(int)(p.y*1.4));
		}

		
		protected void okPressed() {
			for (Object selection : attributeList.getCheckedElements()){
				if (selection instanceof IntelAttribute){
					IntelEntityTypeAttribute a  = new IntelEntityTypeAttribute();
					a.setAttribute((IntelAttribute) selection);
					a.setEntityType(type);
					type.getAttributes().add(a);
				}
			}
			super.okPressed();
		}

		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		}
		
		
		@Override
		protected Control createDialogArea(Composite parent) {
			parent = (Composite) super.createDialogArea(parent);
			parent = new Composite(parent, SWT.NONE);
			parent.setLayout(new GridLayout());
			parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			FilterComposite typeFilter = new FilterComposite(parent, SWT.NONE);
			typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			typeFilter.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					filter.setFilterString(typeFilter.getPatternFilter());
				}
			});
			
			attributeList = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.MULTI);
			attributeList.setContentProvider(ArrayContentProvider.getInstance());
			attributeList.setLabelProvider(AttributeLabelProvider.INSTANCE);
			attributeList.setInput(new String[]{"Loading..."});
			attributeList.getControl().setFocus();
			attributeList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			attributeList.getTable().addKeyListener(new KeyAdapter() {
				//spacebar check
				@Override
				public void keyPressed(KeyEvent e) {
					if (attributeList.getSelection().isEmpty()){
						return;
					}
					if (e.keyCode == SWT.SPACE){
						IStructuredSelection selection = ((IStructuredSelection)attributeList.getSelection());
						selection.getFirstElement();
						boolean value = attributeList.getChecked(selection.getFirstElement() );
						for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
							Object tp = (Object) iterator.next();
							attributeList.setChecked(tp, !value);
						}
						e.doit = false;
								
					}
					
				}
			});
			filter = new NamedItemViewerFilter(attributeList);
			attributeList.setFilters(new ViewerFilter[]{filter});
			
			
			Button btnNew = new Button(parent, SWT.PUSH);
			btnNew.setText("Create New Attribute");
			btnNew.addSelectionListener(new SelectionAdapter() {
				
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (newAttribute()){
						AttributeListDialog.this.okPressed();
					}
				}
			});
			setTitle("Entity Type Attributes");
			getShell().setText("Entity Type Attributes");
			setMessage(MessageFormat.format("Add attributes for entity type {0}", type.getName()));
			
			loadAttributes.setSystem(true);
			loadAttributes.schedule(0);
			
			return parent;
		}
		
		private boolean newAttribute(){
			IntelAttribute attribute = new IntelAttribute();
			attribute.setConservationArea(SmartDB.getCurrentConservationArea());
			
			if (type.getAttributes() == null) type.setAttributes(new ArrayList<IntelEntityTypeAttribute>());
			AttributeDialog ad = new AttributeDialog(getShell(), attribute);
			ad.open();
			
			if (attribute.getUuid() != null){
				//not saved
				
				//refresh
				IntelEntityTypeAttribute eta = new IntelEntityTypeAttribute();
				eta.setAttribute(attribute);
				eta.setEntityType(type);
				type.getAttributes().add(eta);
				
				attributeList.refresh();
				return true;
			}else{
				return false;
			}
		}
		
		
		@Override
		public boolean isResizable(){
			return true;
		}	
	}
}