package org.wcs.smart.i2.ui.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.AttributeListItemLabelProvider;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.ui.OnOffButton;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

public class NewEntityDialog extends TitleAreaDialog{

	private static final String LAST_TYPE_KEY = "org.wcs.smart.i2.ui.dialogs.newentity.lasttype";
	
	private TableComboViewer cmbEntityType;
	
	private Composite attributePanel;
	
	private List<Object> attributeControls;
	
	private Job loadEntityTypes = new Job("load entity types"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<IntelEntityType> types = new ArrayList<IntelEntityType>();
			Session s = HibernateManager.openSession();
//			s.addEventListeners(new IntelEntityListener());
			try{
				types.addAll(EntityTypeManager.INSTANCE.getEntityTypes(s, SmartDB.getCurrentConservationArea()));
			}finally{
				s.close();
			}
			
			IntelEntityType toSelect = types.isEmpty() ? null : types.get(0);
			String uuid = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(LAST_TYPE_KEY);
			if (uuid != null){
				try{
					UUID u = UuidUtils.stringToUuid(uuid);
					for (IntelEntityType t : types){
						if (t.getUuid().equals(u)){
							toSelect = t;
							break;
						}
					}
				}catch (Exception ex){
					//eat me
				}
			}
			
			final IntelEntityType sel = toSelect;
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					cmbEntityType.setInput(types);
					if (sel != null) cmbEntityType.setSelection(new StructuredSelection(sel));
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	public NewEntityDialog(Shell parentShell) {
		super(parentShell);
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
		
		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				cd.dispose();
				
			}
		});
		return cd;
	}
	
	protected void okPressed() {
		IntelEntityType type = null;
		Object x = ((IStructuredSelection)cmbEntityType.getSelection()).getFirstElement();
		if (x instanceof IntelEntityType){
			type = (IntelEntityType)x;
		}else{
			MessageDialog.openWarning(getShell(), "Error", "Invalid type selected for entity");
			//TODO: error
			return;
		}
		IntelEntity newEntity = new IntelEntity();
		newEntity.setEntityType(type);
		newEntity.setAttributes(new ArrayList<IntelEntityAttributeValue>());
		for (Object c : attributeControls){
			IntelAttribute attribute = null;
			if (c instanceof Control){
				attribute = (IntelAttribute) ((Control)c).getData();
			}else if (c instanceof Viewer){
				attribute = (IntelAttribute) ((Viewer)c).getControl().getData();
			}
			
			IntelEntityAttributeValue ev = new IntelEntityAttributeValue();
			ev.setAttribute(attribute);
			ev.setEntity(newEntity);
			
			boolean add = false;
			if (attribute.getType() == IAttributeType.BOOLEAN){
				if (((OnOffButton)c).isEnabled()){
					add = true;
					if (((OnOffButton)c).getSelection()){
						ev.setNumberValue(1d);
					}else{
						ev.setNumberValue(0d);
					}
				}
			}else if (attribute.getType() == IAttributeType.DATE){
				if (((DateTime)c).getEnabled()){
					add = true;
					ev.setStringValue( SmartUtils.getTime((DateTime)c).toString());
				}
			}else if (attribute.getType() == IAttributeType.LIST){
				IStructuredSelection selection = (IStructuredSelection)((ComboViewer)c).getSelection();
				if (!selection.isEmpty()){
					Object item = selection.getFirstElement();
					if (item instanceof IntelAttributeListItem){
						add = true;
						ev.setAttributeListItem((IntelAttributeListItem) item);
					}
				}
			}else if (attribute.getType() == IAttributeType.NUMERIC){
				try{
					String value = ((Text)c).getText();
					if (!value.trim().isEmpty()){
						Double d = Double.parseDouble(value);
						ev.setNumberValue(d);
						add = true;
					}
				}catch (Exception ex){
					//
				}
			}else if (attribute.getType() == IAttributeType.TEXT){
				String value = ((Text)c).getText();
				if (!value.trim().isEmpty()){
					ev.setStringValue(value.trim());
					add = true;
				}
			}
			if (add){
				newEntity.getAttributes().add(ev);
			}
		}
		
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			session.saveOrUpdate(newEntity);
			session.getTransaction().commit();
			
			super.okPressed();
		}catch (Exception ex){
			MessageDialog.openWarning(getShell(), "Error", "Could not save entity: " + ex.getMessage());
			Intelligence2PlugIn.log(ex.getMessage(), ex);
		}finally{
			session.close();
		}
		
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	private void modified(){
		boolean isError = false;
		getButton(IDialogConstants.OK_ID).setEnabled(!isError);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Entity Type:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		
		cmbEntityType = new TableComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbEntityType.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityType.setLabelProvider(EntityTypeLabelProvider.INSTANCE);
		cmbEntityType.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbEntityType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbEntityType.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)cmbEntityType.getSelection()).getFirstElement();
				if (x instanceof IntelEntityType){
					IntelEntityType type = (IntelEntityType)x;
					Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(LAST_TYPE_KEY, UuidUtils.uuidToString(type.getUuid()));
					configureAttributePanel(type);
				}
				
			}
		});
		l = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		attributePanel = new Composite(parent, SWT.NONE);
		attributePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		setTitle("New Entity");
		getShell().setText("New Entity");
		setMessage("Create new a new entity");
		
		loadEntityTypes.setSystem(true);
		loadEntityTypes.schedule(0);
		
		return parent;
	}
	
	private void configureAttributePanel(IntelEntityType type){
		Session s = HibernateManager.openSession();
		try{
			s.saveOrUpdate(type);
			for (IntelEntityTypeAttribute a : type.getAttributes()){
				a.getAttribute().getName();
				if (a.getAttribute().getAttributeList() != null){
					for (IntelAttributeListItem l : a.getAttribute().getAttributeList()){
						l.getName();
					}
				}
			}
		}finally{
			s.close();
		}
		
		for (Control c : attributePanel.getChildren()){
			c.dispose();
		}
		
		attributeControls = new ArrayList<Object>();
		attributePanel.setLayout(new GridLayout(2, false));
		
		createAttributeControl(type.getIdAttribute(), attributePanel);
		
		ScrolledComposite sc = new ScrolledComposite(attributePanel, SWT.V_SCROLL | SWT.BORDER);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		Composite content = new Composite(sc, SWT.NONE);
		sc.setContent(content);
		content.setLayout(new GridLayout(2, false));
		
		for (IntelEntityTypeAttribute a : type.getAttributes()){
			if (!a.getAttribute().equals(type.getIdAttribute())){
				createAttributeControl(a.getAttribute(), content);
			}
		}
		sc.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		attributePanel.layout(true);
		
	}

	private void createAttributeControl(IntelAttribute a, Composite parent){
		Label l = new Label(parent, SWT.NONE);
		l.setText(a.getName() + ":");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		if (a.getType() == IAttributeType.TEXT){
			Text txt= new Text(parent, SWT.BORDER);
			txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			txt.setData(a);
			txt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					modified();
				}
			});
			attributeControls.add(txt);
		}else if (a.getType() == IAttributeType.NUMERIC){
			Text txt= new Text(parent, SWT.BORDER);
			txt.setData(a);
			txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			final ControlDecoration cd = createDecoration(txt);
			txt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					try{
						Double.parseDouble(txt.getText());
						cd.hide();
					}catch(Exception ex){
						cd.show();
						cd.setDescriptionText("Unable to parse number from text");
					}
					modified();
				}
			});
			attributeControls.add(txt);
		}else if (a.getType() ==  IAttributeType.LIST){
			ComboViewer cmbViewer = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
			cmbViewer.setLabelProvider(AttributeListItemLabelProvider.INSTANCE);
			List<Object> items = new ArrayList<Object>();
			items.add("");
			items.addAll(a.getAttributeList());
			cmbViewer.setInput(items);
			cmbViewer.getControl().setData(a);
			cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					modified();
				}
			});
			cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			attributeControls.add(cmbViewer);
		}else if (a.getType() ==  IAttributeType.DATE){
			Composite t = new Composite(parent, SWT.NONE);
			t.setLayout(new GridLayout(2, false));
			((GridLayout)t.getLayout()).marginWidth = 0;
			((GridLayout)t.getLayout()).marginHeight = 0;
			
			Button btnCh = new Button(t, SWT.CHECK);
			btnCh.setSelection(false);
			
			DateTime txt= new DateTime(t, SWT.DROP_DOWN | SWT.DATE | SWT.LONG);
			txt.setEnabled(false);
			txt.setData(a);
			txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			txt.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					modified();
				}
			});
			btnCh.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					txt.setEnabled(btnCh.getSelection());
				}
			});
			attributeControls.add(txt);
		}else if (a.getType() ==  IAttributeType.BOOLEAN){
			Composite t = new Composite(parent, SWT.NONE);
			t.setLayout(new GridLayout(2, false));
			((GridLayout)t.getLayout()).marginWidth = 0;
			((GridLayout)t.getLayout()).marginHeight = 0;
			
			Button btnCh = new Button(t, SWT.CHECK);
			btnCh.setSelection(false);
			
			OnOffButton btn = new OnOffButton(t, SWT.TOGGLE);
			btn.setData(a);
			btn.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					modified();
				}
			});
			btnCh.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					btn.setEnabled(btnCh.getSelection());
				}
			});
			btn.setSelection(true);
		}
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
}