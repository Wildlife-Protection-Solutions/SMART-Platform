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
package org.wcs.smart.er.ui.mision;

import java.text.MessageFormat;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

/**
 * Mission id composite.
 * @author Emily
 *
 */
public class IdComposite extends MissionComposite{

	protected Text txtName;
	protected ControlDecoration cdTxt;
	
	public IdComposite(){
		super();
	}
	
	@Override
	public Control createControl(final Composite parent) {	
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(3, false));
		
		Label lblNewLabel = new Label(part, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText(Messages.IdComposite_IdLabel);
		
		txtName = new Text(part, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		txtName.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				validate();					
				fireChangeListeners();
			}
		});
		txtName.setTextLimit(Mission.MAX_LENGTH_ID);
		cdTxt = createDecoration(txtName);
		
		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		return part;
	}

	@Override
	public void init(Mission mission, Session session) {
		if (mission.getId() != null){
			txtName.setText(mission.getId());
		}else{
			txtName.setText(""); //$NON-NLS-1$
		}
		validate();
	}

	@Override
	public void updateDesign(Mission mission) {
		mission.setId(txtName.getText());
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
	
		String name = txtName.getText();
		if (!SmartUtils.isSimpleString(name, RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Mission.MAX_LENGTH_ID)){
			error = true;
			cdTxt.setDescriptionText(MessageFormat.format(Messages.IdComposite_IdError, new Object[]{Mission.MAX_LENGTH_ID, RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
			cdTxt.show();
		}else{
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
		return Messages.IdComposite_Title;
	}
	
	@Override
	public String getDescription(){
		return Messages.IdComposite_Description;
	}
}
