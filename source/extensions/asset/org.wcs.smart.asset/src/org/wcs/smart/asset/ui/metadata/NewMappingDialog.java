package org.wcs.smart.asset.ui.metadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetMetadataMapping.MetadataType;
import org.wcs.smart.asset.model.mapping.IMetadataField;
import org.wcs.smart.asset.model.mapping.XmpMetadataField;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifImageDirectory;
import com.drew.metadata.exif.ExifInteropDirectory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.drew.metadata.exif.GpsDirectory;

public class NewMappingDialog extends TitleAreaDialog {

	private Text txtExifTag;
	private ComboViewer cmbExifDirectory;
	private ComboViewer cmbExifMappingField;
	private ComboViewer cmbType;
	
	private Button btnExifMulti;
	private Button btnExifSingle;
	
	private AssetMetadataMapping newMapping;
	
	private List<ExifValueMapping> exifTagValueMappings;
	private TableViewer tblExifValueMapping;
	
	public NewMappingDialog(Shell parentShell) {
		super(parentShell);
		
		exifTagValueMappings = new ArrayList<>();
	}

	public AssetMetadataMapping getMapping() {
		return newMapping;
	}

	@Override
	protected void okPressed() {
		newMapping = new AssetMetadataMapping();
		newMapping.setConservationArea(SmartDB.getCurrentConservationArea());
		newMapping.setMetadataType((MetadataType) cmbType.getStructuredSelection().getFirstElement());
		
		if (btnExifSingle.getSelection()) {
			Object x = cmbExifMappingField.getStructuredSelection().getFirstElement();
			if (x instanceof AssetMetadataMapping.AssetField) {
				newMapping.setMappedAssetField((AssetMetadataMapping.AssetField)x);
			}
//			newMapping.setMappedAttribute(attribute);
//			newMapping.setMappedCategory(category);
//			newMapping.setMappedListItem(listItem);
//			newMapping.setMappedTreeNode(treeNode);
		}
		
		IMetadataField<?> field = null;
		if (newMapping.getMetadataType() == MetadataType.EXIF) {
			field = new XmpMetadataField(cmbExifDirectory.getCombo().getText().trim(), txtExifTag.getText().trim());
		}else if (newMapping.getMetadataType() == MetadataType.XMP) {
			
		}
		newMapping.setMetadataKey(field);
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
		
		l = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Composite stackPanel = new Composite(main, SWT.BORDER);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackPanel.setLayout(new StackLayout());
		
		Composite exifPanel = createExifPanel(stackPanel);
		Composite xmpPanel = createXmpPanel(stackPanel);
		
		cmbType.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				// TODO Auto-generated method stub
				AssetMetadataMapping.MetadataType type = (AssetMetadataMapping.MetadataType)cmbType.getStructuredSelection().getFirstElement();
				if (type == AssetMetadataMapping.MetadataType.XMP) {
					((StackLayout)stackPanel.getLayout()).topControl = xmpPanel;
				}else if (type == AssetMetadataMapping.MetadataType.EXIF) {
					((StackLayout)stackPanel.getLayout()).topControl = exifPanel;
				}
				stackPanel.layout();
			}
		});
		cmbType.setSelection(new StructuredSelection(AssetMetadataMapping.MetadataType.EXIF));
		return parent;
	}
	
	private Composite createXmpPanel(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		Label l = new Label(panel, SWT.NONE);
		l.setText("TODO:");
		return panel;
	}
	
	private Composite createExifPanel(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		
		Label l = new Label(panel, SWT.NONE);
		l.setText("EXIF Directory:");
		
		cmbExifDirectory = new ComboViewer(panel, SWT.DROP_DOWN);
		cmbExifDirectory.setLabelProvider(new LabelProvider());
		cmbExifDirectory.setContentProvider(ArrayContentProvider.getInstance());
		cmbExifDirectory.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbExifDirectory.setInput(getDirectoryNames());
		cmbExifDirectory.getControl().addListener(SWT.Modify, e->modified());
		cmbExifDirectory.getControl().addListener(SWT.Selection, e->modified());
		
		l = new Label(panel, SWT.NONE);
		l.setText("EXIF Tag:");
		
		txtExifTag = new Text(panel, SWT.BORDER);
		txtExifTag.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtExifTag.addListener(SWT.Modify, e->modified());
		
		Link linkSelectFromFile = new Link(panel, SWT.NONE);
		linkSelectFromFile.setText("<a>" + "Select Directory/Tag From File ..." + "</a>");
		linkSelectFromFile.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		linkSelectFromFile.addListener(SWT.Selection, e -> selectExifTagFromFile());
		
		btnExifSingle = new Button(panel, SWT.RADIO);
		btnExifSingle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		btnExifSingle.setText("Single Mapping");
		btnExifSingle.setText("Map tag to field");
		
		cmbExifMappingField = new ComboViewer(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbExifMappingField.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)cmbExifMappingField.getControl().getLayoutData()).horizontalIndent = 30;
		cmbExifMappingField.setContentProvider(ArrayContentProvider.getInstance());
		cmbExifMappingField.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetMetadataMapping.AssetField) return ((AssetMetadataMapping.AssetField) element).name();
				return super.getText(element);
			}
		});
		cmbExifMappingField.addSelectionChangedListener(e->modified());
		cmbExifMappingField.setInput( AssetMetadataMapping.AssetField.values());
		
		
		btnExifMulti = new Button(panel, SWT.RADIO);
		btnExifMulti.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		btnExifMulti.setText("Map to data model");
		btnExifMulti.setToolTipText("Map individual tag values to specific data model elements");

		Composite valuePart = new Composite(panel, SWT.NONE);
		valuePart.setLayout(new GridLayout(2, false));
		valuePart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		tblExifValueMapping = new TableViewer(valuePart, SWT.BORDER);
		tblExifValueMapping.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tblExifValueMapping.getControl().getLayoutData()).horizontalIndent = 30;
		tblExifValueMapping.setContentProvider(ArrayContentProvider.getInstance());
		tblExifValueMapping.getTable().setLinesVisible(true);
		tblExifValueMapping.getTable().setHeaderVisible(true);
		
		TableViewerColumn colTag = new TableViewerColumn(tblExifValueMapping, SWT.NONE);
		colTag.getColumn().setText("Tag Value");
		colTag.getColumn().setWidth(100);
		colTag.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ExifValueMapping) return ((ExifValueMapping) element).tagValue;
				return super.getText(element);
			}
		});
		
		TableViewerColumn colMapping = new TableViewerColumn(tblExifValueMapping, SWT.NONE);
		colMapping.getColumn().setText("Data Model Mapping");
		colMapping.getColumn().setWidth(100);
		colMapping.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ExifValueMapping) {
					ExifValueMapping v = (ExifValueMapping)element;
					StringBuilder sb = new StringBuilder();
					if (v.treeNode != null) sb.append(v.treeNode.getName());
					if (v.listItem != null) sb.append(v.listItem.getName());
					if (v.attribute != null) {
						if (sb.length() != 0) sb.append( " - ");
						sb.append(v.attribute.getName());
					}
					if (v.category != null) {
						if (sb.length() != 0) sb.append( " - ");
						sb.append(v.category.getFullCategoryName());
					}
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		tblExifValueMapping.setInput(exifTagValueMappings);
		tblExifValueMapping.getTable().setEnabled(false);
		
		Composite btnPanel = new Composite(valuePart, SWT.NONE);
		btnPanel.setLayout(new GridLayout());
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnAdd = new Button(btnPanel, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnRemove = new Button(btnPanel, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.addListener(SWT.Selection, e->removeExifValueMappings());
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Listener listener = e->{
			cmbExifMappingField.getControl().setEnabled(btnExifSingle.getSelection());
			tblExifValueMapping.getControl().setEnabled(!btnExifSingle.getSelection());
			btnAdd.setEnabled(!btnExifSingle.getSelection());
			btnRemove.setEnabled(!btnExifSingle.getSelection());
		};
		btnExifSingle.addListener(SWT.Selection, listener);
		btnExifMulti.addListener(SWT.Selection, listener);

		btnExifSingle.setSelection(true);
		
		return panel;
	}

	private void removeExifValueMappings() {
		List<ExifValueMapping> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = tblExifValueMapping.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object x = (Object)iterator.next();
			if (x instanceof ExifValueMapping) toDelete.add((ExifValueMapping) x);
		}
		this.exifTagValueMappings.removeAll(toDelete);
	}
	
	private void selectExifTagFromFile() {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
		String f = fd.open();
		if (f == null) return;
		
		Path p = Paths.get(f);
		if (!Files.exists(p)) {
			MessageDialog.openError(getShell(), "Not Found", MessageFormat.format("File {0} not found.", p.toString()));
			return;
		}
		
		HashMap<String, List<String[]>> tags = FileMetadataReader.readExifMetadata(p);
		if (tags == null ||  tags.isEmpty()) {
			MessageDialog.openError(getShell(), "Metadata Error", MessageFormat.format("Could not read exif metadata from file {0}.", p.toString()));
			return;
		}
				
		ExifTagSelector dialog = new ExifTagSelector(getShell(), tags);
		if (dialog.open() != ExifTagSelector.OK) return;
		txtExifTag.setText(dialog.getDirectoryTag()[1]);
		cmbExifDirectory.getCombo().setText(dialog.getDirectoryTag()[0]);
		modified();
		
	}
	
	public void modified() {
		//TODO:
		String message = null;
		
		if (cmbType.getStructuredSelection().getFirstElement() == AssetMetadataMapping.MetadataType.EXIF) {
			String directory = cmbExifDirectory.getCombo().getText().trim();
			if (directory.isEmpty()) {
				message = "EXIF directory field cannot be empty";
			}
			String tag = txtExifTag.getText().trim();
			if (tag.isEmpty()) {
				message ="EXIF tag field cannot be empty";
			}
			if (btnExifSingle.getSelection()) {
				Object mappedTo = cmbExifMappingField.getStructuredSelection().getFirstElement();
				if (!(mappedTo instanceof AssetMetadataMapping.AssetField)) {
					message = "EXIF mapping to value must be selected";
				}
			}
			
		}else if (cmbType.getStructuredSelection().getFirstElement() == AssetMetadataMapping.MetadataType.XMP) {
			
		}
		setErrorMessage(message);
		if (getButton(IDialogConstants.OK_ID) != null) getButton(IDialogConstants.OK_ID).setEnabled(message == null);
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private static String[] getDirectoryNames() {
		//TODO: we could add more here
		return new String[] {
				(new GpsDirectory()).getName(),
				new ExifIFD0Directory().getName(),
				new ExifImageDirectory().getName(),
				new ExifInteropDirectory().getName(),
				new ExifSubIFDDirectory().getName(),
				new ExifThumbnailDirectory().getName()	
		};
	}
	
	private class ExifValueMapping{
		String tagValue;
		Category category;
		Attribute attribute;
		AttributeListItem listItem;
		AttributeTreeNode treeNode;
	}
}