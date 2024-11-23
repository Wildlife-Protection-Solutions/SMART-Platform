package org.wcs.smart.patrol.internal.ui.properties;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.ui.PlatformUI;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.celleditor.ComboBoxViewerCellEditor;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.export.dialog.CsvCaImportDialog;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolDynamicMenuManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.export.PatrolTransportCsvExportConfig;
import org.wcs.smart.patrol.internal.export.PatrolTransportCsvImportConfig;
import org.wcs.smart.patrol.internal.export.PatrolTransportCsvImporter;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributePatrolType;
import org.wcs.smart.patrol.model.PatrolTransportGroup;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.ui.IconSelectionDialog;
import org.wcs.smart.ui.IconSelectionDialog.Type;
import org.wcs.smart.ui.NamedIconItemLabelProvider;
import org.wcs.smart.ui.SmartLabelProvider;
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
	private List<PatrolAttributePatrolType> deleteAttributes;
	
	private CTabFolder tabs;
	private Text txtTypeName;
	private Label txtTypeKey;
//	private ComboViewer cmbPilot;
	private ComboViewer cmbActive;
	private PatrolType currentType;
	private Label lblIcon;
	private Composite detailsPanel;
	private Button btnDisableTransport;
	private TableViewer tblAttributes;
	private TableViewer tblTransportTypes;
	private TableViewer tblTransportGroupTypes;
	private int transportTableEditIndex;
	private LanguageViewer languageViewer;
	private int transportGroupTableEditIndex = -1;

	boolean fireEvents = true;
	
	private NamedIconItemLabelProvider lblProvider = new NamedIconItemLabelProvider();
	
	final private ConservationArea currentCa = SmartDB.getCurrentConservationArea();

	private String menuTerm;
	private boolean needsRestart = false;
	
	public TrackTypeDialog(Shell parent) {
		super(parent);
		toDelete = new ArrayList<>();
		deleteAttributes = new ArrayList<>();
		menuTerm = PatrolDynamicMenuManager.INSTANCE.getCurrentTerm();			
	}

	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);

		Composite lc = new Composite(composite, SWT.NONE);
		lc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lc.setLayout(new GridLayout(2, false));
		
		Label l = new Label(lc, SWT.NONE);
		l.setText(Messages.TrackTypeDialog_language);
				
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
		detailsItem.setText(Messages.TrackTypeDialog_detailstab);
		
		Composite details = createDetailsPanel(tabs);
		detailsItem.setControl(details);

		CTabItem transportTypeItem = new CTabItem(tabs, SWT.NONE);
		transportTypeItem.setText(LabelConstants.TRANSPORT_MODE);
		Composite types = createTransportTypePanel(tabs);
		transportTypeItem.setControl(types);
		
		CTabItem transportTypeGroupItem = new CTabItem(tabs, SWT.NONE);
		transportTypeGroupItem.setText(LabelConstants.ENVIRONMENT_NAME);
		Composite groups = createTransportTypeGroupPanel(tabs);
		transportTypeGroupItem.setControl(groups);
		
		CTabItem attributeTypeItem = new CTabItem(tabs, SWT.NONE);
		attributeTypeItem.setText(LabelConstants.CUSTOM_METADATA_NAME);
		Composite attributes = createAttributePanel(tabs);
		attributeTypeItem.setControl(attributes);
		
		String title = Messages.TrackTypeDialog_title; 
		setTitle(title);
		getShell().setText(title);
		setMessage(Messages.TrackTypeDialog_message);
		
		tabs.setSelection(detailsItem);
		tabs.setVisible(false);
		loadDataJob.schedule();
		
		return composite; 
	}
	
	
	private void createNewType() {
		PatrolType type = new PatrolType();
		type.setConservationArea(currentCa);
		type.setIsActive(true);
		type.setTransportTypes(new ArrayList<>());
		type.setCustomAttributes(new ArrayList<>());
		type.setTransportGroups(new ArrayList<>());
		type.setName(MessageFormat.format(Messages.TrackTypeDialog_new, LabelConstants.TRACK_TYPE));
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
				MessageFormat.format(Messages.TrackTypeDialog_deletetrackteyp, getName(pt)));
		if (!doDelete) return;
		
		if(pt.getUuid() != null) {
			try(Session session = HibernateManager.openSession()){
				for(PatrolTransportType tt : pt.getTransportTypes()) {
					if (tt.getUuid() == null) continue;
					if (!DeleteManager.canDelete(tt, session)) {
						throw new Exception(MessageFormat.format(Messages.TrackTypeDialog_deletemodeerror, getName(tt), getName(pt)));
					}	
				}
				if (!DeleteManager.canDelete(pt, session)) {
					throw new Exception(MessageFormat.format(Messages.TrackTypeDialog_deletetrackerror, getName(pt)));
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
	
	@SuppressWarnings("unchecked")
	private void importTypes() {
		PatrolTransportCsvImportConfig config = new PatrolTransportCsvImportConfig();
        CsvCaImportDialog dialog = new CsvCaImportDialog(getShell(), config);
        if (dialog.open() != Window.OK) return;

        Object[] data = ((PatrolTransportCsvImporter)config.getImporter()).getImportedData();
        List<PatrolType> importedTypes = (List<PatrolType>) data[0];
        
        List<PatrolTransportType> importedTransports = (List<PatrolTransportType>) data[1];
        
        //add types
        List<PatrolTransportGroup> allGroups = new ArrayList<>();
        for (PatrolType t : this.types) {
        	allGroups.addAll(t.getTransportGroups());
        }
        
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
			
			for (PatrolTransportGroup group : type.getTransportGroups()) {
				if (DataModelManager.INSTANCE.validateKey(group.getKeyId(), allGroups) != null) {
					group.setKeyId(DataModelManager.INSTANCE.generateKey(group.getKeyId(), allGroups));
				}
				allGroups.add(group);
			}
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
		
		Composite header = SmartUiUtils.createHeaderLabel(c, LabelConstants.TRACK_TYPE);
		header.setLayout(new GridLayout(2, false));
		
		ToolBar tbtypes = new ToolBar(header, SWT.FLAT);
		tbtypes.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));

		ToolItem importType = new ToolItem(tbtypes, SWT.PUSH);
		importType.setToolTipText(Messages.TrackTypeDialog_importtypestooltip);
		importType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
		importType.addListener(SWT.Selection, e->importTypes());
		
		ToolItem exportType = new ToolItem(tbtypes, SWT.PUSH);
		exportType.setToolTipText(Messages.TrackTypeDialog_exporttypestooltip);
		exportType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		exportType.addListener(SWT.Selection, e->exportTypes());
		
		ToolItem deleteType = new ToolItem(tbtypes, SWT.PUSH);
		deleteType.setToolTipText(Messages.TrackTypeDialog_deletetypetooltip);
		deleteType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteType.addListener(SWT.Selection, e->deleteType());
		
		ToolItem newType = new ToolItem(tbtypes, SWT.PUSH);
		newType.setToolTipText(Messages.TrackTypeDialog_newtypetooltip);
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
	
	private List<PatrolTransportGroup> getAllTransportGroups(){
		List<PatrolTransportGroup> all = new ArrayList<>();
		for(PatrolType t:types) {
			all.addAll(t.getTransportGroups());
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
	private void editTransportGroupKey(){
		PatrolTransportGroup x = (PatrolTransportGroup)((IStructuredSelection)tblTransportGroupTypes.getSelection()).getFirstElement();
		String currentKey = x.getKeyId();
		
		InputDialog id = new KeyInputDialog(getShell(), currentKey,  getAllTransportGroups());
		int ret = id.open();
		if (ret != Window.CANCEL) {
			x.setKeyId(id.getValue());
			modified();
			tblTransportGroupTypes.refresh(x);
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
	
	private void editTransportGroupIcon() {
		IconSelectionDialog dialog = new IconSelectionDialog(tblTransportGroupTypes.getControl().getShell(), Type.SELECT);
		if (dialog.open()  != Window.OK) return ;
		setTransportGroupIcon(dialog.getSelectedIcon());
	}
	
	private void setTransportGroupIcon(Icon icon) {
		PatrolTransportGroup type = (PatrolTransportGroup)((IStructuredSelection)tblTransportGroupTypes.getSelection()).getFirstElement();
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
				MessageFormat.format(Messages.TrackTypeDialog_confirmdeletemode, new Object[]{getName(ttype)}));
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
		
		modified();
		tblTransportTypes.refresh();
		
	}
	
	private void deleteTransportGroup(){
		
		PatrolTransportGroup group = (PatrolTransportGroup) ((IStructuredSelection)tblTransportGroupTypes.getSelection()).getFirstElement();
		if (group == null) return;
			
		boolean ok = MessageDialog.openConfirm(getShell(), 
				Messages.PatrolTypePropertyPage_DeleteDialogTitle, 
				MessageFormat.format(Messages.TrackTypeDialog_confirmdeleteenv, getName(group)));
		if (!ok) return;
		
		try(Session s = HibernateManager.openSession()){
			ok = true;
			if (group.getUuid() != null){
				if (!DeleteManager.canDelete(group, s)){
					ok = false;
				}
			}
			if (ok){
				group.getTransportTypes().forEach(tt->tt.setTransportGroup(null));
				group.getTransportTypes().clear();
				group.getPatrolType().getTransportGroups().remove(group);			
				group.setPatrolType(null);
				modified();
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(MessageFormat.format("Could not delete environment {0}." + ex.getLocalizedMessage(), getName(group)), ex); //$NON-NLS-1$
		}
		
		modified();
		tblTransportGroupTypes.refresh();
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
	
	private void togglePilot(){
		PatrolTransportType pt = (PatrolTransportType)((IStructuredSelection)tblTransportTypes.getSelection()).getFirstElement();
		pt.setRequiresPilot(!pt.getRequiresPilot());

		tblTransportTypes.refresh();
		modified();
	}
	
	private void addTransportType(){
		if (currentType == null) return;
		
		PatrolTransportType newPtt = new PatrolTransportType();
		newPtt.setConservationArea(currentType.getConservationArea());
		newPtt.setIsActive(true);
		newPtt.setPatrolType(currentType);
		newPtt.setRequiresPilot(false);
		newPtt.setMaxSpeed(PatrolTransportType.MAX_SPEED_MAX_VALUE);
		
		if (currentType.getTransportTypes() == null) currentType.setTransportTypes(new ArrayList<>());
		currentType.getTransportTypes().add(newPtt);
		
		newPtt.updateName(newPtt.getConservationArea().getDefaultLanguage(), MessageFormat.format(Messages.PatrolTypePropertyPage_DefaultTransportionTypeName2, LabelConstants.TRANSPORT_MODE));
		newPtt.setName(newPtt.findName(newPtt.getConservationArea().getDefaultLanguage()));
		
		newPtt.setKeyId(DataModelManager.INSTANCE.generateKey(newPtt.getName(), getAllTransportTypes()));
		
		tblTransportTypes.refresh();
		modified();
	}
	
	private void addTransportGroup(){
		if (currentType == null) return;
		
		PatrolTransportGroup newgroup = new PatrolTransportGroup();
		newgroup.setPatrolType(currentType);
		newgroup.setTransportTypes(new ArrayList<>());
		
		if (currentType.getTransportGroups() == null) currentType.setTransportGroups(new ArrayList<>());
		currentType.getTransportGroups().add(newgroup);
		
		newgroup.updateName(currentType.getConservationArea().getDefaultLanguage(), MessageFormat.format(Messages.TrackTypeDialog_newenv, LabelConstants.ENVIRONMENT_NAME));
		newgroup.setName(newgroup.findName(currentType.getConservationArea().getDefaultLanguage()));
		
		newgroup.setKeyId(DataModelManager.INSTANCE.generateKey(newgroup.getName(), getAllTransportGroups()));
		
		tblTransportGroupTypes.refresh();
		modified();
	}
	
	private Composite createTransportTypeGroupPanel(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		panel.setLayout(new GridLayout(2, false));
		
		Composite tableTransportComp = new Composite(panel, SWT.NONE);
		tableTransportComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableTransportComp.setLayout(tableLayout);
		
		tblTransportGroupTypes = new TableViewer( tableTransportComp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		
		// Icon Column
		TableViewerColumn viewerColumn = new TableViewerColumn(tblTransportGroupTypes,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setResizable(true);
		column.setText(DialogConstants.ICON_TEXT);

		TableColumnLayout layout = (TableColumnLayout) tblTransportGroupTypes.getTable().getParent().getLayout();
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
		
		//Name
		viewerColumn = new TableViewerColumn(tblTransportGroupTypes,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(LabelConstants.ENVIRONMENT_NAME);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) tblTransportGroupTypes.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
					
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return lblProvider.getText(element);
			}
		});
				
		viewerColumn.setEditingSupport(new EditingSupport(tblTransportGroupTypes){
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(tblTransportGroupTypes.getTable());
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
				if (element instanceof PatrolTransportGroup group){
					if (!group.findName(languageViewer.getCurrentSelection()).equals((String)value)){
						if(SmartUtils.isSimpleString(((String)value).trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, PatrolType.MAX_TRANSPORT_NAME_LENGTH)){
							group.updateName(languageViewer.getCurrentSelection(), ((String)value).trim());
							if (group.getKeyId() == null || group.getUuid() == null){
								group.setKeyId(DataModelManager.INSTANCE.generateKey(group.findName(group.getPatrolType().getConservationArea().getDefaultLanguage()), getAllTransportGroups()));
							}
							modified();
						}else{
							//invalid name
							MessageDialog.openError(getShell(), INVALID_TYPE_DIALOG_TITLE, 
									MessageFormat.format(Messages.TrackTypeDialog_envnameerror, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, PatrolType.MAX_TRANSPORT_NAME_LENGTH));
							modified();
						}				
						tblTransportGroupTypes.refresh();
											
			}}}});
			
		final TableViewerColumn vc2 = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			tblTransportGroupTypes.getTable().setSortColumn(vc2.getColumn());
			int dir = tblTransportGroupTypes.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			tblTransportGroupTypes.getTable().setSortDirection(dir);
					
			int change = dir == SWT.DOWN ? -1 : 1;
			currentType.getTransportTypes().sort((a,b)-> change * Collator.getInstance().compare(getName(a), getName(b)));
			tblTransportGroupTypes.refresh();
		});
				
		// Key Column 
		viewerColumn = new TableViewerColumn(tblTransportGroupTypes,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(LabelConstants.TRANSTYPE_KEY);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) tblTransportGroupTypes.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
					
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof NamedKeyItem ni) return ni.getKeyId();
				return super.getText(element);
			}
		});
		
		final TableViewerColumn vc5 = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			tblTransportGroupTypes.getTable().setSortColumn(vc5.getColumn());
			int dir = tblTransportGroupTypes.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			tblTransportGroupTypes.getTable().setSortDirection(dir);
			int change = dir == SWT.DOWN ? -1 : 1;
			currentType.getTransportTypes().sort((a,b)-> change * Collator.getInstance().compare(a.getKeyId(), b.getKeyId()));
			tblTransportGroupTypes.refresh();
		});
		
		
		tblTransportGroupTypes.setContentProvider(ArrayContentProvider.getInstance());
		tblTransportGroupTypes.getTable().setHeaderVisible(true);
		tblTransportGroupTypes.getTable().setLinesVisible(true);
		
		
		tblTransportGroupTypes.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = tblTransportGroupTypes.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				if (cell.getColumnIndex() == tblTransportGroupTypes.getTable().getColumnCount()-1){
					editTransportGroupKey();				
				}else if (cell.getColumnIndex() == 0) {
					editTransportGroupIcon();
				}
			}
		});

		Composite composite = new Composite(panel, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,1, 1));
		((GridLayout)composite.getLayout()).marginWidth = 0;
		((GridLayout)composite.getLayout()).marginHeight = 0;

		Button btnAddGroup = new Button(composite, SWT.NONE);
		btnAddGroup.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAddGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,1, 1));
		btnAddGroup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAddGroup.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAddGroup.addListener(SWT.Selection, e->addTransportGroup());
		
		Button btnEditGroup = new Button(composite, SWT.NONE);
		btnEditGroup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEditGroup.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEditGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1,1));
		btnEditGroup.setText(DialogConstants.EDIT_KEY_BUTTON_TEXT);
		btnEditGroup.setEnabled(false);
		btnEditGroup.addListener(SWT.Selection, e->editTransportGroupKey());
		
		Button btnDeleteGroup = new Button(composite, SWT.NONE);
		btnDeleteGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,false, 1, 1));
		btnDeleteGroup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDeleteGroup.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDeleteGroup.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDeleteGroup.setEnabled(false);
		btnDeleteGroup.addListener(SWT.Selection, e->deleteTransportGroup());	
		
		//table menu
		Menu tableMenu = new Menu(tblTransportTypes.getControl());
		tblTransportGroupTypes.getControl().setMenu(tableMenu);
		
		MenuItem miAdd = new MenuItem(tableMenu, SWT.CASCADE);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->addTransportGroup());

		new MenuItem(tableMenu, SWT.SEPARATOR);
		
		MenuItem editKey = new MenuItem(tableMenu, SWT.PUSH);
		editKey.setText(DialogConstants.EDIT_BUTTON_TEXT);
		editKey.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editKey.addListener(SWT.Selection, evt->{
			if (transportGroupTableEditIndex == -1) return;
			if (transportGroupTableEditIndex == 0) {
				editTransportTypeIcon();
			}else if (transportGroupTableEditIndex == tblTransportGroupTypes.getTable().getColumnCount()-1) {
				editTransportTypeKey();
			}else {
				tblTransportGroupTypes.editElement((PatrolTransportType) tblTransportGroupTypes.getStructuredSelection().getFirstElement(), transportTableEditIndex);
			}
		});
		
		MenuItem clearIcon = new MenuItem(tableMenu, SWT.PUSH);
		clearIcon.setText(LabelConstants.CLEAR_IMAGE);
		clearIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		clearIcon.addListener(SWT.Selection, l->setTransportGroupIcon(null));
		
		new MenuItem(tableMenu, SWT.SEPARATOR);

		MenuItem delete = new MenuItem(tableMenu, SWT.PUSH);
		delete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		delete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		delete.addListener(SWT.Selection, l->deleteTransportGroup());
		
		tableMenu.addMenuListener(new MenuListener() {			
			@Override
			public void menuShown(MenuEvent e) {
				boolean isSelected = !tblTransportGroupTypes.getSelection().isEmpty();
				delete.setEnabled(isSelected);
				editKey.setEnabled(isSelected);
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		
		tblTransportGroupTypes.getTable().addListener(SWT.MenuDetect, evt->{
			ViewerCell cell = tblTransportTypes.getCell(tblTransportGroupTypes.getControl().toControl(evt.x,  evt.y));
			transportGroupTableEditIndex = -1;
			if (cell != null) transportGroupTableEditIndex = cell.getColumnIndex();	
		});
		
		

		tblTransportGroupTypes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean isSelected = !tblTransportGroupTypes.getSelection().isEmpty();
				btnDeleteGroup.setEnabled(isSelected);
				btnEditGroup.setEnabled(isSelected);
			}
		});
		
		
		return panel;
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
				}else if (cell.getColumnIndex() == 4){
					togglePilot();
				}else if (cell.getColumnIndex() == 5){
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
		miAdd.addListener(SWT.Selection, e->addTransportType());
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
		l.setText(Messages.TrackTypeDialog_metadatasharedinfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite wrapper = new Composite(panel, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableColumnLayout tableLayout = new TableColumnLayout();
		wrapper.setLayout(tableLayout);
		
		tblAttributes = new TableViewer(wrapper, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		tblAttributes.setContentProvider(ArrayContentProvider.getInstance());
		tblAttributes.setInput(new String[] {DialogConstants.LOADING_TEXT});
		tblAttributes.getTable().setHeaderVisible(true);
		
		
		tblAttributes.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = tblAttributes.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				if (cell.getColumnIndex() == tblAttributes.getTable().getColumnCount()-1){
					toggleAttributeEnabledState();				
				}else {
					editAttribute();
				}
			}
		});

		// Icon Column
		TableViewerColumn viewerColumn = new TableViewerColumn(tblAttributes,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setResizable(false);
		column.setText(DialogConstants.ICON_TEXT);

		TableColumnLayout layout = (TableColumnLayout) tblAttributes.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
		column.setWidth( 32 + 20);
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public Image getImage(Object element) {
				if (element instanceof PatrolAttributePatrolType pt) {
					return lblProvider.getImage(pt.getPatrolAttribute());
				}
				return null;
			}
			@Override
			public String getText(Object element) {
				return null;
			}
		});
		
		//Name
		viewerColumn = new TableViewerColumn(tblAttributes,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(Messages.TrackTypeDialog_attributecol);
		column.setResizable(true);

		layout = (TableColumnLayout) tblAttributes.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
							
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof PatrolAttributePatrolType pt) {
							return lblProvider.getText(pt.getPatrolAttribute());
						}
						return super.getText(element);
					}
					@Override
					public Color getForeground(Object element) {
						if (element instanceof PatrolAttributePatrolType pt){
							if (!pt.getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
						}
						return null;
					}
		});
		
		// Enabled/Disabled 
		viewerColumn = new TableViewerColumn(tblAttributes,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(ACTIVE_LABEL);
		column.setResizable(true);

		layout = (TableColumnLayout) tblTransportGroupTypes.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
							
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolAttributePatrolType pt) {
					if (pt.getIsActive()) return ACTIVE_LABEL;
					return DISABLED_LABEL;
				}
				return ""; //$NON-NLS-1$
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolAttributePatrolType pt){
					if (!pt.getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		});
				
				
		
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
		
		Button btnEnable = new Button(buttonPanel, SWT.PUSH);
		btnEnable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		btnEnable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		btnEnable.addListener(SWT.Selection, e->toggleAttributeEnabledState());
		btnEnable.setEnabled(false);
		btnEnable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
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

		new MenuItem(mnu, SWT.SEPARATOR);

		MenuItem miEdit = new MenuItem(mnu, SWT.PUSH);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.addListener(SWT.Selection, e->editAttribute());
		miEdit.setEnabled(false);
		
		MenuItem disableItem = new MenuItem(mnu, SWT.PUSH);
		disableItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		disableItem.addListener(SWT.Selection, e->toggleAttributeEnabledState());
		disableItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));

		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem miDelete = new MenuItem(mnu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->deleteAttribute());
		miDelete.setEnabled(false);
		
		tblAttributes.addSelectionChangedListener(e->{
			boolean enabled = !tblAttributes.getStructuredSelection().isEmpty() && (tblAttributes.getStructuredSelection().getFirstElement() instanceof PatrolAttributePatrolType);
			miEdit.setEnabled(enabled);
			miDelete.setEnabled(enabled);
			btnEdit.setEnabled(enabled);
			btnDelete.setEnabled(enabled);	
			
			btnEnable.setEnabled(enabled);
			
			if (enabled) {
				if ( ((PatrolAttributePatrolType)tblAttributes.getStructuredSelection().getFirstElement()).getIsActive()) {
					btnEnable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					btnEnable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
					disableItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					disableItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
				}else {
					btnEnable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					btnEnable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
					disableItem.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					disableItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
				}
			}
			
		});
		return panel;
	}
	
	private Composite createDetailsPanel(Composite parent) {
		
		detailsPanel = new Composite(parent, SWT.NONE);
		detailsPanel.setVisible(false);
		
		detailsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		detailsPanel.setLayout(new GridLayout(3, false));

		Label l = new Label(detailsPanel, SWT.NONE);
		l.setText(Messages.TrackTypeDialog_enabled);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbActive = new ComboViewer(detailsPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbActive.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
		cmbActive.setContentProvider(ArrayContentProvider.getInstance());
		cmbActive.setInput(new Boolean[] {Boolean.TRUE, Boolean.FALSE});
		cmbActive.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if ((Boolean)element) return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
				return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
			}
		});
		cmbActive.addSelectionChangedListener(e->{
			if (currentType == null) return;
			currentType.setIsActive((boolean)cmbActive.getStructuredSelection().getFirstElement());
			tblTrackTypes.refresh();
			modified();
		});
		
		
		
		l = new Label(detailsPanel, SWT.NONE);
		l.setText(Messages.TrackTypeDialog_name);
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
		l.setText(Messages.TrackTypeDialog_key);
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
		l.setText(Messages.TrackTypeDialog_icon);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		lblIcon = new Label(detailsPanel, SWT.NONE);
				
		Composite temp = new Composite(detailsPanel, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		((GridLayout)temp.getLayout()).marginWidth = 0 ;
		((GridLayout)temp.getLayout()).marginHeight = 0 ;
		
		Button btnClearIcon = new Button(temp, SWT.NONE);
		btnClearIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnClearIcon.setText(Messages.TrackTypeDialog_clear);
		btnClearIcon.addListener(SWT.Selection, e->setTypeIcon(null));
		
		Button btnEditIcon = new Button(temp, SWT.NONE);
		btnEditIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEditIcon.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEditIcon.addListener(SWT.Selection, e->editTypeIcon());
		
		
//		l = new Label(detailsPanel, SWT.NONE);
//		l.setText("Requires Pilot:");
//		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
//		
//		cmbPilot = new ComboViewer(detailsPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
//		cmbPilot.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
//		cmbPilot.setContentProvider(ArrayContentProvider.getInstance());
//		cmbPilot.setInput(new Boolean[] {Boolean.TRUE, Boolean.FALSE});
//		cmbPilot.setLabelProvider(new LabelProvider() {
//			public String getText(Object element) {
//				if ((Boolean)element) return "Yes";
//				return "No";
//			}
//		});
//		cmbPilot.addSelectionChangedListener(e->{
//			if (currentType == null) return;
//			currentType.setRequiresPilot((boolean)cmbPilot.getStructuredSelection().getFirstElement());
//			modified();
//		});
//	
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
				t.setIsActive(true);
				t.setPatrolAttribute(a);
				t.setPatrolType(currentType);
				currentType.getCustomAttributes().add(t);
				if (a.getPatrolTypes() == null) a.setPatrolTypes(new ArrayList<>());
				a.getPatrolTypes().add(t);
				added = true;
				
				List<PatrolAttributePatrolType> tmp = new ArrayList<>();
				for (PatrolAttributePatrolType d : deleteAttributes) {
					if (d.getPatrolType().equals(currentType) && d.getPatrolAttribute().equals(a)) {
						tmp.add(d);
					}
				}
				deleteAttributes.removeAll(tmp);
			}
		}
		
		updateAttributeTable();
		if (added) modified();
	}
	
	
	private void toggleAttributeEnabledState() {
		if (tblAttributes.getStructuredSelection().isEmpty()) return;
		if (currentType == null) return;
		Object x = tblAttributes.getStructuredSelection().getFirstElement();
		if (x == null || !(x instanceof PatrolAttributePatrolType)) return;
		PatrolAttributePatrolType link = (PatrolAttributePatrolType)x;
		link.setIsActive(!link.getIsActive());		
		tblAttributes.refresh();
		modified();
	}
	
	private void editAttribute() {
		if (tblAttributes.getStructuredSelection().isEmpty()) return;
		if (currentType == null) return;
		Object x = tblAttributes.getStructuredSelection().getFirstElement();
		
		if (x == null || !(x instanceof PatrolAttributePatrolType)) return;
		
		PatrolAttributePatrolType link = (PatrolAttributePatrolType)x;
		
		PatrolAttribute pa = allAttributes.get(allAttributes.indexOf(link.getPatrolAttribute()));
		
		
		for (PatrolAttributePatrolType type : currentType.getCustomAttributes()) {
			if(type.getPatrolType().equals(currentType)) continue;
			if (type.getPatrolAttribute().equals(pa)) {
				MessageDialog.openInformation(getShell(), DialogConstants.EDIT_BUTTON_TEXT, Messages.TrackTypeDialog_modifywarning);
				break;
			}
		}
		
		
		EditPatrolAttributeDialog dialog = new EditPatrolAttributeDialog(getShell(), pa, allAttributes);
		dialog.open();

		//reload attribute from database and update local cache
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
		if (x == null || !(x instanceof PatrolAttributePatrolType)) return;
		
		PatrolAttributePatrolType pa = (PatrolAttributePatrolType) x;
		if (!MessageDialog.openQuestion(getShell(), DialogConstants.DELETE_BUTTON_TEXT,
				MessageFormat.format(Messages.TrackTypeDialog_deletequestion, 
						getName(pa.getPatrolAttribute()), getName(currentType), getName(currentType)))) {
			return;
		}
		
		PatrolAttributePatrolType patoDelete = null;
		for(PatrolAttributePatrolType i : currentType.getCustomAttributes()) {
			if (i.getPatrolAttribute().equals(pa.getPatrolAttribute()) && i.getPatrolType().equals(currentType)) {
				patoDelete = i;
				break;
			}
		}
		if (patoDelete == null) return;
		
		patoDelete.getPatrolAttribute().getPatrolTypes().remove(patoDelete);
		currentType.getCustomAttributes().remove(patoDelete);
		deleteAttributes.add(patoDelete);
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
		tblTransportGroupTypes.refresh(true);
		
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
				
				cmbActive.setSelection(new StructuredSelection(currentType.getIsActive()));
				
				lblIcon.setImage(lblProvider.getImage(currentType));
				
				updateAttributeTable();
				tblTransportTypes.setInput(currentType.getTransportTypes());
				
				tblTransportGroupTypes.setInput(currentType.getTransportGroups());
			}
			
			detailsPanel.layout(true);
		}finally {
			fireEvents = true;
		}

	}
	
	private void updateAttributeTable() {
		List<PatrolAttributePatrolType> tattributes = new ArrayList<>(currentType.getCustomAttributes());
		tattributes.sort((a,b)->Collator.getInstance().compare(a.getPatrolAttribute().getName(), b.getPatrolAttribute().getName()));
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
		return new Point(900, 500);
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
		if (needsRestart) {
			if (MessageDialog.openQuestion(getShell(), Messages.TrackTypeDialog_restart, Messages.TrackTypeDialog_restartmessage)) {
				PlatformUI.getWorkbench().restart();
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
						tt.setIcon(HibernateManager.saveOrMerge(session, tt.getIcon()));
					}
					for (PatrolTransportGroup g: pt.getTransportGroups()) {
						g.setIcon(HibernateManager.saveOrMerge(session, g.getIcon()));
					}
				}
				session.flush();
				for (PatrolType pt : types) {
					
					pt.setIcon(HibernateManager.saveOrMerge(session, pt.getIcon()));
					
					if (pt.getUuid() == null) session.persist(pt);
					
					session.flush();
					
					for (PatrolTransportGroup tt : pt.getTransportGroups()) {
						if (tt.getUuid() != null) {
							session.merge(tt);
						}else {
							session.persist(tt);
						}
					}
					
					session.flush();
					
					for (PatrolTransportType tt : pt.getTransportTypes()) {
						if (tt.getUuid() != null) {
							session.merge(tt);
						}else {
							session.persist(tt);
						}
					}
					session.flush();
					
					pt = session.merge(pt);
					session.flush();	
				}
				
				//delete any patrol data
				for (PatrolAttributePatrolType delete : deleteAttributes) {
					String hql = "DELETE FROM PatrolAttributeValue WHERE id.patrolAttribute = :attribute and id.patrol.patrolType = :type"; //$NON-NLS-1$
					session.createMutationQuery(hql)
					.setParameter("attribute",  delete.getPatrolAttribute()) //$NON-NLS-1$
					.setParameter("type", delete.getPatrolType()).executeUpdate(); //$NON-NLS-1$			
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
					
					boolean dodelete = MessageDialog.openQuestion(getShell(), LabelConstants.CUSTOM_METADATA_NAME, 
							MessageFormat.format(Messages.TrackTypeDialog_attributenotused, getName(pa)));
					if (dodelete) {
						session.remove(pa);
					}					
				}
				
				session.getTransaction().commit();
				
				deleteAttributes.clear();
				toDelete.clear();
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				
				loadDataJob.schedule();
				
				String newTerm = PatrolDynamicMenuManager.INSTANCE.updateTerm();
				needsRestart = !newTerm.equalsIgnoreCase(menuTerm);
				
				return true;
			}catch (Exception ex) {
				SmartPatrolPlugIn.displayLog(MessageFormat.format(Messages.TrackTypeDialog_saveerror, ex.getMessage()), ex);
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
		column.setText(LabelConstants.TRANSPORT_MODE);
		column.setToolTipText(column.getText());
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
		
		/* Group Speed */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(LabelConstants.ENVIRONMENT_NAME);
		column.setToolTipText(column.getText());
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
			
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolTransportType pt) {
					return lblProvider.getText(pt.getTransportGroup());
				}
				return ""; //$NON-NLS-1$
			}
			
			@Override
			public Image getImage(Object element) {
				return null;
			}
			
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolTransportType pt){
					if (!pt.getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
				
			}
		});
		
		
		viewerColumn.setEditingSupport(new EditingSupport(viewer){
			@Override
			protected CellEditor getCellEditor(Object element) {
				ComboBoxViewerCellEditor ceditor = new ComboBoxViewerCellEditor(viewer.getTable(), SWT.READ_ONLY);
				ceditor.setLabelProvider(lblProvider);
				ceditor.setContentProvider(ArrayContentProvider.getInstance());
				List<Object> all = new ArrayList<>(currentType.getTransportGroups());
				all.add(0, ""); //$NON-NLS-1$
				ceditor.setInput(all);
				return ceditor;
			}

			@Override
			protected boolean canEdit(Object element) {
				return (currentType != null) && element instanceof PatrolTransportType;
			}
			
			@Override
			protected Object getValue(Object element) {
				return ((PatrolTransportType)element).getTransportGroup();
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof PatrolTransportType ttype){
					if (value instanceof PatrolTransportGroup g) {
						ttype.setTransportGroup(g);
					}else {
						ttype.setTransportGroup(null);
					}
					modified();
					viewer.refresh();
				}					
			}});
		
		/* Max Speed */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(Messages.TrackTypeDialog_maxspeedcol);
		column.setToolTipText(column.getText());
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
						MessageDialog.openError(getShell(), INVALID_TYPE_DIALOG_TITLE, MessageFormat.format(Messages.TrackTypeDialog_invalidmaxspeed, PatrolTransportType.MAX_SPEED_MIN_VALUE, PatrolTransportType.MAX_SPEED_MAX_VALUE));
					}
					viewer.refresh();
				}					
			}});
		
		/* Requires Pilot */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(Messages.TrackTypeDialog_pilotrequired);
		column.setToolTipText(column.getText());
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
			
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolTransportType tt){
					if (tt.getRequiresPilot()) return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
					return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
				}
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolTransportType tt){
					if (!tt.getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
				
			}
		});
		final TableViewerColumn vc6 = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			viewer.getTable().setSortColumn(vc6.getColumn());
			int dir = viewer.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			viewer.getTable().setSortDirection(dir);
			
			int change = dir == SWT.DOWN ? -1 : 1;
			currentType.getTransportTypes().sort((a,b)-> change * Boolean.compare(a.getRequiresPilot(), b.getRequiresPilot()));
			viewer.refresh();
		});
		
		/* Active Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(ACTIVE_LABEL);
		column.setToolTipText(column.getText());
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
		column.setToolTipText(column.getText());
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
					
					t.getTransportGroups().forEach(g->{
						Hibernate.initialize(g);
						g.getTransportTypes().size();
						if (g.getIcon() != null) {
							g.getIcon().getFiles().forEach(f->{
								f.computeFileLocation(session);
							});
						}
					});
				});
				
				allAttributes = session.createQuery("FROM PatrolAttribute WHERE conservationArea = :ca", PatrolAttribute.class) //$NON-NLS-1$
						.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
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
