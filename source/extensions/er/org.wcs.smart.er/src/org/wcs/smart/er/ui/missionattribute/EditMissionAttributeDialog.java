package org.wcs.smart.er.ui.missionattribute;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.ui.properties.LanguageViewer;

public class EditMissionAttributeDialog extends TitleAreaDialog {
	
	
	private Collection<? extends NamedKeyItem> siblings;	//attributes that are siblings to the current attribute being updated/created
	private MissionAttribute toUpdate;	//attribute to update
	
	private Session currentSession;
	
	
	private ComboViewer cmbType;
	private TableViewer lstViewer;
	
	private Button btnAdd;
	private Button btnEdit;
	private Button btnDelete;
	
	private Text txtName;
	private Text txtKey;
	
	private ControlDecoration cdKey;
	private ControlDecoration cdTxt;
	
	private Composite listPanel;
	
	private HashMap<Language, String> copyNames;
	
	private boolean generateKey = true;
	
	/**
	 * creates a new dialog
	 * 
	 * @param parentShell
	 * @param toUpdate Attribute to update
	 * @param siblings Sibling attributes to the attribute being updated
	 * @param defaultLang the current language being modified
	 * @param currentSession active hibernate session
	 */
	public EditMissionAttributeDialog(Shell parentShell,  
			MissionAttribute toUpdate,  
			Collection<? extends NamedKeyItem> siblings, 
			Session currentSession) {
		
		super(parentShell);
		this.toUpdate = toUpdate;
		this.siblings = siblings;
		this.currentSession = currentSession;
		
		copyNames = new HashMap<Language, String>();
		for (org.wcs.smart.ca.Label l : toUpdate.getNames()){
			copyNames.put(l.getLanguage(), l.getValue());
		}
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		Composite myparent = (Composite) super.createDialogArea(parent);
			
		Composite composite = new Composite(myparent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(3, false));

		/* Type */
		Label lblNewLabel_2 = new Label(composite, SWT.NONE);
		lblNewLabel_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_2.setText("Type:");
		
		cmbType = new ComboViewer(composite, SWT.SIMPLE | SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				return ((Attribute.AttributeType)element).name();
			}
		});
		cmbType.setInput(new Object[]{
				Attribute.AttributeType.NUMERIC,
				Attribute.AttributeType.TEXT,
				Attribute.AttributeType.LIST
		});
		
		Combo combo = cmbType.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		if (toUpdate.getUuid() != null){
			combo.setEnabled(false);
		}
		
		combo.addSelectionListener(new SelectionAdapter() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getType() == Attribute.AttributeType.LIST){
					listPanel.setVisible(true);
				}else{
					listPanel.setVisible(false);
				}
				validate();
			}
		});
		
		Label l = new Label(composite, SWT.NONE);
		l.setText("Language:");
		
		final LanguageViewer lang = new LanguageViewer(composite, SWT.NONE, SmartDB.getCurrentConservationArea());
		lang.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		lang.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				generateKey = false;
				if (copyNames.get(lang.getCurrentSelection()) == null){
					txtName.setText("");
				}else{
					txtName.setText(copyNames.get(lang.getCurrentSelection()));
				}
			}
		});
		/* Name & Key */
		l = new Label(composite, SWT.NONE);
		l.setText("Name:");
		
		txtName = new Text(composite, SWT.BORDER);
		if (toUpdate.getName() != null){
			txtName.setText(toUpdate.getName());
		}
		if (toUpdate.getKeyId() == null){
			txtName.addListener(SWT.Modify, new Listener(){
				@Override
				public void handleEvent(Event event) {
					if (generateKey || (!generateKey && txtKey.getText().isEmpty())){
						generateKey = true;
						txtKey.setText(toUpdate.generateKey(txtName.getText(), siblings));
					}
					copyNames.put(lang.getCurrentSelection(), txtName.getText());
					validate();
				}});
		}
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cdTxt = createDecoration(txtName);
		
		l = new Label(composite, SWT.NONE);
		l.setText("Key ID:");
		
		txtKey = new Text(composite, SWT.BORDER);
		if (toUpdate.getKeyId() != null){
			txtKey.setText(toUpdate.getKeyId());
		}
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtKey.setEditable(false);
		cdKey = createDecoration(txtKey);
		
		Button btnEditKey = new Button(composite, SWT.PUSH);
		btnEditKey.setText("Change");
		btnEditKey.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!MessageDialog
						.openConfirm(
								getShell(),
								"Edit Key",
								"Modifying the keys will affect Cross Conservation Area analysis. Are you sure you want to continue?")) {
					return;
				}
				InputDialog id = new KeyInputDialog(getShell(), 
						txtKey.getText(), siblings);
				int ret = id.open();
				if (ret != Window.CANCEL) {
					txtKey.setText(id.getValue());
				}
				validate();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		listPanel = new Composite(composite, SWT.NONE);
		listPanel.setLayout(new GridLayout(2, false));
		listPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		lstViewer = new TableViewer(listPanel,SWT.BORDER);
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		lstViewer.setLabelProvider(AttributeLabelProvider.getInstance());
		lstViewer.setInput(toUpdate.getAttributeList());
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite buttonPnl = new Composite(listPanel, SWT.NONE);
		buttonPnl.setLayout(new GridLayout(1, false));
		buttonPnl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnAdd = new Button(buttonPnl, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		btnEdit = new Button(buttonPnl, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		btnDelete = new Button(buttonPnl, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		
		if (toUpdate.getKeyId() == null){
			getShell().setText("New Mission Attribute");
			setMessage("New Mission Attribute");
			setTitle("Create a new attribute for use with survey missions");
		}else{
			getShell().setText("Edit Mission Attribute");
			setMessage(MessageFormat.format("Edit: {0}", new Object[]{toUpdate.getName()}));
			setTitle("Modify the mission attribute.");
		}
		
		cmbType.setSelection(new StructuredSelection(AttributeType.NUMERIC));
		listPanel.setVisible(false);
		return composite;
	}
	
	
	private void validate(){
		boolean error = false;
		String errormsg = NamedKeyItem.validateKey(txtKey.getText(), siblings);
		if (errormsg != null){
			cdKey.setDescriptionText(errormsg);
			cdKey.show();
			error = true;
		}else{
			cdKey.hide();
		}
		
		boolean hide = true;
		for (Entry<Language, String> entry : copyNames.entrySet()){
			errormsg = DataModel.validateName(entry.getValue(), entry.getKey());
			if (errormsg != null){
				cdTxt.setDescriptionText(errormsg);
				cdTxt.show();
				error = true;
				hide = false;
			}
		}
		if (hide){
			cdTxt.hide();
		}
		
		if (getButton(OK) != null){
			getButton(OK).setEnabled(!error);
		}
	}
	
	
	private Attribute.AttributeType getType(){
		return (AttributeType) ((IStructuredSelection)cmbType.getSelection()).getFirstElement();
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.FINISH_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
		validate();
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
	
	/*
	 * updates the attribute to update with the
	 * information in the attribute panel
	 */
	private void updateAttribute(){
		
		toUpdate.setKeyId(txtKey.getText());
		for (Language l : toUpdate.getConservationArea().getLanguages()){
			toUpdate.updateName(l, copyNames.get(l));
		}
		
		toUpdate.setName(txtName.getText());
		toUpdate.setType(getType());
		
		if (toUpdate.getType() == AttributeType.LIST){
			toUpdate.setAttributeList(new ArrayList<MissionAttributeListItem>());
		}else{
			if (toUpdate.getAttributeList() != null){
				for (MissionAttributeListItem mi : toUpdate.getAttributeList()){
					mi.setAttribute(null);
				}
				toUpdate.getAttributeList().clear();
			}
		}
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			setReturnCode(OK);
			updateAttribute();
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
		}
		
		close();
	}
}
