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
package org.wcs.smart.query.ui;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.query.internal.Messages;

/**
 * A header composite that contains a
 * query name and id information allowing users to modify the 
 * query name.
 * 
 * <b>Events:</b> SWT.Selection - the text field of 
 * the event object is set to the new name
 * 
 * @author egouge
 * @since 1.0.0
 */
public class QueryHeaderComposite extends Composite {

	private static final String ID_LABEL = Messages.QueryHeaderComposite_IdFieldLabel;

	private static final int MAX_NAME_LENGTH = 1024;
	
	private Label lblName;
	private Text txtName;
	
	private Label lblId;
	private Label lblType;
	
	private String name = null;
	private String id = null;
	private String type = null;
	
	private boolean cancelled = false;
	/**
	 * 
	 */
	public QueryHeaderComposite(Composite parent, FormToolkit toolkit, Font headerFont, Color headerColor) {
		
		super(parent, SWT.NONE);
		toolkit.adapt(this);
		createComposite(headerFont, headerColor, toolkit);
	}

	public void setText(String text, String id, String type){
		this.name = text;
		this.id = id;
		this.type = type;
		lblName.setText(this.name);
		lblType.setText(type);
		if (this.id != null){
			lblId.setText(ID_LABEL + this.id);
		}else{
			lblId.setText(ID_LABEL);
		}
		super.layout();
	}
	
	
	private void createComposite(Font headerFont, Color headerColor, FormToolkit toolkit) {
		GridLayout gl = new GridLayout(3, false);
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		setLayout(gl);
		lblType = toolkit.createLabel(this, ""); //$NON-NLS-1$
		lblType.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false, 3, 1));
		FontData[] fd = lblType.getFont().getFontData();
		fd[0].setHeight(fd[0].getHeight()-1);
		final Font f = new Font(lblType.getDisplay(), fd[0]);
		lblType.setFont(f);
		lblType.addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				f.dispose();
			}
		});
		Label lblSummary = toolkit.createLabel(this, Messages.QueryHeaderComposite_QueryNameLabel);
		lblSummary.setForeground(headerColor);
		lblSummary.setFont(headerFont);
		
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
		
		lblId = toolkit.createLabel(this, ""); //$NON-NLS-1$
		lblId.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		txtName.setVisible(false);

		lblId.setFont(headerFont);
		lblName.setFont(headerFont);
		txtName.setFont(headerFont);

		lblId.setForeground(headerColor);
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
				if (e.keyCode == SWT.ESC) {
					txtName.setText(name);
					txtName.setVisible(false);
					lblName.setVisible(true);
					cancelled = true;
				} else if (e.keyCode == SWT.CR) {
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
			MessageDialog.openError(getShell(), Messages.QueryHeaderComposite_NameToLongDialogTitle,
					MessageFormat.format(Messages.QueryHeaderComposite_NameToLongMsg, new Object[]{MAX_NAME_LENGTH}));
			return false;
		}
		return true;
	}
	
	private void fireNameChange(){
		setText(this.name, this.id, this.type);
		Listener[] listeners = getListeners(SWT.Selection);
		for (int i = 0; i < listeners.length; i ++){
			Event e = new Event();
			e.text = this.name;
			listeners[i].handleEvent(e);
		}
	}
}
