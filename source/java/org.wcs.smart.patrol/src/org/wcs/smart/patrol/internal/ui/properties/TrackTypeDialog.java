package org.wcs.smart.patrol.internal.ui.properties;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.export.dialog.CsvCaImportDialog;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.export.PatrolTransportCsvExportConfig;
import org.wcs.smart.patrol.internal.export.PatrolTransportCsvImportConfig;
import org.wcs.smart.patrol.internal.export.PatrolTransportCsvImporter;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributePatrolType;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.ui.IconSelectionDialog;
import org.wcs.smart.ui.IconSelectionDialog.Type;
import org.wcs.smart.ui.NamedIconItemLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

import com.ibm.icu.text.MessageFormat;

public class TrackTypeDialog extends SmartStyledTitleDialog {

	private static final String DISABLED_LABEL = Messages.PatrolTypePropertyPage_DisabledLabel;
	private static final String ACTIVE_LABEL = Messages.PatrolTypePropertyPage_ActiveLabel;
	private static final String INVALID_TYPE_DIALOG_TITLE = Messages.PatrolTypePropertyPage_InvalidType_DialogTitle;
	
	private TableViewer tblTrackTypes;
	private List<PatrolType> types;
	private List<PatrolAttribute> allAttributes;
	private List<PatrolType> toDelete;
	
	private CTabFolder tabs;
	private Text txtTypeName;
	private Label txtTypeKey;
	private ComboViewer cmbPilot;
	private ComboViewer cmbActive;
	private PatrolType currentType;
	private Label lblIcon;
	private Composite detailsPanel;
	private Button btnDisableTransport;
	private TableViewer tblAttributes;
	private TableViewer tblTransportTypes;
	private int transportTableEditIndex;
	private LanguageViewer languageViewer;

	boolean fireEvents = true;
	
	private NamedIconItemLabelProvider lblProvider = new NamedIconItemLabelProvider();
	
	final private ConservationArea currentCa = SmartDB.getCurrentConservationArea();
	
