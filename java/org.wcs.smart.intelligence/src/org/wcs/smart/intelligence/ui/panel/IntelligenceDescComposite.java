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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;

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

	/**
	 * @param parent
	 * @param style
	 */
	public IntelligenceDescComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.IntelligenceDesc_Message);
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        
        nameLabel = new Label(this, SWT.NONE);
        nameLabel.setText(Messages.IntelligenceDesc_Name_Label);
        nameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        shortName = new Text(this, SWT.BORDER | SWT.LEFT);
        shortName.setTextLimit(32);
        shortName.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				fireDataValidStateListeners();
			}
		});

        GridData data = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        data.horizontalIndent = 8;
        data.widthHint = 170;
        shortName.setLayoutData(data);

        Label descLabel = new Label(this, SWT.NONE);
        descLabel.setText(Messages.IntelligenceDesc_Description_Label);
        descLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        description = new Text(this, SWT.BORDER | SWT.LEFT| SWT.WRAP | SWT.V_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd.heightHint = 80;
        gd.horizontalIndent = 8;

        description.setLayoutData(gd);
	}
	
	@Override
	public boolean updateModel(Intelligence intelligence) {
    	intelligence.setShortName(shortName.getText());
    	intelligence.setDescription(description.getText());
        return true;
	}

	@Override
	public void initFromModel(Intelligence intelligence) {
		shortName.setText(intelligence.getShortName());
		description.setText(intelligence.getDescription());
	}

	@Override
	public boolean isDataValid() {
    	return shortName != null && shortName.getText() != null && !shortName.getText().isEmpty();
	}
	
	@Override
	public void applyNewMode(CompositeMode state) {
		switch (state) {
		case WIZARD:
			nameLabel.setVisible(true);
			shortName.setVisible(true);
			setMessage(Messages.IntelligenceDesc_Message);
			break;
		case EDITOR:
			nameLabel.setVisible(false);
			shortName.setVisible(false);
			setMessage(Messages.IntelligenceDesc_Message);
			break;
		default:
			break;
		}
		super.applyNewMode(state);
	}
}
