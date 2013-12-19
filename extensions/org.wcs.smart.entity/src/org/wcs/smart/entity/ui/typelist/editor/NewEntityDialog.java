package org.wcs.smart.entity.ui.typelist.editor;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.Entity.Status;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

public class NewEntityDialog extends TitleAreaDialog {

	private EntityType etype;
	private EntityAttributeComposite eaComp;
	private Entity newEntity;
	
	public NewEntityDialog(Shell parentShell, 
			EntityType etype,
			Entity toUpdate) {
		super(parentShell);
		
		this.etype = etype;
		
		if (toUpdate == null){
			newEntity = new Entity();
			newEntity.setAttributes(new ArrayList<EntityAttributeValue>());
			newEntity.setEntityType(etype);
			newEntity.setStatus(Status.ACTIVE);
		}else{
			newEntity = toUpdate;
		}
	}

	public Entity getEntity(){
		return this.newEntity;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.OK_ID).setText(DialogConstants.SAVE_TEXT);
	}
	
	@Override
	public boolean close(){
		return super.close();
	}
	
	protected boolean save(){
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			s.saveOrUpdate(etype.getDmAttribute());
			
			AttributeListItem ai = new AttributeListItem();
			ai.setIsActive(newEntity.getStatus() == Status.ACTIVE);
			ai.setAttribute(etype.getDmAttribute());
			ai.setListOrder(etype.getDmAttribute().getAttributeList().size());
			ai.setName(newEntity.getId());
			ai.setKeyId(NamedKeyItem.generateKey(newEntity.getId(),
					etype.getDmAttribute().getAttributeList()));
			
			etype.getDmAttribute().getAttributeList().add(ai);
			
			newEntity.setAttributeListItem(ai);
			
			s.saveOrUpdate(newEntity);
			s.getTransaction().commit();
		}catch (Exception ex){
			EntityPlugIn.displayLog("Error saving entity." + "\n\n" + ex.getMessage(), ex);
			s.getTransaction().rollback();
		}finally{
			s.close();
		}
		return true;
	}
	
	@Override
	protected void okPressed() {
		if (save()){
			super.okPressed();
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		super.getShell().setText("New Entity");
		setTitle("New Entity");
		super.setMessage(MessageFormat.format("Create a new entity of type {0}", new Object[]{etype.getName()}));
		
		Session s = HibernateManager.openSession();
		s.saveOrUpdate(etype);
		for (EntityAttribute a : etype.getAttributes()){
			s.saveOrUpdate(a.getDmAttribute());
		}
		Composite composite = (Composite) super.createDialogArea(parent);
		composite = new Composite(composite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout());
		
		eaComp = new EntityAttributeComposite();
		eaComp.setEntityType(etype);
		eaComp.createEditComposite(composite);
		
		super.getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				eaComp.dispose();
			}
		});
		eaComp.setEditEntity(newEntity);
		s.close();
		return composite;
	}

	protected boolean isResizable() {
		return true;
	}

	
}
