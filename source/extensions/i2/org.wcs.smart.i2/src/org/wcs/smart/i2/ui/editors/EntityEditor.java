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

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

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
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
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
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
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
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.AttachmentManager;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.diagram.RelationshipGraphComposite;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityRelationship.Source;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
import org.wcs.smart.i2.model.OtherAttributeGroup;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.AttributeValueLabelProvider;
import org.wcs.smart.i2.ui.EntityPerspective;
import org.wcs.smart.i2.ui.IntelAttachmentPropertiesDialog;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.RelationshipGroupLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.ui.Resources;
import org.wcs.smart.i2.ui.SectionTabHeader;
import org.wcs.smart.i2.ui.dialogs.AttributeFieldEditor;
import org.wcs.smart.i2.ui.dialogs.RelationshipAttributeDialog;
import org.wcs.smart.i2.ui.dialogs.RelationshipSelectorDialog;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.i2.ui.entity.exporter.EntityRelationshipExportDialog;
import org.wcs.smart.i2.ui.handler.NewRecordHandler;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.i2.ui.views.FileSearchView;
import org.wcs.smart.i2.ui.views.IntelEntitySelectionTransfer;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;

/**
 * Entity editor
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class EntityEditor extends EditorPart implements MapPart{
	
	private static final String ERROR_LINK_NOT_FOUND = Messages.EntityEditor_LinkNotFound;

	private static final String DATA_MODEL_ATTRIBUTE_GROUP = Messages.EntityEditor_DataModelAttGroup;

	private static final String TBLRECORD_LBLPROVIDER_KEY = "LBLPROVIDER"; //$NON-NLS-1$

	public static final String ID = "org.wcs.smart.i2.editor.entity"; //$NON-NLS-1$

	private static final int THUMB_SIZE = 150;
	private Font boldFont = null;
	
	private boolean isEditMode;
	private boolean isDirty;
	
	private Font headerFont;
	
	private EntityEditorInput input;
	private IntelEntity entity;
	private boolean hasHiddenRelationships;
	private List<IntelEntityRelationship> relationships;
	private List<IntelEntityTypeAttributeGroup> groups;
	
	private List<AttributeFieldEditor> fieldEditors = null;
	
	private FormToolkit toolkit;
	private Canvas lblMainImage;
	private Image imgMain;
	
	private Label lblCreated;
	private Label lblModified;
	private Label lblIdentifier;
	private Label lblType;
	private Label lblTypeImage;
	private Label lblProfile;
	private Label lblProfileColor;
	private Button lnkNewRecord;

	private Composite compAttributes;
	private Composite compAttachments;
	private Text txtScratchpad;
	private Text txtDmListItem;
	private Text txtDmActive;
	private SashForm mainSash;
	private int[] mainSashMinSize;
	
	private AttachmentTable attachmentTable;
	private Composite attachmentEditPanel;
	private Composite relationshipWarningPanel, relationshipButtonEditPanel, relationshipEditPanel;
	private EntityEditorMapComposite mapPart ;
	
	private Composite compMap;
	private Composite compRecords;
	private Composite compRelationships;
	private Composite compRelationshipDiagram;
	private TableViewer tblRecords;
	private TreeViewer relationshipTree;
	private List<IntelEntityAttachment> attachmentsToDelete = new ArrayList<IntelEntityAttachment>();
	private List<IntelEntityRelationship> relationshipsToAdd = new ArrayList<IntelEntityRelationship>();
	private List<IntelEntityRelationship> relationshipsToDelete = new ArrayList<IntelEntityRelationship>();
	
	private RelationshipGraphComposite graphComposite;
	
	private IEclipseContext context;
	private IEventBroker eventBroker;

	private ToolItem deleteItem;
	private ToolItem editItem;
	private ToolItem wsetItem;
	private ToolItem printItem;
	private ToolItem exportItem;
	private ToolItem saveItem;
	
	private List<EventHandler> eventHandles = null;
	
	private AttributeValueLabelProvider attributeLabelProvider = new AttributeValueLabelProvider();
	
	private IPerspectiveListener perspectiveListener = new PerspectiveAdapter() {
		@Override
		public void perspectiveActivated(IWorkbenchPage page,
				IPerspectiveDescriptor perspective) {
			if (isDirty && perspective.getId().equals(IntelDataAnalysisPerspective.ID)){
				//save and be done with it
				setEditMode(false);
			}else if (perspective.getId().equals(IntelDataAssessmentPerspective.ID) ||
					perspective.getId().equals(EntityPerspective.ID)){
				setEditMode(true);
			}
		}
	};
	
	private Job loadEntity = new Job("load entity"){ //$NON-NLS-1$
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			entity = null;
			IntelEntity temp = null;
			try(Session s = HibernateManager.openSession()){
				temp = (IntelEntity) s.get(IntelEntity.class, input.getUuid());
				if (temp == null){
					//close editor
					closeEditor(false);
					return Status.OK_STATUS;
				}
				temp.getProfile().getName();
				temp.getEntityType().getIcon();
				if (temp.getEntityType().getDmAttribute() != null) {
					temp.getEntityType().getDmAttribute().getName();
					if (temp.getDmAttributeListItem() != null) temp.getDmAttributeListItem().getNames().size();
				}
				for(IntelEntityTypeAttribute a : temp.getEntityType().getAttributes()){
					a.getAttribute().getName();
					if (a.getAttribute().getAttributeList() != null){
						for(IntelAttributeListItem i : a.getAttribute().getAttributeList()){
							i.getName();
						}
					}
				}
				
				for (IntelEntityAttributeValue v : temp.getAttributes()){
					if (v.getAttributeListItem() != null) v.getAttributeListItem().getName();
					if (v.getEmployee() != null) SmartLabelProvider.getFullLabel(v.getEmployee());
					v.getAttribute().getName();
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
				CriteriaBuilder cb = s.getCriteriaBuilder();
				CriteriaQuery<IntelEntityTypeAttributeGroup> c = cb.createQuery(IntelEntityTypeAttributeGroup.class);
				Root<IntelEntityTypeAttributeGroup> from = c.from(IntelEntityTypeAttributeGroup.class);
				c.where(cb.equal(from.get("entityType"), temp.getEntityType())); //$NON-NLS-1$
				c.orderBy(cb.asc(from.get("order"))); //$NON-NLS-1$
				groups = s.createQuery(c).getResultList();
				
				
				temp.getPrimaryAttachment();
				
				CriteriaQuery<IntelEntityRelationship> c2 = cb.createQuery(IntelEntityRelationship.class);
				Root<IntelEntityRelationship> from2 = c2.from(IntelEntityRelationship.class);
				c2.where(cb.or(
						cb.equal(from2.get("sourceEntity"), temp), //$NON-NLS-1$
						cb.equal(from2.get("targetEntity"), temp) //$NON-NLS-1$
						));
				List<IntelEntityRelationship> all = s.createQuery(c2).getResultList();		
				relationships = new ArrayList<>();
				
				hasHiddenRelationships = false;
				for (IntelEntityRelationship r : all){
					
					if (!ProfilesManager.INSTANCE.getActiveProfiles().contains(r.getRelationshipType().getSourceProfile()) ||
						!ProfilesManager.INSTANCE.getActiveProfiles().contains(r.getRelationshipType().getTargetProfile()) ||
						!IntelSecurityManager.INSTANCE.canViewEntities(r.getRelationshipType().getSourceProfile()) ||
						!IntelSecurityManager.INSTANCE.canViewEntities(r.getRelationshipType().getTargetProfile()) ) {
						hasHiddenRelationships = true;
						continue;
					}
					
					relationships.add(r);
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
							if (e.getAttributeListItem() != null) e.getAttributeListItem().getName();
							if (e.getEmployee() != null) SmartLabelProvider.getFullLabel(e.getEmployee());
						});
					}
					if (r.getSourceId() != null){
						if (r.getSource().equals(Source.ENTITY)){
							IntelEntity src = (IntelEntity) s.get(IntelEntity.class, r.getSourceId());
							if (src != null){
								src.getIdAttributeAsText();
								r.setSourceObject(src);
							}
						}else if (r.getSource().equals(Source.RECORD)){
							IntelRecord src = (IntelRecord)s.get(IntelRecord.class, r.getSourceId());
							if (src != null){
								src.getTitle();
								r.setSourceObject(src);
							}
						}
					}
				}
				
			}
			
			entity = temp;
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					initControl();
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private Job loadRecords = new Job("loading entity records"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelRecord> records = new ArrayList<IntelRecord>();
			Set<IntelRecordSource> sources = new HashSet<IntelRecordSource>();
			try(Session s = HibernateManager.openSession()){
				List<IntelEntityRecord> entityRecords =  QueryFactory.buildQuery(s, IntelEntityRecord.class, "id.entity", entity).getResultList(); //$NON-NLS-1$
				for (IntelEntityRecord r : entityRecords){
					records.add(r.getRecord());
					r.getRecord().getDateCreated();
					r.getRecord().getDateModified();
					r.getRecord().getTitle();
					if (r.getRecord().getRecordSource() != null){
						r.getRecord().getRecordSource().getIcon();
						sources.add(r.getRecord().getRecordSource());
					}
				}
			}
			Collections.sort(records, (a,b)-> -1*a.getPrimaryDate().compareTo(b.getPrimaryDate()));
			
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
				MessageDialog.openError(getSite().getShell(), Messages.EntityEditor_SaveDialogTitle, MessageFormat.format(Messages.EntityEditor_SaveDialogMsg, editor.getAttribute().getName()));
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
		entity.setComment(txtScratchpad.getText());

		List<IntelEntity> otherEntityModified = new ArrayList<IntelEntity>();
		
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
	
			s.beginTransaction();
			try {
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
				
				if (entity.getDmAttributeListItem() != null) {
					entity.getDmAttributeListItem().updateName(entity.getConservationArea().getDefaultLanguage(), entity.getIdAttributeAsText());
					entity.getDmAttributeListItem().setName(entity.getIdAttributeAsText());
					s.saveOrUpdate(entity.getDmAttributeListItem());
				}
				
				entity.updateActiveValue();
				s.getTransaction().commit();
				clearLists();
			}catch (Exception ex){
				if (s.getTransaction().isActive())s.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(Messages.EntityEditor_SaveError + ex.getMessage(), ex);
				return;
			}
		}
		
		lblIdentifier.setText(entity.getIdAttributeAsText());
		lblModified.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(entity.getDateModified()));
		if (txtDmListItem != null) {
			if (entity.getDmAttributeListItem() != null) {
				txtDmListItem.setText(MessageFormat.format("{0} [{1}]", entity.getDmAttributeListItem().getName(), entity.getDmAttributeListItem().getKeyId())); //$NON-NLS-1$
				txtDmActive.setText(entity.getDmAttributeListItem().getIsActive() ? SmartLabelProvider.BOOLEAN_TRUE_LABEL : SmartLabelProvider.BOOLEAN_FALSE_LABEL);
			}else {
				txtDmListItem.setText(ERROR_LINK_NOT_FOUND);
				txtDmActive.setText(ERROR_LINK_NOT_FOUND);
			}
		}

		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put(UIEvents.EventTags.ELEMENT, context.get(MPart.class));
		data.put(IEventBroker.DATA, entity);
		eventBroker.send(IntelEvents.ENTITY_MODIFIED, data);
		
		if (!otherEntityModified.isEmpty()) eventBroker.send(IntelEvents.ENTITY_MODIFIED, otherEntityModified);
		
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
		setEditMode(this.input.getDefaultEditMode());
		super.setPartName(input.getName());
	}


	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	private void closeEditor(boolean promptsave){
		getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(EntityEditor.this, promptsave);
	}
	
	private void subscribeToEvents(){
		eventHandles = new ArrayList<EventHandler>();
		
		EventHandler handler = (e) -> {
			if (IntelSecurityManager.INSTANCE.canEditWorkingSet())
					wsetItem.setEnabled(WorkingSetManager.INSTANCE.isSet());
				else
					wsetItem.setEnabled(false);
		};
		eventHandles.add(handler);
		eventBroker.subscribe(IntelEvents.ACTIVE_WS_SET, handler);
		
		//on delete close editor
		handler = new EventHandler() {
			@Override
			public void handleEvent(org.osgi.service.event.Event event) {
				Object data = event.getProperty(IEventBroker.DATA);
				if (data != null ){
					if (data.equals(entity) || data.equals(entity.getEntityType())){
						closeEditor(false);
					}else if (data instanceof Collection){
						Collection<?> items = (Collection<?>) data;
						items.forEach(x->{
							if (x.equals(entity) || x.equals(entity.getEntityType())) getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(EntityEditor.this, false);
						});
					}
				}
				
			}
		};
		eventHandles.add(handler);
		eventBroker.subscribe(IntelEvents.ENTITY_DELETE, handler);
		eventBroker.subscribe(IntelEvents.ENTITY_TYPE_DELETE, handler);
		
		
		
		EventHandler promptToReset = new EventHandler(){
			@Override
			public void handleEvent(org.osgi.service.event.Event event) {
				if (context.get(MPart.class) == event.getProperty(UIEvents.EventTags.ELEMENT)){
					eventBroker.unsubscribe(this);
					if (MessageDialog.openQuestion(getSite().getShell(), Messages.EntityEditor_ModifiedTitle, MessageFormat.format(Messages.EntityEditor_ModifiedMsg, entity.getIdAttributeAsText()))){
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
				if (data == null) return;
				boolean equalsEntity = false;
				boolean equalsEntityType = false;
				boolean hasRelation = false;
				if (data.equals(entity)){
					equalsEntity = true;
				}else if (data.equals(entity.getEntityType())){
					equalsEntityType = true;
				}else if (data instanceof IntelRelationshipType){
					hasRelation = hasRelation((IntelRelationshipType)data);
				}else if (data instanceof Collection){
					for (Object x : ((Collection<?>)data)){
						if (x.equals(entity)){
							equalsEntity = true;
							break;
						}else if (x.equals(entity.getEntityType())){
							equalsEntityType = true;
							break;
						}else if (data instanceof IntelRelationshipType){
							hasRelation = hasRelation((IntelRelationshipType)data);
						}
					}
				}
				
				if (context.get(MPart.class) != event.getProperty(UIEvents.EventTags.ELEMENT)){
					if (data != null && (
						equalsEntity || equalsEntityType || hasRelation )){
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
		
		handler = event->{
			if (!ProfilesManager.INSTANCE.getActiveProfiles().contains(getEntity().getProfile())) {
				//close entity
				closeEditor(true);
			}else {
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
		};
		eventHandles.add(handler);
		eventBroker.subscribe(IntelEvents.PROFILES_ALL, handler);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		
		context = (IEclipseContext) getSite().getService(IEclipseContext.class);
		eventBroker = context.get(IEventBroker.class);
		//configure tags for part to ensure shows in both perspectives
		MPart part = context.get(MPart.class); 
		if (!part.getTags().contains(IntelDataAssessmentPerspective.ID)) part.getTags().add(IntelDataAssessmentPerspective.ID);
		if (!part.getTags().contains(IntelDataAnalysisPerspective.ID)) part.getTags().add(IntelDataAnalysisPerspective.ID);
		if (!part.getTags().contains(EntityPerspective.ID)) part.getTags().add(EntityPerspective.ID);

		
		getSite().getWorkbenchWindow().addPerspectiveListener(perspectiveListener);
		
		
		toolkit = new FormToolkit(parent.getDisplay());

		parent.setLayout(createGridLayoutNoMargin(1));
		
		mainSash = new SashForm(parent, SWT.VERTICAL);
		mainSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		int minSize1 = createTopPanel(mainSash);
		int minSize2 = createBottomPanel(mainSash);
		mainSashMinSize = new int[]{minSize1, minSize2};
		mainSash.setWeights(new int[]{1,2});
		
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
		getSite().getWorkbenchWindow().removePerspectiveListener(perspectiveListener);
		//remove all event subscriptions
		if (eventHandles != null) eventHandles.forEach((h)->eventBroker.unsubscribe(h));
		eventHandles = null;
		
		attributeLabelProvider.dispose();
		if (mapPart != null) mapPart.dispose();
		
		super.dispose();
		
		this.mapPart = null;

	}
	
	
	private void maximizeMainPanel(int index){
		int totalHeight = mainSash.getClientArea().height;
		int weights[] = new int[mainSashMinSize.length];
		for (int i= 0; i < weights.length; i ++){
			weights[i] = mainSashMinSize[i];
			if (i == index){
				totalHeight -= weights[i];
			}
		}
		weights[index] = totalHeight;
		mainSash.setWeights(weights);
	}
	
	private int createBottomPanel(Composite parent){
		Composite bottom = toolkit.createComposite(parent);
		bottom.setLayout(new GridLayout());
		((GridLayout)bottom.getLayout()).marginWidth = 0;
		((GridLayout)bottom.getLayout()).marginHeight = 0;
		
		SectionTabHeader tabList = new SectionTabHeader(new String[]{Messages.EntityEditor_MapTitle, Messages.EntityEditor_RecordsTitle, Messages.EntityEditor_RelationshipsTitle, Messages.EntityEditor_RelationshipDiagramTitle}, bottom, toolkit, ()->maximizeMainPanel(1));
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
		((GridLayout)compRecords.getLayout()).marginHeight = 0;
		createRecordsPanel(compRecords);
		
		compRelationships = toolkit.createComposite(tabPart, SWT.NONE);
		compRelationships.setLayout(new GridLayout());
		((GridLayout)compRelationships.getLayout()).marginHeight = 0;
		createRelationshipPanel(compRelationships);
		addEntityDropTarget(compRelationships);

		compRelationshipDiagram = toolkit.createComposite(tabPart, SWT.NONE);
		compRelationshipDiagram.setLayout(new GridLayout());
		((GridLayout)compRelationshipDiagram.getLayout()).marginWidth = 0;
		((GridLayout)compRelationshipDiagram.getLayout()).marginHeight = 0;
		graphComposite = new RelationshipGraphComposite(compRelationshipDiagram, toolkit, this);
		
		tabList.setContent(new Composite[]{compMap,  compRecords, compRelationships, compRelationshipDiagram}, tabPart);
		tabList.selectTab(0);
		return tabList.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
	}

	private void addEntityDropTarget(Composite comp){
		DropTarget dropTarget = new DropTarget(comp, DND.DROP_LINK);
		dropTarget.setTransfer(new Transfer[]{IntelEntitySelectionTransfer.getTransfer()});
		dropTarget.addDropListener(new DropTargetListener() {		
			private PaintListener paintListener = new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION));
					e.gc.setLineWidth(2);
					e.gc.drawRectangle(0, 0, e.width, e.height);
				}
			};
			
			@Override
			public void dropAccept(DropTargetEvent event) {
			}
			
			@Override
			public void drop(DropTargetEvent event) {
				ISelection s = IntelEntitySelectionTransfer.getTransfer().getSelection();
				
				if (s != null && s instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection)s;
					for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
						Object element = (Object)iterator.next();
						if (element instanceof IntelEntity){
							
							RelationshipSelectorDialog dialog = new RelationshipSelectorDialog( getSite().getShell(),
									getEntity().getProfile(), getEntity().getEntityType(), ((IntelEntity)element).getProfile(), ((IntelEntity)element).getEntityType() );
							dialog.open();
							if (dialog.getRelationshipType() != null){
								addRelationship(dialog.getRelationshipType(), (IntelEntity) element);
							}
						}
					}
				}
				
				comp.removePaintListener(paintListener);
				comp.redraw();
			}
			
			@Override
			public void dragOver(DropTargetEvent event) {
				 
			}
			
			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				event.detail = DND.DROP_LINK;
			}
			
			@Override
			public void dragLeave(DropTargetEvent event) {
				comp.removePaintListener(paintListener);
				comp.redraw();
			}
			
			@Override
			public void dragEnter(DropTargetEvent event) {
				if (getEditMode()) {
					event.detail = DND.DROP_LINK;
					comp.addPaintListener(paintListener);
					comp.redraw();
				}
			}
		});
	}
	private int createTopPanel(Composite parent){
		
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight()  + 1);
		headerFont = new Font(parent.getDisplay(), fd);
		
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		((GridLayout)panel.getLayout()).horizontalSpacing = 0;
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = toolkit.createComposite(panel, SWT.NONE);
		leftPart.setLayout(new GridLayout(2, false));
		leftPart.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		lblMainImage = new Canvas(leftPart, SWT.BORDER);
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

		toolkit.createLabel(leftPart, Messages.EntityEditor_ProfileLabel);
		
		Composite t = toolkit.createComposite(leftPart);
		t.setLayout(new GridLayout(2, false));
		((GridLayout)t.getLayout()).marginWidth = 0;
		((GridLayout)t.getLayout()).marginHeight = 0;
		((GridLayout)t.getLayout()).horizontalSpacing = 0;
		t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		lblProfileColor = toolkit.createLabel(t, ""); //$NON-NLS-1$
		lblProfileColor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		lblProfile = toolkit.createLabel(t, ""); //$NON-NLS-1$
		lblProfile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		toolkit.createLabel(leftPart, Messages.EntityEditor_TypeLabel);
		
		t = toolkit.createComposite(leftPart);
		t.setLayout(new GridLayout(2, false));
		((GridLayout)t.getLayout()).marginWidth = 0;
		((GridLayout)t.getLayout()).marginHeight = 0;
		((GridLayout)t.getLayout()).horizontalSpacing = 0;
		t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		lblTypeImage = toolkit.createLabel(t, ""); //$NON-NLS-1$
		lblTypeImage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		lblType = toolkit.createLabel(t, ""); //$NON-NLS-1$
		lblType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblType.getLayoutData()).widthHint = THUMB_SIZE - 50;

		Composite rightPart = toolkit.createComposite(panel, SWT.NONE);
		rightPart.setLayout(new GridLayout());
		rightPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)rightPart.getLayout()).verticalSpacing = 2;
		
		Composite header = toolkit.createComposite(rightPart);
		header.setLayout(new GridLayout(2, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		WidgetElement.setCSSClass(header, "SMARTFormHeader");  //$NON-NLS-1$
		
		lblIdentifier = toolkit.createLabel(header, ""); //$NON-NLS-1$
		lblIdentifier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lblIdentifier.setFont(headerFont);

		ToolBar buttonBar = new ToolBar(header, SWT.HORIZONTAL | SWT.FLAT);
		buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		Menu formatsOpMenu = new Menu(getSite().getShell(), SWT.POP_UP);
		buttonBar.addListener(SWT.Dispose, d->formatsOpMenu.dispose());
		
		for (EmitterInfo einfo : ReportEngineManager.getBirtReportEngine().getEmitterInfo()){
			MenuItem mi = new MenuItem(formatsOpMenu,SWT.PUSH);
			mi.setText(einfo.getFormat());
			if (einfo.getIcon() != null){
				IConfigurationElement confElem = einfo.getEmitter();
				if ( confElem != null ){
					String pluginId = confElem.getDeclaringExtension( ).getNamespace( );
					Bundle bundle = Platform.getBundle( pluginId );
					mi.setImage( UIHelper.getImage( bundle, einfo.getIcon(), false ));
				}
			}
			
			mi.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					IntelReportManager.INSTANCE.exportEntity(getEntity(), mapPart.getDateFilter(), einfo);
				}
			});
		}
		
		saveItem = new ToolItem(buttonBar, SWT.PUSH);
		saveItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.SAVE_ICON));
		saveItem.setToolTipText(Messages.EntityEditor_savetooltip);
		saveItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent event){
				getSite().getPage().saveEditor(EntityEditor.this, false);
			}
		});
		saveItem.setEnabled(false);
		
		exportItem = new ToolItem(buttonBar, SWT.PUSH);
		exportItem.setToolTipText(Messages.EntityEditor_exporttooltip);
		exportItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY_EXPORT));
		exportItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EntityRelationshipExportDialog dialog = new EntityRelationshipExportDialog(entity, getSite().getShell());
				dialog.open();
			}
		});
		
		printItem = new ToolItem(buttonBar, SWT.DROP_DOWN);
		printItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.PDF_ICON));
		printItem.setToolTipText(Messages.EntityEditor_printtooltip);
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
						if (i.getFormat().equalsIgnoreCase("PDF")){ //$NON-NLS-1$
							IntelReportManager.INSTANCE.exportEntity(getEntity(), mapPart.getDateFilter(), i);
							return;
						}
					}
					MessageDialog.openError(getSite().getShell(), Messages.EntityEditor_PdfErrorTitle, Messages.EntityEditor_PdfErrorMsg);
			    }
			}	
		});
		
		ToolItem refreshItem = new ToolItem(buttonBar, SWT.PUSH);
		refreshItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		refreshItem.setToolTipText(Messages.EntityEditor_refreshtooltip);
		refreshItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean doAction = true;
				if (isDirty){
					if (!MessageDialog.openConfirm(getSite().getShell(), Messages.EntityEditor_RefreshDialogTitle, Messages.EntityEditor_RefreshDialogMessage)){
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
		deleteItem.setToolTipText(Messages.EntityEditor_deleteTooltip);
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (MessageDialog.openConfirm(getSite().getShell(), Messages.EntityEditor_DeleteEntityTitle, Messages.EntityEditor_DeleteEntityMessage)){
					
					//look for any dirty record editors and save them first
					List<RecordEditor> editors = new ArrayList<>();
					StringBuilder names = new StringBuilder();
					for(MPart p : context.get(EPartService.class).getParts()){
						Object x = E3Utils.getSourceObject(p);
						if ( x instanceof RecordEditor && ((RecordEditor)x).isDirty()){
							editors.add((RecordEditor) x);
							names.append(((RecordEditor)x).getPartName());
							names.append(", "); //$NON-NLS-1$
						}
					}
					if (!editors.isEmpty()){
						StringBuilder sb = new StringBuilder();
						sb.append(Messages.EntityEditor_DeleteCloseWarning);
						sb.append("\n"); //$NON-NLS-1$
						sb.append(names.substring(0, names.length() - 2));
						
						if (!MessageDialog.openQuestion(getSite().getShell(), Messages.EntityEditor_38, sb.toString())){
							return;
						}
						for (RecordEditor p : editors){
							try{
								getSite().getPage().saveEditor(p, false);
							}catch (Exception ex){
								Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
							}
						}
					}
					ProgressMonitorDialog pmd = new ProgressMonitorDialog(getSite().getShell());
					try{
						pmd.run(true, false, new IRunnableWithProgress() {
							
							@Override
							public void run(IProgressMonitor monitor) throws InvocationTargetException,
									InterruptedException {
								monitor.beginTask(Messages.EntityEditor_DeleteTaskName, IProgressMonitor.UNKNOWN);
								try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
									s.beginTransaction();
									try {
										EntityManager.INSTANCE.deleteEntity(entity, s);
										s.getTransaction().commit();
									}catch (Exception ex){
										s.getTransaction().rollback();
										throw new InvocationTargetException(ex);
									}
								}
							}
						});
					}catch (Exception ex){
						Intelligence2PlugIn.displayLog(Messages.EntityEditor_DeleteError + ex.getMessage(), ex);
						return;
					}
					eventBroker.send(IntelEvents.ENTITY_DELETE, entity);
				}
				
			}
		});
		
		if (IntelSecurityManager.INSTANCE.canDeleteEntity(input.getProfileUuid()) && IntelSecurityManager.INSTANCE.canEditEntity(input.getProfileUuid())){
			deleteItem.setEnabled(getEditMode());	
		}else if (IntelSecurityManager.INSTANCE.canDeleteEntity(input.getProfileUuid())  ) {
			deleteItem.setEnabled(true);
		}else{
			deleteItem.setEnabled(false);
		}
		
		wsetItem = new ToolItem(buttonBar, SWT.PUSH);
		wsetItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
		wsetItem.setToolTipText(Messages.EntityEditor_addtowstooltip);
		wsetItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				WorkingSetManager.INSTANCE.addEntityToActiveWorkingSet(Collections.singleton(getEntity()), context);
			}
		});
		wsetItem.setEnabled(IntelSecurityManager.INSTANCE.canEditWorkingSet() && WorkingSetManager.INSTANCE.isSet());
		
		
		editItem = new ToolItem(buttonBar, SWT.CHECK);
		editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editItem.setToolTipText(Messages.EntityEditor_editingtooltip);
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setEditMode(!getEditMode());
			}
		});
		
		SectionTabHeader tabList = new SectionTabHeader(new String[]{
				Messages.EntityEditor_AttributeTitle, 
				Messages.EntityEditor_FilesTitle,
				Messages.EntityEditor_ScratchpadTitle,
				Messages.EntityEditor_HistoryLabel}, rightPart, toolkit, ()->maximizeMainPanel(0));
		
		tabList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite tabPart = toolkit.createComposite(rightPart, SWT.NONE);
		tabPart.setLayout(new StackLayout());
		tabPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		compAttributes = toolkit.createComposite(tabPart, SWT.NONE);
		compAttributes.setLayout(new GridLayout());
		((GridLayout)compAttributes.getLayout()).marginWidth = 0;
		((GridLayout)compAttributes.getLayout()).marginHeight = 0;
		
		compAttachments = toolkit.createComposite(tabPart, SWT.NONE);
		compAttachments.setLayout(new GridLayout());
		((GridLayout)compAttachments.getLayout()).marginHeight = 0;
		((GridLayout)compAttachments.getLayout()).marginWidth = 0;
		
		createAttachmentPanel(compAttachments);
		
		Composite compScratchpad = toolkit.createComposite(tabPart, SWT.NONE);
		compScratchpad.setLayout(new GridLayout());
		((GridLayout)compScratchpad.getLayout()).marginHeight = 0;
		((GridLayout)compScratchpad.getLayout()).marginWidth = 0;
		
		txtScratchpad = toolkit.createText(compScratchpad, "", SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP); //$NON-NLS-1$
		txtScratchpad.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtScratchpad.addListener(SWT.Modify, e->setDirty(true));
		txtScratchpad.setTextLimit(IntelEntity.SCRATCH_MAX_LENGTH);
		
		Composite compHistory = toolkit.createComposite(tabPart, SWT.NONE);
		compHistory.setLayout(new GridLayout());
		((GridLayout)compHistory.getLayout()).marginHeight = 0;
		((GridLayout)compHistory.getLayout()).marginWidth = 0;
				
		createHistoryPanel(compHistory);
		
		tabList.setContent(new Composite[]{compAttributes,  compAttachments, compScratchpad, compHistory}, tabPart);
		tabList.selectTab(0);
		
		if (!IntelSecurityManager.INSTANCE.canEditEntity(input.getProfileUuid())){
			setEditMode(false);
			editItem.setEnabled(false);
		}else {
			setEditMode(true);
		}
		
		return leftPart.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
	}
	
	private void createHistoryPanel(Composite parent) {
		
		Composite c = toolkit.createComposite(parent);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		toolkit.createLabel(c, Messages.EntityEditor_CreatedLabel);
		lblCreated = toolkit.createLabel(c, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(LocalDateTime.now()));
		lblCreated.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(c, Messages.EntityEditor_ModifiedLabel);
		lblModified= toolkit.createLabel(c, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(LocalDateTime.now()));
		lblModified.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
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
	
	public boolean getEditMode(){
		return this.isEditMode;
	}
	
	public void setEditMode(boolean isEdit){
		if (isEdit && !(IntelSecurityManager.INSTANCE.canEditEntity(input.getProfileUuid()) 
				|| IntelSecurityManager.INSTANCE.canCreateEntity(input.getProfileUuid()))){
			//cannot change the edit more; this user cannot edit entities
			return;
		}
		
		if (isEditMode && !isEdit && isDirty){
			doSave(new NullProgressMonitor());
		}
		this.isEditMode = isEdit;
		if (entity != null) initControl();
		if (deleteItem != null && !deleteItem.isDisposed()) {
			if (IntelSecurityManager.INSTANCE.canDeleteEntity(input.getProfileUuid()) && IntelSecurityManager.INSTANCE.canEditEntity(input.getProfileUuid())){
				deleteItem.setEnabled(isEdit);	
			}else if (IntelSecurityManager.INSTANCE.canDeleteEntity(input.getProfileUuid())  ) {
				deleteItem.setEnabled(true);
			}else{
				deleteItem.setEnabled(false);
			}
		}
		if (editItem != null && !editItem.isDisposed()) editItem.setSelection(isEdit);
		if (lnkNewRecord != null && !lnkNewRecord.isDisposed()){
			if (isEdit) {
				lnkNewRecord.setVisible(true);
				((GridData)lnkNewRecord.getLayoutData()).heightHint = lnkNewRecord.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
			}else {
				lnkNewRecord.setVisible(false);
				((GridData)lnkNewRecord.getLayoutData()).heightHint = 0;
			}
			lnkNewRecord.getParent().layout(true);
		}
	}
	
	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		saveItem.setEnabled(isDirty);
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	@Override
	public boolean isDirty(){
		return this.isDirty;
	}
	
	private void openEntity(IntelEntity toOpen){
		(new OpenEntityHandler()).openEntity(toOpen, context);
	}
	private void deleteRelationship(){
		IStructuredSelection sel = (IStructuredSelection) relationshipTree.getSelection();
		if (!sel.isEmpty()){
			if (sel.getFirstElement() instanceof IntelEntityRelationship){
				IntelEntityRelationship r = (IntelEntityRelationship)sel.getFirstElement();
				relationships.remove(r);
				relationshipsToDelete.add(r);
				((RelationshipContentProvider)relationshipTree.getContentProvider()).refresh();
				relationshipTree.refresh();
				setDirty(true);
			}
		}
	}
	
	private void addRelationship(IntelRelationshipType rType, IntelEntity targetEntity){
		if (rType == null) return;
		
		try(Session session = HibernateManager.openSession()){
			rType = session.get(IntelRelationshipType.class, rType.getUuid());
			targetEntity = session.get(IntelEntity.class, targetEntity.getUuid());
			targetEntity.getAttributes().size();
			
			if (rType.getRelationshipGroup() != null) rType.getRelationshipGroup().getNames().size();
			rType.getSourceProfile().equals(getEntity().getProfile());
			rType.getTargetProfile().equals(targetEntity.getProfile());
			
			rType.getAttributes().forEach(e->e.getAttribute().getName());
			
			targetEntity.getEntityType().equals(null);
		}
		

		IntelEntity src = entity;
		IntelEntity trg = targetEntity;
		
		IntelEntityRelationship newRelationship = new IntelEntityRelationship();
		newRelationship.setRelationshipType(rType);
		newRelationship.setSource(IntelEntityRelationship.Source.ENTITY);
		newRelationship.setSourceId(entity.getUuid());
		newRelationship.setSourceObject(entity);
		
		if (src.getProfile().equals(rType.getSourceProfile()) && 
				trg.getProfile().equals(rType.getTargetProfile()) &&
				(rType.getSourceEntityType() == null || src.getEntityType().equals(rType.getSourceEntityType())) &&
				(rType.getTargetEntityType() == null || trg.getEntityType().equals(rType.getTargetEntityType()))
				) {
			//ok
		}else if (src.getProfile().equals(rType.getTargetProfile()) &&
				trg.getProfile().equals(rType.getSourceProfile()) &&
				(rType.getTargetEntityType() == null || src.getEntityType().equals(rType.getTargetEntityType())) &&
				(rType.getSourceEntityType() == null || trg.getEntityType().equals(rType.getSourceEntityType()))
				) {
			//switch these
			IntelEntity temp = src;
			src = trg;
			trg = temp;
		}else {
			return;
		}
		
		newRelationship.setSourceEntity(src);
		newRelationship.setTargetEntity(trg);
		
		//check duplicates
		for (IntelEntityRelationship existing : relationships){
			if (existing.getSourceEntity().equals(newRelationship.getSourceEntity()) && 
					existing.getTargetEntity().equals(newRelationship.getTargetEntity()) &&
					existing.getRelationshipType().equals(newRelationship.getRelationshipType())){
				MessageDialog.openInformation(getEditorSite().getShell(), Messages.EntityEditor_RelationshipInfoDialog, Messages.EntityEditor_RelationshipInfoMsg);
				return;
			}
		}
		if (!newRelationship.getRelationshipType().getAttributes().isEmpty()){
			//edit 
			if (!editRelationshipAttributes(newRelationship)) {
				return;
			}
		}
			
		relationships.add(newRelationship);
		relationshipsToAdd.add(newRelationship);
		((RelationshipContentProvider)relationshipTree.getContentProvider()).refresh();
		relationshipTree.refresh();
		setDirty(true);
	}
	
	private void createRelationshipPanel(Composite parent){

		relationshipEditPanel = toolkit.createComposite(parent, SWT.NONE);
		relationshipEditPanel.setLayout(createGridLayoutNoMargin(2));
		relationshipEditPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		((GridLayout)relationshipEditPanel.getLayout()).verticalSpacing = 0;
		
		relationshipWarningPanel = toolkit.createComposite(relationshipEditPanel, SWT.NONE);
		relationshipWarningPanel.setLayout(createGridLayoutNoMargin(2));
		relationshipWarningPanel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		Label l = toolkit.createLabel(relationshipWarningPanel, ""); //$NON-NLS-1$
		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		l = toolkit.createLabel(relationshipWarningPanel, Messages.EntityEditor_HiddenMsg);
		l.setToolTipText(Messages.EntityEditor_HiddenTooltip);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		relationshipButtonEditPanel = toolkit.createComposite(relationshipEditPanel, SWT.NONE);
		relationshipButtonEditPanel.setLayout(createGridLayoutNoMargin(3));
		relationshipButtonEditPanel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		Button addRelationship = toolkit.createButton(relationshipButtonEditPanel, Messages.EntityEditor_NewRelationshipBtn, SWT.PUSH);
		addRelationship.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addRelationship.setToolTipText(Messages.EntityEditor_deleteRelationshiptooltip);
		addRelationship.addListener(SWT.Selection, e->{
			EntityRelationshipListShell shell = new EntityRelationshipListShell(getSite().getShell(), entity, context){
				protected void doEvent(){
					if (getRelationshipType() != null){
						addRelationship(getRelationshipType(), getTargetEntity());
						close();
					}
				}
			};
			int x = addRelationship.getLocation().x + addRelationship.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			int y =  addRelationship.getLocation().y;
			shell.open(addRelationship.toDisplay(x,y), new Point(addRelationship.getSize().x, 0), true);
		} );
				
		Button deleteRelationship = toolkit.createButton(relationshipButtonEditPanel, DialogConstants.DELETE_BUTTON_TEXT, SWT.PUSH);
		deleteRelationship.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteRelationship.setToolTipText(Messages.EntityEditor_deleteRelationshiptooltip);
		deleteRelationship.addListener(SWT.Selection, e-> deleteRelationship());
		
		Button editRelationship = toolkit.createButton(relationshipButtonEditPanel, DialogConstants.EDIT_BUTTON_TEXT, SWT.PUSH);
		editRelationship.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editRelationship.setToolTipText(Messages.EntityEditor_editRelationshiptooltip);
		editRelationship.addListener(SWT.Selection, e-> {
			IStructuredSelection sel = (IStructuredSelection) relationshipTree.getSelection();
			if (!sel.isEmpty()){
				if (sel.getFirstElement() instanceof IntelEntityRelationship){
					editRelationshipAttributes((IntelEntityRelationship) sel.getFirstElement());
				}
			}
		});
		
		Tree rTree = new Tree(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

		relationshipTree = new TreeViewer(rTree);
		toolkit.adapt(relationshipTree.getTree());
		relationshipTree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		relationshipTree.setContentProvider(new RelationshipContentProvider( input.getUuid() ));
		relationshipTree.getTree().setHeaderVisible(true);

		relationshipTree.getTree().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = relationshipTree.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				int colIndex = cell.getColumnIndex();
				if (colIndex == 4){
					Object x = ((IStructuredSelection)relationshipTree.getSelection()).getFirstElement();
					if (getEditMode() && x instanceof IntelEntityRelationship){
						editRelationshipAttributes((IntelEntityRelationship) x);
					}
				}else if (colIndex == 3){
					//source
					Object x = ((IStructuredSelection)relationshipTree.getSelection()).getFirstElement();
					if (x instanceof IntelEntityRelationship){
						IntelEntityRelationship r = (IntelEntityRelationship)x;
						if (r.getSourceObject() != null){
							if (r.getSource() == Source.ENTITY && !r.getSourceObject().equals(getEntity())){
								openEntity((IntelEntity)r.getSourceObject());
							}else if (r.getSource() == Source.RECORD){
								openRecord((IntelRecord)r.getSourceObject());
							}
						}
					}
				}else if (colIndex == 1){
					Object x = ((IStructuredSelection)relationshipTree.getSelection()).getFirstElement();
					if (x instanceof IntelEntityRelationship){
						IntelEntityRelationship r = (IntelEntityRelationship)x;
						if (!r.getSourceEntity().equals(getEntity())){
							openEntity(r.getSourceEntity());
						}
					}
				}else if (colIndex == 2){
					Object x = ((IStructuredSelection)relationshipTree.getSelection()).getFirstElement();
					if (x instanceof IntelEntityRelationship){
						IntelEntityRelationship r = (IntelEntityRelationship)x;
						if (!r.getTargetEntity().equals(getEntity())){
							openEntity(r.getTargetEntity());
						}
					}
				}
			}
					
		});
		
		
		TreeViewerColumn colType = new TreeViewerColumn(relationshipTree, SWT.DEFAULT);
		colType.getColumn().setText(Messages.EntityEditor_RelationshipColumnName);
		colType.getColumn().setWidth(150);
		colType.setLabelProvider(new RelationshipLabelProvider(0));
		
		TreeViewerColumn colRelationshipSrc = new TreeViewerColumn(relationshipTree, SWT.DEFAULT);
		colRelationshipSrc.getColumn().setText(Messages.EntityEditor_RelSourceColumnName);
		colRelationshipSrc.getColumn().setWidth(150);
		colRelationshipSrc.setLabelProvider(new RelationshipLabelProvider(1));
		
		TreeViewerColumn colRelationshipTrg = new TreeViewerColumn(relationshipTree, SWT.DEFAULT);
		colRelationshipTrg.getColumn().setText(Messages.EntityEditor_RelTargetColumnName);
		colRelationshipTrg.getColumn().setWidth(150);
		colRelationshipTrg.setLabelProvider(new RelationshipLabelProvider(2));
		
		TreeViewerColumn colSource = new TreeViewerColumn(relationshipTree, SWT.DEFAULT);
		colSource.getColumn().setText(Messages.EntityEditor_RelCreateSourceColumnName);
		colSource.getColumn().setWidth(75);
		colSource.setLabelProvider(new RelationshipLabelProvider(3));
		
		TreeViewerColumn colAttributes = new TreeViewerColumn(relationshipTree, SWT.DEFAULT);
		colAttributes.getColumn().setText(Messages.EntityEditor_RelAttributeColumnName);
		colAttributes.setLabelProvider(new RelationshipLabelProvider(4));
		relationshipTree.getTree().addPaintListener(new PaintListener() {			
			@Override
			public void paintControl(PaintEvent e) {
				relationshipTree.getTree().removePaintListener(this);
				int size = parent.getParent().computeSize(SWT.DEFAULT, SWT.DEFAULT).x - 350;
				if (size < 350) size = 350;
				colAttributes.getColumn().setWidth(size);		
			}
		});
		//create table listener for displaying entity relationship info
		new AbstractEntityEditorShellListener<Object, EntityRelationshipDetailsShell>(relationshipTree, 4) {			
			@Override
			protected EntityRelationshipDetailsShell getShellDialog(Object currentSelection) {
				if (currentSelection instanceof IntelEntityRelationship){
					IntelEntityRelationship relationship = (IntelEntityRelationship) currentSelection;
					return new EntityRelationshipDetailsShell(getSite().getShell(),relationship);
				}
				return null;
			}
		};

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

				Object x = ((IStructuredSelection)relationshipTree.getSelection()).getFirstElement();
				if (x instanceof IntelEntityRelationship){
					IntelEntityRelationship r = (IntelEntityRelationship)x;
					mnuOpen = new MenuItem(thumbMenu,SWT.DEFAULT,0);
					mnuOpen.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY));
					if (r.getSourceEntity().equals(getEntity())){
						mnuOpen.setText(MessageFormat.format(Messages.EntityEditor_OpenTargetMenuLabel,r.getTargetEntity().getIdAttributeAsText()));
						mnuOpen.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								openEntity(r.getTargetEntity());
							}
						});
					}else if (r.getTargetEntity().equals(getEntity())){
						mnuOpen.setText(MessageFormat.format(Messages.EntityEditor_OpenSourceMenuLabel,r.getSourceEntity().getIdAttributeAsText()));
						mnuOpen.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								openEntity(r.getSourceEntity());
							}
						});
					}
				}
			
				if (getEditMode()){
				
					if (mnuEdit == null){
						mnuEdit = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuEdit.setText(Messages.EntityEditor_EditAttributeMneuLabel);
						mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
						mnuEdit.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								IStructuredSelection sel = (IStructuredSelection) relationshipTree.getSelection();
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
		
		relationshipTree.getTree().setMenu(mnuRelationship.createMenu(relationshipTree.getTree()));
	}

	private boolean editRelationshipAttributes(IntelEntityRelationship relation){
		RelationshipAttributeDialog dialog = new RelationshipAttributeDialog(relationshipTree.getControl().getShell(), relation);
		if (dialog.open() == Window.OK){
			relationshipTree.refresh();
			setDirty(true);
			return true;
		}
		return false;
	}
	
	private void createRecordsPanel(Composite parent){
		
		lnkNewRecord = toolkit.createButton(parent, Messages.EntityEditor_NewRecordLabel, SWT.NONE);
		lnkNewRecord.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		lnkNewRecord.addListener(SWT.Selection, e->createNewRecord());
		lnkNewRecord.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		if (!getEditMode()) {
			((GridData)lnkNewRecord.getLayoutData()).heightHint = 0;
			lnkNewRecord.setVisible(false);
		}
		tblRecords = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		tblRecords.setContentProvider(ArrayContentProvider.getInstance());
		tblRecords.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblRecords.getTable().setHeaderVisible(true);
		tblRecords.getTable().setLinesVisible(true);
		
		String[] columns = new String[]{Messages.EntityEditor_ShortNameColumnLabel, Messages.EntityEditor_RecordDatefieldName, Messages.EntityEditor_SourceColumnLabel, Messages.EntityEditor_StatusColumnLabel};
		ColumnLabelProvider[] lbls = new ColumnLabelProvider[]{
				new RecordLabelProvider(RecordLabelProvider.RecordField.TITLE),
				new RecordLabelProvider(RecordLabelProvider.RecordField.PRIMARY_DATE),
				new RecordLabelProvider(RecordLabelProvider.RecordField.SOURCE),
				new RecordLabelProvider(RecordLabelProvider.RecordField.STATUS),
		};
		int[] width = new int[]{400, 100, 100, 100};
		tblRecords.getTable().setData(TBLRECORD_LBLPROVIDER_KEY, lbls[0]);
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
		
		//records details tooltip
		new AbstractEntityEditorShellListener<IntelRecord, RecordDetailsShell>(tblRecords) {			
			@Override
			protected RecordDetailsShell getShellDialog(IntelRecord currentSelection) {
				
				if (shellDialog == null || shellDialog.isDisposed()){
					return  new RecordDetailsShell(getSite().getShell(),currentSelection);
				}else if (!shellDialog.getRecord().equals(currentSelection)){
					shellDialog.setRecord(currentSelection);
				}
				return shellDialog;
			}
		};
		
		
		Menu recordsMenu = new Menu(tblRecords.getTable());
		tblRecords.getTable().setMenu(recordsMenu);
		
		recordsMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuHidden(MenuEvent e) {
			}

			@Override
			public void menuShown(MenuEvent e) {
				for (MenuItem mi : recordsMenu.getItems()) mi.dispose();
				if (IntelSecurityManager.INSTANCE.canViewRecords(input.getProfileUuid())) {
					MenuItem open = new MenuItem(recordsMenu, SWT.PUSH);
					open.setText(Messages.EntityEditor_OpenRecordMnuItem);
					open.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							openSelectedRecord();
						}
					});
				}
				
				if (getEditMode() && IntelSecurityManager.INSTANCE.canCreateRecord(input.getProfileUuid())) {
					new MenuItem(recordsMenu, SWT.SEPARATOR);
					
					MenuItem open = new MenuItem(recordsMenu, SWT.PUSH);
					open.setText(Messages.EntityEditor_1);
					open.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
					open.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							createNewRecord();
						}
					});
					
					
				}
			}
			
		});
		
		
	}
	
	private void createNewRecord() {
		if (!IntelSecurityManager.INSTANCE.canCreateRecord(input.getProfileUuid())) return;
		IEclipseContext ctx = context.createChild();
		ctx.set(NewRecordHandler.ENTITY_UUID_LINK, Collections.singleton(getEntity().getUuid()));
		ctx.set(NewRecordHandler.PROFILE_LINK, getEntity().getProfile());

		(new NewRecordHandler()).createNewRecord(ctx);
	}
	
	
	private void openSelectedRecord(){
		if (!IntelSecurityManager.INSTANCE.canViewRecords(input.getProfileUuid())) return;
		if (tblRecords.getSelection().isEmpty()) return;
		Object x = ((IStructuredSelection)tblRecords.getSelection()).getFirstElement();
		if (x instanceof IntelRecord){
			openRecord((IntelRecord)x);
		}
	}
	
	private void openRecord(IntelRecord record){
		(new OpenRecordHandler()).openRecord((IntelRecord) record, false);
	}
	
	private void createAttachmentPanel(Composite parent){
		
		attachmentEditPanel = toolkit.createComposite(parent);
		attachmentEditPanel.setLayout(createGridLayoutNoMargin(1));
		attachmentEditPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnAddAttachment = toolkit.createButton(attachmentEditPanel, Messages.EntityEditor_AddAttachmentBtn, SWT.PUSH);
		btnAddAttachment.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAddAttachment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {	
				FileDialog dialog = new FileDialog(getSite().getShell(), SWT.OPEN | SWT.MULTI);
				dialog.open();
				
				if (dialog.getFileNames() != null){
					for (String file : dialog.getFileNames()){
						IntelAttachment ia = new IntelAttachment();
						ia.setConservationArea(SmartDB.getCurrentConservationArea());
						ia.setCopyFromLocation(Paths.get(dialog.getFilterPath()).resolve(file));
						ia.setCreatedBy(SmartDB.getCurrentEmployee());
						ia.setDateCreated(LocalDateTime.now());
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
			private MenuItem mnuProperties;
			private MenuItem mnuSearch;
			private MenuItem mnuSep;
			
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
					mnuOpen.setText(Messages.EntityEditor_OpenAttachmentMnuItem);
					mnuOpen.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (!attachmentTable.getSelection().isEmpty()){
								AttachmentUtil.openAttachment(attachmentTable.getSelection().get(0));
							}
						}
					});
				}
				if (mnuSearch == null){
					mnuSearch = new MenuItem(thumbMenu, SWT.DEFAULT);
					mnuSearch.setText(Messages.EntityEditor_SearchAttachMnu);
					mnuSearch.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ATTACHMENT_SEARCH));
					mnuSearch.addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							FileSearchView.doSearch(context, attachmentTable.getSelection());
						}
					});
				}
				
				if (getEditMode()){
					if (mnuDelete == null){
						mnuSep = new MenuItem(thumbMenu, SWT.SEPARATOR);
						mnuDelete = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
						mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
						mnuDelete.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								for (IntelAttachment toDelete : attachmentTable.getSelection()){
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
								}
								refreshAttachmentTable();
								setDirty(true);
							}	
						});
					}
					if (mnuPrimary == null){
						mnuPrimary = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuPrimary.setText(Messages.EntityEditor_SetPrimaryImageMenu);
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
					if (mnuSep != null){
						mnuSep.dispose();
						mnuSep = null;
					}
					if (mnuPrimary != null){
						mnuPrimary.dispose();
						mnuPrimary = null;
					}
				}
				
				
				if (mnuProperties == null){
					new MenuItem(thumbMenu, SWT.SEPARATOR);
					mnuProperties = new MenuItem(thumbMenu, SWT.DEFAULT);
					mnuProperties.setText(Messages.EntityEditor_PropertiesMnuItem);
					mnuProperties.addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (!attachmentTable.getSelection().isEmpty()){
								IntelAttachmentPropertiesDialog dialog = new IntelAttachmentPropertiesDialog(getSite().getShell(), attachmentTable.getSelection().get(0));
								dialog.open();
							}
						}
					});
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
	
	public void updatePositionAttribute(IntelAttribute ia, Double x, Double y){
		IntelEntityAttributeValue tmp = new IntelEntityAttributeValue();
		tmp.setNumberValue(x);
		tmp.setNumberValue2(y);
		for (AttributeFieldEditor e : fieldEditors){
			if (e.getAttribute().equals(ia)){
				e.initControl(tmp);
				setDirty(true);
				return;
			}
		}
	}
	
	private synchronized void initControl(){
		IntelEntity entity = getEntity();
		
		if (entity.getEntityType().getIcon() != null){
			super.setTitleImage( Resources.INSTANCE.getImage(entity.getEntityType()));
		}
		
		fieldEditors = new ArrayList<AttributeFieldEditor>();
		if (lblType.isDisposed()) return;
		 
		lblCreated.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(entity.getDateCreated()));
		lblModified.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(entity.getDateModified()));
		
		lblIdentifier.setText(entity.getIdAttributeAsText());
		
		lblType.setText(entity.getEntityType().getName());
		lblType.setToolTipText(entity.getEntityType().getName());
		lblTypeImage.setImage(Resources.INSTANCE.getImage(entity.getEntityType()));
		
		lblProfile.setText(entity.getProfile().getName());
		lblProfileColor.setImage(Resources.INSTANCE.getImage(entity.getProfile()));
		
		lblProfile.getParent().getParent().layout();
		lblType.getParent().getParent().layout();
				
		Listener[] ls = txtScratchpad.getListeners(SWT.Modify);
		for (Listener l : ls) txtScratchpad.removeListener(SWT.Modify, l);
		if (entity.getComment() == null){ 
			txtScratchpad.setText(""); //$NON-NLS-1$
		}else{
			txtScratchpad.setText(entity.getComment());
		}
		txtScratchpad.setEditable(getEditMode());
		for (Listener l : ls) txtScratchpad.addListener(SWT.Modify, l);
		
		if (entity.getPrimaryAttachment() != null){
			if (Files.exists(entity.getPrimaryAttachment().getAttachmentFile())){
				Thumbnail thum = new Thumbnail(entity.getPrimaryAttachment(), THUMB_SIZE);
				imgMain = thum.getImage();
				lblMainImage.redraw();
			}
		}
			
		//attribute composite
		for (Control kid : compAttributes.getChildren()){
			kid.dispose();
		}
		
		//groups
		List<String> groupHeaders = new ArrayList<String>();
		for (IntelEntityTypeAttributeGroup g : groups){
			groupHeaders.add(g.getName());
		}
		//only add other group if an attribute belongs in it
		for (IntelEntityTypeAttribute a : entity.getEntityType().getAttributes()){
			if (a.getAttributeGroup() == null){
				groupHeaders.add(OtherAttributeGroup.INSTANCE.getName());
				break;
			}
		}
		
		if (entity.getEntityType().getDmAttribute() != null) {
			groupHeaders.add(DATA_MODEL_ATTRIBUTE_GROUP);
		}
		
		Composite outer = toolkit.createComposite(compAttributes, SWT.NONE);
		outer.setLayout(new GridLayout());
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight = 0;
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SectionTabHeader tabList = new SectionTabHeader(groupHeaders.toArray(new String[groupHeaders.size()]), outer, toolkit);
		tabList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite tabPart = toolkit.createComposite(outer, SWT.NONE);
		tabPart.setLayout(new StackLayout());
		tabPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		Composite[] parts = new Composite[groupHeaders.size()];
		int counter = 0;
		
		for (int i = 0; i < groupHeaders.size(); i ++){
			IntelEntityTypeAttributeGroup group = null;
			if (i < groups.size()){
				group = groups.get(i);
			}
			
			if (groupHeaders.get(i) == DATA_MODEL_ATTRIBUTE_GROUP) {
				Composite part = toolkit.createComposite(tabPart);
				part.setLayout(new GridLayout(2, false));
				part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				toolkit.createLabel(part, Messages.EntityEditor_DmAttribute);
				
				Text txt = toolkit.createText(part, ""); //$NON-NLS-1$
				txt.setEditable(false);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				if (entity.getDmAttributeListItem() != null) {
					txt.setText( MessageFormat.format("{0} [{1}]",entity.getEntityType().getDmAttribute().getName(), entity.getEntityType().getDmAttribute().getKeyId() )); //$NON-NLS-1$
				}else {
					txt.setText(ERROR_LINK_NOT_FOUND);
				}
				
				toolkit.createLabel(part, Messages.EntityEditor_ListItemAtt);
				
				txtDmListItem = toolkit.createText(part, ""); //$NON-NLS-1$
				txtDmListItem.setEditable(false);
				txtDmListItem.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				if (entity.getDmAttributeListItem() != null) {
					txtDmListItem.setText(MessageFormat.format("{0} [{1}]", entity.getDmAttributeListItem().getName(), entity.getDmAttributeListItem().getKeyId())); //$NON-NLS-1$
				}else {
					txtDmListItem.setText(ERROR_LINK_NOT_FOUND);
				}
				
				toolkit.createLabel(part, Messages.EntityEditor_ActiveLabel);
				
				txtDmActive = toolkit.createText(part, ""); //$NON-NLS-1$
				txtDmActive.setEditable(false);
				txtDmActive.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				if (entity.getDmAttributeListItem() != null) {
					txtDmActive.setText( entity.getDmAttributeListItem().getIsActive() ? SmartLabelProvider.BOOLEAN_TRUE_LABEL : SmartLabelProvider.BOOLEAN_FALSE_LABEL );
				}else {
					txtDmActive.setText(ERROR_LINK_NOT_FOUND);
				}
				
				parts[counter++] = part;

				continue;
			}
			ScrolledForm attributelist = toolkit.createScrolledForm(tabPart);
			attributelist.getBody().setLayout(new GridLayout());
			attributelist.setExpandHorizontal(true);
			attributelist.setExpandVertical(true);
			attributelist.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			parts[counter++] = attributelist;
		
			Composite part = toolkit.createComposite(attributelist.getBody(), SWT.NONE);
			part.setLayout(createGridLayoutNoMargin(2));
			part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			part.setVisible(false);
			((GridLayout)part.getLayout()).horizontalSpacing = 7; 
			for (IntelEntityTypeAttribute a : entity.getEntityType().getAttributes()){
				if (group == null){
					if (a.getAttributeGroup() != null) continue;
				}else{
					if (!group.equals(a.getAttributeGroup())) continue;
				}
				if (getEditMode()){
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
					if (a.getAttribute().equals(entity.getEntityType().getIdAttribute())){
						addDuplicateIdChecker(e);
					}
					if (e.getAttribute().getType() == IntelAttribute.AttributeType.POSITION){
						//modify position attributes we need to update map
						e.addSelectionListener(new SelectionAdapter() {	
							@Override
							public void widgetSelected(SelectionEvent event) {
								IntelEntityAttributeValue tmp = new IntelEntityAttributeValue();
								tmp.setAttribute(e.getAttribute());
								e.updateValue(tmp);						
								mapPart.refreshLayerValue(tmp);
							}
						});
					}
					fieldEditors.add(e);
					
//					if (e.getTextAttributeControl() != null) {
//						e.getTextAttributeControl().addListener(SWT.Resize, ex->{
//							attributelist.reflow(true);
//						});
//					}
				}else{
					Label key = toolkit.createLabel(part, a.getAttribute().getName() + ":"); //$NON-NLS-1$
					key.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
					
					String text = ""; //$NON-NLS-1$
					for (IntelEntityAttributeValue v : entity.getAttributes()){
						if (v.getAttribute().equals(a.getAttribute())){
							text = attributeLabelProvider.getText(v);
							break;
							
						}
					}
					Text tmp = toolkit.createText(part, text, SWT.BORDER);
					tmp.setEditable(false);
					tmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}
				
			}
//			attributelist.reflow(true);
			attributelist.layout(true);
			part.setVisible(true);
		}
		
		tabList.setContent(parts, tabPart);
		tabList.selectTab(0);
		
		compAttributes.layout(true);
		
		//attachment composite
		if (getEditMode()){
			attachmentEditPanel.setVisible(true);
			((GridData)attachmentEditPanel.getLayoutData()).heightHint = attachmentEditPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		}else{
			attachmentEditPanel.setVisible(false);
			((GridData)attachmentEditPanel.getLayoutData()).heightHint = 0;
		}
		
		if (getEditMode() || hasHiddenRelationships) {
			relationshipEditPanel.setVisible(true);
			((GridData)relationshipEditPanel.getLayoutData()).heightHint = relationshipEditPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
			
			relationshipWarningPanel.setVisible(hasHiddenRelationships);
			relationshipButtonEditPanel.setVisible(getEditMode());
			
		}else {
			relationshipEditPanel.setVisible(false);
			((GridData)relationshipEditPanel.getLayoutData()).heightHint = 0;
		}
		attachmentEditPanel.getParent().layout(true);
		relationshipEditPanel.getParent().layout(true);
		
		refreshAttachmentTable();
		
		relationshipTree.setInput(relationships);
		relationshipTree.refresh();
		relationshipTree.expandAll();
		
		graphComposite.setInput(entity);
		
		mapPart.refresh();
		loadRecords.schedule(0);
	}
	
	private void addDuplicateIdChecker(AttributeFieldEditor editor){
		//check for duplicate identifiers
		editor.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				try(Session session = HibernateManager.openSession()){
					IntelEntityType etype = (IntelEntityType)session.get(IntelEntityType.class, entity.getEntityType().getUuid());
					IntelEntityAttributeValue tmp = new IntelEntityAttributeValue();
					tmp.setAttribute(etype.getIdAttribute());
					editor.updateValue(tmp);
					if (EntityManager.INSTANCE.isDuplicateId(tmp.getAttributeValue(), etype, SmartDB.getCurrentConservationArea(), session, entity.getUuid())){
						String warnMessage = Messages.EntityEditor_DuplciateIdWarning; 
						editor.setWarningMessage(warnMessage);
					}else{
						editor.setWarningMessage(null);
					}
				}
			}
		});
		
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
		private RelationshipTypeLabelProvider ll = null;
		private RelationshipGroupLabelProvider lg = null;
		private AttributeValueLabelProvider la = null;
		
		public RelationshipLabelProvider(int columnIndex){
			this.columnIndex = columnIndex;
			if (columnIndex == 0){
				ll = new RelationshipTypeLabelProvider();
				lg = new RelationshipGroupLabelProvider();
			}else if (columnIndex == 4){
				la = new AttributeValueLabelProvider();
			}
		}
		
		@Override
		public void dispose(){
			super.dispose();
			if (ll != null) ll.dispose();
			if (lg != null) lg.dispose();
			if (la != null) la.dispose();
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
				IntelEntityRelationship r = (IntelEntityRelationship) element;
				if (r.getSourceObject() == null){
					switch(r.getSource()){
					case ENTITY:
						return Messages.EntityEditor_EntitySrcLabel;
					case RECORD: 
						return Messages.EntityEditor_RecordSrcLabel;
					}
				}else{
					switch(r.getSource()){
					case ENTITY:
						return MessageFormat.format(Messages.EntityEditor_EntitySrcLabel2, ((IntelEntity)r.getSourceObject()).getIdAttributeAsText());
					case RECORD: 
						return MessageFormat.format(Messages.EntityEditor_RecordSrcLabel2, ((IntelRecord)r.getSourceObject()).getTitle());
					}
				}
			}
		}else if (columnIndex == 4){
			if (element instanceof IntelEntityRelationship){
				
				StringBuilder sb = new StringBuilder();
				IntelEntityRelationship relation = (IntelEntityRelationship)element;
				if (relation.getAttributes() == null) return ""; //$NON-NLS-1$
				
				for (IntelRelationshipTypeAttribute attribute: relation.getRelationshipType().getAttributes()){
					for (IntelEntityRelationshipAttributeValue value : relation.getAttributes()){
						if (value.getAttribute().equals(attribute.getAttribute())){
							sb.append(value.getAttribute().getName());
							sb.append(": "); //$NON-NLS-1$
							sb.append(la.getText(value));
							sb.append(" / "); //$NON-NLS-1$
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
		return ""; //$NON-NLS-1$
		}
	}

	@Override
	public Map getMap() {
		if (mapPart == null) return null;
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
