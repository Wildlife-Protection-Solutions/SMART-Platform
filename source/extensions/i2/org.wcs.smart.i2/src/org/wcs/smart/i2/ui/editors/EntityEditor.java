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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.birt.core.framework.IConfigurationElement;
import org.eclipse.birt.report.designer.internal.ui.util.UIHelper;
import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
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
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.graphics.Point;
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
import org.osgi.framework.Bundle;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.AttachmentManager;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
import org.wcs.smart.i2.ui.AttributeValueLabelProvider;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.RelationshipGroupLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.ui.dialogs.AttributeFieldEditor;
import org.wcs.smart.i2.ui.dialogs.RelationshipAttributeDialog;
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
	private ToolItem printItem;
	
	private EntityRelationshipDetailsShell detailsShell;
	
	private List<EventHandler> eventHandles = null;
	
	private AttributeValueLabelProvider attributeLabelProvider = new AttributeValueLabelProvider();
	
	private Job loadEntity = new Job("load entity"){

		@SuppressWarnings("unchecked")
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
							Intelligence2PlugIn.log(e.getMessage(), e);
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
					r.getRelationshipType().getAttributes().size();
					
					if (r.getAttributes() != null){
						r.getAttributes().forEach((e) -> {
							e.getAttribute().getName();
							e.getAttributeValue();
							if (e.getAttributeListItem() != null){
								e.getAttributeListItem().getName();
							}
						});
					}
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

		@SuppressWarnings("unchecked")
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

		List<IntelEntity> otherEntityModified = new ArrayList<IntelEntity>();
		
		Session s = HibernateManager.openSession(new AttachmentInterceptor());
		try{
			s.beginTransaction();
			
			if (entity.getEntityAttachments() != null){
				for (IntelEntityAttachment a : entity.getEntityAttachments()){
					s.saveOrUpdate(a.getAttachment());
				}
			}
			s.saveOrUpdate(entity);
			
			for(IntelEntityRelationship r : relationships){
				s.saveOrUpdate(r);
			}
			
			for(IntelEntityRelationship r : relationshipsToAdd){
				s.saveOrUpdate(r);
				if (!r.getSourceEntity().equals(entity)){
					otherEntityModified.add(r.getSourceEntity());
				}
				if (!r.getTargetEntity().equals(entity)){
					otherEntityModified.add(r.getTargetEntity());
				}
			}
			
			for(IntelEntityRelationship r : relationshipsToDelete){
				s.delete(r);
				if (!r.getSourceEntity().equals(entity)){
					otherEntityModified.add(r.getSourceEntity());
				}
				if (!r.getTargetEntity().equals(entity)){
					otherEntityModified.add(r.getTargetEntity());
				}
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
			
			//validate attributes against type
			//this is done in case the type has modified and removed an attribute 
			Set<IntelAttribute> attributes = new HashSet<>();
			IntelEntityType type = (IntelEntityType) s.get(IntelEntityType.class, entity.getEntityType().getUuid());
			type.getAttributes().forEach(a -> attributes.add(a.getAttribute()));
			List<IntelEntityAttributeValue> toRemove = new ArrayList<IntelEntityAttributeValue>();
			for (IntelEntityAttributeValue item : entity.getAttributes()){
				if (!attributes.contains(item.getAttribute())){
					toRemove.add(item);
				}
			}
			for (IntelEntityAttributeValue item : toRemove){
				entity.getAttributes().remove(item);
			}
			
			
			s.getTransaction().commit();
			clearLists();
		}catch (Exception ex){
			if (s.getTransaction().isActive())s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Error saving entity changes: " + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		
		lblIdentifier.setText(entity.getIdAttributeAsText());
		lblModified.setText(DateFormat.getInstance().format(entity.getDateModified()));
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put(UIEvents.EventTags.ELEMENT, context.get(MPart.class));
		data.put(IEventBroker.DATA, entity);
		eventBroker.send(IntelEvents.ENTITY_MODIFIED, data);
		
		for (IntelEntity o : otherEntityModified){
			eventBroker.send(IntelEvents.ENTITY_MODIFIED, o);
		}
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
					if (data != null && (
						data.equals(entity) || 
					    (data instanceof IntelEntityType && ((IntelEntityType)data).equals(entity.getEntityType())) ||
					    (data instanceof IntelRelationshipType && hasRelation((IntelRelationshipType)data)) 
					    )){
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
		eventBroker.subscribe(IntelEvents.ENTITY_TYPE_MODIFIED, handler);
		eventBroker.subscribe(IntelEvents.RELATION_TYPE_MODIFIED, handler);
		eventBroker.subscribe(IntelEvents.RELATION_TYPE_DELETE, handler);
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
		
		attributeLabelProvider.dispose();
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
		
		compRelationships = toolkit.createComposite(tabPart, SWT.BORDER);
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
					gc.drawRectangle(0, 0, r.width - 1, r.height - 1);
				}
				
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
		
		Menu formatsOpMenu = new Menu(getSite().getShell(), SWT.POP_UP);
		for (EmitterInfo einfo : ReportEngineManager.getBirtReportEngine().getEmitterInfo()){
			
			MenuItem mi = new MenuItem(formatsOpMenu,SWT.PUSH);
			mi.setText(einfo.getFormat());
			if (einfo.getIcon() != null){
				IConfigurationElement confElem = einfo.getEmitter();
				if ( confElem != null ){
					String pluginId = confElem.getDeclaringExtension( ).getNamespace( );
					Bundle bundle = Platform.getBundle( pluginId );
					mi.setImage( UIHelper.getImage( bundle, einfo.getIcon(), false ));
					mi.addListener (SWT.Dispose, e-> {if (!mi.getImage().isDisposed()) mi.getImage().dispose();});
				}
			}
			
			mi.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					IntelReportManager.INSTANCE.exportEntity(getEntity(), mapPart.getDateFilter(), einfo);
				}
			});
		}
		
		printItem = new ToolItem(buttonBar, SWT.DROP_DOWN);
		printItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_PDF));
		printItem.setToolTipText("print to pdf");
		printItem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent event){
				 if (event.detail == SWT.ARROW) {
			          Rectangle rect = printItem.getBounds();
			          Point pt = new Point(rect.x, rect.y + rect.height);
			          pt = buttonBar.toDisplay(pt);
			          formatsOpMenu.setLocation(pt.x, pt.y);
			          formatsOpMenu.setVisible(true);
			    }else{
					for (EmitterInfo i : ReportEngineManager.getBirtReportEngine().getEmitterInfo()){
						if (i.getFormat().equalsIgnoreCase("PDF")){
							IntelReportManager.INSTANCE.exportEntity(getEntity(), mapPart.getDateFilter(), i);
							return;
						}
					}
					MessageDialog.openError(getSite().getShell(), "Error", "Could not find PDF exporter.");
			    }
			}	
		});
		
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
					Session s = HibernateManager.openSession(new AttachmentInterceptor());
					try{
						s.beginTransaction();
						EntityManager.INSTANCE.deleteEntity(entity, s);
						s.getTransaction().commit();
					}catch (Exception ex){
						s.getTransaction().rollback();
						Intelligence2PlugIn.displayLog("Error deleting entity. " + ex.getMessage(), ex);
						return;
					}finally{
						s.close();
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
	
	public boolean hasRelation(IntelRelationshipType type){
		for (IntelEntityRelationship r : relationships){
			if (r.getRelationshipType().equals(type)) return true;
		}
		for (IntelEntityRelationship r : relationshipsToAdd){
			if (r.getRelationshipType().equals(type)) return true;
		}
		return false;
	}
	
	public void setEditMode(boolean isEdit){
		if (isEditMode && !isEdit && isDirty){
			doSave(new NullProgressMonitor());
		}
		this.isEditMode = isEdit;
		if (entity != null) initControl(entity);
	
		if (!deleteItem.isDisposed()) deleteItem.setEnabled(isEdit);
		if (!editItem.isDisposed()) editItem.setSelection(isEdit);
	}
	
	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	@Override
	public boolean isDirty(){
		return this.isDirty;
	}
	
	private void openRelationship(IntelEntity toOpen){
		(new OpenEntityHandler()).openEntity(toOpen, context);
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
				EntityRelationshipListShell shell = new EntityRelationshipListShell(getSite().getShell(), entity){
					protected void doEvent(){
						if (getRelationshipType() != null){

							IntelRelationshipType rType = getRelationshipType();
							IntelEntity e1 = entity;
							IntelEntity e2 = getTargetEntity();
							
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
								newRelationship.setSourceEntity(e2);
								newRelationship.setTargetEntity(e1);
								add = true;
							} 
							//check duplicates
							if (add){
								for (IntelEntityRelationship existing : relationships){
									if (existing.getSourceEntity().equals(newRelationship.getSourceEntity()) && 
											existing.getTargetEntity().equals(newRelationship.getTargetEntity()) &&
											existing.getRelationshipType().equals(newRelationship.getRelationshipType())){
										add = false;
										MessageDialog.openInformation(parentShell, "Relationship", "Relationship already exists between these entities. Cannot duplicate relationships.");
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
								
								if (!newRelationship.getRelationshipType().getAttributes().isEmpty()){
									//edit 
									editRelationshipAttributes(newRelationship);
								}
							}
						}
					}
				};
				
				int x = btnAddRelationship.getLocation().x + btnAddRelationship.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				int y =  btnAddRelationship.getLocation().y;
				shell.open(btnAddRelationship.toDisplay(x,y));
			}
		});
		
		Tree rTree = new Tree(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		
		treeRelationships = new TreeViewer(rTree);
		toolkit.adapt(treeRelationships.getTree());
		treeRelationships.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeRelationships.setContentProvider(new RelationshipContentProvider( input.getUuid() ));
		treeRelationships.getTree().setHeaderVisible(true);

		treeRelationships.getTree().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				int colIndex = treeRelationships.getCell(new Point(event.x, event.y)).getColumnIndex();
				if (colIndex == 3){
					Object x = ((IStructuredSelection)treeRelationships.getSelection()).getFirstElement();
					if (x instanceof IntelEntityRelationship){
						editRelationshipAttributes((IntelEntityRelationship) x);
					}
				}else if (colIndex == 1){
					Object x = ((IStructuredSelection)treeRelationships.getSelection()).getFirstElement();
					if (x instanceof IntelEntityRelationship){
						IntelEntityRelationship r = (IntelEntityRelationship)x;
						if (!r.getSourceEntity().equals(getEntity())){
							openRelationship(r.getSourceEntity());
						}
					}
				}else if (colIndex == 2){
					Object x = ((IStructuredSelection)treeRelationships.getSelection()).getFirstElement();
					if (x instanceof IntelEntityRelationship){
						IntelEntityRelationship r = (IntelEntityRelationship)x;
						if (!r.getTargetEntity().equals(getEntity())){
							openRelationship(r.getTargetEntity());
						}
					}
				}
			}
					
		});
		
		
		TreeViewerColumn colType = new TreeViewerColumn(treeRelationships, SWT.DEFAULT);
		colType.getColumn().setText("Relationship");
		colType.getColumn().setWidth(150);
		colType.setLabelProvider(new RelationshipLabelProvider(0));
		
		TreeViewerColumn colRelationshipSrc = new TreeViewerColumn(treeRelationships, SWT.DEFAULT);
		colRelationshipSrc.getColumn().setText("Source Relation");
		colRelationshipSrc.getColumn().setWidth(150);
		colRelationshipSrc.setLabelProvider(new RelationshipLabelProvider(1));
		
		TreeViewerColumn colRelationshipTrg = new TreeViewerColumn(treeRelationships, SWT.DEFAULT);
		colRelationshipTrg.getColumn().setText("Target Relation");
		colRelationshipTrg.getColumn().setWidth(150);
		colRelationshipTrg.setLabelProvider(new RelationshipLabelProvider(2));
		
		TreeViewerColumn colAttributes = new TreeViewerColumn(treeRelationships, SWT.DEFAULT);
		colAttributes.getColumn().setText("Attributes");
		colAttributes.setLabelProvider(new RelationshipLabelProvider(3));
		treeRelationships.getTree().addPaintListener(new PaintListener() {			
			@Override
			public void paintControl(PaintEvent e) {
				treeRelationships.getTree().removePaintListener(this);
				int size = parent.getParent().computeSize(SWT.DEFAULT, SWT.DEFAULT).x - 350;
				if (size < 350) size = 350;
				colAttributes.getColumn().setWidth(size);		
			}
		});
		
		//tooltip shell
		Listener tableListener = new Listener(){
			private boolean doHover = false;
			
			@Override
			public void handleEvent(Event event) {
				switch(event.type){
					case SWT.MouseDoubleClick:
					case SWT.MouseDown:
					case SWT.MouseUp:
						doHover = false;
						break;
					case SWT.MouseMove:
						doHover= true;
						break;
					case SWT.MouseHover:
						if (doHover){
							doHover(event.x,event.y);
						}
						break;
				}
					
			}
			private void doHover(int x, int y){
				
				ViewerCell cell = treeRelationships.getCell(new Point(x, y));
				if (cell == null) return;
				if (cell.getColumnIndex() != 3){
					if (detailsShell != null && !detailsShell.isDisposed()){
						detailsShell.close();
					}
					return;
				}
				if (cell != null && cell.getElement() instanceof IntelEntityRelationship){
					IntelEntityRelationship relationship = (IntelEntityRelationship) cell.getElement();
					if (detailsShell == null || detailsShell.isDisposed() || !detailsShell.getRelationship().equals(relationship)){
						detailsShell = new EntityRelationshipDetailsShell(getSite().getShell(),relationship);
					
						int height = detailsShell.getSize().y;
						Point p  = treeRelationships.getTree().toDisplay(x, y);
						detailsShell.open(new Point(p.x, p.y - height));
					}
				}else{
					if (detailsShell != null && !detailsShell.isDisposed()){
						detailsShell.close();
					}
					return;
				}
			}
			
		};
		treeRelationships.getTree().addListener(SWT.MouseDoubleClick, tableListener);
		treeRelationships.getTree().addListener(SWT.MouseDown, tableListener);
		treeRelationships.getTree().addListener(SWT.MouseUp, tableListener);
		treeRelationships.getTree().addListener(SWT.MouseMove, tableListener);
		treeRelationships.getTree().addListener(SWT.MouseHover, tableListener);	
		
		
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
				if (mnuOpen != null){
					mnuOpen.dispose();
					mnuOpen = null;
				}

				Object x = ((IStructuredSelection)treeRelationships.getSelection()).getFirstElement();
				if (x instanceof IntelEntityRelationship){
					IntelEntityRelationship r = (IntelEntityRelationship)x;
					mnuOpen = new MenuItem(thumbMenu,SWT.DEFAULT,0);
					mnuOpen.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY));
					if (r.getSourceEntity().equals(getEntity())){
						mnuOpen.setText(MessageFormat.format("Open {0}",r.getTargetEntity().getIdAttributeAsText()));
						mnuOpen.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								openRelationship(r.getTargetEntity());
							}
						});
					}else if (r.getTargetEntity().equals(getEntity())){
						mnuOpen.setText(MessageFormat.format("Open {0}",r.getSourceEntity().getIdAttributeAsText()));
						mnuOpen.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								openRelationship(r.getSourceEntity());
							}
						});
					}
				}
			
				if (isEditMode){
				
					if (mnuEdit == null){
						mnuEdit = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuEdit.setText("Edit Attributes");
						mnuEdit.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
						mnuEdit.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								//TODO: Editing Relationships
								IStructuredSelection sel = (IStructuredSelection) treeRelationships.getSelection();
								if (!sel.isEmpty()){
									if (sel.getFirstElement() instanceof IntelEntityRelationship){
										editRelationshipAttributes((IntelEntityRelationship)sel.getFirstElement());
									}
								}
							}	
						});
					}
					if (mnuDelete == null){
						new MenuItem(thumbMenu,SWT.SEPARATOR);
						
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

	private void editRelationshipAttributes(IntelEntityRelationship relation){
		RelationshipAttributeDialog dialog = new RelationshipAttributeDialog(treeRelationships.getControl().getShell(), relation);
		if (dialog.open() == Window.OK){
			treeRelationships.refresh();
			setDirty(true);
		}
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
		if (lblCreated.isDisposed()) return;
		 
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
						text = attributeLabelProvider.getText(v);
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
		private RelationshipTypeLabelProvider ll = new RelationshipTypeLabelProvider();
		private RelationshipGroupLabelProvider lg = new RelationshipGroupLabelProvider();
		private AttributeValueLabelProvider la = new AttributeValueLabelProvider();
		public RelationshipLabelProvider(int columnIndex){
			this.columnIndex = columnIndex;
		}
		
		@Override
		public void dispose(){
			super.dispose();
			ll.dispose();
			lg.dispose();
			la.dispose();
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
					return ll.getImage(((IntelEntityRelationship) element).getRelationshipType());
				}
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (columnIndex == 0){
				if (element instanceof IntelRelationshipGroup){
				return lg.getText(element);
			}else if (element instanceof IntelEntityRelationship){
				return ((IntelEntityRelationship) element).getRelationshipType().getName();
			}
		}else if (columnIndex == 1){
			if (element instanceof IntelEntityRelationship){
				IntelEntityRelationship r = (IntelEntityRelationship) element;
				return r.getSourceEntity().getIdAttributeAsText();
			}
		}else if (columnIndex == 2){
			if (element instanceof IntelEntityRelationship){
				IntelEntityRelationship r = (IntelEntityRelationship) element;
				return r.getTargetEntity().getIdAttributeAsText();
			}
		}else if (columnIndex == 3){
			if (element instanceof IntelEntityRelationship){
				
				StringBuilder sb = new StringBuilder();
				IntelEntityRelationship relation = (IntelEntityRelationship)element;
				if (relation.getAttributes() == null) return "";
				
				for (IntelRelationshipTypeAttribute attribute: relation.getRelationshipType().getAttributes()){
					for (IntelEntityRelationshipAttributeValue value : relation.getAttributes()){
						if (value.getAttribute().equals(attribute.getAttribute())){
							sb.append(value.getAttribute().getName());
							sb.append(": ");
							sb.append(la.getText(value));
							sb.append(" / ");
							break;
						}
					}
				}
				if (sb.length() > 0){
					return sb.substring(0, sb.length() - 3);
				}
				return sb.toString();
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
