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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
import org.wcs.smart.i2.ui.AttributeValueLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.ui.SmartShellDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Shell dialog for displaying details of an intel location.
 * 
 * @author Emily
 *
 */
public class EntityRelationshipDetailsShell extends SmartShellDialog{
	
	private IntelEntityRelationship relationship;
	private Color bgColor = null;
	private Font headerBoldFont = null;
	private Font boldFont = null;
	
	private boolean blnMouseDown;
	private int xPos, yPos;
	private Composite header;
	
	private Composite details;
	private Composite owner;
	private ScrolledComposite scroll;
	
	public EntityRelationshipDetailsShell(Display ownerDisplay, IntelEntityRelationship relationship){
		super(ownerDisplay, SWT.RESIZE);
		this.relationship = relationship;
	}
	
	public void createContents(Composite parent){
		owner = new Composite(parent, SWT.NONE);
		owner.setLayout(new GridLayout());
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		header = new Composite(owner, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label image = new Label(header, SWT.NONE);
		image.setImage(RelationshipTypeLabelProvider.INSTANCE.getImage(relationship.getRelationshipType()));
		image.addListener(SWT.Dispose, (e)->{
			if (!image.getImage().isDisposed()) image.getImage().dispose();
		});
		
		Label lblType = new Label(header, SWT.NONE);
		lblType.setText(relationship.getRelationshipType().getName());
		FontData fd = lblType.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.height = fd.height + 1;
		headerBoldFont = new Font(owner.getDisplay(), fd);
		lblType.setFont(headerBoldFont);
		lblType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		header.addListener(SWT.MouseDown, this);
		header.addListener(SWT.MouseUp, this);
		header.addListener(SWT.MouseMove, this);
		header.addListener(SWT.MouseEnter, this);
		header.addListener(SWT.MouseExit, this);
		
		Label sep = new Label(owner, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite topDetails = new Composite(owner, SWT.NONE);
		topDetails.setLayout(new GridLayout(2, false));
		topDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label ll = new Label(topDetails, SWT.NONE);
		ll.setText("Source Entity:");
		ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		fd = ll.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(owner.getDisplay(), fd);
		ll.setFont(boldFont);
		
		ll = new Label(topDetails, SWT.NONE);
		ll.setText(relationship.getSourceEntity().getIdAttributeAsText());
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ll = new Label(topDetails, SWT.NONE);
		ll.setText("Target Entity:");
		ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		ll.setFont(boldFont);
		
		ll = new Label(topDetails, SWT.NONE);
		ll.setText(relationship.getTargetEntity().getIdAttributeAsText());
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		sep = new Label(owner, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		scroll = new ScrolledComposite(owner, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		details = new Composite(scroll, SWT.NONE);
		details.setLayout(new GridLayout(2, false));
		details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)details.getLayout()).marginWidth = 0;
		((GridLayout)details.getLayout()).marginHeight = 0;
		
		scroll.setContent(details);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		
		initDetails(relationship.getRelationshipType());
		
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
		
		int height = parent.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		if (height > 500) height = 500;
		shell.setSize(400, height);
	}

	private void initDetails(IntelRelationshipType type){
		List<IntelRelationshipTypeAttribute> all = new ArrayList<IntelRelationshipTypeAttribute>();
		all.addAll(type.getAttributes());
		Collections.sort(all, (a, b) -> Collator.getInstance().compare(a.getAttribute().getName(), b.getAttribute().getName()));
		
		for (IntelRelationshipTypeAttribute a : all){
			Label ll = new Label(details, SWT.NONE);
			ll.setText(MessageFormat.format("{0}:", a.getAttribute().getName()));
			ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
			ll.setFont(boldFont);
			
			ll = new Label(details, SWT.NONE);
			for (IntelEntityRelationshipAttributeValue value : relationship.getAttributes()){
				if (value.getAttribute().equals(a.getAttribute())){
					ll.setText(AttributeValueLabelProvider.INSTANCE.getText(value));
					break;
				}
			}
		}
		
		scroll.setMinSize(details.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	public IntelEntityRelationship getRelationship(){
		return relationship;
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		
		if (event.type == SWT.Dispose){
			if (bgColor != null){
				bgColor.dispose();
				bgColor = null;
			}
			if (headerBoldFont != null){
				headerBoldFont.dispose();
				headerBoldFont = null;
			}
			return;
		}else if (event.type == SWT.Deactivate){
			close();
			return;
		}else if (event.type == SWT.MouseDown){
			if (event.widget != header) return;
			blnMouseDown = true;
			xPos = event.x;
			yPos = event.y;
		} else if (event.type == SWT.MouseUp) {
			if (event.widget != header) return;
			blnMouseDown = false;
		} else if (event.type == SWT.MouseMove) {
			if (event.widget != header) return;
			if (blnMouseDown) {
				shell.setLocation(shell.getLocation().x + (event.x - xPos),
						shell.getLocation().y + (event.y - yPos));
			}
		} else if (event.type == SWT.MouseEnter) {
			if (event.widget != header) return;
			shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_SIZEALL));
		} else if (event.type == SWT.MouseExit) {
			if (event.widget != header) return;
			shell.setCursor(null);
		}
	}
	
}
