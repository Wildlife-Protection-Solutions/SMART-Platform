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
package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.ui.ObservationTreeViewer;
import org.wcs.smart.i2.ui.SmartShellDialog;

/**
 * Shell for displaying the observation details of an intel location.
 * 
 * @author Emily
 *
 */
public class ObservationDetailsShell extends SmartShellDialog{
	
	private IntelLocation location;
	
	private Color bgColor = null;
	private Font boldFont = null;
	
	private boolean blnMouseDown;
	private int xPos, yPos;
	private Label lblId;
	

	public ObservationDetailsShell(Shell ownerShell, IntelLocation location){
		super(ownerShell, SWT.RESIZE);
		this.location = location;

	}
	
	public void createContents(Composite parent){
		Composite owner = new Composite(parent, SWT.NONE);
		owner.setLayout(new GridLayout());
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lblId = new Label(owner, SWT.NONE);
		lblId.setText(location.getId());
		FontData fd = lblId.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.height = fd.height + 1;
		boldFont = new Font(owner.getDisplay(), fd);
		lblId.setFont(boldFont);
		lblId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblId.addListener(SWT.MouseDown, this);
		lblId.addListener(SWT.MouseUp, this);
		lblId.addListener(SWT.MouseMove, this);
		lblId.addListener(SWT.MouseEnter, this);
		lblId.addListener(SWT.MouseExit, this);
		
		
		if (location.getObservations() != null && !location.getObservations().isEmpty()){
			ObservationTreeViewer observationTree = new ObservationTreeViewer(owner, SWT.FULL_SELECTION | SWT.V_SCROLL);
			observationTree.getViewer().getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			observationTree.setInput(location);
			observationTree.expandToLevel(2);
			
		}else{
			Label l = new Label(owner, SWT.NONE);
			l.setText(Messages.ObservationDetailsShell_NoneLabel);
		}
		
		//configure background color
		bgColor = new Color(parent.getDisplay(), 255, 255, 225);
		List<Control> items = new ArrayList<Control>();
		items.add(owner);
		while(!items.isEmpty()){
			Control c = items.remove(0);
			c.setBackground(bgColor);
			if (c instanceof Composite){
				for (Control kid : ((Composite) c).getChildren()){
					items.add(kid);
				}
			}
		}	
	}

	
	public IntelLocation getLocationRecord(){
		return location;
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		
		if (event.type == SWT.Dispose){
			if (bgColor != null){
				bgColor.dispose();
				bgColor = null;
			}
			if (boldFont != null){
				boldFont.dispose();
				boldFont = null;
			}
			return;
		}else if (event.type == SWT.Deactivate){
			close();
			return;
		}else if (event.type == SWT.MouseDown){
			if (event.widget != lblId) return;
			blnMouseDown = true;
			xPos = event.x;
			yPos = event.y;
		} else if (event.type == SWT.MouseUp) {
			if (event.widget != lblId) return;
			blnMouseDown = false;
		} else if (event.type == SWT.MouseMove) {
			if (event.widget != lblId) return;
			if (blnMouseDown) {
				shell.setLocation(shell.getLocation().x + (event.x - xPos),
						shell.getLocation().y + (event.y - yPos));
			}
		} else if (event.type == SWT.MouseEnter) {
			if (event.widget != lblId) return;
			shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_SIZEALL));
		} else if (event.type == SWT.MouseExit) {
			if (event.widget != lblId) return;
			shell.setCursor(null);
		}
	}
	
}
