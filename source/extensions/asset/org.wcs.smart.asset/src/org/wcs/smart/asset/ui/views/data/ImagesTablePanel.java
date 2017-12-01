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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.ui.AttachmentTable;
import org.wcs.smart.asset.ui.DataDisplaySettings;
import org.wcs.smart.asset.ui.DataDisplaySettings.IconSize;
import org.wcs.smart.common.attachment.ISmartAttachment;

public class ImagesTablePanel {
	
	public static final String ICON_SIZE_PREF = DataImporterView.ID + ".iconsize";

	private AttachmentTable tblResultsImg;
	private ScrolledComposite scroll;
	
	private DataImportPage view;
	 
	public ImagesTablePanel(Composite parent, DataImportPage view, FormToolkit toolkit) {
		this.view = view;
		createComposite(parent, toolkit);
	}
	
	public void refresh() {
		tblResultsImg.refresh();
		scroll.setMinSize(tblResultsImg.computeSize(scroll.getBounds().width, SWT.DEFAULT));
	}
	
	public Control getControl() {
		return this.scroll;
	}
	
	public void setThumbnailSize(IconSize size) {
		AssetPlugIn.getDefault().getPreferenceStore().setValue(ICON_SIZE_PREF, size.name());
		tblResultsImg.setThumbnailSize(size.getSize());
		scroll.setMinSize(tblResultsImg.computeSize(scroll.getBounds().width, SWT.DEFAULT));
		
	}
	
	private void createComposite(Composite parent, FormToolkit toolkit) {
		scroll = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.BORDER);
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		toolkit.adapt(scroll);
		
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
		
		AttachmentTable.IMenuCreator menuCreator = new AttachmentTable.IMenuCreator() {
			@Override
			public Menu createMenu(AttachmentTable parent) {
				Menu mnu = new Menu(parent);
				
				MenuItem mnuRemoveFile = new MenuItem(mnu, SWT.PUSH);
				mnuRemoveFile.setText("Remove File");
				mnuRemoveFile.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
				mnuRemoveFile.addListener(SWT.Selection, e->view.removeFiles());
				
				new MenuItem(mnu, SWT.SEPARATOR);
				
				MenuItem mnuGroup = new MenuItem(mnu, SWT.PUSH);
				mnuGroup.setText("Create Custom Incident Group...");
				mnuGroup.addListener(SWT.Selection, e->view.groupSelected());
				
				MenuItem mnuRemoveGroup = new MenuItem(mnu, SWT.PUSH);
				mnuRemoveGroup.setText("Remove Custom Incident Group...");
				mnuRemoveGroup.addListener(SWT.Selection, e->view.ungroupSelected());
				
				mnu.addMenuListener(new MenuListener() {
					@Override
					public void menuShown(MenuEvent e) {
						boolean canRemove = false;
						boolean canGroup = true;
						for (FileProxy fp : view.getSelection()) {
							if (fp.isValid() && fp.isFixed()) {
								canRemove = true;
							}
							if (!fp.isValid()) canGroup = false;
						}
						mnuRemoveGroup.setEnabled(canRemove);
						mnuGroup.setEnabled(canGroup);
					}
					
					@Override
					public void menuHidden(MenuEvent e) {}
				});
				return mnu;
			}
		};
		
		tblResultsImg = new AttachmentTable(scroll, toolkit, menuCreator, view.getProcessor().getFileDetails(), iconSize, 10) {
			protected void colorThumb(Composite composite, ISmartAttachment file) {
				FileProxy proxy = (FileProxy)file;
				if (proxy.getIncidentGroup() == null) return;
				int colorIndex = proxy.getIncidentGroup() % view.getRowColors().length;
				composite.setBackground(view.getRowColors()[colorIndex]);
				
				
			}
		};
		
		tblResultsImg.addListener(SWT.Selection, e->view.setSelection(tblResultsImg.getSelection()));		
		scroll.setContent(tblResultsImg);
		scroll.setMinSize(tblResultsImg.computeSize(scroll.getBounds().width, SWT.DEFAULT));
		scroll.addListener(SWT.Resize, e->{
			scroll.setMinSize(tblResultsImg.computeSize(scroll.getBounds().width, SWT.DEFAULT));
		});
	}
}
