package org.wcs.smart.entity.ui.typelist.editor;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.Entity.Status;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

public class NewEntityDialog extends TitleAreaDialog {

	private EntityType etype;
	private EntityEditPanelComposite eaComp;
	private Entity newEntity;
	
	private Session session;
	
	public NewEntityDialog(Shell parentShell, 
			EntityType etype,
			Entity toUpdate) {
		super(parentShell);
		
		session = HibernateManager.openSession();
		this.etype = (EntityType) session.load(EntityType.class, etype.getUuid());
		
		if (toUpdate == null){
			newEntity = new Entity();
			newEntity.setAttributes(new ArrayList<EntityAttributeValue>());
			newEntity.setEntityType(etype);
			newEntity.setStatus(Status.ACTIVE);
		}else{
			newEntity = (Entity) session.load(Entity.class,toUpdate.getUuid());
		}
	}

	public Entity getEntity(){
		return this.newEntity;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText(DialogConstants.SAVE_TEXT);
	}
	
	@Override
	public boolean close(){
		session.close();
		return super.close();
	}
	
	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		if (p.y > 500){
			p.y = 500;
		}
		if (p.x > 500){
			p.x = 500;
		}
		return p;
	}
	
	protected boolean save(){
		boolean isNew = newEntity.getUuid() == null;
		session.beginTransaction();
		try{
			if (isNew){
				eaComp.updateEntity();
				
				AttributeListItem ai = new AttributeListItem();
				ai.setIsActive(newEntity.getStatus() == Status.ACTIVE);
				ai.setAttribute(etype.getDmAttribute());
				ai.setListOrder(etype.getDmAttribute().getAttributeList().size());
				ai.setName(newEntity.getId());
				ai.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), newEntity.getId());
				ai.updateName(SmartDB.getCurrentLanguage(), newEntity.getId());
				ai.setKeyId(NamedKeyItem.generateKey(newEntity.getId(),
					etype.getDmAttribute().getAttributeList()));
			
				etype.getDmAttribute().getAttributeList().add(ai);
				session.saveOrUpdate(ai);
				newEntity.setAttributeListItem(ai);
				
				session.saveOrUpdate(newEntity);
				
			}else{
				AttributeListItem ai = (AttributeListItem) session.load(AttributeListItem.class, newEntity.getAttributeListItem().getUuid());
				
				session.saveOrUpdate(newEntity);
				eaComp.updateEntity();
				
				ai.setName(newEntity.getId());
				ai.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), newEntity.getId());
				ai.updateName(SmartDB.getCurrentLanguage(), newEntity.getId());
				ai.setIsActive(newEntity.getStatus() == Status.ACTIVE);
				
			}
		
			session.getTransaction().commit();
		}catch (Exception ex){
			EntityPlugIn.displayLog("Error saving entity." + "\n\n" + ex.getMessage(), ex);
			session.getTransaction().rollback();
			return false;
		}
		return true;
	}
	
	@Override
	protected void okPressed() {
		String error = eaComp.isValid();
		if (error == null){
			if (save()){
				super.okPressed();
			}
		}else{
			MessageDialog.openError(getShell(), "Error", error);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		if (newEntity.getUuid() == null){
			super.getShell().setText("New Entity");
			setTitle("New Entity");
			super.setMessage(MessageFormat.format("Create a new entity of type {0}", new Object[]{etype.getName()}));
		}else{
			super.getShell().setText("Edit Entity");
			setTitle("Edit Entity");
			super.setMessage(MessageFormat.format("Edit the entity {0}.", new Object[]{newEntity.getId()}));
		}
		super.setTitleImage(EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.ENTITY_TYPE_WIZBAN));

		Composite composite = (Composite) super.createDialogArea(parent);
		composite = new Composite(composite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		composite.setLayout(gl);
		
		ScrolledComposite scroll = new ScrolledComposite(composite, SWT.H_SCROLL | SWT.V_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		eaComp = new EntityEditPanelComposite(scroll);
		eaComp.setEntityType(etype);
		
		
		scroll.setContent(eaComp);
		scroll.setMinSize(eaComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		
		super.getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				eaComp.dispose();
			}
		});
		eaComp.setEditEntity(newEntity);
		
		return composite;
	}

	protected boolean isResizable() {
		return true;
	}

	
}
