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
package org.wcs.smart.i2.ui.editors;

import java.io.File;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.AttachmentManager;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.AttributeValueLabelProvider;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.RelationshipGroupLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.ui.dialogs.AttributeFieldEditor;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Entity editor.
 * @author Emily
 *
 */
public class EntityEditor extends EditorPart implements MapPart{
	
	public static final String ID = "org.wcs.smart.i2.editor.entity"; //$NON-NLS-1$

	private static final int THUMB_SIZE = 150;
	private Font boldFont = null;
	
	private boolean isEditMode;
	private boolean isDirty;
	
	private Font headerFont;
	
	private EntityEditorInput input;
	private IntelEntity entity;
	private List<IntelEntityRelationship> relationships;
	
	private List<AttributeFieldEditor> fieldEditors = null;
	
	private FormToolkit toolkit;
	private Canvas lblMainImage;
	private Image imgMain;
	
	private Label lblCreated;
	private Label lblModified;
	private Label lblIdentifier;
	
	private Composite compAttributes;
	private Composite compAttachments;
	
	private AttachmentTable attachmentTable;
	private Composite attachmentEditPanel;
	private Composite relationshipEditPanel; 
	private EntityEditorMapComposite mapPart ;
	
	private Composite compMap;
	private Composite compRecords;
	private Composite compRelationships;
	private TableViewer tblRecords;
	private TreeViewer treeRelationships;
	private List<IntelEntityAttachment> attachmentsToDelete = new ArrayList<IntelEntityAttachment>();
	private List<IntelEntityRelationship> relationshipsToAdd = new ArrayList<IntelEntityRelationship>();
	private List<IntelEntityRelationship> relationshipsToDelete = new ArrayList<IntelEntityRelationship>();
	
	private IEclipseContext context;
	private IEventBroker eventBroker;

	private ToolItem deleteItem;
	private ToolItem editItem;
	private ToolItem wsetItem;
	
	private List<EventHandler> eventHandles = null;
	
