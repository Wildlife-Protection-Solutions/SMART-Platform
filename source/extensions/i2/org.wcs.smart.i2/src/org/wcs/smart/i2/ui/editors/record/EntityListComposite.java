/*
 * Copyright (C) 2016 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.i2.ui.editors.record;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.ui.views.IntelEntitySelectionTransfer;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Entity composite component for the record editor
 * 
 * @author Emily
 *
 */
public class EntityListComposite extends Composite{

	private EntityList lstEntities;
	
	private RecordEditor editor;
	private List<IntelEntityRecord> intelLinksToDelete = new ArrayList<IntelEntityRecord>();
	private List<IntelEntityRecord> intelLinksAdded = new ArrayList<IntelEntityRecord>();
	
	private Composite compEntityEdit;
	
	public EntityListComposite(Composite parent, FormToolkit toolkit, RecordEditor editor){
		super(parent, SWT.NONE);
		this.editor = editor;
		toolkit.adapt(this);
		
		GridLayout gl = new GridLayout();
		gl.marginHeight = 2;
		gl.marginWidth = 2;
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
				EntityListShell shell = new EntityListShell(EntityListComposite.this.getShell(), editor);
				
				int x = btnAdd.getLocation().x + btnAdd.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				int y =  btnAdd.getLocation().y;
				shell.addListener(SWT.Close, new Listener() {
					@Override
					public void handleEvent(Event event) {
						IntelEntity target = shell.getTargetEntity();
						if (target != null){
							linkEntity(target);
						}
					}
				});
				shell.open(btnAdd.toDisplay(x,y));
			}
			
		});
		updateEditMode();
		
		lstEntities = new EntityList(this, toolkit);
		lstEntities.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		DropTarget dropTarget = new DropTarget(this, DND.DROP_LINK);
		final IntelEntitySelectionTransfer objTransfer = IntelEntitySelectionTransfer.getTransfer();
		dropTarget.setTransfer(new Transfer[]{objTransfer});
		dropTarget.addDropListener(new DropTargetAdapter() {		
			private PaintListener paintListener = new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION));
					e.gc.setLineWidth(2);
					e.gc.drawRectangle(0, 0, e.width, e.height);
				}
			};

			@Override
			public void drop(DropTargetEvent event) {
				if (objTransfer.isSupportedType(event.currentDataType)) {
					ISelection s = objTransfer.getSelection();
					if (s instanceof IStructuredSelection) {
						IStructuredSelection sel = (IStructuredSelection)s;
						for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
							Object element = (Object)iterator.next();
							if (element instanceof IntelEntity){
								linkEntity((IntelEntity)element);		
							}
							
						}
					}
				}
				EntityListComposite.this.removePaintListener(paintListener);
				EntityListComposite.this.redraw();
			}

			@Override
			public void dragLeave(DropTargetEvent event) {
				EntityListComposite.this.removePaintListener(paintListener);
				EntityListComposite.this.redraw();
			}
			
			@Override
			public void dragEnter(DropTargetEvent event) {
				event.detail = DND.DROP_LINK;
				EntityListComposite.this.addPaintListener(paintListener);
				EntityListComposite.this.redraw();
			}
		});
		
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
								Intelligence2PlugIn.log(e.getMessage(), e);
							}
						}
						if (toadd.getEntityAttachments() != null){
							for(IntelEntityAttachment a : toadd.getEntityAttachments()){
								try {
									a.getAttachment().computeFileLocation(s);
								} catch (Exception e) {
									Intelligence2PlugIn.log(e.getMessage(), e);
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
							
							intelLinksAdded.add(r);
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
	
	public void refreshEntities(){
		lstEntities.refreshTable();
	}
	
	public void deleteEntityLink(){
		boolean modified = false;
		List<IntelEntity> r = lstEntities.getCurrentSelection();
		if (r != null && !r.isEmpty()){
			for (IntelEntity toDelete : r){
				//remove any links not yet saved links to attachments and locations
				for (Iterator<IntelEntityAttachment> iterator = editor.getSummaryPage().getAttachmentPanel().getNewEntityAttachments().iterator(); iterator.hasNext();) {
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
					intelLinksAdded.remove(toRemove);
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
	
	public List<IntelEntityRecord> getEntityLinksAdded(){
		return this.intelLinksAdded;
	}
}
