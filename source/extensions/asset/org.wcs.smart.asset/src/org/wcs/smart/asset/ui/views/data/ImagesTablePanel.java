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
package org.wcs.smart.asset.ui.views.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.ui.DataDisplaySettings;
import org.wcs.smart.asset.ui.DataDisplaySettings.IconSize;
import org.wcs.smart.asset.ui.ImageGallery;
import org.wcs.smart.common.attachment.ISmartAttachment;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
/**
 * Table of images imported grouped by incident.
 * 
 * @author Emily
 *
 */
public class ImagesTablePanel {
	
	public static final String ICON_SIZE_PREF = DataImporterView.ID + ".iconsize"; //$NON-NLS-1$

	private ImageGallery tblResultsImg;
	
	private DataImportPage view;
	 
	public ImagesTablePanel(Composite parent, DataImportPage view, FormToolkit toolkit) {
		this.view = view;
		createComposite(parent, toolkit);
		
		parent.addListener(SWT.Dispose, e->{
			dispose();
		});
	}
	
	public void dispose() {
		if (tblResultsImg != null && !tblResultsImg.isDisposed()) tblResultsImg.dispose();
		tblResultsImg = null;
		view = null;
	}
	
	public void refresh() {
		tblResultsImg.refresh();
	}
	
	public Control getControl() {
		return this.tblResultsImg;
	}
	
	public void setThumbnailSize(IconSize size) {
		AssetPlugIn.getDefault().getPreferenceStore().setValue(ICON_SIZE_PREF, size.name());
		tblResultsImg.setThumbnailSize(size.getSize());
	}
	
	private void createComposite(Composite parent, FormToolkit toolkit) {
		int iconSize = DataDisplaySettings.IconSize.MEDIUM.getSize();
		try {
			String iconSizePref = AssetPlugIn.getDefault().getPreferenceStore().getString(ICON_SIZE_PREF);
			if (iconSizePref != null) {
				DataDisplaySettings.IconSize icon = DataDisplaySettings.IconSize.valueOf(iconSizePref);
				if (icon != null) iconSize = icon.getSize();
			}
		}catch (Exception ex) {
			//unable to read preference
		}
		
		ImageGallery.IMenuCreator menuCreator = new ImageGallery.IMenuCreator() {
			@Override
			public ContextMenu createMenu() {
				ContextMenu mnu = new ContextMenu();
				
				MenuItem mnuRemoveFile = new MenuItem();
				mnuRemoveFile.setText(Messages.ImagesTablePanel_RemoveFile);
//				mnuRemoveFile.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
				mnuRemoveFile.setOnAction(e->{view.removeFiles(); e.consume();});
				mnu.getItems().add(mnuRemoveFile);
				
				mnu.getItems().add(new SeparatorMenuItem());
								
				MenuItem mnuGroup = new MenuItem();
				mnuGroup.setText(Messages.ImagesTablePanel_CustomGroup);
				mnuGroup.setOnAction(e->{view.groupSelected(); e.consume();});
				mnu.getItems().add(mnuGroup);
				
				MenuItem mnuRemoveGroup = new MenuItem();
				mnuRemoveGroup.setText(Messages.ImagesTablePanel_RemoveGroup);
				mnuRemoveGroup.setOnAction(e->{view.ungroupSelected(); e.consume();});
				mnu.getItems().add(mnuRemoveGroup);
				
				mnu.setOnShowing(e->{
					boolean canRemove = false;
					boolean canGroup = true;
					for (FileProxy fp : view.getSelection()) {
						if (fp.isValid() && fp.isFixed()) {
							canRemove = true;
						}
						if (!fp.isValid()) canGroup = false;
					}
					mnuRemoveGroup.setDisable(!canRemove);
					mnuGroup.setDisable(!canGroup);
					
				});
				return mnu;
			}
		};
		
		tblResultsImg = new ImageGallery(parent, toolkit, menuCreator, view.getProcessor().getFiles(), iconSize, 2)
		{
			@Override
			protected String getThumbColor(ISmartAttachment file) {
				FileProxy proxy = (FileProxy)file;
				if (proxy.getIncidentGroup() == null) return super.getThumbColor(file);
				
				int colorIndex = proxy.getIncidentGroup() % view.getRowColors().length;
				return toHex(view.getRowColors()[colorIndex]);	
			}
			
			@Override
			protected String getMouseOverBackground(ISmartAttachment file) {
				return getThumbColor(file);
			}
			
			@Override
			protected String getSelectionBackgroundColor(ISmartAttachment file) {
				return getThumbColor(file);
			}
		};
		
		tblResultsImg.addListener(SWT.Selection, e->view.setSelection(tblResultsImg.getSelection()));
	}
}
