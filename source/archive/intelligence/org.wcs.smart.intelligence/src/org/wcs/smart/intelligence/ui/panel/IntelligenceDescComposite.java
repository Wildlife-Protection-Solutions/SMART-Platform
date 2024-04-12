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
package org.wcs.smart.intelligence.ui.panel;

import java.text.MessageFormat;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for collecting the intelligence description information
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceDescComposite extends IntelligenceComposite {

	private Label nameLabel;
	
	private Text shortName;
    private Text description;

    private ControlDecoration shortNameDecoration;
    
	/**
	 * @param parent
	 * @param style
	 */
	public IntelligenceDescComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.IntelligenceDesc_Message);
		createControls();
		validate();
	}

	private void createControls() {
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        
        nameLabel = new Label(this, SWT.NONE);
        nameLabel.setText(Messages.IntelligenceDesc_Name_Label);
        nameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        shortName = new Text(this, SWT.BORDER | SWT.LEFT);
        shortName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!isShortNameValid()) {
					shortNameDecoration.show();
				} else {
					shortNameDecoration.hide();
				}
				fireInputChangeListeners();
			}
		});

        GridData data = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        data.horizontalIndent = 8;
        data.widthHint = 170;
        shortName.setLayoutData(data);

		shortNameDecoration = new ControlDecoration(shortName, SWT.LEFT);
		shortNameDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		shortNameDecoration.setShowHover(true);
		shortNameDecoration.setDescriptionText(Messages.IntelligenceDesc_NameRequired_Error);
        
        Label descLabel = new Label(this, SWT.NONE);
        descLabel.setText(Messages.IntelligenceDesc_Description_Label);
        descLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        description = new Text(this, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
        
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd.heightHint = 80;
        gd.widthHint = 50;
        gd.horizontalIndent = 8;

        description.setLayoutData(gd);
        description.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
				fireInputChangeListeners();
			}
		});
        
	}
	
	@Override
	protected void updateModelInternal(Intelligence intelligence) {
    	intelligence.setName(shortName.getText());
    	intelligence.setDescription(description.getText());
	}

	@Override
	public void initFromModel(Intelligence intelligence) {
		String value = intelligence.getName() == null ? "" : intelligence.getName(); //$NON-NLS-1$
		shortName.setText(value);
		value = intelligence.getDescription() == null ? "" : intelligence.getDescription(); //$NON-NLS-1$
		description.setText(value);
	}

	@Override
	protected void validate() {
		if (!isShortNameValid()) {
			setErrorMessage(Messages.IntelligenceDesc_NameRequired_Error);
			return;
		}
		if (!SmartUtils.isSimpleString(shortName.getText(),
						SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX,
						org.wcs.smart.ca.Label.MAX_LENGTH, 0)) {
		
			setErrorMessage(MessageFormat.format(Messages.IntelligenceDescComposite_NameTooLong, new Object[]{org.wcs.smart.ca.Label.MAX_LENGTH,SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
			return;
		}
		if (description.getText().length() > Intelligence.MAX_DESCRIPTION_LENTH){
			setErrorMessage(MessageFormat.format(Messages.IntelligenceDescComposite_DescTooLong, new Object[]{Intelligence.MAX_DESCRIPTION_LENTH}));
			return;
		}
		setErrorMessage(null);
	}
	
	private boolean isShortNameValid() {
    	return shortName != null && shortName.getText() != null && !shortName.getText().isEmpty();
	}
}
