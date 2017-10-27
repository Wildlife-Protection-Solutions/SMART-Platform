package org.wcs.smart.asset.ui.views.asset;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetCoreLabelProvider;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.Asset.Status;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetHistoryRecord;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.asset.ui.AttributeFieldEditor;
import org.wcs.smart.asset.ui.CommentDialog;
import org.wcs.smart.asset.ui.IdFieldHeader;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

public class AssetEditor extends EditorPart {

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
	
	private Label lblStatus;
	private Label lblStatusImage;
	
	private IdFieldHeader lblId;
	private Label lblAssetTypeImage;
	private Label lblAssetType;
	private List<AttributeFieldEditor> attributeEditors = null;
	
	private TableViewer tblHistory;
	
	private List<AssetHistoryRecord> activeHistoryRecords;
	private List<AssetHistoryRecord> toDeleteHistoryRecords;
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		boolean isNew = asset.getUuid() == null;
		try(Session s = HibernateManager.openSession()){
			try {
				s.beginTransaction();
				s.saveOrUpdate(asset);
				
				if (activeHistoryRecords != null) activeHistoryRecords.forEach(r->s.saveOrUpdate(r));
				if (toDeleteHistoryRecords != null) toDeleteHistoryRecords.forEach(r->s.delete(r));
				
				s.getTransaction().commit();
				
				((AssetEditorInput)getEditorInput()).setAssetUuid(asset.getUuid());
				setDirty(false);
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
		lblStatus.setText(asset.getStatus(true).name()); //TODO: gui name for status
		lblStatusImage.setImage(AssetCoreLabelProvider.getStatusImage(asset));
		
		initializeAttributePanel(asset);
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
			
			refreshStatus();
			
			lblAssetType.setText(asset.getAssetType().getName());
			Image img = AWTSWTImageUtils.convertToSWTImage(asset.getAssetType().getIconAsImage());
			lblAssetTypeImage.setImage(img);
			lblAssetTypeImage.addListener(SWT.Dispose, e->img.dispose());
			lblId.setText(asset.getId());
			
			setPartName(asset.getId());
			setTitleImage(img);
			
			initializeAttributePanel(asset);
			initializeHistoryPanel(asset);
			
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
			//TODO: validate id
			asset.setId(e.text);
			setPartName(e.text);
			setDirty(true);
		});

		lblStatusImage = toolkit.createLabel(headerComp, "");
		lblStatus = toolkit.createLabel(headerComp, "");
		toolkit.createLabel(headerComp, "-");
		lblAssetType = toolkit.createLabel(headerComp, "");
				
		Composite headerSection = toolkit.createComposite(body);
		headerSection.setLayout(new GridLayout(4, false));
		((GridLayout)headerSection.getLayout()).marginWidth = 0;
		((GridLayout)headerSection.getLayout()).marginHeight = 0;
		
