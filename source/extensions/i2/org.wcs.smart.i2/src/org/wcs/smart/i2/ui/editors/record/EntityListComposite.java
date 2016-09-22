package org.wcs.smart.i2.ui.editors.record;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.ui.properties.DialogConstants;

public class EntityListComposite extends Composite{

	private EntityList lstEntities;
	
	private RecordEditor editor;
	private List<IntelEntityRecord> intelLinksToDelete = new ArrayList<IntelEntityRecord>();
	
	private Composite compEntityEdit;
	
	public EntityListComposite(Composite parent, FormToolkit toolkit, RecordEditor editor){
		super(parent, SWT.NONE);
		this.editor = editor;
		toolkit.adapt(this);
		
		GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		setLayout(gl);
		
		compEntityEdit = toolkit.createComposite(this, SWT.NONE);
		compEntityEdit.setLayout(new GridLayout());
		compEntityEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)compEntityEdit.getLayout()).marginHeight = 0 ;
		((GridLayout)compEntityEdit.getLayout()).marginWidth = 0 ;
		
		Button btnAdd = new Button(compEntityEdit, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				EntityListShell shell = new EntityListShell(EntityListComposite.this.getDisplay(), editor);
				
				int x = btnAdd.getLocation().x + btnAdd.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				int y =  btnAdd.getLocation().y;
				shell.getShell().setLocation(btnAdd.toDisplay(x,y));
				shell.getShell().open();
				
				shell.getShell().addListener(SWT.Close, new Listener() {
					@Override
					public void handleEvent(Event event) {
						IntelEntity target = shell.getTargetEntity();
						if (target != null){
							linkEntity(target);
						}
					}
				});
			}
			
		});
		updateEditMode();
		
		lstEntities = new EntityList(this, toolkit);
		lstEntities.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	public RecordEditor getEditor(){
		return this.editor;
	}
	
	public void linkEntity(final IntelEntity entity){
		Job linkEntity = new Job("link entity job"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				IntelEntity toadd = null;
				try{
					toadd = (IntelEntity) s.get(IntelEntity.class, entity.getUuid());
					if(toadd != null){
						toadd.getIdAttributeAsText();
						if (toadd.getPrimaryAttachment() != null){
							try {
								toadd.getPrimaryAttachment().computeFileLocation(s);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if (toadd.getEntityAttachments() != null){
							for(IntelEntityAttachment a : toadd.getEntityAttachments()){
								try {
									a.getAttachment().computeFileLocation(s);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}	
							}
						}
						
					}
				}finally{
					s.close();
				}
				if (toadd == null) return Status.OK_STATUS;
				
				synchronized (editor.getRecord()) {
					if (editor.getRecord() != null){
						if (editor.getRecord().getEntities() == null){
							editor.getRecord().setEntities(new ArrayList<IntelEntityRecord>());
						}
						boolean add = true;
						
						for (IntelEntityRecord r : editor.getRecord().getEntities()){
							if (r.getEntity().equals(toadd)){
								add = false;
							}
						}
						
						if (add){
							IntelEntityRecord r = new IntelEntityRecord();
							r.setEntity(toadd);
							r.setRecord(editor.getRecord());
							editor.getRecord().getEntities().add(r);
							editor.setDirty(true);
						}
						Display.getDefault().syncExec(() -> init());
					}
				}
				return Status.OK_STATUS;
			}
			
		};
		linkEntity.schedule();
		
	}
	
	
	public void init(){
		lstEntities.setEntities(editor.getRecord().getEntities());
	}
	
	
	public void deleteEntityLink(){
		boolean modified = false;
		List<IntelEntity> r = lstEntities.getCurrentSelection();
		if (r != null && !r.isEmpty()){
			for (IntelEntity toDelete : r){
				//TODO: remove any links not yet saved links to attachments and locations
				for (Iterator<IntelEntityAttachment> iterator = editor.getAttachmentPanel().getNewEntityAttachments().iterator(); iterator.hasNext();) {
					IntelEntityAttachment a = (IntelEntityAttachment) iterator.next();
					if (a.getEntity().equals(toDelete)) iterator.remove();
				}
				
				IntelEntityRecord toRemove = null;
				for (IntelEntityRecord er : editor.getRecord().getEntities()){
					if (er.getEntity().equals(toDelete)){
						toRemove = er;
						break;
					}
				}
				if (toRemove != null){
					intelLinksToDelete.add(toRemove);
					editor.getRecord().getEntities().remove(toRemove);
					modified = true;
				}
			}
			
		}
		if (modified){
			init();
			editor.setDirty(true);
		}
	}

	public void updateEditMode(){
		if (editor.getEditMode()){
			compEntityEdit.setVisible(true);
			((GridData)compEntityEdit.getLayoutData()).heightHint = compEntityEdit.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		}else{
			compEntityEdit.setVisible(false);
			((GridData)compEntityEdit.getLayoutData()).heightHint = 0;
		}
		compEntityEdit.getParent().layout();
	}
	
	
	public List<IntelEntityRecord> getEntityLinksToDelete(){
		return this.intelLinksToDelete;
	}
}
