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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.AttachmentManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

public class RecordEditor extends EditorPart{
	
	public static final String ID = "org.wcs.smart.i2.editor.record"; //$NON-NLS-1$

	private RecordEditorInput input;

	private FormToolkit  toolkit;
	
	private boolean isEditMode = false;
	private boolean isDirty = false;
	
	private Button btnEditMode;
	private Composite topPart;
	
	
	private IntelRecord record;
	private Label headerLabel;
	
	private EntityListComposite entityPanel;
	private AttachmentListComposite attachmentPanel;
	private LocationListComposite locationPanel;
	
	private Job loadRecordJob = new Job("load intelligence record"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			record = null;
			IntelRecord temp = null;
			UUID uuid = null;
			if (input.getRecord() == null){
				uuid = input.getUuid();
			}else{
				uuid = input.getRecord().getUuid();
			}
			Session s = HibernateManager.openSession();
			try{
				temp = (IntelRecord) s.get(IntelRecord.class, uuid);
				temp.getCreatedBy().getFamilyName();
				temp.getLastModifiedBy().getFamilyName();
				if (temp.getAttachments().size() > 0){
					for (IntelRecordAttachment a : temp.getAttachments()){
						try{
							a.getAttachment().computeFileLocation(s);
						}catch (Exception ex){
							//TODO: 
						}
					}
				}
				if (temp.getEntities() != null){
					for (IntelEntityRecord rr : temp.getEntities()){
						rr.getEntity().getIdAttributeAsText();
						rr.getEntity().getEntityType().getName();
						if (rr.getEntity().getPrimaryAttachment()!= null){
							try{
								rr.getEntity().getPrimaryAttachment().computeFileLocation(s);
							}catch (Exception ex){
								//TODO:
							}
						}
						if (rr.getEntity().getEntityAttachments() != null){
							for (IntelEntityAttachment a : rr.getEntity().getEntityAttachments()){
								try{
									a.getAttachment().computeFileLocation(s);
								}catch (Exception ex){
									//TODO:
								}
							}
						}
					}
				}
				if (temp.getLocations() != null){
					for (IntelLocation loc : temp.getLocations()){
						loc.getId();
					}
				}
			}finally{
				s.close();
			}
			record = temp;
			
			
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					initPage();
				}
				
			});
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
		
	};
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		List<IntelEntity> modifiedEntities = new ArrayList<IntelEntity>();
		
		Session s = HibernateManager.openSession(new AttachmentInterceptor());
		try{
			s.beginTransaction();
			
			if (record.getAttachments() != null){
				for (IntelRecordAttachment a : record.getAttachments()){
					s.saveOrUpdate(a.getAttachment());
				}
			}
			
			for (IntelEntityAttachment entityAttachments : attachmentPanel.getNewEntityAttachments()){
				s.save(entityAttachments);
				modifiedEntities.add(entityAttachments.getEntity());
			}
			
			for (IntelEntityRecord r : entityPanel.getEntityLinksToDelete()){
				s.delete(r);
			}
			
			s.saveOrUpdate(record);
			s.flush();
			
			for (IntelRecordAttachment ea : attachmentPanel.getAttachmentsToDelete()){
				if (ea.getAttachment().getUuid() != null){
					if (AttachmentManager.INSTANCE.canDelete(ea.getAttachment(), s)){
						s.delete(ea);
						s.delete(ea.getAttachment());
					}
				}
			}
			
			s.getTransaction().commit();
			
			attachmentPanel.getAttachmentsToDelete().clear();
			attachmentPanel.getNewEntityAttachments().clear();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Unable to save changes to intelligence record. " + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		
		//TODO:: fire events (need event broker)
		//1. for record
		//2.  one for each entity in the modifiedEntities list
		setDirty(false);
		headerLabel.setText(record.getTitle());
		super.setPartName(record.getTitle());
	}

	@Override
	public void doSaveAs() {		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
		
		this.input = (RecordEditorInput)input;
		super.setPartName(input.getName());
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean isDirty){
		boolean fire = isDirty != this.isDirty;
		this.isDirty = isDirty;
		if (fire) firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	
	public IntelRecord getRecord(){
		return this.record;
	}
	
	public void linkEntity(IntelEntity entity){
		Job link = new Job("linking entity"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IntelEntity tolink = null;
				Session s = HibernateManager.openSession();
				try{
					tolink = (IntelEntity) s.get(IntelEntity.class, entity.getUuid());
					if (tolink != null){
						tolink.getIdAttributeAsText();
						if (tolink.getPrimaryAttachment() != null){
							try{
								tolink.getPrimaryAttachment().computeFileLocation(s);
							}catch (Exception ex){
								//TODO:
							}
						}
						if (tolink.getEntityAttachments() != null){
							for (IntelEntityAttachment a : tolink.getEntityAttachments()){
								try{
									a.getAttachment().computeFileLocation(s);	
								}catch (Exception ex){
									//TODO;
								}
								
							}
						}
					}
				}finally{
					s.close();
				}
				if (tolink != null){
					final IntelEntity link = tolink;
					Display.getDefault().syncExec(() -> {entityPanel.linkEntity(link);});
				}
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}	
		};
		link.schedule();
	}
	
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth= 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).verticalSpacing= 0;
		
		
		Composite buttonPanel = toolkit.createComposite(parent, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(2, false));
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)buttonPanel.getLayout()).marginWidth= 5;
		((GridLayout)buttonPanel.getLayout()).marginHeight =5;
		
		headerLabel = toolkit.createLabel(buttonPanel, "");
		FontData fd = headerLabel.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font headerFont = new Font(parent.getShell().getDisplay(), fd);
		headerLabel.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				headerFont.dispose();	
			}
		});
		headerLabel.setFont(headerFont);
		headerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		btnEditMode = toolkit.createButton(buttonPanel, DialogConstants.EDIT_BUTTON_TEXT, SWT.TOGGLE);
		btnEditMode.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setEditMode(!isEditMode);
				
			}
		});
		btnEditMode.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		btnEditMode.setSelection(isEditMode);
		
		SashForm sash = new SashForm(parent, SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite top = createHeader(sash, "Narrative");
		topPart = toolkit.createComposite(top, SWT.NONE);
		topPart.setLayout(new GridLayout(5, false));
		((GridLayout)topPart.getLayout()).marginWidth = 0;
		((GridLayout)topPart.getLayout()).marginHeight = 0;
		topPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Composite expEntities = createHeader(sash, "Entities");
		entityPanel = new EntityListComposite(expEntities, toolkit, this);
		entityPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)entityPanel.getLayoutData()).horizontalSpan = 0;
		((GridData)entityPanel.getLayoutData()).verticalSpan = 0;
				
		Composite expAttachments = createHeader(sash, "Attachments");
		attachmentPanel = new AttachmentListComposite(expAttachments, toolkit, this);
		attachmentPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite expLocation = createHeader(sash, "Locations");
		locationPanel = new LocationListComposite(expLocation, toolkit, this);
		locationPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); 
		
		loadRecordJob.schedule();
	}


	private Composite createHeader(SashForm parent, String text){
		Composite all = toolkit.createComposite(parent, SWT.NONE);
		all.setLayout(new GridLayout());
		((GridLayout)all.getLayout()).marginWidth = 0;
		((GridLayout)all.getLayout()).marginHeight = 0;
		
		Composite header = toolkit.createComposite(all, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)header.getLayout()).marginWidth = 2;
		((GridLayout)header.getLayout()).marginHeight = 2;
		
		
		Label l =toolkit.createLabel(header, text);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		final Font boldFont = new Font(parent.getDisplay(), fd);
		l.addDisposeListener((e) -> {boldFont.dispose();});
		l.setFont(boldFont);

		l.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				
				int[] weights = parent.getWeights();
				
				//find the index of the control
				int index = -1;
				for (int i = 0; i < parent.getChildren().length; i ++){
					if (parent.getChildren()[i] == all){
						index = i;
						break;
					}
				}
				if (index >= 0){
					//compute the new minimum height for the control
					int minHeight =  header.getClientArea().height;
					int totalweight = 0;
					for (int w : weights){
						totalweight += w;
					}
					double unitperpixel = totalweight / (parent.getClientArea().height* 1.0);
					double units = unitperpixel * minHeight;
					minHeight = (int)units + 5;
					
					if (weights[index] <= minHeight + 10 ){
						//compressed; we want to expand
						int adjust = (int) (unitperpixel * (parent.getClientArea().height / weights.length)) - minHeight;

						//take away from the previous or next collapsed panel
						int inc = -1;
						if (index == 0) inc = 1;						
						for (int i = index; i >=0 && i <= weights.length; i += inc){
							if ((weights[i] > minHeight + 10) && (weights[i] - adjust > minHeight)){
								weights[i] = weights[i] - adjust;
								weights[index] = adjust;
								break;
							}
						}
					}else{
						if (index == 0){
							//see if we can find one that is not collapses; otherwise use index 0
							int changeIndex = 1;
							for (int i = 1; i < weights.length; i ++){
								if (weights[i] > minHeight + 10){
									changeIndex = i; 
									break;
								}
							}
							weights[changeIndex] = weights[0] + weights[changeIndex] - minHeight;
							weights[0] = minHeight;
						}else{
							weights[index-1] = weights[index-1]+ weights[index] - minHeight;
							weights[index] = minHeight;
						}
					}
					
				}
				parent.setWeights(weights);
			}
		});
		return all;
		
	}
	
	public void setEditMode(boolean editMode){
		if (editMode == isEditMode) return;
		if (isEditMode && !editMode && isDirty){
			doSave(new NullProgressMonitor());
		}
		this.isEditMode = editMode;
		
		btnEditMode.setSelection(isEditMode);
		if (record != null){
			initPage();
		}
		
		
	}
	
	private void initPage(){
		if (topPart != null){
			for (Control c : topPart.getChildren()){
				c.dispose();
			}
		}
		headerLabel.setText(record.getTitle());
		toolkit.createLabel(topPart, "Title:");
		int style = SWT.BORDER;
		if (!isEditMode){
			style = SWT.NONE;
		}
		Text txtShortName = toolkit.createText(topPart, record.getTitle(), style);
		txtShortName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				record.setTitle(txtShortName.getText());
				setDirty(true);
			}
		});
		txtShortName.setEditable(isEditMode);
		txtShortName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		txtShortName.setTextLimit(IntelRecord.MAX_TITLE_LENGTH);
		
		toolkit.createLabel(topPart, "Status:");
		if (isEditMode){
			ComboViewer cmbStatus = new ComboViewer(topPart, SWT.DROP_DOWN | SWT.READ_ONLY);
			toolkit.adapt(cmbStatus.getControl(), true, true);
			cmbStatus.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
			cmbStatus.setContentProvider(ArrayContentProvider.getInstance());
			cmbStatus.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof IntelRecord.Status){
						return ((Enum<Status>) element).name();
					}
					return super.getText(element);
				}
			});
			cmbStatus.setInput(IntelRecord.Status.values());
			cmbStatus.setSelection(new StructuredSelection(record.getStatus()));
			cmbStatus.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					record.setStatus( (Status) ((IStructuredSelection)cmbStatus.getSelection()).getFirstElement());
					setDirty(true);
				}
			});
			
		}else{
			Label l = toolkit.createLabel(topPart, record.getStatus().name());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		}
		
		
		toolkit.createLabel(topPart, "");
		toolkit.createLabel(topPart, "Date Created:");
		
		Label l = toolkit.createLabel(topPart, record.getDateCreated() == null ? "" : DateFormat.getDateInstance().format(record.getDateCreated()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(topPart, "Created By:");
		l = toolkit.createLabel(topPart, record.getCreatedBy() == null ? "" : SmartLabelProvider.getFullLabel(record.getCreatedBy()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(topPart, "");
		toolkit.createLabel(topPart, "Date Last Modified:");
		l = toolkit.createLabel(topPart, record.getDateModified() == null ? "" : DateFormat.getDateInstance().format(record.getDateModified()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(topPart, "Last Modified By:");
		l = toolkit.createLabel(topPart, record.getLastModifiedBy() == null ? "" : SmartLabelProvider.getFullLabel(record.getLastModifiedBy()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		l = toolkit.createLabel(topPart, "Narrative:");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		Text txtDescription = toolkit.createText(topPart, record.getDescription(), SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		if (isEditMode){
			txtDescription.addModifyListener(new ModifyListener() {
				
				@Override
				public void modifyText(ModifyEvent e) {
					record.setDescription(txtDescription.getText());
					setDirty(true);
				}
			});
		}else{
			txtDescription.setEditable(false);
		}
	
		topPart.layout();
		
		entityPanel.init();
		locationPanel.init();
		
		attachmentPanel.refreshAttachmentTable();
		attachmentPanel.updateEditMode();
	}
	
	@Override
	public void setFocus() {

	}
	
	public AttachmentListComposite getAttachmentPanel(){
		return this.attachmentPanel;
	}
	
	public EntityListComposite getEntityPanel(){
		return this.entityPanel;
	}
	
	@Override
	public void dispose(){
		if (toolkit != null) toolkit.dispose();
	}

	public boolean getEditMode() {
		return this.isEditMode;
	}

}
