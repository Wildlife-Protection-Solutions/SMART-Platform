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
package org.wcs.smart.plan.ui.panel;

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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for editing plan information
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanIdNameDescComposite extends PlanComposite {

	private Label nameLabel;
	private Label idLabel;
	
	private Text name;
	private Text id;
    private Text description;

    private ControlDecoration idDecoration;
    
	/**
	 * @param parent
	 * @param style
	 */
	public PlanIdNameDescComposite(Composite parent, int style) {
		super(parent, style);
		setMessage("Edit Plan Id");
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        
        idLabel = new Label(this, SWT.NONE);
        idLabel.setText("Plan ID:");
        idLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        id = new Text(this, SWT.BORDER | SWT.LEFT);
        id.setTextLimit(32);

        id.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!isIdValid()) {
					idDecoration.show();
				} else {
					idDecoration.hide();
				}
				fireDataValidStateListeners();
				fireInputChangeListeners();
			}
		});
        GridData idgd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        idgd.horizontalIndent = 8;
        idgd.widthHint = 170;
        id.setLayoutData(idgd);
                
        nameLabel = new Label(this, SWT.NONE);
        nameLabel.setText("Plan Name:");
        nameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        name = new Text(this, SWT.BORDER | SWT.LEFT);
        name.setTextLimit(32);
        name.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				fireInputChangeListeners();
			}
		});
        
        GridData data = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        data.horizontalIndent = 8;
        data.widthHint = 170;
        name.setLayoutData(data);
        
        Label descLabel = new Label(this, SWT.NONE);
        descLabel.setText("Description:");
        descLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        description = new Text(this, SWT.BORDER | SWT.LEFT| SWT.WRAP | SWT.V_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd.heightHint = 80;
        gd.horizontalIndent = 8;

        description.setLayoutData(gd);
        description.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				fireInputChangeListeners();
			}
		});
        
        idDecoration = new ControlDecoration(id, SWT.LEFT);
        idDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        idDecoration.setShowHover(true);
        idDecoration.setDescriptionText("Invalid Name");
        
	}
	
	@Override
	public boolean updateModel(Plan plan) {
		plan.setId(id.getText());
    	plan.setName(name.getText());
    	plan.setDescription(description.getText());
        return true;
	}

	@Override
	public void initFromModel(Plan plan) {
		if(plan.getName() != null){
			name.setText(plan.getName());
		}
		if(plan.getId() != null){
			id.setText(plan.getId());
		}
		if(plan.getDescription() != null){
			description.setText(plan.getDescription());
		}
	}

	@Override
	public boolean isDataValid() {
		return isIdValid();
	}
	
	private boolean isIdValid() {
		boolean idIsSimple = SmartUtils.isSimpleString(id.getText(),
				SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX,
				32, 2);

		if(PlanHibernateManager.isDuplicatePlanId( HibernateManager.openSession(), id.getText(), true)){
			idDecoration.show();
			idDecoration.setDescriptionText("Plan Id is already in the database, choose a unique ID");
//			setPageComplete(false);
			return false;
		}else if(id.getText() == null || !idIsSimple){
			idDecoration.show();
			idDecoration.setDescriptionText("Plan ID Cannot contain characters other than a-Z 0-9 _ : & ' and spaces");
//			setPageComplete(false);
			return false;
		}else{
//			setPageComplete(true);
		}
    	return true;
	}
}
