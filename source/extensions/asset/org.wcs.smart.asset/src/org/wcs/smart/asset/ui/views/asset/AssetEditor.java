/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.asset;

import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetCoreLabelProvider;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.Asset.Status;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetHistoryRecord;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.ui.AttributeFieldEditor;
import org.wcs.smart.asset.ui.CommentDialog;
import org.wcs.smart.asset.ui.DateCommentDialog;
import org.wcs.smart.asset.ui.IdFieldHeader;
import org.wcs.smart.asset.ui.SectionHeader;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Asset Editor
 * 
 * @author Emily
 *
 */
public class AssetEditor extends EditorPart implements MapPart {

	public static final String ID = "org.wcs.smart.asset.ui.views.asset"; //$NON-NLS-1$
	
	private Asset asset;
	
	private Composite currentPanel;
	private Composite detailsPanel;
	private Composite deploymentPanel;
	private Composite eventsPanel;
	
	private boolean isDirty;
	
	private IEclipseContext parentContext;
	private List<EventHandler> handlers = null;
	
	private FormToolkit toolkit;
	private Form pageForm;

	private Label lblRetiredState = null;
	private Hyperlink changeRetiredState = null;
	
	private Label lblStatus;
	private Label lblStatusImage;
	
	private IdFieldHeader lblId;
	private Label lblAssetTypeImage;
	private Label lblAssetType;
	private List<AttributeFieldEditor> attributeEditors = null;
	
	private TableViewer tblEvents;
	
	private List<AssetHistoryRecord> activeHistoryRecords;
	private List<AssetHistoryRecord> toDeleteHistoryRecords;

