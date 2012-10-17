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

import java.util.Collection;

import org.eclipse.jface.dialogs.IInputValidator;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.datamodel.HkeyObject;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite that has a method to add name and key fields.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class NameKeyComposite extends Composite {

	
	protected Text txtName;
	protected Text txtKey;
	
	protected ControlDecoration cdKey;
	protected ControlDecoration cdTxt;
	
	private String originalKey = null;
	
	/**
	 * 
	 */
	public NameKeyComposite(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * Updates the data model object with the values
	 * from the composite fields
	 * @param dmObject object to update
	 * @param defaultLang language being processed
	 */
	protected void updateFields(DmObject dmObject, Language defaultLang){
		dmObject.updateName(defaultLang, txtName.getText().trim());
		dmObject.setKeyId(txtKey.getText());
		if (dmObject instanceof HkeyObject){
			((HkeyObject)dmObject).updateHkey();
		}
	}
	
	/**
	 * Initializes the name and key values with the 
	 * data from the data model object
	 * 
	 * @param dmObject data model object
	 * @param defaultLang language
	 */
	protected void initFields(DmObject dmObject, Language defaultLang){
		if (txtKey != null && dmObject.getKeyId() != null){
			txtKey.setText(dmObject.getKeyId());
			originalKey = dmObject.getKeyId();
		}
		if (txtName != null ){
			txtName.setText(dmObject.findName(defaultLang));
		}
	}
	/**
	 * Creates name and key fields, adding them to the parent.
	 * <p>
	 * Assumption is that the parent layout is 
	 * a grid layout with two columns.</p>
	 * 
	 * @param parent parent composite to add fields to
	 * @param canEdit if fields can be editing
	 * @param createNew <code>true</code> if a new object is being created or <code>false</code> if exisitng object being modified
	 */
	protected void createNameKeyFields(Composite parent, final boolean canEdit, boolean createNew){
		final KeyListener generateKeyListener = new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				String newKey = DataModel.generateKey(txtName.getText(), getSiblings());
				txtKey.setText(newKey);
				if (canEdit){
					validate();
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
			}
		};
		
		/* Name */
		Label lblNewLabel = new Label(parent, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText("Name:");
		
		txtName = new Text(parent, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		if (!canEdit){
			txtName.setEditable(false);
		}else if (createNew){
			txtName.addKeyListener(generateKeyListener);
		}else if (canEdit){
			txtName.addListener(SWT.Modify, new Listener() {
				@Override
				public void handleEvent(Event event) {
					validate();					
				}
			});
		}
		txtName.setTextLimit(1024);
	
		
		/* Key */
		Label lblNewLabel_1 = new Label(parent, SWT.NONE);
		lblNewLabel_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_1.setText("Key:");
		
		txtKey = new Text(parent, SWT.BORDER);
		txtKey.setEditable(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		if (canEdit){			
			cdKey = createDecoration(txtKey);
			cdKey.setDescriptionText("Invalid key.  It must not be blank");
			
			cdTxt = createDecoration(txtName);
			cdTxt.setDescriptionText("Invalid Category Name.  It must not be blank");
			
			txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
			Button btnChangeKey = new Button(parent, SWT.NONE);
			btnChangeKey.setText("Change...");
			btnChangeKey.setToolTipText("Modify the element key.");
			btnChangeKey.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {

					if (!MessageDialog
							.openConfirm(
									getShell(),
									"Set Key",
									"Keys should not be changed unless you understand the implications.  The keys of the data model effect reporting across multiple conservation areas.  Are you sure you want to continue?")) {
						return;
					}
					InputDialog id = new InputDialog(getShell(), "Set Key",
							"Enter the new key values for the category.",
							txtKey.getText(), new IInputValidator() {

								@Override
								public String isValid(String newText) {
									if (originalKey != null && originalKey.equals(newText)){
										//same key
										return "";
									}
									String error = DataModel.validateKey(newText, getSiblings());
									return error;

								}
							});
					int ret = id.open();
					if (ret != Window.CANCEL) {
						txtKey.setText(id.getValue());
						txtName.removeKeyListener(generateKeyListener);
					}
					validate();
				}
			});
		}
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
	
	/**
	 * Validate the name and key fields.  
	 * @return
	 */
	protected boolean validate(){
		boolean error = false;
		
		cdKey.hide();
		cdTxt.hide();
						
		if (!SmartUtils.isSimpleString(txtKey.getText().trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, DmObject.MAX_KEY_LENGTH)){
			cdKey.setDescriptionText("Invalid key.  It must not be blank, and can only contain the characters " + SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
			cdKey.show();
			error = true;
		}
		
		if (!SmartUtils.isSimpleString(txtName.getText().trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, DmObject.MAX_NAME_LENGTH)){
			cdTxt.setDescriptionText("Invalid Category Name.  It must not be blank, and can only contain the characters " + SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
			cdTxt.show();
			error = true;
		}
		return error;
	}
	/**
	 * 
	 * @return the siblings of the current dmobject being modified
	 */
	protected abstract Collection<? extends DmObject> getSiblings();

}