	public TrackTypeDialog(Shell parent) {
		super(parent);
		toDelete = new ArrayList<>();
	}

	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);

		Composite lc = new Composite(composite, SWT.NONE);
		lc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lc.setLayout(new GridLayout(2, false));
		
		Label l = new Label(lc, SWT.NONE);
		l.setText("Language:");
				
		languageViewer = new LanguageViewer(lc, SWT.NONE, currentCa);
		languageViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		languageViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				lblProvider.setLanguage(languageViewer.getCurrentSelection());
				tblTrackTypes.refresh();
				updateDetails();
			}
		});
		lblProvider.setLanguage(languageViewer.getCurrentSelection());
		
		SashForm parts = new SashForm(composite,  SWT.NONE);
		parts.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = new Composite(parts, SWT.BORDER);
		leftPart.setLayout(new GridLayout());
		((GridLayout)leftPart.getLayout()).marginWidth = 0;
		((GridLayout)leftPart.getLayout()).marginHeight = 0;
		
		createTrackTypesPanel(leftPart);
		
		Composite rightPart = new Composite(parts, SWT.BORDER);
		rightPart.setLayout(new GridLayout());
		((GridLayout)rightPart.getLayout()).marginWidth = 0;
		((GridLayout)rightPart.getLayout()).marginHeight = 0;
		
		
		parts.setWeights(1,2);
		
		
		
		tabs = new CTabFolder(rightPart,  SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		CTabItem detailsItem = new CTabItem(tabs, SWT.NONE);
		detailsItem.setText("Details");
		
		Composite details = createDetailsPanel(tabs);
		detailsItem.setControl(details);

		CTabItem transportTypeItem = new CTabItem(tabs, SWT.NONE);
		transportTypeItem.setText("Transport Types");
		Composite types = createTransportTypePanel(tabs);
		transportTypeItem.setControl(types);
		
		CTabItem attributeTypeItem = new CTabItem(tabs, SWT.NONE);
		attributeTypeItem.setText("Custom Attributes");
		Composite attributes = createAttributePanel(tabs);
		attributeTypeItem.setControl(attributes);
		
		setTitle("Track Type Configuration");
		getShell().setText("Track Type Configuration");
		setMessage("Configure the track types, associated transportation types and custom attributes.");
		
		tabs.setSelection(detailsItem);
		tabs.setVisible(false);
		loadDataJob.schedule();
		
		return composite; 
	}
	
	
	private void createNewType() {
		PatrolType type = new PatrolType();
		type.setConservationArea(currentCa);
		type.setIsActive(true);
		type.setRequiresPilot(false);
		type.setTransportTypes(new ArrayList<>());
		type.setCustomAttributes(new ArrayList<>());
		type.setName("New Track Type");
		type.updateName(currentCa.getDefaultLanguage(), type.getName());
		type.updateName(SmartDB.getCurrentLanguage(), type.getName());
		type.setKeyId(DataModelManager.INSTANCE.generateKey(type.getName(), types));
		types.add(type);
		
		tblTrackTypes.refresh();
		tblTrackTypes.setSelection(new StructuredSelection(type));
		modified();
		
	}
		
	private void deleteType() {
		Object x= tblTrackTypes.getStructuredSelection().getFirstElement();
		if (x == null || (!(x instanceof PatrolType))) return;
		
		PatrolType pt = (PatrolType)x;
		
		boolean doDelete = MessageDialog.openConfirm(getShell(), DialogConstants.DELETE_BUTTON_TEXT, 
				MessageFormat.format("Are you sure you want to delete the track type {0}?", getName(pt)));
		if (!doDelete) return;
		
		if(pt.getUuid() != null) {
			try(Session session = HibernateManager.openSession()){
				for(PatrolTransportType tt : pt.getTransportTypes()) {
					if (tt.getUuid() == null) continue;
					if (!DeleteManager.canDelete(tt, session)) {
						throw new Exception(MessageFormat.format("Cannot delete transport type {0} associated with type {1}.", getName(tt), getName(pt)));
					}	
				}
				if (!DeleteManager.canDelete(pt, session)) {
					throw new Exception(MessageFormat.format("Cannot delete track type {0}", getName(pt)));
				}
				
				
			}catch (Exception ex) {
				MessageDialog.openError(getShell(), DialogConstants.DELETE_BUTTON_TEXT, ex.getMessage());
				return;
			}
		}
		
		toDelete.add(pt);
		types.remove(pt);
		
		tblTrackTypes.refresh();
		modified();
		updateDetails();
	}
	
	private void importTypes() {
		PatrolTransportCsvImportConfig config = new PatrolTransportCsvImportConfig();
        CsvCaImportDialog dialog = new CsvCaImportDialog(getShell(), config);
        if (dialog.open() != Window.OK) return;

        Object[] data = ((PatrolTransportCsvImporter)config.getImporter()).getImportedData();
        List<PatrolType> importedTypes = (List<PatrolType>) data[0];
        
        List<PatrolTransportType> importedTransports = (List<PatrolTransportType>) data[1];
        
        //add types
		for (PatrolType type : importedTypes) {
			if (DataModelManager.INSTANCE.validateKey(type.getKeyId(), this.types) != null) {
				type.setKeyId(DataModelManager.INSTANCE.generateKey(type.getKeyId(), this.types));
			}
			this.types.add(type);

			List<PatrolAttributePatrolType> newLinks = new ArrayList<>();
			for (PatrolAttributePatrolType link : type.getCustomAttributes()) {
				PatrolAttribute a = allAttributes.get(allAttributes.indexOf(link.getPatrolAttribute()));
				PatrolAttributePatrolType nlink = new PatrolAttributePatrolType();
				nlink.setPatrolAttribute(a);
				nlink.setPatrolType(type);
				a.getPatrolTypes().add(nlink);		
				newLinks.add(nlink);
			}
			type.setCustomAttributes(newLinks);
		}
		
		//link transports to types
		for (PatrolTransportType type : importedTransports){
        	if (DataModelManager.INSTANCE.validateKey(type.getKeyId(), getAllTransportTypes()) != null){
                type.setKeyId(DataModelManager.INSTANCE.generateKey(type.getKeyId(), getAllTransportTypes()));
            }

        	if (type.getPatrolType().getUuid() != null && 
        			type.getPatrolType().getUuid().equals(UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR))) {
        		//the transport type was not added to one of the imported types, so 
        		//lets search the existing types for it
        		
        		//find a patrol type
        		String ptypekey = type.getPatrolType().getKeyId();
        		type.setPatrolType(null);
        		for (PatrolType ptype : this.types) {
        			if (ptypekey.equalsIgnoreCase(ptype.getKeyId())) {
        				type.setPatrolType(ptype);
        				break;
        			}
        		}
        		if (type.getPatrolType() == null) {
        			//create a new patrol type
        			PatrolType temp = new PatrolType();
        			temp.setConservationArea(type.getConservationArea());
        			temp.setIsActive(true);
        			temp.setKeyId(ptypekey);
        			temp.setName(ptypekey);
        			temp.updateName(type.getConservationArea().getDefaultLanguage(), ptypekey);
        			if (DataModelManager.INSTANCE.validateKey(temp.getKeyId(), types) != null){
        				temp.setKeyId(DataModelManager.INSTANCE.generateKey(temp.getKeyId(), types));
                    }
        			temp.setTransportTypes(new ArrayList<>());
        			temp.getTransportTypes().add(type);
                	this.types.add(temp);
        		}	
        	}
        }

		modified();
		tblTrackTypes.refresh();
	}
        
	private void exportTypes() {
		 CsvExportDialog dialog = new CsvExportDialog(getShell(), new PatrolTransportCsvExportConfig());
	     dialog.open();
	}
	
	private void createTrackTypesPanel(Composite parent) {
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite header = SmartUiUtils.createHeaderLabel(c, "Track Types");
		header.setLayout(new GridLayout(2, false));
		
		ToolBar tbtypes = new ToolBar(header, SWT.FLAT);
		tbtypes.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));

		ToolItem importType = new ToolItem(tbtypes, SWT.PUSH);
		importType.setToolTipText("import transport types");
		importType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
		importType.addListener(SWT.Selection, e->importTypes());
		
		ToolItem exportType = new ToolItem(tbtypes, SWT.PUSH);
		exportType.setToolTipText("export transport types");
		exportType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		exportType.addListener(SWT.Selection, e->exportTypes());
		
		ToolItem deleteType = new ToolItem(tbtypes, SWT.PUSH);
		deleteType.setToolTipText("delete track type");
		deleteType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteType.addListener(SWT.Selection, e->deleteType());
		
		ToolItem newType = new ToolItem(tbtypes, SWT.PUSH);
		newType.setToolTipText("create new track type");
		newType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		newType.addListener(SWT.Selection, e->createNewType());
		
		Composite wrapper = new Composite(c, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblTrackTypes = new TableViewer(wrapper, SWT.FULL_SELECTION | SWT.V_SCROLL);
		tblTrackTypes.setLabelProvider(
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return lblProvider.getText(element);
					}
					@Override

					public Image getImage(Object element) {
						return lblProvider.getImage(element);
					}
					
					@Override
					public Color getForeground(Object element) {
						if (element instanceof PatrolType){
							if (!((PatrolType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
						}
						return null;
					}
				});
		
		tblTrackTypes.setContentProvider(ArrayContentProvider.getInstance());
		tblTrackTypes.setInput(new String[] {DialogConstants.LOADING_TEXT});
		tblTrackTypes.addSelectionChangedListener(e->updateDetails());
	
		TableColumn tc = new TableColumn(tblTrackTypes.getTable(), SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		layout.setColumnData(tc, new ColumnWeightData(100));
		wrapper.setLayout(layout);
		
		Menu mnu = new Menu(tblTrackTypes.getControl());
		MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->createNewType());
		;
		MenuItem miDelete = new MenuItem(mnu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->deleteType());
		
		tblTrackTypes.getControl().setMenu(mnu);
		
		miDelete.setEnabled(false);
		deleteType.setEnabled(false);
		tblTrackTypes.addSelectionChangedListener(e->{
			boolean hasSelection = !tblTrackTypes.getStructuredSelection().isEmpty();
			miDelete.setEnabled(hasSelection);
			deleteType.setEnabled(hasSelection);
		});
	}

	private List<PatrolTransportType> getAllTransportTypes(){
		List<PatrolTransportType> all = new ArrayList<>();
		for(PatrolType t:types) {
			all.addAll(t.getTransportTypes());
		}
		return all;
	}
	
	private void editTransportTypeKey(){
		PatrolTransportType x = (PatrolTransportType)((IStructuredSelection)tblTransportTypes.getSelection()).getFirstElement();
		String currentKey = x.getKeyId();
		
		InputDialog id = new KeyInputDialog(getShell(), currentKey,  getAllTransportTypes());
		int ret = id.open();
		if (ret != Window.CANCEL) {
			x.setKeyId(id.getValue());
			modified();
			tblTransportTypes.refresh(x);
		}
	}
	
	
	private void editTransportTypeIcon() {
		IconSelectionDialog dialog = new IconSelectionDialog(tblTransportTypes.getControl().getShell(), Type.SELECT);
		if (dialog.open()  != Window.OK) return ;
		setTransportTypeIcon(dialog.getSelectedIcon());
	}
	
	private void setTransportTypeIcon(Icon icon) {
		PatrolTransportType type = (PatrolTransportType)((IStructuredSelection)tblTransportTypes.getSelection()).getFirstElement();
		if (type == null) return;
		type.setIcon(icon);
		clearImageCache();
		modified();
	}
	
	private void deleteTransportType(){
		
		PatrolTransportType ttype = (PatrolTransportType) ((IStructuredSelection)tblTransportTypes.getSelection()).getFirstElement();
		if (ttype == null){
			return;
		}

		boolean ok = MessageDialog.openConfirm(getShell(), 
				Messages.PatrolTypePropertyPage_DeleteDialogTitle, 
				MessageFormat.format(Messages.PatrolTypePropertyPage_DeleteWarningMessage, new Object[]{getName(ttype)}));
		if (!ok){
			return;
		}
		
		try(Session s = HibernateManager.openSession()){
			ok = true;
			if (ttype.getUuid() != null){
				if (!DeleteManager.canDelete(ttype, s)){
					ok = false;
				}
			}
			if (ok){
				ttype.getPatrolType().getTransportTypes().remove(ttype);
				ttype.setPatrolType(null);
				modified();
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(MessageFormat.format(Messages.PatrolTypePropertyPage_Error_DeletingTransport + " " + ex.getLocalizedMessage(), new Object[]{ getName(ttype)}), ex); //$NON-NLS-1$
		}
		
		tblTransportTypes.refresh();
		
	}
	
	private void disableTransportType(){
		PatrolTransportType pt = (PatrolTransportType)((IStructuredSelection)tblTransportTypes.getSelection()).getFirstElement();
		pt.setIsActive(!pt.getIsActive());

		tblTransportTypes.refresh();
		modified();
		
		if (btnDisableTransport.getToolTipText().equals(DialogConstants.DISABLE_BUTTON_TEXT)){
			btnDisableTransport.setText(DialogConstants.ENABLE_BUTTON_TEXT);
			btnDisableTransport.setToolTipText(DialogConstants.ENABLE_BUTTON_TEXT);
			btnDisableTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
		}else{
			btnDisableTransport.setText(DialogConstants.DISABLE_BUTTON_TEXT);
			btnDisableTransport.setToolTipText(DialogConstants.DISABLE_BUTTON_TEXT);
			btnDisableTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));

		}
	}
	
	private void addTransportType(){
		if (currentType == null) return;
		
		PatrolTransportType newPtt = new PatrolTransportType();
		newPtt.setConservationArea(currentType.getConservationArea());
		newPtt.setIsActive(true);
		newPtt.setPatrolType(currentType);
		newPtt.setMaxSpeed(PatrolTransportType.MAX_SPEED_MAX_VALUE);
		
		if (currentType.getTransportTypes() == null) currentType.setTransportTypes(new ArrayList<>());
		currentType.getTransportTypes().add(newPtt);
		
		newPtt.updateName(newPtt.getConservationArea().getDefaultLanguage(), Messages.PatrolTypePropertyPage_DefaultTransportionTypeName);
		newPtt.setName(newPtt.findName(newPtt.getConservationArea().getDefaultLanguage()));
		
		newPtt.setKeyId(DataModelManager.INSTANCE.generateKey(newPtt.getName(), getAllTransportTypes()));
		
		tblTransportTypes.refresh();
		modified();
	}
	
	private Composite createTransportTypePanel(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		panel.setLayout(new GridLayout(2, false));
		
		Composite tableTransportComp = new Composite(panel, SWT.NONE);
		tableTransportComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableTransportComp.setLayout(tableLayout);
		
		tblTransportTypes = new TableViewer( tableTransportComp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		createTransportColumns(tblTransportTypes);
		tblTransportTypes.setContentProvider(ArrayContentProvider.getInstance());
		tblTransportTypes.getTable().setHeaderVisible(true);
		tblTransportTypes.getTable().setLinesVisible(true);
		
		
		tblTransportTypes.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = tblTransportTypes.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				if (cell.getColumnIndex() == tblTransportTypes.getTable().getColumnCount()-1){
					editTransportTypeKey();
				}else if (cell.getColumnIndex() == 3){
					disableTransportType();
				}else if (cell.getColumnIndex() == 0) {
					editTransportTypeIcon();
				}
			}
		});

		Composite composite = new Composite(panel, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,1, 1));
		((GridLayout)composite.getLayout()).marginWidth = 0;
		((GridLayout)composite.getLayout()).marginHeight = 0;

		Button btnAddTransport = new Button(composite, SWT.NONE);
		btnAddTransport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAddTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,1, 1));
		btnAddTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAddTransport.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAddTransport.addListener(SWT.Selection, e->addTransportType());
		
		Button btnEditTransport = new Button(composite, SWT.NONE);
		btnEditTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEditTransport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEditTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1,1));
		btnEditTransport.setText(DialogConstants.EDIT_KEY_BUTTON_TEXT);
		btnEditTransport.setEnabled(false);
		btnEditTransport.addListener(SWT.Selection, e->editTransportTypeKey());
		
		btnDisableTransport = new Button(composite, SWT.NONE);
		btnDisableTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDisableTransport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDisableTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		btnDisableTransport.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnDisableTransport.setEnabled(false);
		btnDisableTransport.addListener(SWT.Selection, e->disableTransportType());
		
		Button btnDeleteTransport = new Button(composite, SWT.NONE);
		btnDeleteTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,false, 1, 1));
		btnDeleteTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDeleteTransport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDeleteTransport.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDeleteTransport.setEnabled(false);
		btnDeleteTransport.addListener(SWT.Selection, e->deleteTransportType());	
		
		//table menu
		Menu tableMenu = new Menu(tblTransportTypes.getControl());
		tblTransportTypes.getControl().setMenu(tableMenu);
		
		MenuItem miAdd = new MenuItem(tableMenu, SWT.CASCADE);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		
		new MenuItem(tableMenu, SWT.SEPARATOR);
		
		MenuItem editKey = new MenuItem(tableMenu, SWT.PUSH);
		editKey.setText(DialogConstants.EDIT_BUTTON_TEXT);
		editKey.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editKey.addListener(SWT.Selection, evt->{
			if (transportTableEditIndex == -1) return;
			if (transportTableEditIndex == 0) {
				editTransportTypeIcon();
			}else if (transportTableEditIndex == tblTransportTypes.getTable().getColumnCount()-1) {
				editTransportTypeKey();
			}else {
				tblTransportTypes.editElement((PatrolTransportType) tblTransportTypes.getStructuredSelection().getFirstElement(), transportTableEditIndex);
			}
		});
		
		MenuItem clearIcon = new MenuItem(tableMenu, SWT.PUSH);
		clearIcon.setText(LabelConstants.CLEAR_IMAGE);
		clearIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		clearIcon.addListener(SWT.Selection, l->setTransportTypeIcon(null));
		
		MenuItem disableItem = new MenuItem(tableMenu, SWT.PUSH);
		disableItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		disableItem.addListener(SWT.Selection, l->disableTransportType());
		
		new MenuItem(tableMenu, SWT.SEPARATOR);

		MenuItem delete = new MenuItem(tableMenu, SWT.PUSH);
		delete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		delete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		delete.addListener(SWT.Selection, l->deleteTransportType());
		
		tableMenu.addMenuListener(new MenuListener() {			
			@Override
			public void menuShown(MenuEvent e) {
				boolean isSelected = !tblTransportTypes.getSelection().isEmpty();
				delete.setEnabled(isSelected);
				editKey.setEnabled(isSelected);
				disableItem.setEnabled(isSelected);
				
				if (isSelected){
					Object x = ((IStructuredSelection)tblTransportTypes.getSelection()).getFirstElement();
					if (x instanceof PatrolTransportType){
						if (((PatrolTransportType) x).getIsActive()){
							disableItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
							disableItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
						}else{
							disableItem.setText(DialogConstants.ENABLE_BUTTON_TEXT);
							disableItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
						}
					}
				}
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		
		tblTransportTypes.getTable().addListener(SWT.MenuDetect, evt->{
			ViewerCell cell = tblTransportTypes.getCell(tblTransportTypes.getControl().toControl(evt.x,  evt.y));
			transportTableEditIndex = -1;
			if (cell != null) transportTableEditIndex = cell.getColumnIndex();	
		});
		
		

		tblTransportTypes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				PatrolTransportType pt = (PatrolTransportType)((IStructuredSelection)tblTransportTypes.getSelection()).getFirstElement();
				
				if (pt == null){
					btnDisableTransport.setEnabled(false);
					btnDeleteTransport.setEnabled(false);
					btnEditTransport.setEnabled(false);
					return;
				}
				if (pt.getIsActive()){
					btnDisableTransport.setToolTipText(DialogConstants.DISABLE_BUTTON_TEXT);
					btnDisableTransport.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					btnDisableTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
				}else{
					btnDisableTransport.setToolTipText(DialogConstants.ENABLE_BUTTON_TEXT);
					btnDisableTransport.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					btnDisableTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
				}
				btnDisableTransport.setEnabled(true);
				btnDeleteTransport.setEnabled(true);
				btnEditTransport.setEnabled(true);
				
			}
		});
		
		
		return panel;
	}
	
	private Composite createAttributePanel(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		panel.setLayout(new GridLayout(2, false));
		
		Label l = new Label(panel, SWT.WRAP);
		l.setText("Custom attributes can be shared across track data types.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite wrapper = new Composite(panel, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblAttributes = new TableViewer(wrapper, SWT.BORDER | SWT.V_SCROLL);
		tblAttributes.setContentProvider(ArrayContentProvider.getInstance());
		tblAttributes.setLabelProvider(lblProvider);	
		tblAttributes.setInput(new String[] {DialogConstants.LOADING_TEXT});
		TableColumn tc = new TableColumn(tblAttributes.getTable(), SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		layout.setColumnData(tc, new ColumnWeightData(100));
		wrapper.setLayout(layout);
		
		Composite buttonPanel = new Composite(panel, SWT.NONE);
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		buttonPanel.setLayout(new GridLayout());
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		
		Button btnAdd = new Button(buttonPanel, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.addListener(SWT.Selection, e->addAttributeToType());
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnEdit = new Button(buttonPanel, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.addListener(SWT.Selection, e->editAttribute());
		btnEdit.setEnabled(false);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.addListener(SWT.Selection, e->deleteAttribute());
		btnDelete.setEnabled(false);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		
		Menu mnu = new Menu(tblAttributes.getControl());
		tblAttributes.getControl().setMenu(mnu);
		
		MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->addAttributeToType());

		MenuItem miEdit = new MenuItem(mnu, SWT.PUSH);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.addListener(SWT.Selection, e->editAttribute());
		miEdit.setEnabled(false);
		
		MenuItem miDelete = new MenuItem(mnu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->deleteAttribute());
		miDelete.setEnabled(false);
		
		tblAttributes.addSelectionChangedListener(e->{
			boolean enabled = !tblAttributes.getStructuredSelection().isEmpty() && (tblAttributes.getStructuredSelection().getFirstElement() instanceof PatrolAttribute);
			miEdit.setEnabled(enabled);
			miDelete.setEnabled(enabled);
			btnEdit.setEnabled(enabled);
			btnDelete.setEnabled(enabled);			
		});
		return panel;
	}
	
	private Composite createDetailsPanel(Composite parent) {
		
		detailsPanel = new Composite(parent, SWT.NONE);
		detailsPanel.setVisible(false);
		
		detailsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		detailsPanel.setLayout(new GridLayout(3, false));

		Label l = new Label(detailsPanel, SWT.NONE);
		l.setText("Enabled:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbActive = new ComboViewer(detailsPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbActive.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
		cmbActive.setContentProvider(ArrayContentProvider.getInstance());
		cmbActive.setInput(new Boolean[] {Boolean.TRUE, Boolean.FALSE});
		cmbActive.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if ((Boolean)element) return "Yes";
				return "No";
			}
		});
		cmbActive.addSelectionChangedListener(e->{
			if (currentType == null) return;
			currentType.setIsActive((boolean)cmbActive.getStructuredSelection().getFirstElement());
			tblTrackTypes.refresh();
			modified();
		});
		
		
		
		l = new Label(detailsPanel, SWT.NONE);
		l.setText("Name:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		txtTypeName = new Text(detailsPanel, SWT.BORDER);
		txtTypeName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		txtTypeName.addListener(SWT.FocusOut, e->{
			if (currentType == null) return;
			String text = txtTypeName.getText();
			currentType.setName(text);
			currentType.updateName(languageViewer.getCurrentSelection(), text);
			if (currentType.getUuid() == null) {
				List<PatrolType> kids = new ArrayList<>(types);
				kids.remove(currentType);
				currentType.setKeyId(DataModelManager.INSTANCE.generateKey(currentType.findName(currentCa.getDefaultLanguage()), kids));
				txtTypeKey.setText(currentType.getKeyId());
			}
			tblTrackTypes.refresh();
			modified();
			
		});
		
		
		l = new Label(detailsPanel, SWT.NONE);
		l.setText("Key:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		txtTypeKey = new Label(detailsPanel,SWT.NONE);
		txtTypeKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button btnEditTypeKey = new Button(detailsPanel, SWT.PUSH);
		btnEditTypeKey.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		btnEditTypeKey.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEditTypeKey.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEditTypeKey.addListener(SWT.Selection,e->{
			if (currentType == null) return;
			String currentKey = currentType.getKeyId();
			
			InputDialog id = new KeyInputDialog(getShell(), currentKey,  types);
			int ret = id.open();
			if (ret != Window.CANCEL) {
				currentType.setKeyId(id.getValue());
				txtTypeKey.setText(currentType.getKeyId());
				modified();				
			}
		});
		
		
		l = new Label(detailsPanel, SWT.NONE);
		l.setText("Icon:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		lblIcon = new Label(detailsPanel, SWT.NONE);
				
		Composite temp = new Composite(detailsPanel, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		((GridLayout)temp.getLayout()).marginWidth = 0 ;
		((GridLayout)temp.getLayout()).marginHeight = 0 ;
		
		Button btnClearIcon = new Button(temp, SWT.NONE);
		btnClearIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnClearIcon.setText("Clear");
		btnClearIcon.addListener(SWT.Selection, e->setTypeIcon(null));
		
		Button btnEditIcon = new Button(temp, SWT.NONE);
		btnEditIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEditIcon.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEditIcon.addListener(SWT.Selection, e->editTypeIcon());
		
		
		l = new Label(detailsPanel, SWT.NONE);
		l.setText("Requires Pilot:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbPilot = new ComboViewer(detailsPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbPilot.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
		cmbPilot.setContentProvider(ArrayContentProvider.getInstance());
		cmbPilot.setInput(new Boolean[] {Boolean.TRUE, Boolean.FALSE});
		cmbPilot.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if ((Boolean)element) return "Yes";
				return "No";
			}
		});
		cmbPilot.addSelectionChangedListener(e->{
			if (currentType == null) return;
			currentType.setRequiresPilot((boolean)cmbPilot.getStructuredSelection().getFirstElement());
			modified();
		});
	
		return detailsPanel;
	}
	
	private void addAttributeToType() {
		if (currentType == null) return;
		
		SelectPatrolAttributeDialog dialog = new SelectPatrolAttributeDialog(getShell(), allAttributes);
		if (dialog.open() != Window.OK) return;
		
		List<PatrolAttribute> toAdd = dialog.getSelection();
		if (toAdd == null || toAdd.isEmpty()) return;
		
		boolean added = false;
		for (PatrolAttribute a : toAdd) {
			if (!allAttributes.contains(a)) allAttributes.add(a);
			
			boolean add = true;
			for(PatrolAttributePatrolType t : currentType.getCustomAttributes()) {
				if (t.getPatrolAttribute().equals(a)) {
					add = false;
					break;
				}
			}
			if (add) {
				PatrolAttributePatrolType t = new PatrolAttributePatrolType();
				t.setPatrolAttribute(a);
				t.setPatrolType(currentType);
				currentType.getCustomAttributes().add(t);
				if (a.getPatrolTypes() == null) a.setPatrolTypes(new ArrayList<>());
				a.getPatrolTypes().add(t);
				added = true;
			}
		}
		
		updateAttributeTable();
		if (added) modified();
	}
	
	private void editAttribute() {
		if (tblAttributes.getStructuredSelection().isEmpty()) return;
		if (currentType == null) return;
		Object x = tblAttributes.getStructuredSelection().getFirstElement();
		
		if (x == null || !(x instanceof PatrolAttribute)) return;
		
		PatrolAttribute pa = (PatrolAttribute) x;
		pa = allAttributes.get(allAttributes.indexOf(pa));
		
		
		for (PatrolAttributePatrolType type : currentType.getCustomAttributes()) {
			if(type.getPatrolType().equals(currentType)) continue;
			if (type.getPatrolAttribute().equals(pa)) {
				MessageDialog.openInformation(getShell(), DialogConstants.EDIT_BUTTON_TEXT, "Modifying this attribute changes it across all track types that are associated with this attribute.");
				break;
			}
		}
		
		
		EditPatrolAttributeDialog dialog = new EditPatrolAttributeDialog(getShell(), pa, allAttributes);
		dialog.open();
		//TODO: reload attribute from database and update local cache
		try(Session session = HibernateManager.openSession()){
			PatrolAttribute updated = session.get(PatrolAttribute.class, pa.getUuid());
			if (updated.getIcon() != null) updated.getIcon().getFiles().forEach(f->{
				f.getIconSet().getName();
				f.computeFileLocation(session);	
			});
			updated.getPatrolTypes().size();
			
			allAttributes.remove(updated);
			allAttributes.add(updated);
			for (PatrolType pt : types) {
				for(PatrolAttributePatrolType at : pt.getCustomAttributes()) {
					if (at.getPatrolAttribute().equals(updated)) {
						at.setPatrolAttribute(updated);
					}
				}
			}
		}
		updateAttributeTable();
		clearImageCache();
		
	}
	
	
	
	
	private void deleteAttribute() {
		if (tblAttributes.getStructuredSelection().isEmpty()) return;
		if (currentType == null) return;
		Object x = tblAttributes.getStructuredSelection().getFirstElement();
		if (x == null || !(x instanceof PatrolAttribute)) return;
		
		PatrolAttribute pa = (PatrolAttribute) x;
		if (!MessageDialog.openQuestion(getShell(), DialogConstants.DELETE_BUTTON_TEXT,
				MessageFormat.format("Are you sure you want to remove the custom attribute {0} from the type {1}?", 
						getName(pa), getName(currentType)))) {
			return;
		}
		
		PatrolAttributePatrolType patoDelete = null;
		for(PatrolAttributePatrolType i : currentType.getCustomAttributes()) {
			if (i.getPatrolAttribute().equals(pa) && i.getPatrolType().equals(currentType)) {
				patoDelete = i;
				break;
			}
		}
		if (patoDelete == null) return;
		
		patoDelete.getPatrolAttribute().getPatrolTypes().remove(patoDelete);
		currentType.getCustomAttributes().remove(patoDelete);
		updateAttributeTable();
		modified();
	}
	
	private void editTypeIcon() {
		if (currentType == null) return;		
		IconSelectionDialog dialog = new IconSelectionDialog(getShell(), Type.SELECT);
		if (dialog.open()  != Window.OK) return ;
		setTypeIcon(dialog.getSelectedIcon());
	}
	
	private void setTypeIcon(Icon newIcon) {
		currentType.setIcon(newIcon);
		clearImageCache();
		
		lblIcon.setImage(lblProvider.getImage(currentType));
		lblIcon.getParent().layout();
		modified();
	}
	
	private void clearImageCache() {
		lblProvider.clearCachedImages();
		
		tblAttributes.refresh(true);
		tblTrackTypes.refresh(true);
		tblTransportTypes.refresh(true);
		if (currentType != null) lblIcon.setImage(lblProvider.getImage(currentType));
	}
	
	
	public void updateDetails() {
		try {
			fireEvents = false;
			
			currentType = null;
			if (!tblTrackTypes.getSelection().isEmpty()) {
				Object x = tblTrackTypes.getStructuredSelection().getFirstElement();
				if (x instanceof PatrolType t) currentType = t;
			}
			
			if (currentType == null) {
				tabs.setVisible(false);
				tblAttributes.setInput(new Object[] {});
			}else {
				
				tabs.setVisible(true);
				txtTypeKey.setText(currentType.getKeyId());
				txtTypeName.setText(getName(currentType));
				
				cmbPilot.setSelection(new StructuredSelection(currentType.getRequiresPilot()));
				cmbActive.setSelection(new StructuredSelection(currentType.getIsActive()));
				
				lblIcon.setImage(lblProvider.getImage(currentType));
				
				updateAttributeTable();
				tblTransportTypes.setInput(currentType.getTransportTypes());
			}
			
			detailsPanel.layout(true);
		}finally {
			fireEvents = true;
		}

	}
	
	private void updateAttributeTable() {
		List<PatrolAttribute> tattributes = currentType.getCustomAttributes().stream().map(e->e.getPatrolAttribute()).collect(Collectors.toList());
		Collections.sort(tattributes);
		tblAttributes.setInput(tattributes);
		tblAttributes.refresh(true);
	}
	
	private void modified() {
		if (!fireEvents) return;

		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) btn.setEnabled(true);
	}
	
	
	@Override
	protected Point getInitialSize(){
		return new Point(750, 500);
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}
	
	@Override
	public void okPressed() {
		doSave();
	}
	
	@Override
	public void cancelPressed() {
		if (getButton(IDialogConstants.OK_ID).isEnabled()) {
			
			if (MessageDialog.openQuestion(getShell(), 
					Messages.EditPatrolAttributeDialog_savetitle, 
					Messages.EditPatrolAttributeDialog_savemessage)) {
				if (!doSave()) return;
			}			
		}
		super.cancelPressed();
	}
	
	private boolean doSave() {
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				for (UuidItem i : toDelete) {
					if (i.getUuid() != null) session.remove(i);
				}
				session.flush();
				for (PatrolType pt : types) {
					for (PatrolTransportType tt : pt.getTransportTypes()) {
						HibernateManager.saveOrMerge(session, tt.getIcon());
					}
				}
				session.flush();
				for (PatrolType pt : types) {
					HibernateManager.saveOrMerge(session, pt.getIcon());
					
					if (pt.getUuid() == null) {
						session.persist(pt);
					}else {
						pt = session.merge(pt);
					}
					session.flush();	
				}
				
				List<PatrolAttribute> pas = QueryFactory.buildQuery(session, PatrolAttribute.class, 
						new Object[] {"conservationArea", currentCa}).list(); //$NON-NLS-1$
				for (PatrolAttribute pa : pas) {
					if (!pa.getPatrolTypes().isEmpty()) continue;
					
					boolean canDelete = true; 
					try {
						if (!DeleteManager.canDelete(pa, session)) {
							canDelete = false;
						}
					}catch (Exception ex) {
						canDelete = false;
					}
					if (!canDelete) continue;
					
					boolean dodelete = MessageDialog.openQuestion(getShell(), "Patrol Attribute", 
							MessageFormat.format("The custom attribute ''{0}'' is not associated with any track type. Do you want to remove it from the Conservation Area?", getName(pa)));
					if (dodelete) {
						session.remove(pa);
					}					
				}
				
				session.getTransaction().commit();
				
				toDelete.clear();
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				
				loadDataJob.schedule();
				return true;
			}catch (Exception ex) {
				SmartPatrolPlugIn.displayLog(MessageFormat.format("Unable to save changes: {0}", ex.getMessage()), ex);
				return false;
			}
		}
	}
	
	/*
	 * Creates transport type
	 */
	private void createTransportColumns(final TableViewer viewer) {
		
		// Icon Column
		TableViewerColumn viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setResizable(true);
		column.setText(DialogConstants.ICON_TEXT);

		TableColumnLayout layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
		column.setWidth( 32 + 20);
		
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ""; //$NON-NLS-1$
			}
			
			@Override
			public Image getImage(Object element) {
				return lblProvider.getImage(element);
			}
		});
			
		
		
		/* Transport Type Name Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(Messages.PatrolTypePropertyPage_TransportType_ColumnHeader);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
			
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return lblProvider.getText(element);
			}
			
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolTransportType){
					if (!((PatrolTransportType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		});
		viewerColumn.setEditingSupport(new EditingSupport(viewer){
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(viewer.getTable());
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
			@Override
			protected Object getValue(Object element) {
				return lblProvider.getText(element);
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof PatrolTransportType ttype){
					if (!ttype.findName(languageViewer.getCurrentSelection()).equals((String)value)){
						if(SmartUtils.isSimpleString(((String)value).trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, PatrolType.MAX_TRANSPORT_NAME_LENGTH)){
							Integer matches = 0;
							for (Iterator<PatrolTransportType> itr = ttype.getPatrolType().getTransportTypes().iterator(); itr.hasNext();) {
								PatrolTransportType a = itr.next();
								if( a != element && a.findName(languageViewer.getCurrentSelection()).equalsIgnoreCase(((String)value).trim())){
									matches++;
								}
							} 
							if(matches > 0){
								MessageDialog.openError(getShell(), INVALID_TYPE_DIALOG_TITLE, Messages.PatrolTypePropertyPage_Error_DuplicateTransportOption);
								modified();
							}else{
								ttype.updateName(languageViewer.getCurrentSelection(), ((String)value).trim());
								
								if (ttype.getKeyId() == null || ttype.getUuid() == null){
									ttype.setKeyId(DataModelManager.INSTANCE.generateKey(ttype.findName(ttype.getConservationArea().getDefaultLanguage()), getAllTransportTypes()));
								}
								modified();
							}
						}else{
							//invalid agency name, don't update it.
							MessageDialog.openError(getShell(), INVALID_TYPE_DIALOG_TITLE, 
									MessageFormat.format(Messages.PatrolTypePropertyPage_Error_InvalidTransportType, new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, PatrolType.MAX_TRANSPORT_NAME_LENGTH}));
							modified();
						}				
					}
					viewer.refresh();
				}					
			}});
		final TableViewerColumn vc2 = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			viewer.getTable().setSortColumn(vc2.getColumn());
			int dir = viewer.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			viewer.getTable().setSortDirection(dir);
			
			int change = dir == SWT.DOWN ? -1 : 1;
			currentType.getTransportTypes().sort((a,b)-> change * Collator.getInstance().compare(getName(a), getName(b)));
			viewer.refresh();
		});
		
		/* Max Speed */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText("Max Speed (km/h)");
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
			
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolTransportType pt){
					return String.valueOf(pt.getMaxSpeed());
				}
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolTransportType pt){
					if (!pt.getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
				
			}
		});
		final TableViewerColumn vc3 = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			viewer.getTable().setSortColumn(vc3.getColumn());
			int dir = viewer.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			viewer.getTable().setSortDirection(dir);
			
			int change = dir == SWT.DOWN ? -1 : 1;
			currentType.getTransportTypes().sort((a,b)-> change * Integer.compare(a.getMaxSpeed(), b.getMaxSpeed()));
			viewer.refresh();
		});
		
		viewerColumn.setEditingSupport(new EditingSupport(viewer){
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(viewer.getTable());
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
			@Override
			protected Object getValue(Object element) {
				if (element instanceof PatrolTransportType tt) return String.valueOf(tt.getMaxSpeed());
				return null;
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof PatrolTransportType ttype){
					try {
						Integer newValue = Integer.valueOf(value.toString());
						if (newValue > PatrolTransportType.MAX_SPEED_MAX_VALUE || newValue < PatrolTransportType.MAX_SPEED_MIN_VALUE) {
							throw new Exception();
						}
						ttype.setMaxSpeed(newValue);
						modified();
					}catch (Exception ex) {
						MessageDialog.openError(getShell(), INVALID_TYPE_DIALOG_TITLE, MessageFormat.format("Max speed must be between {0} and {1}.", PatrolTransportType.MAX_SPEED_MIN_VALUE, PatrolTransportType.MAX_SPEED_MAX_VALUE));
					}
					viewer.refresh();
				}					
			}});
		
		/* Active Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(ACTIVE_LABEL);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
			
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolTransportType){
					if (((PatrolTransportType)element).getIsActive()){
						return ACTIVE_LABEL;
					}else{
						return DISABLED_LABEL;
					}
				}
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolTransportType){
					if (!((PatrolTransportType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
				
			}
		});
		final TableViewerColumn vc1 = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			viewer.getTable().setSortColumn(vc1.getColumn());
			int dir = viewer.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			viewer.getTable().setSortDirection(dir);
			
			int change = dir == SWT.DOWN ? -1 : 1;
			currentType.getTransportTypes().sort((a,b)-> change * Boolean.compare(a.getIsActive(), b.getIsActive()));
			viewer.refresh();
		});
		
		
		
		/* Key Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(LabelConstants.TRANSTYPE_KEY);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
			
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolTransportType){
					return ((PatrolTransportType) element).getKeyId();
				}
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolTransportType){
					if (!((PatrolTransportType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
				
			}
		});
		final TableViewerColumn vc5 = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			viewer.getTable().setSortColumn(vc5.getColumn());
			int dir = viewer.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			viewer.getTable().setSortDirection(dir);
			int change = dir == SWT.DOWN ? -1 : 1;
			currentType.getTransportTypes().sort((a,b)-> change * Collator.getInstance().compare(a.getKeyId(), b.getKeyId()));
			viewer.refresh();
		});
	}
	
	private String getName(NamedItem item) {
		if (item == null) return ""; //$NON-NLS-1$
		if (this.languageViewer == null) return item.getName();
		String x = item.findNameNull(languageViewer.getCurrentSelection());
		if ( x != null) return x;
		return item.getName();
	}
	
	private Job loadDataJob = new Job(DialogConstants.LOADING_TEXT) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				
				types = PatrolHibernateManager.getPatrolTypes(SmartDB.getCurrentConservationArea(), session);
				types.forEach(t->{
					t.getName();
					t.getTransportTypes().forEach(tt->tt.getName());
					
					if (t.getCustomAttributes() == null) t.setCustomAttributes(new ArrayList<>());
					
					t.getCustomAttributes().forEach(a->{
						Hibernate.initialize(a);
						Hibernate.initialize(a.getPatrolAttribute());
						a.getPatrolAttribute().getName();
						if (a.getPatrolAttribute().getIcon() != null) a.getPatrolAttribute().getIcon().getFiles().forEach(f->{
							f.getIconSet().getName();
							f.computeFileLocation(session);	
						});
						
					});
				});
				
				allAttributes = session.createQuery("FROM PatrolAttribute WHERE conservationArea = :ca", PatrolAttribute.class)
						.setParameter("ca", SmartDB.getCurrentConservationArea())
						.list();
				
				for (PatrolAttribute a : allAttributes) {
					if (a.getIcon() != null) a.getIcon().getFiles().forEach(f->{
						f.getIconSet().getName();
						f.computeFileLocation(session);	
					});
					a.getPatrolTypes().size();
				}
			}
			
			Collections.sort(types);
			Collections.sort(allAttributes);
			
			Display.getDefault().asyncExec(()->{
				if (tblTrackTypes.getControl().isDisposed()) return;
				
				Object x = tblTrackTypes.getStructuredSelection().getFirstElement();
				tblTrackTypes.setInput(types);
				if (x != null) tblTrackTypes.setSelection(new StructuredSelection(x));
				updateDetails();
			});
			return Status.OK_STATUS;
		}
		
	};
	
	
	
	
}
