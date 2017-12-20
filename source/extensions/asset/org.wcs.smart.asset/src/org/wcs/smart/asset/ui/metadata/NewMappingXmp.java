package org.wcs.smart.asset.ui.metadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.mapping.XmpMetadataField;
import org.wcs.smart.hibernate.SmartDB;

public class NewMappingXmp extends AbstractNewMappingComposite{

	private Text txtXmpPath;
	
	public NewMappingXmp(NewMappingDialog dialog) {
		super(dialog);
		
	}
	
	@Override
	public String validate() {
		String tag = txtXmpPath.getText().trim();
		if (tag.isEmpty()) {
			return "Xmp Path field cannot be empty";
		}
		return super.validate();
	}
	
	

	public List<AssetMetadataMapping> getMappings() {
		String path = txtXmpPath.getText().trim();
		
		if (btnExifSingle.getSelection()) {
			Object x = cmbExifMappingField.getStructuredSelection().getFirstElement();
			if (x instanceof AssetMetadataMapping.AssetProperty) {
				XmpMetadataField field = new XmpMetadataField(path);
				AssetMetadataMapping map = new AssetMetadataMapping();
				map.setConservationArea(SmartDB.getCurrentConservationArea());
				map.setMetadataType(AssetMetadataMapping.MetadataType.XMP);
				map.setMappedAssetProperty((AssetMetadataMapping.AssetProperty)x);
				map.setMetadataKey(field);
				return Collections.singletonList(map);
			}
		}else if (btnExifMulti.getSelection()) {
			List<AssetMetadataMapping> mappings = new ArrayList<>();
			
			for (MetadataValueMapping m : exifTagValueMappings) {
				XmpMetadataField field = new XmpMetadataField(path, m.tagValue);
				AssetMetadataMapping map = new AssetMetadataMapping();
				map.setConservationArea(SmartDB.getCurrentConservationArea());
				map.setMetadataType(AssetMetadataMapping.MetadataType.XMP);
				map.setMappedAttribute(m.attribute);
				map.setMappedCategory(m.category);
				map.setMappedListItem(m.listItem);
				map.setMappedTreeNode(m.treeNode);
				map.setMetadataKey(field);
				mappings.add(map);
			}
			return mappings;
		}
		return Collections.emptyList();
	}
	
	@Override
	public Composite createPanel(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		
		Label l = new Label(panel, SWT.NONE);
		l.setText("XMP Path:");
		l.setToolTipText("path seperator is colon (:)");
		
		txtXmpPath = new Text(panel, SWT.BORDER);
		txtXmpPath.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtXmpPath.addListener(SWT.Modify, e->modified());
		
		Link linkSelectFromFile = new Link(panel, SWT.NONE);
		linkSelectFromFile.setText("<a>" + "Select Path From File ..." + "</a>");
		linkSelectFromFile.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		linkSelectFromFile.addListener(SWT.Selection, e -> selectXmpPathFromFile());
		
		Composite p = createMappingPanel(panel);
		p.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		Link linkaddFromFile = new Link(panel, SWT.NONE);
		linkaddFromFile.setText("<a>" + "Add All Values found in Files ..." + "</a>");
		linkaddFromFile.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		linkaddFromFile.addListener(SWT.Selection, e -> selectValuesFromFiles());
		
		return panel;
	}
	
	private void selectValuesFromFiles() {
		FileDialog fd = new FileDialog(dialog.getShell(), SWT.OPEN | SWT.MULTI);
		String f = fd.open();
		if (f == null) return;
		String path = txtXmpPath.getText().trim();
		
		Path p = Paths.get(f);
		Set<String> values = new HashSet<>();
		for (String file : fd.getFileNames()) {
			Path mfile = p.getParent().resolve(file);

			try {
				List<String[]> data = FileMetadataReader.readXmpMetadata(mfile);
				for (String[] i : data) {
					if (i[1] != null && !i[1].trim().isEmpty()) {
						if (i[0].equalsIgnoreCase(path)) {
							values.add(i[1]);
							
						}
					}
				}
			}catch (Exception ex) {
				AssetPlugIn.log(ex.getMessage(), ex);
			}	
		}
		for (String x : values) {
			MetadataValueMapping mapping = new MetadataValueMapping();
			mapping.tagValue = x;
			exifTagValueMappings.add(mapping);
		}
		
		tblExifValueMapping.refresh();		
		modified();
	}
	
	
	private void selectXmpPathFromFile() {
		FileDialog fd = new FileDialog(dialog.getShell(), SWT.OPEN);
		String f = fd.open();
		if (f == null) return;
		
		Path p = Paths.get(f);
		if (!Files.exists(p)) {
			MessageDialog.openError(dialog.getShell(), "Not Found", MessageFormat.format("File {0} not found.", p.toString()));
			return;
		}
		
		List<String[]> data = null;
		try {
			data = FileMetadataReader.readXmpMetadata(p);
		}catch (Exception ex) {
			MessageDialog.openError(dialog.getShell(), "Metadata Error", MessageFormat.format("Could not read xmp metadata from file {0}.", p.toString()));
			return;	
		}
		
		XmpTagSelector selectorDialog = new XmpTagSelector(dialog.getShell(), data);
		if (selectorDialog.open() != ExifTagSelector.OK) return;
		
		txtXmpPath.setText(selectorDialog.getXmpPath());
		
		modified();
	}
	
}
