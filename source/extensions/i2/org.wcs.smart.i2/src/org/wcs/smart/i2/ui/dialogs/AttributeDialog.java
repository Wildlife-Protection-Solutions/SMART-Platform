package org.wcs.smart.i2.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelAttributeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.ui.AttributeTypeLabelProvider;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;

public class AttributeDialog extends TitleAreaDialog {

	private IntelAttribute attribute;
	
	private NameKeyComposite nameKeyInfo;
	private ComboViewer cmbType;
	private List<IntelAttribute> attributeSiblings;
	
	private AttributeListPanel listPanel;
	
	private Job siblingsJob = new Job("get siblings"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try{
				attributeSiblings = IntelAttributeManager.INSTANCE.getAttributes(s, SmartDB.getCurrentConservationArea());
				attributeSiblings.remove(attribute);
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					nameKeyInfo.initFields(attribute, attributeSiblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());
					
				}
				
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
	public AttributeDialog(Shell parentShell, IntelAttribute attribute) {
		super(parentShell);
		this.attribute = attribute;
	}
	
	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*1.4));
	}
	
	protected void okPressed() {
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.saveOrUpdate(attribute);
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
		
		if (attribute.getUuid() != null){
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
		
		initFields();
	}
	
	private void modified(){
		if (!nameKeyInfo.validate()){
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}else{
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		nameKeyInfo = new NameKeyComposite();
		nameKeyInfo.createControls(parent, true, attribute.getUuid() == null, new IChangeListener() {
			@Override
			public void itemModified() {
				if (!nameKeyInfo.validate()){
					nameKeyInfo.updateFields(attribute);
				}
				modified();
			}
		});
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Type:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		cmbType = new ComboViewer(parent);
		cmbType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(AttributeTypeLabelProvider.INSTANCE);
		cmbType.setInput(IAttributeType.values());
		cmbType.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				modified();
				
				IAttributeType type = (IAttributeType) ((IStructuredSelection)cmbType.getSelection()).getFirstElement();
				if (type != IAttributeType.LIST){
					//clean out list items
					listPanel.setVisible(false);
					attribute.setAttributeList(new ArrayList<IntelAttributeListItem>());
				}else{
					listPanel.setVisible(true);
				}
				attribute.setType(type);
				
			}
		});
		
		listPanel = new AttributeListPanel(parent, attribute);
		listPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		setTitle("Intelligence Attribute");
		getShell().setText("Intelligence Attribute");
		setMessage("Create or edit intelligence attributes.");
		
		
		
		return parent;
		
	}
	
	private void initFields(){
		if (attribute.getType() != null){
			cmbType.setSelection(new StructuredSelection(attribute.getType()));
		}else{
			cmbType.setSelection(new StructuredSelection(IAttributeType.NUMERIC));
		}
		
		cmbType.getControl().setEnabled(attribute.getUuid() == null);
		siblingsJob.setSystem(true);
		siblingsJob.schedule(0);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
