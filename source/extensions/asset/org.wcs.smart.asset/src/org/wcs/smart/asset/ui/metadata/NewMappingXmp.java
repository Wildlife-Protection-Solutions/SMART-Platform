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
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetMetadataMapping.MetadataType;
import org.wcs.smart.asset.model.mapping.XmpMetadataField;
import org.wcs.smart.hibernate.SmartDB;

/**
 * New XMP mapping page.
 * 
 * @author Emily
 *
 */
public class NewMappingXmp extends AbstractNewMappingComposite{

	private Text txtXmpPath;
	
	public NewMappingXmp(NewMappingDialog dialog) {
		super(dialog);
		
	}
	
	@Override
	public String validate() {
		String tag = txtXmpPath.getText().trim();
		if (tag.isEmpty()) {
			return Messages.NewMappingXmp_Pathrequired;
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
		l.setText(Messages.NewMappingXmp_PathLabel);
		l.setToolTipText(Messages.NewMappingXmp_Pathtooltip);
		
		txtXmpPath = new Text(panel, SWT.BORDER);
		txtXmpPath.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtXmpPath.addListener(SWT.Modify, e->modified());
		
		Link linkSelectFromFile = new Link(panel, SWT.NONE);
		linkSelectFromFile.setText("<a>" + Messages.NewMappingXmp_SelectfromFile + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		linkSelectFromFile.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		linkSelectFromFile.addListener(SWT.Selection, e -> selectXmpPathFromFile());
		
		Composite p = createMappingPanel(panel);
		p.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		Link linkaddFromFile = new Link(panel, SWT.NONE);
		linkaddFromFile.setText("<a>" + Messages.NewMappingXmp_AllValuesOption + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		linkaddFromFile.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		linkaddFromFile.addListener(SWT.Selection, e -> selectValuesFromFiles());
		
		if (dialog.getEditItem() != null && dialog.getEditItem().getMetadataType() == MetadataType.XMP) {
			txtXmpPath.setText( ((XmpMetadataField)dialog.getEditItem().getMetadataField()).getPath() );
		}
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
			MessageDialog.openError(dialog.getShell(), Messages.NewMappingXmp_NotFoundTitle, MessageFormat.format(Messages.NewMappingXmp_NotFoundMsg, p.toString()));
			return;
		}
		
		List<String[]> data = null;
		try {
			data = FileMetadataReader.readXmpMetadata(p);
		}catch (Exception ex) {
			MessageDialog.openError(dialog.getShell(), Messages.NewMappingXmp_ErrorTitle, MessageFormat.format(Messages.NewMappingXmp_ErrorMsg, p.toString()));
			return;	
		}
		
		XmpTagSelector selectorDialog = new XmpTagSelector(dialog.getShell(), data);
		if (selectorDialog.open() != ExifTagSelector.OK) return;
		
		txtXmpPath.setText(selectorDialog.getXmpPath());
		
		modified();
	}
	
}
