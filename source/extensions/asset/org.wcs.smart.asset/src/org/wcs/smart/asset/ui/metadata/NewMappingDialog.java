package org.wcs.smart.asset.ui.metadata;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

public class NewMappingDialog extends TitleAreaDialog {

	private ComboViewer cmbType;
	
	private List<AssetMetadataMapping> mappings;
	
	private NewMappingExif exifPanel; 
	
	private DataModel cachedDm;
	
	public NewMappingDialog(Shell parentShell) {
		super(parentShell);
	}

	public List<AssetMetadataMapping> getMappings() {
		return mappings;
	}

	public DataModel getCachedDataModel() {
		if (cachedDm == null) {
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			try {
				pmd.run(true, false,  new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask("Loading Data Model", 2);
						
						try(Session session = HibernateManager.openSession()){
							List<Category> cats = QueryFactory.buildQuery(session, Category.class, 
									new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
									new Object[] {"parent", null}).list();
							
							List<Category> toVisit = new ArrayList<>();
							toVisit.addAll(cats);
							while(!toVisit.isEmpty()) {
								Category c = toVisit.remove(0);
								c.getName();
								c.getAttributes().forEach(e->e.getAttribute().getName());
								toVisit.addAll(c.getChildren());
							}
							monitor.worked(1);
							
							List<Attribute> atts = QueryFactory.buildQuery(session, Attribute.class,
									new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list();
							List<AttributeTreeNode> toVisit2 = new ArrayList<>();
							for (Attribute a : atts) {
								a.getName();
								if (a.getAttributeList() != null) a.getAttributeList().forEach(b->b.getName());
								if (a.getTree() != null) toVisit2.addAll(a.getTree());
							}
							while(!toVisit2.isEmpty()) {
								AttributeTreeNode n = toVisit2.remove(0);
								n.getName();
								toVisit2.addAll(n.getChildren());
							}
							monitor.worked(1);
							cachedDm = new DataModel(SmartDB.getCurrentConservationArea(), cats, atts);
						}
					}
				});
			}catch (Exception ex) {
				AssetPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		return this.cachedDm;
	}
	
	@Override
	protected void okPressed() {
//		newMapping = new AssetMetadataMapping();
//		newMapping.setConservationArea(SmartDB.getCurrentConservationArea());
//		newMapping.setMetadataType((MetadataType) cmbType.getStructuredSelection().getFirstElement());
//		
		//TODO:
		if (cmbType.getStructuredSelection().getFirstElement() == (AssetMetadataMapping.MetadataType.EXIF)) {
			mappings = exifPanel.getMappings();
		}
		
//		if (btnExifSingle.getSelection()) {
//			Object x = cmbExifMappingField.getStructuredSelection().getFirstElement();
//			if (x instanceof AssetMetadataMapping.AssetField) {
//				newMapping.setMappedAssetField((AssetMetadataMapping.AssetField)x);
//			}
////			newMapping.setMappedAttribute(attribute);
////			newMapping.setMappedCategory(category);
////			newMapping.setMappedListItem(listItem);
////			newMapping.setMappedTreeNode(treeNode);
//		}
//		
//		IMetadataField<?> field = null;
//		if (newMapping.getMetadataType() == MetadataType.EXIF) {
//			field = new XmpMetadataField(cmbExifDirectory.getCombo().getText().trim(), txtExifTag.getText().trim());
//		}else if (newMapping.getMetadataType() == MetadataType.XMP) {
//			
//		}
//		newMapping.setMetadataKey(field);
		super.okPressed();
	}
	
	@Override
	protected void cancelPressed(){
		super.cancelPressed();
	}

	protected void createButtonsForButtonBar(Composite parent) {
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		btnOk.setEnabled(false);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite header = new Composite(main, SWT.BORDER);
		header.setLayout(new GridLayout(2, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(header, SWT.NONE);
		l.setText("Metadata Type:");
		
		cmbType = new ComboViewer(header, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetMetadataMapping.MetadataType) return ((AssetMetadataMapping.MetadataType) element).name();
				return super.getText(element);
			}
		});
		cmbType.setInput(AssetMetadataMapping.MetadataType.values());
		
//		l = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
//		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Composite stackPanel = new Composite(main, SWT.BORDER);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackPanel.setLayout(new StackLayout());
		
		exifPanel = new NewMappingExif(this);
		Composite exifPanelComposite = exifPanel.createExifPanel(stackPanel);
		Composite xmpPanel = createXmpPanel(stackPanel);
		
		cmbType.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				// TODO Auto-generated method stub
				AssetMetadataMapping.MetadataType type = (AssetMetadataMapping.MetadataType)cmbType.getStructuredSelection().getFirstElement();
				if (type == AssetMetadataMapping.MetadataType.XMP) {
					((StackLayout)stackPanel.getLayout()).topControl = xmpPanel;
				}else if (type == AssetMetadataMapping.MetadataType.EXIF) {
					((StackLayout)stackPanel.getLayout()).topControl = exifPanelComposite;
				}
				stackPanel.layout();
			}
		});
		cmbType.setSelection(new StructuredSelection(AssetMetadataMapping.MetadataType.EXIF));
		
		setTitle("Asset Metadata Mapping");
		getShell().setText("Asset Metadata Mapping");
		setMessage("Configure file metadata mappings.");
		return parent;
	}
	
	private Composite createXmpPanel(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		Label l = new Label(panel, SWT.NONE);
		l.setText("TODO:");
		return panel;
	}
	
	
	
	public void modified() {
		//TODO:
		String message = null;
		if (cmbType.getStructuredSelection().getFirstElement() == AssetMetadataMapping.MetadataType.EXIF) {
			message = exifPanel.validate();
		}else if (cmbType.getStructuredSelection().getFirstElement() == AssetMetadataMapping.MetadataType.XMP) {
			
		}
		setErrorMessage(message);
		if (getButton(IDialogConstants.OK_ID) != null) getButton(IDialogConstants.OK_ID).setEnabled(message == null);
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
}