	private AssetDeploymentPage deploymentPage;
	private AssetCurrentPage currentPage;
	private Composite sectionBody;
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		boolean isNew = asset.getUuid() == null;
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			try {
				String query =  "SELECT count(*) FROM Asset WHERE LOWER(id) = :id AND conservationArea = :ca ";
				if (!isNew) {
					query += " AND uuid != :uuid";
				}
				Query<?> q = s.createQuery(query)
				.setParameter("id", asset.getId().toLowerCase())
				.setParameter("ca", asset.getConservationArea());
				if (!isNew) {
					q.setParameter("uuid", asset.getUuid());
				}
				Long cnt = (Long) q.uniqueResult();
				if (cnt > 0) {
					MessageDialog.openError(getSite().getShell(), "Save Asset", 
						MessageFormat.format("The id ''{0}'' is already used by another asset in the system. You cannot duplicate Asset IDs.  Change the asset id and try again.", asset.getId())
							);
					return;
				}
				
				s.beginTransaction();
				s.saveOrUpdate(asset);
				
				if (activeHistoryRecords != null) activeHistoryRecords.forEach(r->s.saveOrUpdate(r));
				if (toDeleteHistoryRecords != null) toDeleteHistoryRecords.forEach(r->s.delete(r));
				
				if (deploymentPage != null) {
					deploymentPage.getModifiedDeployments().forEach(r->s.saveOrUpdate(r));
				
					s.flush();
					for (AssetDeployment deploy : deploymentPage.getDeletedDeployments()) {
						//delete deployment & all associated waypoints/observations
						AssetDeployment d = s.get(AssetDeployment.class, deploy.getUuid());
						List<AssetWaypoint> dd = new ArrayList<>(d.getAssetWaypoints());
						d.getAssetWaypoints().clear();
						for (AssetWaypoint aw : dd) {
							Waypoint w = aw.getWaypoint();
							s.delete(aw);
							s.delete(w);
						}
						s.flush();
						s.delete(d);
					}
				}
				
				s.getTransaction().commit();
				
				((AssetEditorInput)getEditorInput()).setAssetUuid(asset.getUuid());
				setDirty(false);
				
				toDeleteHistoryRecords.clear();
				if (deploymentPage != null) {
					deploymentPage.getDeletedDeployments().clear();
					deploymentPage.getModifiedDeployments().clear();
				}
			}catch (Exception ex) {
				s.getTransaction().rollback();
				AssetPlugIn.displayLog(
						MessageFormat.format("Unable to save changes to asset: {0}. {1}", asset.getId(), ex.getMessage()), ex);
				return;
			}
		}
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put(UIEvents.EventTags.ELEMENT, parentContext.get(MPart.class));
		data.put(IEventBroker.DATA, Collections.singletonList(asset));
		parentContext.get(IEventBroker.class).post(isNew? AssetEvents.ASSET_NEW : AssetEvents.ASSET_MODIFIED, data);	
		
		refreshStatus();
		if (deploymentPage != null) deploymentPage.refreshSummaryStatistics();
	}

	@Override
	public void doSaveAs() {
		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.setInput(input);
		super.setSite(site);
		parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
	}
	
	private void subscribeToEvent(String eventTopic, EventHandler handler){
		parentContext.get(IEventBroker.class).subscribe(eventTopic, handler);
		handlers.add(handler);
	}
	
	private void refreshStatus() {
		lblStatus.setText(asset.getStatus(true).getGuiName(Locale.getDefault()));
		lblStatusImage.setImage(AssetCoreLabelProvider.getStatusImage(asset));
		lblStatus.getParent().layout(true);
		
		initializeAttributePanel(asset);
		
		if (currentPage != null) {
			AssetDeployment activeDeployment = null;
			if (asset.getUuid() != null) {
				try(Session session = HibernateManager.openSession()){
					activeDeployment = QueryFactory.buildQuery(session, AssetDeployment.class, 
						new Object[] {"asset.uuid", asset.getUuid()},
						new Object[] {"endDate", null}).uniqueResult();
				}
			}
			currentPage.initializePanel(activeDeployment);
		}
	}
	
	private void initData() {
		AssetEditorInput in = (AssetEditorInput) super.getEditorInput();
		
		activeHistoryRecords = new ArrayList<>();
		toDeleteHistoryRecords = new ArrayList<>();
		
		try(Session session = HibernateManager.openSession()){
			if (in.getAssetUuid() == null) {
				if (in.getAssetTypeUuid() != null) {
					AssetType type = session.get(AssetType.class, in.getAssetTypeUuid());
					asset = new Asset();
					asset.setConservationArea(SmartDB.getCurrentConservationArea());
					asset.setAssetType(type);
					asset.setAttributeValues(new ArrayList<>());
					asset.setIsRetired(false);
					asset.setId("AssetId");
					setDirty(true);
				}
			}else {
				asset = session.get(Asset.class, in.getAssetUuid());
			}
			
			if (asset == null) {
				throw new Exception("Asset not found; could not initialize element controls");
			}
			if (asset.getAttributeValues() == null) {
				asset.setAttributeValues(new ArrayList<>());
			}else {
				asset.getAttributeValues().forEach(a->{
					if (a.getAttributeListItem() != null) a.getAttributeListItem().getName();
				});
			}
			asset.getAssetType().getAssetAttributes().forEach(a->{
				a.getAttribute().getName();
				if (a.getAttribute().getAttributeList() != null) {
					a.getAttribute().getAttributeList().forEach(l->l.getName());
				}
			});
			
			lblAssetType.setText(asset.getAssetType().getName());
			Image img = AWTSWTImageUtils.convertToSWTImage(asset.getAssetType().getIconAsImage());
			lblAssetTypeImage.setImage(img);
			lblAssetTypeImage.addListener(SWT.Dispose, e->img.dispose());
			lblId.setText(asset.getId());
			
			setPartName(asset.getId());
			setTitleImage(img);
			
			initializeAttributePanel(asset);
			initializeEventsPanel(asset);
			if (deploymentPage != null) deploymentPage.initializePanel(asset); 
			
			AssetDeployment activeDeployment = null;
			if (asset.getUuid() != null) {
				activeDeployment = QueryFactory.buildQuery(session, AssetDeployment.class, 
					new Object[] {"asset", asset},
					new Object[] {"endDate", null}).uniqueResult();
			}
			//updating the currentPAge is covered by the refreshStatus
			refreshStatus();
			
		}catch (Exception ex) {
			AssetPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	public void setDirty(boolean isDirty) {
		boolean fire = isDirty != this.isDirty;
		this.isDirty = isDirty;
		if (fire) firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private void forceCloseEditor(){
		getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(AssetEditor.this, false);
	}
	
	private void createEventHandlers() {
		//on delete close editor
		handlers = new ArrayList<>();
		
		subscribeToEvent(AssetEvents.ASSET_DELETE, (event)->{
			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				Collection<Asset> items = (Collection<Asset>)data;
				for (Asset a : items){
					if (a.equals(asset)) forceCloseEditor();
				}
			}
		});
		
		subscribeToEvent(AssetEvents.ASSET_MODIFIED, (event)->{
			if (parentContext.get(MPart.class) == event.getProperty(UIEvents.EventTags.ELEMENT)) return;

			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				boolean validate = false;
				Collection<Asset> items = (Collection<Asset>)data;
				for (Asset a : items){
					if (a.equals(asset)) {
						validate = true;
						break;
					}
				}
				
				if (validate) {
					if (isDirty) {
						if (!MessageDialog.openQuestion(getSite().getShell(), "Asset Modified", 
								"This asset was modified by another part of the system.  Do you want to reload the page and loose any local changes?  By not reloading your risk overwriting other changes made outside this page." )) {
							return;
						}
					}
					//reload
					initData();
				}
			}
		});
	}
	
	/**
	 * Gets the asset; may return null if asset not yet loaded
	 * @return
	 */
	public Asset getAsset() {
		return this.asset;
	}
		
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth= 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).verticalSpacing= 0;
		
		pageForm = toolkit.createForm(parent);
		pageForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite body = pageForm.getBody();
		body.setLayout(new GridLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite headerComp = toolkit.createComposite(body);
		headerComp.setLayout(new GridLayout(6, false));
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerComp.getLayout()).marginWidth = 0;
		((GridLayout)headerComp.getLayout()).marginHeight = 0;
		lblAssetTypeImage = toolkit.createLabel(headerComp,"");
		
		lblId = new IdFieldHeader(headerComp, toolkit, pageForm.getFont(), pageForm.getForeground());
		lblId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		FontData fd = lblId.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font headerFont = new Font(parent.getShell().getDisplay(), fd);
		lblId.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				headerFont.dispose();	
			}
		});
		lblId.setFont(headerFont);
		lblId.addListener(SWT.Selection, e->{
			String text = e.text.trim();
			if (text.isEmpty() || text.length() > Asset.ID_MAX_LENGTH) {
				MessageDialog.openWarning(getSite().getShell(), "Asset ID", MessageFormat.format("Invalid asset id.  ID must be between {0} and {1} charaters", 1, Asset.ID_MAX_LENGTH));
				lblId.setText(asset.getId());
				return;
			}
			asset.setId(e.text);
			setPartName(e.text);
			setDirty(true);
		});

		lblStatusImage = toolkit.createLabel(headerComp, "");
		lblStatus = toolkit.createLabel(headerComp, "");
		toolkit.createLabel(headerComp, "-");
		lblAssetType = toolkit.createLabel(headerComp, "");
				
		
		String headers[] = new String[] {"Current Status", "Properties", "Deployments", "History"};
		Listener[] actions = new Listener[] {
			event->{
				if (currentPanel == null) currentPanel = createCurrentSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = currentPanel;
				sectionBody.layout(true);},
			event->{
				if (detailsPanel == null) detailsPanel = createDetailsSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = detailsPanel;
				sectionBody.layout(true);},
			event->{
				if (deploymentPanel == null) deploymentPanel = createDeploymentsSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = deploymentPanel;
				sectionBody.layout(true);},
			event->{
				if (eventsPanel == null) eventsPanel = createHistorySection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = eventsPanel;
				sectionBody.layout(true);},
				
		};
		
		SectionHeader headerSection = new SectionHeader(body, SWT.NONE, headers, actions, toolkit);
		headerSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		sectionBody = toolkit.createComposite(body);
		sectionBody.setLayout(new StackLayout());
		sectionBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//create initial panel
		headerSection.selectPanel(0);
		
		createEventHandlers();
		initData();
	}
	
	private Composite createDeploymentsSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		deploymentPage = new AssetDeploymentPage(this);
		ContextInjectionFactory.inject(deploymentPage, parentContext);
		deploymentPage.createDeploymentsSection(panel,  toolkit);
		
		return panel;
		
	}
	private Composite createCurrentSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		currentPage = new AssetCurrentPage(this);
		ContextInjectionFactory.inject(currentPage, parentContext);
		currentPage.createSummarySection(panel, toolkit);
		
		return panel;
	}

	private Composite createDetailsSection(Composite parent) {
		
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		Composite toppanel = toolkit.createComposite(panel, SWT.BORDER);
		toppanel.setLayout(new GridLayout(3, false));
		toppanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = toolkit.createLabel(toppanel, "State: ");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		lblRetiredState = toolkit.createLabel(toppanel, "");
		lblRetiredState.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		changeRetiredState = toolkit.createHyperlink(toppanel, "", SWT.NONE);
		changeRetiredState.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		changeRetiredState.addHyperlinkListener(new HyperlinkAdapter() {			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (asset == null) return;
				
				String action;
				String msg;
				if (!asset.getIsRetired()) {
					action = "Asset Retired - ";
					msg = "Enter a comment related to the retirement";
				}else {
					action = "Asset Unretired - ";
					msg = "Enter a comment related to unretirement of asset";
				}
				CommentDialog dialog = new CommentDialog(getSite().getShell(), "Asset History Comment", msg);
				
				if (dialog.open() != CommentDialog.OK) return;
				
				AssetHistoryRecord historyRecord = new AssetHistoryRecord();
				historyRecord.setAsset(asset);
				historyRecord.setComment(action + dialog.getComment());
				historyRecord.setDate(new Date());
				activeHistoryRecords.add(historyRecord);
				asset.setIsRetired(!asset.getIsRetired());
				refreshStatus();
				setDirty(true);
			}
		});
		
		Composite attributeComp = toolkit.createComposite(panel, SWT.BORDER);
		attributeComp.setLayout(new GridLayout());
		attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		l = toolkit.createLabel(attributeComp, "Attributes");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.setFont(boldFont);
		l.addListener(SWT.Dispose,  e-> boldFont.dispose());
		
		ScrolledComposite attributes = new ScrolledComposite(attributeComp,  SWT.V_SCROLL);
		attributes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributes.setExpandHorizontal(true);
		attributes.setExpandVertical(true);
		
		toolkit.adapt(attributes);
		Composite attributePanel = toolkit.createComposite(attributes);
		attributes.setContent(attributePanel);
		attributePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		attributePanel.setLayout(new GridLayout(2, false));
		
		
		
		attributeEditors = new ArrayList<>();
		for (AssetTypeAttribute attribute : asset.getAssetType().getAssetAttributes()) {
			AttributeFieldEditor editor = new AttributeFieldEditor(attributePanel, attribute.getAttribute());
			editor.adapt(toolkit);
			attributeEditors.add(editor);
			if (editor.getTextAttributeControl() != null) {
				editor.getTextAttributeControl().addListener(SWT.Resize, e-> attributes.setMinSize(attributePanel.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
			}
			editor.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (!editor.isValid()) return;
					AssetAttributeValue toUpdate = null;
					for (AssetAttributeValue v : asset.getAttributeValues()) {
						if (v.getAttribute().equals(editor.getAttribute())) {
							toUpdate = v;
							break;
						}
					}
					boolean isNew = false;
					if (toUpdate == null) {
						isNew = true;
						toUpdate = new AssetAttributeValue();
						toUpdate.setAsset(asset);
						toUpdate.setAttribute(editor.getAttribute());
					}
					if (editor.updateValue(toUpdate)) {
						if (isNew) asset.getAttributeValues().add(toUpdate);
					}else {
						if (!isNew) asset.getAttributeValues().remove(toUpdate);
					}
					setDirty(true);
					
				}
			});
		}
		attributes.setMinSize(attributePanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		initializeAttributePanel(asset);
		return panel;
	}
	
	
	private void initializeAttributePanel(Asset asset) {
		if (lblRetiredState != null) {
			lblRetiredState.setText(asset.getStatus().getGuiName(Locale.getDefault()));
		}
		if (changeRetiredState != null) {
			if (asset.getIsRetired()) {
				changeRetiredState.setText("unretire asset");
			}else {
				changeRetiredState.setText("retire asset");
			}
			changeRetiredState.getParent().layout(true);
		}
		if (attributeEditors != null) {
			attributeEditors.forEach(field ->{
				field.setEnabled(asset.getStatus() != Status.RETIRED);
				for (AssetAttributeValue v : asset.getAttributeValues()) {
					if (v.getAttribute().equals(field.getAttribute())) {
						field.enableChangeListeners(false);
						try {
							field.initControl(v);
						}finally {
							field.enableChangeListeners(true);
						}
						break;
					}
				}
			});
		}
	}
	
	
	
	
	
	private Composite createHistorySection(Composite parent) {
		
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		ToolBar historyToolbar = new ToolBar(panel, SWT.FLAT | SWT.HORIZONTAL);
		historyToolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		ToolItem deleteItem = new ToolItem(historyToolbar,SWT.PUSH);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.setToolTipText("delete selected history records");
		deleteItem.addListener(SWT.Selection, e->deleteHistoryRecords());
		deleteItem.setEnabled(false);
		
		ToolItem editItem = new ToolItem(historyToolbar,SWT.PUSH);
		editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editItem.setToolTipText("edit selected history record");
		editItem.addListener(SWT.Selection, e->editHistoryRecord());
		editItem.setEnabled(false);

		ToolItem addItem = new ToolItem(historyToolbar,SWT.PUSH);
		addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addItem.setToolTipText("create a new history record");
		addItem.addListener(SWT.Selection, e->addHistoryRecord());
		
		tblEvents = new TableViewer(panel, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblEvents.setContentProvider(ArrayContentProvider.getInstance());
		tblEvents.setInput(new String[] {DialogConstants.LOADING_TEXT});
		tblEvents.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblEvents.getTable().setHeaderVisible(true);
		tblEvents.getTable().setLinesVisible(true);
		
		TableViewerColumn col = new TableViewerColumn(tblEvents, SWT.NONE);
		col.getColumn().setText("Date");
		col.getColumn().setWidth(150);
		col.getColumn().setResizable(true);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetHistoryRecord) return DateFormat.getDateTimeInstance().format(((AssetHistoryRecord) element).getDate());
				return super.getText(element);
			}
		});
		col.getColumn().addListener(SWT.Selection, e->{
			if (col.getColumn().equals(tblEvents.getTable().getSortColumn())) {
				int dir = tblEvents.getTable().getSortDirection();
				tblEvents.getTable().setSortDirection(dir == SWT.UP ? SWT.DOWN : SWT.UP);
			}else {
				tblEvents.getTable().setSortColumn(col.getColumn());
			}
			tblEvents.refresh();
		});
		
		TableViewerColumn col2 = new TableViewerColumn(tblEvents, SWT.NONE);
		col2.getColumn().setText("Comment");
		col2.getColumn().setWidth(150);
		col2.getColumn().setResizable(true);
		col2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetHistoryRecord) return ((AssetHistoryRecord) element).getComment();
				return super.getText(element);
			}
		});
		col2.getColumn().setWidth(tblEvents.getControl().getSize().x - col.getColumn().getWidth());
		col2.getColumn().addListener(SWT.Selection, e->{
			if (col2.getColumn().equals(tblEvents.getTable().getSortColumn())) {
				int dir = tblEvents.getTable().getSortDirection();
				tblEvents.getTable().setSortDirection(dir == SWT.UP ? SWT.DOWN : SWT.UP);
			}else {
				tblEvents.getTable().setSortColumn(col2.getColumn());
			}
			tblEvents.refresh();
		});
		
		tblEvents.getControl().addListener(SWT.Resize, e->col2.getColumn().setWidth(tblEvents.getControl().getSize().x - col.getColumn().getWidth()));
		
		tblEvents.getTable().setSortDirection(SWT.DOWN);
		tblEvents.getTable().setSortColumn(col.getColumn());
		tblEvents.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof AssetHistoryRecord && e2 instanceof AssetHistoryRecord) {
					if (tblEvents.getTable().getSortColumn() == col.getColumn()) {
						return (tblEvents.getTable().getSortDirection() == SWT.UP ? 1 : -1) * ((AssetHistoryRecord)e1).getDate().compareTo(((AssetHistoryRecord)e2).getDate());
					}else if (tblEvents.getTable().getSortColumn() == col2.getColumn()){
						return (tblEvents.getTable().getSortDirection() == SWT.UP ? 1 : -1) * Collator.getInstance().compare( ((AssetHistoryRecord)e1).getComment(), ((AssetHistoryRecord)e2).getComment());
					}
				}
				return super.compare(viewer, e1, e2);
			}
		});
		Menu mnu = new Menu(tblEvents.getControl());
		
		MenuItem mnuAdd = new MenuItem(mnu, SWT.PUSH);
		mnuAdd.setText("New ...");
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.addListener(SWT.Selection, e->addHistoryRecord());
		
		MenuItem mnuEdit = new MenuItem(mnu, SWT.PUSH);
		mnuEdit.setText("Edit ...");
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.addListener(SWT.Selection, e->editHistoryRecord());
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText("Delete ...");
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->deleteHistoryRecords());
		
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mnuDelete.setEnabled(!tblEvents.getSelection().isEmpty());
				mnuEdit.setEnabled(!tblEvents.getSelection().isEmpty());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		tblEvents.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				deleteItem.setEnabled(!tblEvents.getSelection().isEmpty());
				editItem.setEnabled(!tblEvents.getSelection().isEmpty());
			}
		});
		tblEvents.getControl().setMenu(mnu);
		
		
		initializeEventsPanel(asset);
		return panel;
	}
	
	
	private void addHistoryRecord() {
		DateCommentDialog dialog = new DateCommentDialog(getSite().getShell(), "New Asset History Record",
				"Enter the details for the new asset history record");
		if (dialog.open() != CommentDialog.OK) return;
		
		AssetHistoryRecord record = new AssetHistoryRecord();
		record.setDate(dialog.getSelectedDateTime());
		record.setAsset(asset);
		record.setComment(dialog.getComment());
		activeHistoryRecords.add(record);
		setDirty(true);
		tblEvents.refresh();	
	}
	
	private void editHistoryRecord() {
		Object x = ((IStructuredSelection)tblEvents.getSelection()).getFirstElement();
		if (!(x instanceof AssetHistoryRecord)) return;
		AssetHistoryRecord toEdit = (AssetHistoryRecord)x;
		
		DateCommentDialog dialog = new DateCommentDialog(getSite().getShell(), "New Asset History Record",
				"Enter the details for the new asset history record");
		dialog.setValues(toEdit.getDate(), toEdit.getComment());
		
		if (dialog.open() != CommentDialog.OK) return;
		
		toEdit.setDate(dialog.getSelectedDateTime());
		toEdit.setComment(dialog.getComment());
		
		setDirty(true);
		tblEvents.refresh();	
	}
	
	private void deleteHistoryRecords() {
		if (tblEvents == null) return;
		List<AssetHistoryRecord> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblEvents.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof AssetHistoryRecord) {
				toDelete.add((AssetHistoryRecord)x);			
			}
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(getSite().getShell(), "Delete Records", 
				MessageFormat.format("Are you sure you want to delete the {0} selected asset history records?", toDelete.size()))){
			return;
		}
		
		toDelete.forEach(x->{
			activeHistoryRecords.remove(x);
			toDeleteHistoryRecords.add((AssetHistoryRecord) x);
		});
		setDirty(true);
		tblEvents.refresh();
	}
	
	private void initializeEventsPanel(Asset asset) {
		if (tblEvents == null) return;
		Job j = new Job("load history records") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (asset.getUuid() != null) {
					try(Session s = HibernateManager.openSession()){
						activeHistoryRecords.addAll(
								QueryFactory.buildQuery(s, AssetHistoryRecord.class, "asset", asset).list()); //$NON-NLS-1$
					}
				}
				Display.getDefault().syncExec(()->{
					tblEvents.setInput(activeHistoryRecords);
				});
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
	}
	
	
	@Override
	public void setFocus() {
		pageForm.setFocus();
	}
	
	@Override
	public void dispose(){
		IEventBroker event = parentContext.get(IEventBroker.class);
		if (handlers != null){
			handlers.forEach((h)->event.unsubscribe(h));
		}
		super.dispose();
	}


	@Override
	public org.locationtech.udig.project.internal.Map getMap() {
		if (currentPage == null || currentPage.getMapViewer() == null) return ApplicationGIS.NO_MAP;
		return currentPage.getMapViewer().getMap();
	}

	@Override
	public void openContextMenu() {
		if (currentPage == null) return;
		currentPage.getMapViewer().openContextMenu();
	}

	@Override
	public void setFont(Control textArea) {
		if (currentPage == null) return;
		currentPage.getMapViewer().setFont(textArea);
	}

	@Override
	public void setSelectionProvider(IMapEditorSelectionProvider selectionProvider) {
		if (currentPage == null) return;
		currentPage.getMapViewer().setSelectionProvider(selectionProvider);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return ((IEditorSite)getSite()).getActionBars().getStatusLineManager();
	}
}
