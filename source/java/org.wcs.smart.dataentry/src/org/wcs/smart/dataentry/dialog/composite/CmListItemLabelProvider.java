/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.dataentry.dialog.composite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.util.SmartUtils;

/**
 * Label provider for configurable model list item which looks for
 * alias first, then displays the default tree name.
 * 
 * @author Emily
 *
 */
public class CmListItemLabelProvider extends NamedItemLabelProvider implements IColorProvider{
	
	private List<Image> images = new ArrayList<>();
	
	@Override
	public String getText(Object element) {
		CmAttributeListItem node = getListItem(element);
		if (node != null){
			String label = null;
			if (currentLanguage == null){
				label = node.findNameNull(SmartDB.getCurrentLanguage());
			}else{
				label = node.findNameNull(currentLanguage);
			}
			if (label != null){
				return label;
			}
			return super.getText(node.getListItem());
		}
		return super.getText(element);
	}

	@Override
	public void dispose() {
		super.dispose();
		images.forEach(e->e.dispose());
	}
	
	@Override
	public Image getImage(Object element) {
		CmAttributeListItem node = getListItem(element);
		if (node != null){
			Path f = node.getImageFile();
			if (f == null || !Files.exists(f)) {
				if (node.getListItem().getIcon() == null) return null;
				IconFile icon = node.getListItem().getIcon().getIconFile(node.getConfig().getModel().getIconSet());
				if (icon != null) {
					f = icon.getAttachmentFile();
				}
			}
			if (f == null) return null;
			Image img = SmartUtils.getImage(f, 16);
			if (img != null) {
				images.add(img);
				return img;
			}
		}
		return null;
	}

	private CmAttributeListItem getListItem(Object element){
		if (element instanceof CmAttributeListItem) {
			return (CmAttributeListItem) element;
		}
		return null;
	}
	
	@Override
	public Color getForeground(Object element) {
		CmAttributeListItem node = getListItem(element);
		if (node != null){
			if (!node.getIsActive()){
				return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
		}
		return null;
	}


	@Override
	public Color getBackground(Object element) {
		return null;
	}
}
