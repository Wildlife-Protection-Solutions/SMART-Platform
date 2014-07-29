package org.wcs.smart.er.ui.missionattribute;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.ca.properties.AddAttributeDialog2;
import org.wcs.smart.ui.properties.DialogConstants;

public class MissionAttributeDialog extends TitleAreaDialog implements SelectionListener{

	private TableViewer lstAttributes;
	private Button btnAdd;
	private Button btnDelete;
	private Button btnEdit;
	
	private List<MissionAttribute> attributes;
	
	private Session session;
	
	public MissionAttributeDialog(Shell parentShell) {
		super(parentShell);
	}

	public boolean close(){
		
		if (session.getTransaction().isActive()){
			session.getTransaction().rollback();
		}
		session.close();
		return super.close();
	}
	
	protected Control createDialogArea(Composite parent) {
		session = HibernateManager.openSession();
		session.beginTransaction();
		
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstAttributes = new TableViewer(main, SWT.BORDER);
		lstAttributes.setContentProvider(ArrayContentProvider.getInstance());
		lstAttributes.setLabelProvider(AttributeLabelProvider.getInstance());
		lstAttributes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite btnComp = new Composite(main, SWT.NONE);
		btnComp.setLayout(new GridLayout(1, false));
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnAdd = new Button(btnComp, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAdd.addSelectionListener(this);
		
		btnDelete = new Button(btnComp, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnEdit = new Button(btnComp, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lstAttributes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				enableButtons();
			}
		});
		
		initData();
		enableButtons();
		
		setTitle("Mission Attributes");
		setMessage("A list of all possible mission attributes for the Conservation Area");
		getShell().setText("Mission Attributes");
		return main;
	}
	
	private void addAttribute(){
		
		MissionAttribute ma = new MissionAttribute();
		ma.setConservationArea(SmartDB.getCurrentConservationArea());
		
		EditMissionAttributeDialog dialog = new EditMissionAttributeDialog(
				getShell(), ma, attributes, session);
		if (dialog.open() == OK){
			attributes.add(ma);
			session.save(ma);
			
			lstAttributes.refresh();
		}
	}
	
	private void enableButtons(){
		boolean isSelected = lstAttributes.getSelection().isEmpty();
		
		btnDelete.setEnabled(!isSelected);
		btnEdit.setEnabled(!isSelected);
	}
	
	private void initData(){
		attributes = session.createCriteria(MissionAttribute.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
		lstAttributes.setInput(attributes);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.widget == btnAdd){
			addAttribute();
		}else if (e.widget == btnDelete){
			
		}else if (e.widget == btnEdit){
			
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
}