		Composite sectionBody = toolkit.createComposite(body);
		sectionBody.setLayout(new StackLayout());
		sectionBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Hyperlink lnkCurrent = toolkit.createHyperlink(headerSection, "Current Status", SWT.NONE);
		lnkCurrent.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (currentPanel == null) currentPanel = createCurrentSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = currentPanel;
				sectionBody.layout(true);			
			}
		});
		Hyperlink lnkDetails = toolkit.createHyperlink(headerSection, "Details", SWT.NONE);
		lnkDetails.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (detailsPanel == null) detailsPanel = createDetailsSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = detailsPanel;
				sectionBody.layout(true);				
			}
		});
		Hyperlink lnkDeployments = toolkit.createHyperlink(headerSection, "Deployment History", SWT.NONE);
		lnkDeployments.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (deploymentPanel == null) deploymentPanel = createDeploymentsSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = deploymentPanel;
				sectionBody.layout(true);					
			}
		});
		Hyperlink lnkEvents = toolkit.createHyperlink(headerSection, "Event Histoary", SWT.NONE);
		lnkEvents.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (eventsPanel == null) eventsPanel = createHistorySection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = eventsPanel;
				sectionBody.layout(true);					
			}
		});
		
		//create initial panel
		createCurrentSection(sectionBody);
		((StackLayout)sectionBody.getLayout()).topControl = currentPanel;
		sectionBody.layout(true);	
		
		createEventHandlers();
		
		initData();
	}
	
	private Composite createCurrentSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		toolkit.createLabel(panel, "CURRENT SECTION");
		return panel;
	}

	private Label lblRetiredState = null;
	private Hyperlink changeRetiredState = null;
	
	private Composite createDetailsSection(Composite parent) {
		
		Composite panel = toolkit.createComposite(parent, SWT.BORDER);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridLayout());
		
		ScrolledComposite attributes = new ScrolledComposite(panel,  SWT.V_SCROLL);
		attributes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributes.setExpandHorizontal(true);
		attributes.setExpandVertical(true);
		
		toolkit.adapt(attributes);
		Composite attributePanel = toolkit.createComposite(attributes);
		attributes.setContent(attributePanel);
		attributePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		attributePanel.setLayout(new GridLayout(2, false));
		
		Label l = toolkit.createLabel(attributePanel, "Is Retired?");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		Composite retiredPanel = toolkit.createComposite(attributePanel);
		retiredPanel.setLayout(new GridLayout(2, false));
		retiredPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)retiredPanel.getLayout()).marginWidth = 0;
		((GridLayout)retiredPanel.getLayout()).marginHeight = 0;
		lblRetiredState = toolkit.createLabel(retiredPanel, "");
		changeRetiredState = toolkit.createHyperlink(retiredPanel, "", SWT.NONE);
		
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
			lblRetiredState.setText(asset.getIsRetired() ? "YES" : "NO");
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
	
	private Composite createDeploymentsSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		toolkit.createLabel(panel, "DEPLOYMENTS SECTION");
		return panel;
	}
	
	
	private Composite createHistorySection(Composite parent) {
		Composite panel = toolkit.createComposite(parent, SWT.BORDER);
		panel.setLayout(new GridLayout());
		
		tblHistory = new TableViewer(panel, SWT.BORDER | SWT.FULL_SELECTION);
		tblHistory.setContentProvider(ArrayContentProvider.getInstance());
		tblHistory.setInput(new String[] {DialogConstants.LOADING_TEXT});
		tblHistory.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblHistory.getTable().setHeaderVisible(true);
		tblHistory.getTable().setLinesVisible(true);
		
		TableViewerColumn col = new TableViewerColumn(tblHistory, SWT.NONE);
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
		TableViewerColumn col2 = new TableViewerColumn(tblHistory, SWT.NONE);
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
		col2.getColumn().setWidth(tblHistory.getControl().getSize().x - col.getColumn().getWidth());
		tblHistory.getControl().addListener(SWT.Resize, e->col2.getColumn().setWidth(tblHistory.getControl().getSize().x - col.getColumn().getWidth()));
		
		Menu mnu = new Menu(tblHistory.getControl());
		
		MenuItem mnuAdd = new MenuItem(mnu, SWT.PUSH);
		mnuAdd.setText("New ...");
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.addListener(SWT.Selection, e->addHistoryRecord());
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText("Delete ...");
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->deleteHistoryRecords());
		
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mnuDelete.setEnabled(!tblHistory.getSelection().isEmpty());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		tblHistory.getControl().setMenu(mnu);
		
		
		initializeHistoryPanel(asset);
		return panel;
	}
	private void addHistoryRecord() {
		CommentDialog dialog = new CommentDialog(getSite().getShell(), "Asset History Record", "Enter comment for asset history record");
		if (dialog.open() != CommentDialog.OK) return;
		
		AssetHistoryRecord record = new AssetHistoryRecord();
		record.setDate(new Date());
		record.setAsset(asset);
		record.setComment(dialog.getComment());
		activeHistoryRecords.add(record);
		setDirty(true);
		tblHistory.refresh();	
	}
	
	private void deleteHistoryRecords() {
		if (tblHistory == null) return;
		boolean modified = false;
		for (Iterator<?> iterator = ((IStructuredSelection)tblHistory.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof AssetHistoryRecord) {
				activeHistoryRecords.remove(x);
				toDeleteHistoryRecords.add((AssetHistoryRecord) x);
				modified = true;
			}
		}
		if (modified) {
			setDirty(true);
			tblHistory.refresh();
		}
		
	}
	private void initializeHistoryPanel(Asset asset) {
		if (tblHistory == null) return;
		Job j = new Job("load history records") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try(Session s = HibernateManager.openSession()){
					activeHistoryRecords.addAll(
							QueryFactory.buildQuery(s, AssetHistoryRecord.class, "asset", asset).list()); //$NON-NLS-1$
				}
				Display.getDefault().syncExec(()->{
					tblHistory.setInput(activeHistoryRecords);
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

}
