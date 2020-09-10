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
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetMetadataMapping.MetadataType;
import org.wcs.smart.asset.model.mapping.ExifMetadataField;
import org.wcs.smart.hibernate.SmartDB;

import com.drew.metadata.Directory;
import com.drew.metadata.Tag;

/**
 * New Exif mapping panel.
 * 
 * @author Emily
 *
 */
public class NewMappingExif extends AbstractNewMappingComposite{

	private Text txtExifTag;
	private Text txtExifTagTxt;
	
	public NewMappingExif(NewMappingDialog dialog) {
		super(dialog);
	}
	
	@Override
	public String validate() {
		String tag = txtExifTag.getText().trim();
		if (tag.isEmpty()) {
			return Messages.NewMappingExif_TagRequired;
		}
		try {
			Integer.parseInt(tag);
		}catch (Exception ex) {
			return Messages.NewMappingExif_InvalidTag;
		}
		
		return super.validate();
	}
	
	public List<AssetMetadataMapping> getMappings() {
		Integer tagNum = Integer.parseInt(txtExifTag.getText());
		
		AssetMetadataMapping.State state = AssetMetadataMapping.State.ENABLED;
		if (dialog.getEditItem() != null) {
			state = dialog.getEditItem().getState();
		}
		
		if (btnExifSingle.getSelection()) {
			Object x = cmbExifMappingField.getStructuredSelection().getFirstElement();
			if (x instanceof AssetMetadataMapping.AssetProperty) {
				ExifMetadataField field = new ExifMetadataField(tagNum);
				AssetMetadataMapping map = new AssetMetadataMapping();
				map.setConservationArea(SmartDB.getCurrentConservationArea());
				map.setMetadataType(AssetMetadataMapping.MetadataType.EXIF);
				map.setMappedAssetProperty((AssetMetadataMapping.AssetProperty)x);
				map.setMetadataKey(field);
				map.setState(state);
				return Collections.singletonList(map);
			}
		}else if (btnExifMulti.getSelection()) {
			List<AssetMetadataMapping> mappings = new ArrayList<>();
			
			for (MetadataValueMapping m : exifTagValueMappings) {
				ExifMetadataField field = new ExifMetadataField(tagNum, m.tagValue);
				AssetMetadataMapping map = new AssetMetadataMapping();
				map.setConservationArea(SmartDB.getCurrentConservationArea());
				map.setMetadataType(AssetMetadataMapping.MetadataType.EXIF);
				map.setMappedAttribute(m.attribute);
				map.setMappedCategory(m.category);
				map.setMappedListItem(m.listItem);
				map.setMappedTreeNode(m.treeNode);
				map.setMetadataKey(field);
				map.setState(state);
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
		l.setText(Messages.NewMappingExif_HexLabel);
		
		txtExifTag = new Text(panel, SWT.BORDER);
		txtExifTag.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtExifTag.addListener(SWT.Modify, e->modified());
		
		l = new Label(panel, SWT.NONE);
		l.setText(Messages.NewMappingExif_NameLabel);
		
		txtExifTagTxt = new Text(panel, SWT.BORDER);
		txtExifTagTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtExifTagTxt.setEnabled(false);
		
		Link linkSelectFromFile = new Link(panel, SWT.NONE);
		linkSelectFromFile.setText("<a>" + Messages.NewMappingExif_SelectFromFile + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		linkSelectFromFile.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		linkSelectFromFile.addListener(SWT.Selection, e -> selectExifTagFromFile());
		
		if (dialog.getEditItem() != null && dialog.getEditItem().getMetadataType() == MetadataType.EXIF) {
			ExifMetadataField field = (ExifMetadataField)dialog.getEditItem().getMetadataField();
			txtExifTag.setText(String.valueOf(field.getTagType()));
			
			
		}
		
		Composite p = createMappingPanel(panel);
		p.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		return panel;
	}
	
	
	
	private void selectExifTagFromFile() {
		FileDialog fd = new FileDialog(dialog.getShell(), SWT.OPEN);
		String f = fd.open();
		if (f == null) return;
		
		Path p = Paths.get(f);
		if (!Files.exists(p)) {
			MessageDialog.openError(dialog.getShell(), Messages.NewMappingExif_NotFoundTitle, MessageFormat.format(Messages.NewMappingExif_NotfoundMessage, p.toString()));
			return;
		}
		
		HashMap<Directory, List<Tag>> tags = FileMetadataReader.readExifMetadata(p);
		if (tags == null ||  tags.isEmpty()) {
			MessageDialog.openError(dialog.getShell(), Messages.NewMappingExif_Errortitle, MessageFormat.format(Messages.NewMappingExif_ErrorMessage, p.toString()));
			return;
		}
				
		ExifTagSelector selectorDialog = new ExifTagSelector(dialog.getShell(), tags);
		if (selectorDialog.open() != ExifTagSelector.OK) return;
		
		txtExifTag.setData(selectorDialog.getDirectoryTag());
		txtExifTag.setText(String.valueOf(selectorDialog.getDirectoryTag().getTagType()));
		txtExifTagTxt.setText(selectorDialog.getDirectoryTag().getTagName() + " [" + selectorDialog.getDirectory().getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		
		modified();
	}
	
}
