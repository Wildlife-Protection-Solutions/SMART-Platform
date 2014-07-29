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
package org.wcs.smart.er.ui.surveydesign;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.KeyInputDialog;

/**
 * Survey design composite that contains the name
 * and key fields.
 * 
 * @author Emily
 *
 */
public class NameIdComposite extends SurveyDesignComposite {

	protected Text txtName;
	protected Text txtKey;
	
	protected ControlDecoration cdKey;
	protected ControlDecoration cdTxt;
	
	private List<? extends NamedKeyItem> otherKeys;
	
	final KeyListener generateKeyListener = new KeyListener() {
		@Override
		public void keyReleased(KeyEvent e) {
			String newKey = NamedKeyItem.generateKey(txtName.getText(), otherKeys);
			txtKey.setText(newKey);
			validate();
		}
		
		@Override
		public void keyPressed(KeyEvent e) {
		}
	};
	
	public NameIdComposite(List<? extends NamedKeyItem> otherKeys){
		super();
		this.otherKeys = otherKeys;
	}
	
	@Override
	public Control createControl(final Composite parent) {	
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(3, false));
		
		Label lblNewLabel = new Label(part, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText("Name:");
		
		txtName = new Text(part, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		txtName.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				validate();					
				fireChangeListeners();
			}
		});
		txtName.setTextLimit(org.wcs.smart.ca.Label.MAX_LENGTH);
		
		/* Key */
		Label lblKey = new Label(part, SWT.NONE);
		lblKey.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblKey.setText("Key");
		lblKey.setToolTipText("The key is used for cross conservation area analysis.");
		
		
		txtKey = new Text(part, SWT.BORDER);
		txtKey.setEditable(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		cdKey = createDecoration(txtKey);
		cdKey.setDescriptionText("Invalid Key.  It must not be blank");
			
		cdTxt = createDecoration(txtName);
		cdTxt.setDescriptionText("Invalid Name.  It must not be blank");
			
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Button btnChangeKey = new Button(part, SWT.NONE);
		btnChangeKey.setText("Change");
		btnChangeKey.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {

				if (!MessageDialog
						.openConfirm(
								parent.getShell(),
								"Edit Key",
								"Modifying the keys will affect cross conservation area analysis. Are you sure you want to continue?")) {
					return;
				}
				InputDialog id = new KeyInputDialog(parent.getShell(), 
						txtKey.getText(), otherKeys);
				int ret = id.open();
				if (ret != Window.CANCEL) {
					txtKey.setText(id.getValue());
					txtName.removeKeyListener(generateKeyListener);
				}
				validate();
				fireChangeListeners();
			}
		});
		
		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		return part;
	}

	@Override
	public void init(SurveyDesign design) {
		if (design.getKeyId() == null){
			txtName.addKeyListener(generateKeyListener);
			txtKey.setText(""); //$NON-NLS-1$
		}else{
			txtKey.setText(design.getKeyId());
		}
		
		if (design.getName() != null){
			txtName.setText(design.getName());
		}else{
			txtName.setText(""); //$NON-NLS-1$
		}
		validate();
	}

	@Override
	public void updateDesign(SurveyDesign design) {
		design.updateName(SmartDB.getCurrentLanguage(), txtName.getText());
		design.setName(txtName.getText());
		design.setKeyId(txtKey.getText());
	}

	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	
	public boolean validate(){
		boolean error = false;
	
		String errormsg = NamedKeyItem.validateKey(txtKey.getText(), new ArrayList<DmObject>());
		if (errormsg != null){
			cdKey.setDescriptionText(errormsg);
			cdKey.show();
			error = true;
		}else{
			cdKey.hide();
		}
		
		boolean hide = true;
		errormsg = DataModel.validateName(txtName.getText(), SmartDB.getCurrentLanguage());
		if (errormsg != null){
			cdTxt.setDescriptionText(errormsg);
			cdTxt.show();
			error = true;
			hide = false;
		}
		if (hide){
			cdTxt.hide();
		}
		
		return error;
	}

	@Override
	public boolean isValid() {
		return !validate();
	}
	
	@Override
	public String getTitle(){
		return "Survey Design Name";
	}
	
	@Override
	public String getDescription(){
		return "Enter the name for the survey design.";
	}
}