	private Job loadEntity = new Job("load entity"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			entity = null;
			IntelEntity temp = null;
			Session s = HibernateManager.openSession();
			try{
				temp = (IntelEntity) s.get(IntelEntity.class, input.getUuid());
				for(IntelEntityTypeAttribute a : temp.getEntityType().getAttributes()){
					a.getAttribute().getName();
					if (a.getAttribute().getAttributeList() != null){
						for(IntelAttributeListItem i : a.getAttribute().getAttributeList()){
							i.getName();
						}
					}
				}
				for (IntelEntityAttributeValue v : temp.getAttributes()){
					v.getAttribute().getName();
					if (v.getAttributeListItem() != null) v.getAttributeListItem().getName();
					v.getAttributeValue();
				}
				if (temp.getEntityAttachments() != null){
					for (IntelEntityAttachment a : temp.getEntityAttachments()){
						try {
							a.getAttachment().computeFileLocation(s);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				temp.getPrimaryAttachment();
				
				relationships = s.createCriteria(IntelEntityRelationship.class)
					.add(Restrictions.or (Restrictions.eq("sourceEntity", temp), Restrictions.eq("targetEntity", temp)))
					.list();
				for (IntelEntityRelationship r : relationships){
					r.getRelationshipType().getName();
					if (r.getRelationshipType().getRelationshipGroup() != null){
						r.getRelationshipType().getRelationshipGroup().getName();
					}
					r.getSourceEntity().getIdAttributeAsText();
					r.getTargetEntity().getIdAttributeAsText();
				}
			}finally{
				s.close();
			}
			entity = temp;
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					
					initControl(entity);
				}
				
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private Job loadRecords = new Job("loading entity records"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelRecord> records = new ArrayList<IntelRecord>();
			Session s = HibernateManager.openSession();
			try{
				List<IntelEntityRecord> entityRecords = s.createCriteria(IntelEntityRecord.class)
						.add(Restrictions.eq("id.entity", entity))
						.list();
				for (IntelEntityRecord r : entityRecords){
					records.add(r.getRecord());
					r.getRecord().getDateCreated();
					r.getRecord().getDateModified();
					r.getRecord().getTitle();
				}
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					tblRecords.setInput(records);
					tblRecords.refresh();
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		for(AttributeFieldEditor editor : fieldEditors){
			if (!editor.isValid()){
				MessageDialog.openError(getSite().getShell(), "Save Error", MessageFormat.format("Fix error with attribute {0} before saving", editor.getAttribute().getName()));
				return;
			}
		}
		for(AttributeFieldEditor editor : fieldEditors){
			IntelEntityAttributeValue value = entity.findAttributeValue(editor.getAttribute());
			if(value == null){
				value = new IntelEntityAttributeValue();
				value.setAttribute(editor.getAttribute());
				value.setEntity(entity);
				
				if (editor.updateValue(value)){
					entity.getAttributes().add(value);
				}
			}else{
				if (!editor.updateValue(value)){
					entity.getAttributes().remove(value);
				}
			}
		}

		Session s = HibernateManager.openSession(new AttachmentInterceptor());
		try{
			s.beginTransaction();
			
			if (entity.getEntityAttachments() != null){
				for (IntelEntityAttachment a : entity.getEntityAttachments()){
					s.saveOrUpdate(a.getAttachment());
				}
			}
			s.saveOrUpdate(entity);
			
			for(IntelEntityRelationship r : relationshipsToAdd){
				s.saveOrUpdate(r);
			}
			for(IntelEntityRelationship r : relationshipsToDelete){
				s.delete(r);
			}
			s.flush();
			
			for (IntelEntityAttachment ea : attachmentsToDelete){
				//TODO: check for other references before we delete this
				if (ea.getAttachment().getUuid() != null){
					if (AttachmentManager.INSTANCE.canDelete(ea.getAttachment(), s)){
						s.delete(ea);
						s.delete(ea.getAttachment());
					}
				}
			}
			s.getTransaction().commit();
			clearLists();
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error saving entity changes: " + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put(UIEvents.EventTags.ELEMENT, context.get(MPart.class));
		data.put(IEventBroker.DATA, entity);
		eventBroker.send(IntelEvents.ENTITY_MODIFIED, data);
		setDirty(false);
		firePropertyChange(IEditorPart.PROP_DIRTY);
		
		super.setPartName(entity.getIdAttributeAsText());
	}

	private void clearLists(){
		attachmentsToDelete.clear();
		relationshipsToAdd.clear();
		relationshipsToDelete.clear();
	}
	
	@Override
	public void doSaveAs() {		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		this.input = (EntityEditorInput) input;
		setInput(input);
		setSite(site);
		
		super.setTitleImage(input.getImageDescriptor().createImage());
		super.setPartName(input.getName());
	}


	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	private void subscribeToEvents(){
		eventHandles = new ArrayList<EventHandler>();
		
		EventHandler handler = (e) -> wsetItem.setEnabled(WorkingSetManager.INSTANCE.isSet());
		eventHandles.add(handler);
		eventBroker.subscribe(IntelEvents.ACTIVE_WS_SET, handler);
		
		//on delete close editor
		handler = new EventHandler() {
			@Override
			public void handleEvent(org.osgi.service.event.Event event) {
				Object data = event.getProperty(IEventBroker.DATA);
				if (data != null && data.equals(entity)){
					getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(EntityEditor.this, false);
				}
			}
		};
		eventHandles.add(handler);
		eventBroker.subscribe(IntelEvents.ENTITY_DELETE, handler);
		
		
		
		EventHandler promptToReset = new EventHandler(){
			@Override
			public void handleEvent(org.osgi.service.event.Event event) {
				if (context.get(MPart.class) == event.getProperty(UIEvents.EventTags.ELEMENT)){
					eventBroker.unsubscribe(this);
					if (MessageDialog.openQuestion(getSite().getShell(), "Entity Modified", MessageFormat.format("The entity ''{0}'' has been changed by another process.  Do you want to reload the editor and loose local changes?", entity.getIdAttributeAsText()))){
						loadEntity.schedule();
						setDirty(false);
					}
				}
			}
		};
		eventHandles.add(promptToReset);
		
		handler = new EventHandler() {
			@Override
			public void handleEvent(org.osgi.service.event.Event event) {
				Object data = event.getProperty(IEventBroker.DATA);
				if (context.get(MPart.class) != event.getProperty(UIEvents.EventTags.ELEMENT)){
					if (data != null && data.equals(entity)){
						if (isDirty){
							//the editor is dirty and the entity has changed behind the scenes; give the user the option of replacing 
							//contents behind the scenes
							eventBroker.subscribe(UIEvents.UILifeCycle.BRINGTOTOP, promptToReset);
							//subscribe to active event
						}else{
							//reload page
							loadEntity.schedule();
						}
					}
				}
			}
		};
		eventHandles.add(handler);
		eventBroker.subscribe(IntelEvents.ENTITY_MODIFIED, handler);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		
		context = (IEclipseContext) getSite().getService(IEclipseContext.class);
		eventBroker = context.get(IEventBroker.class);
		//configure tags for part to ensure shows in both perspectives
		MPart part = context.get(MPart.class); 
		if (!part.getTags().contains(IntelDataAssessmentPerspective.ID)) part.getTags().add(IntelDataAssessmentPerspective.ID);
		if (!part.getTags().contains(IntelDataAnalysisPerspective.ID)) part.getTags().add(IntelDataAnalysisPerspective.ID);
		
		getSite().getWorkbenchWindow().addPerspectiveListener(new PerspectiveAdapter() {
			@Override
			public void perspectiveActivated(IWorkbenchPage page,
					IPerspectiveDescriptor perspective) {
				if (isDirty && perspective.getId().equals(IntelDataAnalysisPerspective.ID)){
					//save and be done with it
					setEditMode(false);
				}else if (perspective.getId().equals(IntelDataAssessmentPerspective.ID)){
					setEditMode(true);
				}
			}
		});
		
		
		toolkit = new FormToolkit(parent.getDisplay());

		parent.setLayout(createGridLayoutNoMargin(1));
		
		SashForm sash = new SashForm(parent, SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createTopPanel(sash);
		
		createBottomPanel(sash);
		sash.setWeights(new int[]{1,2});
		
		loadEntity.schedule();
		
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(parent.getDisplay(), fd);
		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				boldFont.dispose();
				boldFont = null;
				
			}
		});
		

		subscribeToEvents();
	}


	@Override
	public void dispose(){
		if (headerFont != null){ headerFont.dispose(); headerFont = null;}
		
		//remove all event subscriptions
		eventHandles.forEach((h)->eventBroker.unsubscribe(h));
		
		super.dispose();
	}
	
	private void createBottomPanel(Composite parent){
		Composite bottom = toolkit.createComposite(parent);
		bottom.setLayout(new GridLayout());
		
		SectionTabHeader tabList = new SectionTabHeader(new String[]{"Map", "Records", "Relationships"}, bottom, toolkit);
		tabList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite tabPart = toolkit.createComposite(bottom, SWT.NONE);
		tabPart.setLayout(new StackLayout());
		tabPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		compMap = toolkit.createComposite(tabPart, SWT.NONE);
		compMap.setLayout(new GridLayout());
		((GridLayout)compMap.getLayout()).marginWidth = 0;
		((GridLayout)compMap.getLayout()).marginHeight = 0;
		createMapPanel(compMap);
		
		compRecords = toolkit.createComposite(tabPart, SWT.NONE);
		compRecords.setLayout(new GridLayout());
		createRecordsPanel(compRecords);
		
		compRelationships = toolkit.createComposite(tabPart, SWT.NONE);
		compRelationships.setLayout(new GridLayout());
		createRelationshipPanel(compRelationships);
		
		tabList.setContent(new Composite[]{compMap,  compRecords, compRelationships}, tabPart);
		tabList.selectTab(0);
		
	}
	private void createTopPanel(Composite parent){
		
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight()  + 1);
		headerFont = new Font(parent.getDisplay(), fd);
		
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = toolkit.createComposite(panel, SWT.NONE);
		leftPart.setLayout(new GridLayout(2, false));
		leftPart.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		lblMainImage = new Canvas(leftPart, SWT.NONE);
		lblMainImage.addPaintListener(new PaintListener() {			
			@Override
			public void paintControl(PaintEvent e) {
				Rectangle r = lblMainImage.getClientArea();
				GC gc = e.gc;
				gc.setForeground(toolkit.getColors().getBorderColor());
				
				if (imgMain != null  && !imgMain.isDisposed()){
					gc.drawImage(imgMain, 0, 0);
				}else{
					gc.drawLine(0, 0, r.width - 1, r.height - 1);
					gc.drawLine(0, r.height-1, r.width - 1, 0);
				}
				gc.drawRectangle(0, 0, r.width - 1, r.height - 1);
			}
		});
		lblMainImage.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		((GridData)lblMainImage.getLayoutData()).widthHint = THUMB_SIZE;
		((GridData)lblMainImage.getLayoutData()).heightHint = THUMB_SIZE;

		
		toolkit.createLabel(leftPart, "Created:");
		lblCreated = toolkit.createLabel(leftPart, "CREATED");
		
		toolkit.createLabel(leftPart, "Modified:");
		lblModified= toolkit.createLabel(leftPart, DateFormat.getInstance().format(new Date()));
		
		Composite rightPart = toolkit.createComposite(panel, SWT.NONE);
		rightPart.setLayout(new GridLayout(2, false));
		rightPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lblIdentifier = toolkit.createLabel(rightPart, "");
		lblIdentifier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lblIdentifier.setFont(headerFont);

		ToolBar buttonBar = new ToolBar(rightPart, SWT.HORIZONTAL | SWT.FLAT);
		buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		ToolItem refreshItem = new ToolItem(buttonBar, SWT.PUSH);
		refreshItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_REFRESH));
		refreshItem.setToolTipText("refresh record");
		refreshItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean doAction = true;
				if (isDirty){
					if (!MessageDialog.openConfirm(getSite().getShell(), "Refresh", "Changes will be lost.  Are you sure you want to refresh?")){
						doAction = false;
					}
				}
				if (doAction){
					setDirty(false);
					clearLists();
					loadEntity.schedule();				
				}
			}
		});
		
		deleteItem = new ToolItem(buttonBar, SWT.PUSH);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.setToolTipText("delete entity");
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (MessageDialog.openConfirm(getSite().getShell(), "Delete", "Are you sure you want to delete this entity?  This action cannot be undone.")){
					//TODO: delete entity
					Session s = HibernateManager.openSession();
					try{
						s.beginTransaction();
						EntityManager.INSTANCE.deleteEntity(entity, s);
						s.getTransaction().commit();
					}catch (Exception ex){
						Intelligence2PlugIn.displayLog("Error deleting entity. " + ex.getMessage(), ex);
						return;
					}
					eventBroker.send(IntelEvents.ENTITY_DELETE, entity);
					
				}
				
			}
		});
		deleteItem.setEnabled(isEditMode);
		
		wsetItem = new ToolItem(buttonBar, SWT.CHECK);
		wsetItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
		wsetItem.setToolTipText("add to current working set");
		wsetItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				WorkingSetManager.INSTANCE.addToActiveWorkingSet(getEntity(), context);
			}
		});
		wsetItem.setEnabled(WorkingSetManager.INSTANCE.isSet());
		
		
		editItem = new ToolItem(buttonBar, SWT.CHECK);
		editItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
		editItem.setToolTipText("enable or disable editing of record");
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setEditMode(!isEditMode);
				
			}
		});
		
		SectionTabHeader tabList = new SectionTabHeader(new String[]{"Attributes", "Files"}, rightPart, toolkit);
		tabList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite tabPart = toolkit.createComposite(rightPart, SWT.NONE);
		tabPart.setLayout(new StackLayout());
		tabPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		compAttributes = toolkit.createComposite(tabPart, SWT.NONE);
		compAttributes.setLayout(new GridLayout());
		
		compAttachments = toolkit.createComposite(tabPart, SWT.NONE);
		compAttachments.setLayout(new GridLayout());

		createAttachmentPanel(compAttachments);
		
		tabList.setContent(new Composite[]{compAttributes,  compAttachments}, tabPart);
		tabList.selectTab(0);
	}
	
	private void createMapPanel(Composite parent){
		mapPart = new EntityEditorMapComposite(parent, this, toolkit);
		mapPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ApplicationGIS.getToolManager().setCurrentEditor(this);
	}
	
	public IntelEntity getEntity(){
		return this.entity;
	}
	
	public void setEditMode(boolean isEdit){
		if (isEditMode && !isEdit && isDirty){
			doSave(new NullProgressMonitor());
		}
		this.isEditMode = isEdit;
		if (entity != null) initControl(entity);
	
		deleteItem.setEnabled(isEdit);
		editItem.setSelection(isEdit);
	}
	
	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	@Override
	public boolean isDirty(){
		return this.isDirty;
	}
	
	private void openRelationship(){
		IStructuredSelection sel = (IStructuredSelection) treeRelationships.getSelection();
		if (!sel.isEmpty()){
			if (sel.getFirstElement() instanceof IntelEntityRelationship){
				IntelEntityRelationship r = (IntelEntityRelationship)sel.getFirstElement();
				IntelEntity toOpen = r.getSourceEntity();
				if (r.getSourceEntity() == entity){
					toOpen = r.getTargetEntity();
				}
				(new OpenEntityHandler()).openEntity(toOpen, context);
			}
		}
	}
	private void deleteRelationship(){
		IStructuredSelection sel = (IStructuredSelection) treeRelationships.getSelection();
		if (!sel.isEmpty()){
			if (sel.getFirstElement() instanceof IntelEntityRelationship){
				IntelEntityRelationship r = (IntelEntityRelationship)sel.getFirstElement();
				relationships.remove(r);
				relationshipsToDelete.add(r);
				treeRelationships.setInput(relationships);
				treeRelationships.expandAll();
				
				setDirty(true);
			}
		}
	}
	private void createRelationshipPanel(Composite parent){

		relationshipEditPanel = toolkit.createComposite(parent, SWT.NONE);
		relationshipEditPanel.setLayout(createGridLayoutNoMargin(1));
		relationshipEditPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnAddRelationship = toolkit.createButton(relationshipEditPanel, "New Relationship...", SWT.PUSH);
		btnAddRelationship.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				EntityRelationshipListShell shell = new EntityRelationshipListShell(getSite().getShell().getDisplay(), entity);
				
				int x = btnAddRelationship.getLocation().x + btnAddRelationship.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				int y =  btnAddRelationship.getLocation().y;
				shell.open(btnAddRelationship.toDisplay(x,y));
				shell.addListener(SWT.Close, new Listener(){

					@Override
					public void handleEvent(Event event) {
						if (shell.getRelationshipType() != null){

							IntelRelationshipType rType = shell.getRelationshipType();
							IntelEntity e1 = entity;
							IntelEntity e2 = shell.getTargetEntity();
							
							IntelEntityRelationship newRelationship = new IntelEntityRelationship();
							newRelationship.setRelationshipType(rType);
							boolean add = false;
							if (rType.getSourceEntityType() == null && rType.getTargetEntityType() == null){
								newRelationship.setSourceEntity(e1);
								newRelationship.setTargetEntity(e2);
								add = true;
							}else if (rType.getSourceEntityType() == null && rType.getTargetEntityType() != null){
								if (rType.getTargetEntityType().getUuid().equals(e1.getEntityType().getUuid())){
									newRelationship.setSourceEntity(e2);
									newRelationship.setTargetEntity(e1);	
								}else if (rType.getTargetEntityType().getUuid().equals(e2.getEntityType().getUuid())){
									newRelationship.setSourceEntity(e1);
									newRelationship.setTargetEntity(e2);
								}
							}else if (rType.getTargetEntityType() == null && rType.getSourceEntityType() != null){
								if (rType.getSourceEntityType().getUuid().equals(e1.getEntityType().getUuid())){
									newRelationship.setSourceEntity(e1);
									newRelationship.setTargetEntity(e2);	
								}else if (rType.getSourceEntityType().getUuid().equals(e2.getEntityType().getUuid())){
									newRelationship.setSourceEntity(e2);
									newRelationship.setTargetEntity(e1);
								}
							}else if (rType.getSourceEntityType().getUuid().equals(e1.getEntityType().getUuid()) &&
									rType.getTargetEntityType().getUuid().equals(e2.getEntityType().getUuid())){
								newRelationship.setSourceEntity(e1);
								newRelationship.setTargetEntity(e2);
								add = true;
							}else if (rType.getSourceEntityType().getUuid().equals(e2.getEntityType().getUuid()) &&
									rType.getTargetEntityType().getUuid().equals(e1.getEntityType().getUuid())){
								newRelationship.setSourceEntity(e1);
								newRelationship.setTargetEntity(e2);
								add = true;
							} 
							//check duplicates
							if (add){
								for (IntelEntityRelationship existing : relationships){
									if (existing.getSourceEntity().equals(newRelationship.getSourceEntity()) && 
											existing.getTargetEntity().equals(newRelationship.getTargetEntity()) &&
											existing.getRelationshipType().equals(newRelationship.getRelationshipType())){
										add = false;
										break;
									}
											
								}
							}
							if (add){
								relationships.add(newRelationship);
								relationshipsToAdd.add(newRelationship);
								setDirty(true);
								treeRelationships.setInput(relationships);
								treeRelationships.expandAll();
							}
						}
					}
					
				});
				
			}
		});
		
		Tree rTree = new Tree(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		
		treeRelationships = new TreeViewer(rTree);
		toolkit.adapt(treeRelationships.getTree());
		treeRelationships.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeRelationships.setContentProvider(new RelationshipContentProvider());
		treeRelationships.getTree().setHeaderVisible(true);
//		treeRelationships.getTree().setLinesVisible(true);
		treeRelationships.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openRelationship();
				
			}
		});
		TreeViewerColumn colType = new TreeViewerColumn(treeRelationships, SWT.DEFAULT);
		colType.getColumn().setText("Relationship");
		colType.getColumn().setWidth(150);
		colType.setLabelProvider(new RelationshipLabelProvider(0));
		
		TreeViewerColumn colRelationship = new TreeViewerColumn(treeRelationships, SWT.DEFAULT);
		colRelationship.getColumn().setText("Relation");
		colRelationship.getColumn().setWidth(200);
		colRelationship.setLabelProvider(new RelationshipLabelProvider(1));
		
		TreeViewerColumn colAttributes = new TreeViewerColumn(treeRelationships, SWT.DEFAULT);
		colAttributes.getColumn().setText("Attributes");
		colAttributes.getColumn().setWidth(500);
		colAttributes.setLabelProvider(new RelationshipLabelProvider(2));
		
		
		
		
		IMenuCreator mnuRelationship = new IMenuCreator() {
			private MenuItem mnuOpen;
			private MenuItem mnuDelete;
			private MenuItem mnuEdit;
	
			private Menu thumbMenu;
			
			@Override
			public Menu createMenu(Composite parent) {
				thumbMenu = new Menu(parent);
				thumbMenu.addMenuListener(new MenuListener() {
					
					@Override
					public void menuShown(MenuEvent e) {
						createMenu();
					}
					
					@Override
					public void menuHidden(MenuEvent e) {
					}
				});
				parent.setMenu(thumbMenu);
				return thumbMenu;
			}
			
			private void createMenu(){		
				if (mnuOpen == null){
					mnuOpen = new MenuItem(thumbMenu,SWT.DEFAULT);
					mnuOpen.setText("Open");
					mnuOpen.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							openRelationship();
						}
					});
				}
				if (isEditMode){
					if (mnuDelete == null){
						mnuDelete = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
						mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
						mnuDelete.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								deleteRelationship();
							}	
						});
					}
					if (mnuEdit == null){
						mnuEdit = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
						mnuEdit.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								//TODO:
								MessageDialog.openInformation(getEditorSite().getShell(),"TODO", "Implement This; users can switch src and target entities and configure attributes");
							}	
						});
					}
				}else{
					if (mnuDelete != null){
						mnuDelete.dispose();
						mnuDelete = null;
					}
					if (mnuEdit != null){
						mnuEdit.dispose();
						mnuEdit = null;
					}
				}
			}
		};
		
		treeRelationships.getTree().setMenu(mnuRelationship.createMenu(treeRelationships.getTree()));
	}
	
	
	private void createRecordsPanel(Composite parent){
		tblRecords = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		tblRecords.setContentProvider(ArrayContentProvider.getInstance());
		tblRecords.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblRecords.getTable().setHeaderVisible(true);
		tblRecords.getTable().setLinesVisible(true);
		
		String[] columns = new String[]{"Short Name", "Date Recieved", "Date Modified"};
		ColumnLabelProvider[] lbls = new ColumnLabelProvider[]{
				new RecordLabelProvider(RecordLabelProvider.RecordField.TITLE),
				new RecordLabelProvider(RecordLabelProvider.RecordField.DATE_CREATED),
				new RecordLabelProvider(RecordLabelProvider.RecordField.LAST_MODIFIED)
		};
		int[] width = new int[]{400, 100, 100};
		
		for (int i = 0; i < columns.length; i ++){
			TableViewerColumn col = new TableViewerColumn(tblRecords, SWT.NONE);
			col.setLabelProvider(lbls[i]);
			col.getColumn().setText(columns[i]);
			
			col.getColumn().setWidth(width[i]);
		}
		tblRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		tblRecords.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openSelectedRecord();
			}
		});
		
		Menu recordsMenu = new Menu(tblRecords.getTable());
		tblRecords.getTable().setMenu(recordsMenu);
		
		MenuItem open = new MenuItem(recordsMenu, SWT.PUSH);
		open.setText("Open");
		open.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openSelectedRecord();
			}
		});
		
	}
	
	private void openSelectedRecord(){
		if (tblRecords.getSelection().isEmpty()) return;
		Object x = ((IStructuredSelection)tblRecords.getSelection()).getFirstElement();
		if (x instanceof IntelRecord){
			(new OpenRecordHandler()).openRecord((IntelRecord) x, false);
		}
	}
	private void createAttachmentPanel(Composite parent){
		
		attachmentEditPanel = toolkit.createComposite(parent);
		attachmentEditPanel.setLayout(createGridLayoutNoMargin(1));
		attachmentEditPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnAddAttachment = toolkit.createButton(attachmentEditPanel, "Add...", SWT.PUSH);
		btnAddAttachment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {	
				FileDialog dialog = new FileDialog(getSite().getShell(), SWT.OPEN | SWT.MULTI);
				dialog.open();
				
				if (dialog.getFileNames() != null){
					for (String file : dialog.getFileNames()){
						IntelAttachment ia = new IntelAttachment();
						ia.setConservationArea(SmartDB.getCurrentConservationArea());
						ia.setCopyFromLocation(Paths.get(dialog.getFilterPath()).resolve(file).toFile());
						ia.setCreatedBy(SmartDB.getCurrentEmployee());
						ia.setDateCreated(new Date());
						ia.setFilename(Paths.get(dialog.getFilterPath()).resolve(file).getFileName().toString());
						
						IntelEntityAttachment iea = new IntelEntityAttachment();
						iea.setEntity(entity);
						iea.setAttachment(ia);
						if (entity.getEntityAttachments() == null){
							entity.setEntityAttachments(new ArrayList<IntelEntityAttachment>());
						}
						entity.getEntityAttachments().add(iea);
					}
					setDirty(true);
					refreshAttachmentTable();
				}
				
			}
		});
		
		IMenuCreator thumbMenu = new IMenuCreator() {
			private MenuItem mnuOpen;
			private MenuItem mnuDelete;
			private MenuItem mnuPrimary;
	
			private Menu thumbMenu;
			
			@Override
			public Menu createMenu(Composite parent) {
				thumbMenu = new Menu(parent);
				thumbMenu.addMenuListener(new MenuListener() {
					
					@Override
					public void menuShown(MenuEvent e) {
						createMenu();
					}
					
					@Override
					public void menuHidden(MenuEvent e) {
					}
				});
				parent.setMenu(thumbMenu);
				return thumbMenu;
			}
			
			private void createMenu(){		
				if (mnuOpen == null){
					mnuOpen = new MenuItem(thumbMenu,SWT.DEFAULT);
					mnuOpen.setText("Open");
					mnuOpen.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (!attachmentTable.getSelection().isEmpty()){
								AttachmentUtil.openAttachment(attachmentTable.getSelection().get(0));
							}
						}
					});
				}
				if (isEditMode){
					if (mnuDelete == null){
						mnuDelete = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
						mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
						mnuDelete.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								if (!attachmentTable.getSelection().isEmpty()){
									IntelAttachment toDelete = attachmentTable.getSelection().get(0);
									
									if (toDelete.equals(entity.getPrimaryAttachment())){
										entity.setPrimaryAttachment(null);
										if (imgMain != null) imgMain.dispose();
										lblMainImage.redraw();	
									}
									for (IntelEntityAttachment ea : entity.getEntityAttachments()){
										if (ea.getAttachment().equals(toDelete)){
											attachmentsToDelete.add(ea);
											entity.getEntityAttachments().remove(ea);
											break;
										}
									}
									refreshAttachmentTable();
									setDirty(true);
								}
							}	
						});
					}
					if (mnuPrimary == null){
						mnuPrimary = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuPrimary.setText("Set as Primary Image");
						mnuPrimary.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								if (!attachmentTable.getSelection().isEmpty()){
									entity.setPrimaryAttachment(attachmentTable.getSelection().get(0));
									
									Thumbnail thum = new Thumbnail(entity.getPrimaryAttachment(), THUMB_SIZE);
									if (imgMain != null) imgMain.dispose();
									imgMain = thum.getImage();
									lblMainImage.redraw();
									setDirty(true);
								}
							}	
						});
					}
				}else{
					if (mnuDelete != null){
						mnuDelete.dispose();
						mnuDelete = null;
					}
					if (mnuPrimary != null){
						mnuPrimary.dispose();
						mnuPrimary = null;
					}
				}
			}
		};
		attachmentTable = new AttachmentTable(parent, toolkit, thumbMenu);
		attachmentTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	private void refreshAttachmentTable(){
		List<ISmartAttachment> attachments = new ArrayList<ISmartAttachment>();
		if (entity.getEntityAttachments() != null){
			for (IntelEntityAttachment a : entity.getEntityAttachments()){
				attachments.add(a.getAttachment());
			}
		}
		attachmentTable.setAttachments(attachments);
	}
	
	private void initControl(IntelEntity entity){
		fieldEditors = new ArrayList<AttributeFieldEditor>();
				
		lblCreated.setText(DateFormat.getInstance().format(entity.getDateCreated()));
		lblModified.setText(DateFormat.getInstance().format(entity.getDateModified()));
		lblIdentifier.setText(entity.getIdAttributeAsText());
		lblModified.getParent().layout();
		lblModified.redraw();
		
		if (entity.getPrimaryAttachment() != null){
			File imageFile = entity.getPrimaryAttachment().getAttachmentFile();
			if (imageFile.exists()){
				Thumbnail thum = new Thumbnail(entity.getPrimaryAttachment(), THUMB_SIZE);
				imgMain = thum.getImage();
				lblMainImage.redraw();
			}
		}
			
		//attribute composite
		for (Control kid : compAttributes.getChildren()){
			kid.dispose();
		}
		
		ScrolledForm attributelist = toolkit.createScrolledForm(compAttributes);
		attributelist.getBody().setLayout(createGridLayoutNoMargin(1));
		attributelist.setExpandHorizontal(true);
		attributelist.setExpandVertical(true);
		attributelist.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite part = toolkit.createComposite(attributelist.getBody(), SWT.NONE);
		part.setLayout(createGridLayoutNoMargin(2));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		for (IntelEntityTypeAttribute a : entity.getEntityType().getAttributes()){
			if (isEditMode){
				AttributeFieldEditor e = new AttributeFieldEditor(part, a.getAttribute());
				e.adapt(toolkit);
				IntelEntityAttributeValue initValue = entity.findAttributeValue(a.getAttribute());
				if (initValue != null) e.initControl(initValue);
				e.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						EntityEditor.this.setDirty(true);
					}
				});
				fieldEditors.add(e);
			}else{
				Label key = toolkit.createLabel(part, a.getAttribute().getName() + ":");
				key.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
				
				String text = "";
				for (IntelEntityAttributeValue v : entity.getAttributes()){
					if (v.getAttribute().equals(a.getAttribute())){
						text = AttributeValueLabelProvider.INSTANCE.getText(v);
						break;
						
					}
				}
				Text tmp = toolkit.createText(part, text, SWT.BORDER);
				tmp.setEditable(false);
//				tmp.setEnabled(false);
//				Label tmp = toolkit.createLabel(part, text, SWT.BORDER);
				tmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
			
		}
		compAttributes.layout(true);
		
		//attachment composite
		if (isEditMode){
			attachmentEditPanel.setVisible(true);
			((GridData)attachmentEditPanel.getLayoutData()).heightHint = attachmentEditPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
			
			relationshipEditPanel.setVisible(true);
			((GridData)relationshipEditPanel.getLayoutData()).heightHint = relationshipEditPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		}else{
			attachmentEditPanel.setVisible(false);
			((GridData)attachmentEditPanel.getLayoutData()).heightHint = 0;
			
			relationshipEditPanel.setVisible(false);
			((GridData)relationshipEditPanel.getLayoutData()).heightHint = 0;
		}
		attachmentEditPanel.getParent().layout(true);
		relationshipEditPanel.getParent().layout(true);
		
		refreshAttachmentTable();
		
		treeRelationships.setInput(relationships);
		treeRelationships.refresh();
		treeRelationships.expandAll();
		
		mapPart.refresh();
		loadRecords.schedule(0);
	}
	
	private GridLayout createGridLayoutNoMargin(int col){
		GridLayout gd = new GridLayout(col, false);
		gd.marginWidth = 0;
		gd.marginHeight = 0;
		return gd;
	}
	
	
	@Override
	public void setFocus() {
		lblIdentifier.setFocus();
	}

	private class RelationshipLabelProvider extends ColumnLabelProvider{
		private int columnIndex;
		
		public RelationshipLabelProvider(int columnIndex){
			this.columnIndex = columnIndex;
		}
		
		@Override
		public Font getFont(Object element) {
			if (element instanceof IntelRelationshipGroup){
				return boldFont;
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			if (element instanceof IntelRelationshipGroup){
				return toolkit.getColors().getColor(IFormColors.TB_BG);
			}
				
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (columnIndex == 0){
			if (element instanceof IntelEntityRelationship){
				return RelationshipTypeLabelProvider.INSTANCE.getImage(((IntelEntityRelationship) element).getRelationshipType());
			}
		}
		return null;
		}

		@Override
		public String getText(Object element) {
			if (columnIndex == 0){
				if (element instanceof IntelRelationshipGroup){
				return RelationshipGroupLabelProvider.INSTANCE.getText(element);
			}else if (element instanceof IntelEntityRelationship){
				return ((IntelEntityRelationship) element).getRelationshipType().getName();
			}
		}else if (columnIndex == 1){
			if (element instanceof IntelEntityRelationship){
				IntelEntityRelationship r = (IntelEntityRelationship) element;
				if (r.getSourceEntity() == entity){
					return r.getTargetEntity().getIdAttributeAsText();
				}else{
					return r.getSourceEntity().getIdAttributeAsText();
				}
			}
		}else if (columnIndex == 2){
			if (element instanceof IntelEntityRelationship){
				return "TODO: attributes";
			}
		}
		return "";
		}
		
		
	}

	@Override
	public Map getMap() {
		return mapPart.getMap();
	}

	@Override
	public void openContextMenu() {
		mapPart.openContextMenu();
	}

	@Override
	public void setFont(Control textArea) {
		mapPart.setFont(textArea);
	}

	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapPart.setSelectionProvider(selectionProvider);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return mapPart.getStatusLineManager();
	}
}
