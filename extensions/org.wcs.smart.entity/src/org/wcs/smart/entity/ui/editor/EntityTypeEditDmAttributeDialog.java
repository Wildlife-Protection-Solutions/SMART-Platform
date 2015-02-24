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
package org.wcs.smart.entity.ui.editor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolTip;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.ca.properties.AddAttributeDialog2;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for editing the an attribute associated with
 * an entity type.  Users can modify the name, the
 * is required and is primary attributes.
 *  
 * @author Emily
 *
 */
public class EntityTypeEditDmAttributeDialog extends TranslateSimpleListItemDialog  {

	private Button btnIsRequired; 
	private Button btnIsPrimary; 
	private Text txtKey;
	
	private Session openSession;
	private boolean fireEvents;
	
	public EntityTypeEditDmAttributeDialog(Shell parentShell, 
			EntityAttribute attribute,
			Session openSession) {
		super(parentShell, attribute);
		this.openSession = openSession;
		
		this.fireEvents = false;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite translateComp = (Composite) super.createDialogArea(parent);
		
		Composite pre = new Composite(parent, SWT.NONE);
		pre.setLayout(new GridLayout(2, false));
		pre.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		final Label keyLabel = new Label(pre, SWT.NONE);
		keyLabel.setText(Messages.EntityTypeEditDmAttributeDialog_KeyLabel);
		keyLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		final ToolTip tt = new ToolTip(getShell(), SWT.ICON_INFORMATION);
		tt.setAutoHide(false);
		tt.setMessage(Messages.EntityTypeEditDmAttributeDialog_KeyTooltip);
		Listener mouseOver = new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (event.type == SWT.MouseEnter){
					tt.setLocation(keyLabel.toDisplay(event.x+10, event.y+10));
					tt.setVisible(true);
				}else if (event.type == SWT.MouseExit){
					tt.setVisible(false);
				}
			};
		};
		keyLabel.addListener(SWT.MouseEnter, mouseOver);
		keyLabel.addListener(SWT.MouseExit, mouseOver);

		
		Composite c = new Composite(pre, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		txtKey = new Text(c, SWT.BORDER | SWT.READ_ONLY);
		if (((EntityAttribute)item).getKeyId() != null){
			txtKey.setText(((EntityAttribute)item).getKeyId());
		}
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtKey.setEditable(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Link btnEditKey = new Link(c, SWT.NONE);
		btnEditKey.setText("<a>" + DialogConstants.EDIT_LINK_TEXT + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		btnEditKey.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!MessageDialog
						.openConfirm(
								getShell(),
								Messages.EntityTypeEditDmAttributeDialog_EditKeyDialogTitle,
								Messages.EntityTypeEditDmAttributeDialog_EditKeyDialogWarning )) {
					return;
				}
				List<EntityAttribute> siblings = new ArrayList<EntityAttribute>();
				EntityAttribute a = (EntityAttribute)item;
				siblings.addAll(a.getEntityType().getAttributes());
				siblings.remove(a);
				KeyInputDialog id = new KeyInputDialog(getShell(), txtKey.getText(), siblings);
				int ret = id.openNoWarning();
				if (ret != Window.CANCEL) {
					txtKey.setText(id.getValue());
					setDirty(true);
				}
			}
		});
		
		Label l = new Label(pre, SWT.NONE);
		l.setText(Messages.EntityTypeEditDmAttributeDialog_IsRequiredFieldName);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		l.setToolTipText(Messages.EntityTypeEditDmAttributeDialog_RequiredTooltip);
		
		btnIsRequired = new Button(pre, SWT.CHECK);
		btnIsRequired.setSelection(  ((EntityAttribute)item).getIsRequired());
		btnIsRequired.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
			}
		});
		btnIsRequired.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,1,1));
		
		
		l = new Label(pre, SWT.NONE);
		l.setText(Messages.EntityTypeEditDmAttributeDialog_IsPrimaryFieldName);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		l.setToolTipText(Messages.EntityTypeEditDmAttributeDialog_PrimaryTooltip);
		
		btnIsPrimary = new Button(pre, SWT.CHECK);
		btnIsPrimary.setSelection(  ((EntityAttribute)item).getIsPrimary());
		btnIsPrimary.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
			}
		});
		btnIsPrimary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,1,1));

		l = new Label(pre, SWT.NONE);
		l.setText(Messages.EntityTypeEditDmAttributeDialog_DmAttributeLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		l.setToolTipText(Messages.EntityTypeEditDmAttributeDialog_DmAttributeTooltip);
		
		Link lnk = new Link(pre, SWT.NONE);
		lnk.setText("<a>" + ((EntityAttribute)item).getDmAttribute().getName() + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lnk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!MessageDialog.openQuestion(getShell(), 
						Messages.EntityTypeEditDmAttributeDialog_EditDialotTitle, 
						MessageFormat.format(Messages.EntityTypeEditDmAttributeDialog_EditConfirmMessage, new Object[]{((EntityAttribute)item).getDmAttribute().getName()}))){
					return;
				}
				
				try{
					String canEdit = DataModelManager.getInstance().canEdit(((EntityAttribute)item).getDmAttribute(), openSession);
					if (canEdit != null){
						if (!MessageDialog.openQuestion(getShell(), Messages.EntityTypeEditDmAttributeDialog_WarningTitle, 
								Messages.EntityTypeEditDmAttributeDialog_AttributeError + "\n\n" + canEdit + "\n\n" + Messages.EntityTypeEditDmAttributeDialog_ContinueLabel)){   //$NON-NLS-1$ //$NON-NLS-2$
							return;
						}
					}
				}catch (Exception ex){
					SmartPlugIn.displayLog(Messages.EntityTypeEditDmAttributeDialog_CannotEditError + "\n\n" + ex.getMessage(), ex);  //$NON-NLS-1$
					return;
				}
				
				//edit data model attribute dialog
				openSession.beginTransaction();
				try{
					openSession.saveOrUpdate(((EntityAttribute)item).getDmAttribute());
					
					@SuppressWarnings("unchecked")
					List<Attribute> atts = openSession.createCriteria(Attribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
					
					AddAttributeDialog2 d2 = new AddAttributeDialog2(getShell(),
							((EntityAttribute)item).getDmAttribute(),
							atts,
							SmartDB.getCurrentLanguage(),
							openSession);
					if (d2.open() == AddAttributeDialog2.OK){
						fireEvents = true;
						openSession.getTransaction().commit();
					}else{
						openSession.getTransaction().rollback();
					}
					
				}catch(Exception ex){
					EntityPlugIn.displayLog(Messages.EntityTypeEditDmAttributeDialog_EditError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
					openSession.getTransaction().rollback();
				}
					
			}
		});
		
		
		super.getShell().setText(Messages.EntityTypeEditDmAttributeDialog_EditTypeTitle);
		super.setMessage(Messages.EntityTypeEditDmAttributeDialog_EditTypeMessage);
		setTitle(Messages.EntityTypeEditDmAttributeDialog_EditTypeTitle);
		setTitle( ((EntityAttribute)item).getName());
		
		return translateComp;
	}
	
	@Override
	protected boolean validate(){
		boolean ok = true;
		setErrorMessage(null);
		for (org.wcs.smart.ca.Label lbl : input){
			if (!SmartUtils.isSimpleString(lbl.getValue(),
					SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX,
					org.wcs.smart.ca.Label.MAX_LENGTH, 0)) {

				setErrorMessage(MessageFormat
						.format(Messages.EntityTypeEditDmAttributeDialog_InvalidLabel,
								new Object[] {
										SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc,
										org.wcs.smart.ca.Label.MAX_LENGTH }));
				ok = false;
				break;
			
			}
		}
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null){
			if (isDirty){
				btn.setEnabled(ok);
			}else{
				btn.setEnabled(false);
			}
		}
		return ok;
	}
	
	@Override
	protected boolean save(){
		if (!validate()){
			return false;
		}
		
		if (!super.save()){
			return false;
		}
		((EntityAttribute)super.item).setKeyId(txtKey.getText());
		((EntityAttribute)super.item).setIsRequired(btnIsRequired.getSelection());
		((EntityAttribute)super.item).setIsPrimary(btnIsPrimary.getSelection());
		fireEvents = true;
		return true;
	}
	
	/**
	 * 
	 * @return true if EntityType Modified event should
	 * be fired.  This may occur even if the user cancels
	 * the dialog if they have saved the data model attribute 
	 * modifications
	 */
	public boolean fireEvents(){
		return this.fireEvents;
	}
}