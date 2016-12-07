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
package org.wcs.smart.ui.internal.ca;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite that contains conservation 
 * area properties including name, description,
 * designation etc.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CaInfoComposite extends Composite {

	private Text txtIdentifier;
	private Text txtName;
	private Text txtDescription;
	private Text txtDesignation;
	private Text txtOrganization;
	private Text txtPointOfContact;
	private Text txtCountry;
	private Text txtOwner;
	
	private ControlDecoration cdIdentifier;
	private ControlDecoration cdName;
	
	private List<IValidationListener> listeners = new ArrayList<IValidationListener>();
	private List<IChangeListener> changeListeners = new ArrayList<IChangeListener>();
	private boolean isValid = false;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public CaInfoComposite(Composite parent, int style, ConservationArea defaults) {
		super(parent, style);

		KeyAdapter validator = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				validate();
			}
		};
		
		KeyAdapter changeListener = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				fireChangeListeners();
			}
		};
		
		setLayout(new GridLayout(2, false));

		Label lblIdentifier = new Label(this, SWT.NONE);
		GridData data = new GridData(SWT.RIGHT, SWT.CENTER, false,false, 1, 1);
		lblIdentifier.setLayoutData(data);
		lblIdentifier.setText(Messages.CaInfoComposite_IdLabel);
		lblIdentifier.setToolTipText(Messages.CaInfoComposite_Id_tooltip);
		
		int indent = 8;
		
		txtIdentifier = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true,false, 1, 1);
		data.horizontalIndent = indent;
		txtIdentifier.setLayoutData(data);
		txtIdentifier.addKeyListener(validator);
		txtIdentifier.addKeyListener(changeListener);
		txtIdentifier.setTextLimit(ConservationArea.MAX_ID_LENGTH);

		Label lblName = new Label(this, SWT.NONE);
		lblName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,1, 1));
		lblName.setText(Messages.CaInfoComposite_NameLabel);
		lblName.setToolTipText(Messages.CaInfoComposite_Name_Tooltip);

		
		txtName = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true,false, 1, 1);
		data.horizontalIndent = indent;
		txtName.setLayoutData(data);
		txtName.addKeyListener(validator);
		txtName.addKeyListener(changeListener);
		txtName.setTextLimit(ConservationArea.MAX_NAME_LENGTH);
		
		Label lblDescription = new Label(this, SWT.NONE);
		lblDescription.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblDescription.setText(Messages.CaInfoComposite_DescriptionLabel);
		lblDescription.setToolTipText(Messages.CaInfoComposite_Description_Tooltip);
		
		txtDescription = new Text(this, SWT.BORDER);
		txtDescription.setText(""); //$NON-NLS-1$
		data = new GridData(SWT.FILL, SWT.CENTER, true,false, 1, 1);
		data.horizontalIndent = indent;
		data.widthHint = 350;
		txtDescription.setLayoutData(data);
		txtDescription.setTextLimit(ConservationArea.MAX_DESCRIPTION_LENGTH);
		txtDescription.addKeyListener(changeListener);
		
		Label lblDesignation = new Label(this, SWT.NONE);
		lblDesignation.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblDesignation.setText(Messages.CaInfoComposite_DesignationLabel);
		lblDesignation.setToolTipText(Messages.CaInfoComposite_Designation_Tooltip);

		txtDesignation = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true,false, 1, 1);
		data.horizontalIndent = indent;
		data.widthHint = 350;
		txtDesignation.setLayoutData(data);
		txtDesignation.setTextLimit(ConservationArea.MAX_DESIGNATION_LENGTH);
		txtDesignation.addKeyListener(changeListener);

		Label lblOrganization = new Label(this, SWT.NONE);
		lblOrganization.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblOrganization.setText(Messages.CaInfoComposite_OrganizationLabel);
		lblOrganization.setToolTipText(Messages.CaInfoComposite_Organization_Tooltip);

		txtOrganization = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true,false, 1, 1);
		data.horizontalIndent = indent;
		data.widthHint = 350;
		txtOrganization.setLayoutData(data);
		txtOrganization.setTextLimit(ConservationArea.MAX_ORGANIZATION_LENGTH);
		txtOrganization.addKeyListener(changeListener);
		
		Label lblPointOfContact = new Label(this, SWT.NONE);
		lblPointOfContact.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPointOfContact.setText(Messages.CaInfoComposite_PointOfContactLabel);
		lblPointOfContact.setToolTipText(Messages.CaInfoComposite_PointOfContact_Tooltip);

		txtPointOfContact = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true,false, 1, 1);
		data.horizontalIndent = indent;
		data.widthHint = 350;
		txtPointOfContact.setLayoutData(data);
		txtPointOfContact.setTextLimit(ConservationArea.MAX_POINT_OF_CONTACT_LENGTH);
		txtPointOfContact.addKeyListener(changeListener);

		Label lblCountry = new Label(this, SWT.NONE);
		lblCountry.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblCountry.setText(Messages.CaInfoComposite_CountryLabel);
		lblCountry.setToolTipText(Messages.CaInfoComposite_Country_Tooltip);

		txtCountry = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true,false, 1, 1);
		data.horizontalIndent = indent;
		data.widthHint = 350;
		txtCountry.setLayoutData(data);
		txtCountry.setTextLimit(ConservationArea.MAX_COUNTRY_LENGTH);
		txtCountry.addKeyListener(changeListener);

		Label lblOwner = new Label(this, SWT.NONE);
		lblOwner.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblOwner.setText(Messages.CaInfoComposite_OwnerLabel);
		lblOwner.setToolTipText(Messages.CaInfoComposite_Owner_Tooltip);

		txtOwner = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true,false, 1, 1);
		data.horizontalIndent = indent;
		data.widthHint = 350;
		txtOwner.setLayoutData(data);
		txtOwner.setTextLimit(ConservationArea.MAX_OWNER_LENGTH);
		txtOwner.addKeyListener(changeListener);

		cdIdentifier = createDecoration(txtIdentifier);
		cdName= createDecoration(txtName);
		
		
		if (defaults != null){
			updateValues(defaults);
		}
		
		validate();
	}
	
	public void updateValues(ConservationArea ca){
		if (ca.getId() != null){
			txtIdentifier.setText(ca.getId());
		}else{
			txtIdentifier.setText(""); //$NON-NLS-1$
		}
		if (ca.getName() != null){
			txtName.setText(ca.getName());
		}else{
			txtName.setText(""); //$NON-NLS-1$
		}
		if (ca.getDescription() != null){
			txtDescription.setText(ca.getDescription());
		}else{
			txtDescription.setText(""); //$NON-NLS-1$
		}
		if (ca.getDesignation() != null){
			txtDesignation.setText(ca.getDesignation());
		}else{
			txtDesignation.setText(""); //$NON-NLS-1$
		}
		txtOrganization.setText(ca.getOrganization() != null ? ca.getOrganization() : ""); //$NON-NLS-1$
		txtPointOfContact.setText(ca.getPointOfContact() != null ? ca.getPointOfContact() : ""); //$NON-NLS-1$
		txtCountry.setText(ca.getCountry() != null ? ca.getCountry() : ""); //$NON-NLS-1$
		txtOwner.setText(ca.getOwner() != null ? ca.getOwner() : ""); //$NON-NLS-1$
		validate();
	}
	
	/**
	 * Validate the input fields
	 */
	private void validate() {
		isValid = true;
		
		if (txtIdentifier.getText().trim().isEmpty() || txtIdentifier.getText().length() > ConservationArea.MAX_ID_LENGTH) {
			cdIdentifier.setDescriptionText(Messages.CaInfoComposite_Error_NoId );
			cdIdentifier.show();
			isValid = false;
		}
		if (!SmartUtils.isSimpleString(txtIdentifier.getText(), SmartUtils.RegExLevel.ALLOWED_CHARS_MED_REGEX) ){
			cdIdentifier.setDescriptionText(Messages.CaInfoComposite_Error_InvalidCharacters + SmartUtils.RegExLevel.ALLOWED_CHARS_MED_REGEX.textDesc);
			cdIdentifier.show();
			isValid = false;
		}
		if (isValid){
			cdIdentifier.hide();
		}
		
		if (txtName.getText().trim().isEmpty() || txtIdentifier.getText().length() > ConservationArea.MAX_NAME_LENGTH) {
			cdName.setDescriptionText(Messages.CaInfoComposite_Error_NoName);
			cdName.show();
			isValid = false;
		}else{
			cdName.hide();
		}
		
		for(IValidationListener listener : listeners){
			listener.validate();
		}
	}
	
	private void fireChangeListeners(){
		for (IChangeListener listener: changeListeners){
			listener.chageMade();
		}
	}
	
	public String getIdentifier(){
		return this.txtIdentifier.getText();
	}
	public String getCaName(){
		return this.txtName.getText();
	}
	public String getDesignation(){
		return this.txtDesignation.getText();
	}
	public String getDescription(){
		return this.txtDescription.getText();
	}

	public boolean isSameSignature(ConservationArea ca) {
		String id = getIdentifier().trim();
		String name = getCaName().trim();
		return id.equals(ca.getId()) && name.equals(ca.getName());
		
	}
	
	public void updateConservationArea(ConservationArea ca) {
		ca.setId(getIdentifier().trim());
		ca.setName(getCaName().trim());
		ca.setDescription(getDescription().trim());
		ca.setDesignation(getDesignation().trim());
		ca.setOrganization(txtOrganization.getText().trim());
		ca.setPointOfContact(txtPointOfContact.getText().trim());
		ca.setCountry(txtCountry.getText().trim());
		ca.setOwner(txtOwner.getText().trim());
	}
	
	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}

	
	/**
	 * 
	 * @return <code>true</code> if all fields are validated, <code>false</code> otherwise
	 */
	public boolean isValid(){
		return this.isValid;
	}
	/**
	 * adds a listener that is called when the fields in this composite are validated
	 * @param validate
	 */
	public void addValidationListener(IValidationListener validate){
		listeners.add(validate);
	}
	
	public void addChangeListener(IChangeListener change){
		changeListeners.add(change);
	}

	public interface IValidationListener{
		 void validate();
	}
	public interface IChangeListener{
		 void chageMade();
	}
	
}
