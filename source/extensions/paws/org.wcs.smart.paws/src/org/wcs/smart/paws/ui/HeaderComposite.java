/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.paws.internal.Messages;


/**
 * A header composite that contains a name and provides the ability
 * to edit it.
 * 
 * 
 * <b>Events:</b> SWT.Selection - the text field of 
 * the event object is set to the new name
 * 
 * @author egouge
 * @since 1.0.0
 */
public class HeaderComposite extends Composite {
	
	private static final int MAX_NAME_LENGTH = 8192;
	
	private Label lblName;
	private Text txtName;
	
	private String name = null;
	
	private boolean cancelled = false;
	
	
	/**
	 * Creates new header composite 
	 * 
	 * @param parent
	 * @param toolkit
	 * @param headerFont
	 * @param headerColor
	 */
	public HeaderComposite(Composite parent, FormToolkit toolkit, Font headerFont, Color headerColor) {
		super(parent, SWT.NONE);
		toolkit.adapt(this);
		SmartUiUtils.setCSSClass(this, SmartUiUtils.FORM_HEADER_CLASS);
		createComposite(headerFont, headerColor, toolkit);
	}

	/**
	 * Sets the widget text
	 * @param text the new text
	 */
	public void setText(String text){
		this.name = text;
		lblName.setText(this.name);
		super.layout();
	}
		
	private void createComposite(Font headerFont, Color headerColor, FormToolkit toolkit) {
		GridLayout gl = new GridLayout(2, false);
		setLayout(gl);
		
		final Composite it = toolkit.createComposite(this);
		it.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		it.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				Point p = lblName.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				lblName.setBounds(0, 0, it.getBounds().width, p.y);
				txtName.setBounds(0, 0, it.getBounds().width, p.y);

			}
		});
		lblName = toolkit.createLabel(it, ""); //$NON-NLS-1$
		txtName = toolkit.createText(it, ""); //$NON-NLS-1$
		
		txtName.setVisible(false);

		lblName.setFont(headerFont);
		txtName.setFont(headerFont);

		lblName.setForeground(headerColor);

		Point p = lblName.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		lblName.setBounds(0, 0, it.getBounds().width, p.y);
		txtName.setBounds(0, 0, it.getBounds().width, p.y);
		
		txtName.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				txtName.setVisible(false);
				lblName.setVisible(true);
				if (!cancelled){
					if (!name.equals(txtName.getText())){
						if (validateName(txtName.getText())){
							name = txtName.getText();
							fireNameChange();
						}
					}
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		txtName.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				cancelled = false;
				if (e.character == SWT.ESC) {
					txtName.setText(name);
					txtName.setVisible(false);
					lblName.setVisible(true);
					cancelled = true;
				} else if (e.character == SWT.CR || e.character == SWT.LF) {
					txtName.setVisible(false);
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});

		lblName.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
				lblName.setVisible(false);
				txtName.setVisible(true);
				txtName.setText(name);
				txtName.selectAll();
				txtName.setFocus();
			}

			@Override
			public void mouseDown(MouseEvent e) {
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}
		});
	}
	
	private boolean validateName(String name){
		if (name.length() > MAX_NAME_LENGTH){
			MessageDialog.openError(getShell(), Messages.HeaderComposite_InvalidNameTitle,
					MessageFormat.format(Messages.HeaderComposite_InvalidNameMsg, MAX_NAME_LENGTH));
			return false;
		}
		return true;
	}
	
	private void fireNameChange(){
		setText(this.name);
		Listener[] listeners = getListeners(SWT.Selection);
		for (int i = 0; i < listeners.length; i ++){
			Event e = new Event();
			e.text = this.name;
			listeners[i].handleEvent(e);
		}
	}
	
	/**
	 * Gets the widget text
	 * @return the widget text
	 */
	public String getText() {
		return this.name;
	}
	
}
