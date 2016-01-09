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
package org.wcs.smart.cybertracker.properties;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.properties.CyberTrackerPropertiesComposite.IPropsChangeListener;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * The CyberTracker property dialog for managing 
 * CyberTracker application default properties
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerPropertiesDialog extends AbstractPropertyJHeaderDialog {

	private CyberTrackerPropertiesProfile ctProperties;
	
	private Text txtProfileName;
	private ControlDecoration profileNameDecoration;
	
	private CyberTrackerPropertiesComposite tabs;
	
	public CyberTrackerPropertiesDialog(Shell shell, CyberTrackerPropertiesProfile profile) {
		super(shell, Messages.CyberTrackerPropertiesDialog_Title);
		Session s = getSession();
		//this dialog is designed to work with its own session,
		//if it is not possible to close external session before launching this dialog than launch a dialog in a separate thread
		s.beginTransaction();
		ctProperties = (CyberTrackerPropertiesProfile) s.merge(profile);
	}

	@Override
	protected Composite createContent(Composite parent) {
		
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite topCmp = new Composite(main, SWT.NONE);
		GridLayout topLayout = new GridLayout(3, false);
		topLayout.horizontalSpacing = 7; //need this to properly fit error decorator
		topCmp.setLayout(topLayout);
		topCmp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Label lblProfileName = new Label(topCmp, SWT.NONE);
		lblProfileName.setText(Messages.CyberTrackerPropertiesDialog_ProfileName);
		lblProfileName.setToolTipText(Messages.CyberTrackerPropertiesDialog_ProfileName_Tooltip);

		txtProfileName = new Text(topCmp, SWT.BORDER);
		txtProfileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtProfileName.setToolTipText(Messages.CyberTrackerPropertiesDialog_ProfileName_Tooltip);
		txtProfileName.setText(ctProperties.getName() != null ? ctProperties.getName() : ""); //$NON-NLS-1$
		txtProfileName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isProfileNameValid()) {
					ctProperties.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), txtProfileName.getText());
					profileNameDecoration.hide();
				} else {
					profileNameDecoration.show();
				}
				setChangesMade(true);
			}
		});

		profileNameDecoration = new ControlDecoration(txtProfileName, SWT.LEFT);
		profileNameDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		profileNameDecoration.setShowHover(true);
		profileNameDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_ProfileName_Invalid, org.wcs.smart.ca.Label.MAX_LENGTH));
		profileNameDecoration.hide();
		
		Button btnTranslate = new Button(topCmp, SWT.PUSH);
		btnTranslate.setText(Messages.CyberTrackerPropertiesDialog_Translate);
		btnTranslate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ctProperties != null){
					TranslateSimpleListItemDialog translateDialog = new TranslateSimpleListItemDialog(getShell(), ctProperties);
					if (translateDialog.open() == Window.OK){
						updateText(ctProperties);
						setChangesMade(true);
					}
				}
			}
		});
		
		tabs = new CyberTrackerPropertiesComposite(main);
		tabs.populateValuesFromObj(ctProperties);
		tabs.addPropsChangeListener(new IPropsChangeListener(){
			@Override
			public void changesMade() {
				setChangesMade(true);
			}
		});
		
		setTitle(Messages.CyberTrackerPropertiesDialog_Title);
		setMessage(Messages.CyberTrackerPropertiesDialog_Message);
		super.setTitleImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.CT_WIZARD_BANNER));
		
		return main;
	}

	private void updateText(NamedItem item){
		String name = item.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		txtProfileName.setText(name);
	}
	
	protected boolean isProfileNameValid() {
		return txtProfileName != null && txtProfileName.getText() != null 
				&& txtProfileName.getText().length() <= org.wcs.smart.ca.Label.MAX_LENGTH;
	}

	@Override
	protected boolean performSave() {
		if (!isProfileNameValid() || !tabs.recordValuesToObj(ctProperties)) {
			MessageDialog.openError(getShell(), Messages.CyberTrackerPropertiesDialog_Error, Messages.CyberTrackerPropertiesDialog_DataNotValid);
			return false;
		}

		Session session = getSession();
		try {
			session.saveOrUpdate(ctProperties);
			session.getTransaction().commit();
			//start a new transaction
			session.getTransaction().begin();
			setChangesMade(false);
			return true;
		} catch (Exception e) {
			SmartPlugIn.displayLog(Messages.CyberTrackerPropertiesDialog_Save_Error, e);
			return false;
		}
	}

}
