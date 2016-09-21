package org.wcs.smart.i2.ui.editors.record;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.ui.properties.DialogConstants;

public class EntityListComposite extends Composite{

	private EntityList lstEntities;
	
	private RecordEditor editor;
	private List<IntelEntityRecord> intelLinksToDelete = new ArrayList<IntelEntityRecord>();
	

	
	public EntityListComposite(Composite parent, FormToolkit toolkit, RecordEditor editor){
		super(parent, SWT.BORDER);
		this.editor = editor;
		toolkit.adapt(this);
		
		GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		setLayout(gl);
		
		lstEntities = new EntityList(this, toolkit);
		lstEntities.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	public RecordEditor getEditor(){
		return this.editor;
	}
	
	public void linkEntity(IntelEntity entity){
		if (editor.getRecord() != null){
			if (editor.getRecord().getEntities() == null){
				editor.getRecord().setEntities(new ArrayList<IntelEntityRecord>());
			}
			boolean add = true;
			for (IntelEntityRecord r : editor.getRecord().getEntities()){
				if (r.getEntity().equals(entity)){
					add = false;
				}
			}
			
			if (add){
				IntelEntityRecord r = new IntelEntityRecord();
				r.setEntity(entity);
				r.setRecord(editor.getRecord());
				
				editor.getRecord().getEntities().add(r);
				editor.setDirty(true);
			}
			init();
		}
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
	
	public List<IntelEntityRecord> getEntityLinksToDelete(){
		return this.intelLinksToDelete;
	}
}
