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
package org.wcs.smart.ui.internal.ca.properties;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Icon panel for adding icons to data model items
 * 
 * @author Emily
 *
 */
public class IconPanel extends Composite {
	
	private static final int ICON_SIZE = 50;

	private boolean canEdit = false;
	private Icon newIcon = null;
	
	public IconPanel(Composite parent, boolean canEdit) {
		super(parent, SWT.NONE);
		this.canEdit = canEdit;
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;	
	}
	
	private void fireSelectionListeners() {
		Event evnt = new Event();
		for (Listener l : getListeners(SWT.Selection)) {
			l.handleEvent(evnt);
		}
	}

	public void setDmObject(DmObject object) {
		this.newIcon = object.getIcon();
		updateIcon(object.getIcon());
	}
	
	private void updateIcon(Icon icon) {
		for (Control c : getChildren()) c.dispose();
		if (icon == null) {
			Composite c = new Composite(this, SWT.NONE);
			c.setBackground(getBackground());
			c.setLayout(new GridLayout(2, false));
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)c.getLayout()).marginWidth = 0;
			((GridLayout)c.getLayout()).marginHeight = 0;
			
			Label l = new Label(c, SWT.NONE);
			l.setText(Messages.IconPanel_NoIconLabel);
			l.setBackground(getBackground());
			if (canEdit) {
				Link lnk = new Link(c, SWT.NONE);
				lnk.setText("<a>" + DialogConstants.EDIT_LINK_TEXT + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
				lnk.addListener(SWT.Selection, e->editIcons());
				lnk.setBackground(getBackground());
			}
			
			//spacer for images
			Composite images = new Composite(this, SWT.NONE);
			images.setBackground(getBackground());
			images.setLayout(new GridLayout());
			images.setLayoutData(new GridData());
			((GridData)images.getLayoutData()).heightHint = ICON_SIZE;
		}else {
			
			Composite c = new Composite(this, SWT.NONE);
			c.setLayout(new GridLayout(2, false));
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)c.getLayout()).marginWidth = 0;
			((GridLayout)c.getLayout()).marginHeight = 0;
			c.setBackground(getBackground());
			
			Label l = new Label(c, SWT.NONE);
			l.setText(icon.getName() == null ? "" : icon.getName()); //$NON-NLS-1$
			l.setBackground(getBackground());
			
			if (canEdit) {
				Composite cc = new Composite(c, SWT.NONE);
				cc.setLayout(new GridLayout(2, false));
				((GridLayout)cc.getLayout()).marginWidth = 0;
				((GridLayout)cc.getLayout()).marginHeight = 0;
				cc.setBackground(getBackground());
				
				Link lnk = new Link(cc, SWT.NONE);
				lnk.setText("<a>" + DialogConstants.EDIT_LINK_TEXT + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
				lnk.addListener(SWT.Selection, e->editIcons());
				lnk.setBackground(getBackground());
				
				lnk = new Link(cc, SWT.NONE);
				lnk.setText("<a>" + Messages.IconPanel_clearLabel + "</a>");  //$NON-NLS-1$ //$NON-NLS-2$
				lnk.addListener(SWT.Selection, e->clearIcon());
				lnk.setBackground(getBackground());
			}
			
			Composite images = new Composite(this, SWT.NONE);
			images.setLayout(new RowLayout());
			((RowLayout)images.getLayout()).spacing = 10;
			((RowLayout)images.getLayout()).wrap = true;
			images.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			((GridData)images.getLayoutData()).widthHint = images.getClientArea().width;
			images.setBackground(getBackground());

			for (IconFile file : icon.getFiles()) {
				
				Composite temp = new Composite(images, SWT.NONE);
				temp.setLayout(new GridLayout());
				((GridLayout)temp.getLayout()).marginWidth = 0;
				((GridLayout)temp.getLayout()).marginHeight = 0;
				temp.setBackground(getBackground());
				
				Thumbnail t = new Thumbnail(file, ICON_SIZE, true);
				t.setImageName(icon.getName());
				Composite cc = t.createThumbnail(temp, 0, SWT.NONE, false);
				cc.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
				((GridData)cc.getLayoutData()).widthHint = ICON_SIZE;
				((GridData)cc.getLayoutData()).heightHint = ICON_SIZE;
				cc.setBackground(getBackground());
				
				l = new Label(temp, SWT.NONE);
				l.setText(file.getIconSet().getName());		
				l.setBackground(getBackground());
			}
		}
		this.layout(true);
	}
	
	/**
	 * Updates the datamodel icon 
	 * @param object
	 */
	public void updateDmObject(DmObject object) {
		if(newIcon != null) {
			object.setIcon(newIcon);
		}else {
			object.setIcon(null);
		}
	}
	
	private void clearIcon() {
		this.newIcon = null;
		updateIcon(newIcon);
		fireSelectionListeners();
	}
	
	private void editIcons() {
		IconSelectionDialog dialog = new IconSelectionDialog(getShell(), IconSelectionDialog.Type.SELECT);
		if (dialog.open() != Window.OK) return;

		this.newIcon = dialog.getSelectedIcon();
		updateIcon(newIcon);
		fireSelectionListeners();
	}
}
