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
package org.wcs.smart.patrol.internal.ui.editor;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointAttachment;

/**
 * Cell editor for editing attachments for a given waypoint.
 * @author Emily
 * @since 1.0.0
 */
public class AttachmentCellEditor extends DialogCellEditor{

	
	public AttachmentCellEditor(Composite parent){
		super(parent);
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#openDialogBox(org.eclipse.swt.widgets.Control)
	 */
	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		Waypoint wp = (Waypoint)super.getValue();
		
		AttachmentDialog attd = new AttachmentDialog(cellEditorWindow.getShell(), wp);
		
		if (attd.open() == Window.CANCEL){
			return null;
		}
		
		//Update the attachments
		List<WaypointAttachment> attachments = attd.getAttchments();
		if (wp.getAttachments() == null && attachments.size() > 0){
			wp.setAttachments(new ArrayList<WaypointAttachment>());
		}
		for (Iterator<WaypointAttachment> iterator = wp.getAttachments().iterator(); iterator.hasNext();) {
			WaypointAttachment att = iterator.next();
			if (!attachments.remove(att)){
				iterator.remove();
			}
		}
		
		//add reminaing; these should all be new attachments
		for (Iterator<WaypointAttachment> iterator = attachments.iterator(); iterator.hasNext();) {
			WaypointAttachment att = (WaypointAttachment) iterator.next();
			att.setWaypoint(wp);
			wp.getAttachments().add(att);
		}
		return wp;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.CellEditor#getLayoutData()
	 */
	@Override
	public LayoutData getLayoutData() {
		LayoutData data = super.getLayoutData();
		data.minimumHeight = getDefaultLabel().computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		return data;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#updateContents(java.lang.Object)
	 */
	@Override
	 protected void updateContents(Object value) {
		super.updateContents(value);
		if (value == null){
			return;
		}
		String text = "(None)";
		if ( ((Waypoint)value).getAttachments() != null && ((Waypoint)value).getAttachments().size() > 0){
			text = ((Waypoint)value).getAttachments().size() + " Files";
		}
		getDefaultLabel().setText( text);  
    }
	
}
