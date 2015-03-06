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
package org.wcs.smart.ui.ca.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.HkeyObject;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.ui.properties.LanguageViewer;

/**
 * Composite that has a method to add name and key fields.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class NameKeyComposite {

	
	protected Text txtName;
	protected Text txtKey;
	
	protected ControlDecoration cdKey;
	protected ControlDecoration cdTxt;
	
	protected LanguageViewer langViewer = null;
	private Language currentSelection = null;
	private HashMap<Language, String> values = null;
	
	private Collection<? extends NamedKeyItem> siblings;
	
	/**
	 * 
	 */
	public NameKeyComposite(){
	}

	/**
	 * 
	 * @return the current selected language
	 */
	public Language getSelectedLanguage(){
		return langViewer.getCurrentSelection();
	}
	
	/**
	 * Updates the data model object with the values
	 * from the composite fields
	 * @param item object to update
	 */
	public void updateFields(NamedKeyItem item){
		for (Iterator<Entry<Language,String>> iterator = values.entrySet().iterator(); iterator.hasNext();) {
			Entry<Language, String> type = iterator.next();
			if (type.getValue() != null && type.getValue().trim().length() > 0){
				item.updateName(type.getKey(), type.getValue());
			}else{
				//we want to remove this label as long as it's not default
				if (!type.getKey().isDefault()){
					org.wcs.smart.ca.Label toRemove = null;
					for (org.wcs.smart.ca.Label l : item.getNames() ){
						if (l.getLanguage().equals(type.getKey())){
							toRemove = l;
						}
					}
					if (toRemove != null){
						item.getNames().remove(toRemove);
					}
				}
			}
			if (type.getKey().equals(SmartDB.getCurrentLanguage())){
				item.setName(type.getValue());
			}
		}
		
		item.setKeyId(txtKey.getText());
		if (item instanceof HkeyObject){
			((HkeyObject)item).updateHkey();
		}
	}
	
	/**
	 * Initializes the name and key values with the 
	 * data from the data model object
	 * 
	 * @param item data model object
	 * @param defaultLang language
	 */
	public void initFields(NamedKeyItem item, Collection<? extends NamedKeyItem> siblings, Language defaultLang){
		this.siblings = siblings;
		currentSelection = null;
		values = new HashMap<Language, String>();
		if (item.getNames() != null){
			for (org.wcs.smart.ca.Label lbl : item.getNames()){
				values.put(lbl.getLanguage(), lbl.getValue());
			}
		}
		
		if (txtKey != null && item.getKeyId() != null){
			txtKey.setText(item.getKeyId());
		}
		
		if (txtName != null ){
			String x = item.findNameNull(defaultLang);
			if (x == null){
				x = item.getName();
				if (x == null){
					x = ""; //$NON-NLS-1$
				}
			}
			txtName.setText(x);
		}
		if (langViewer != null){
			langViewer.setSelection(new StructuredSelection(defaultLang));
		}
		
		
	}
	/**
	 * Creates name and key fields, adding them to the parent.
	 * <p>
	 * Assumption is that the parent layout is 
	 * a grid layout with three columns.</p>
	 * 
	 * @param parent parent composite to add fields to
	 * @param canEdit if fields can be editing
	 * @param createNew <code>true</code> if a new object is being created or <code>false</code> if exisitng object being modified
	 * @param onChange event fired when a name or key has been modified. Can be null. Only fired if canEdit is true.
	 */
	public void createControls(
			final Composite parent,final boolean canEdit, 
			boolean createNew, final IChangeListener onChange){
		
		values = new HashMap<Language, String>();
		final KeyListener generateKeyListener = new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (currentSelection.isDefault()){
					String newKey = NamedKeyItem.generateKey(txtName.getText(), siblings);
					txtKey.setText(newKey);
				}
				
				if (canEdit && onChange != null){
					onChange.itemModified();
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
			}
		};
		
		/* Name */
		if (canEdit) {
			Label lbl = new Label(parent, SWT.NONE);
			lbl.setText(Messages.NameKeyComposite_LanguageLabel);
			langViewer = new LanguageViewer(parent, SWT.NONE,
					SmartDB.getCurrentConservationArea());
			
			GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
			langViewer.getCombo().setLayoutData(gd);
			
			langViewer
					.addSelectionChangedListener(new ISelectionChangedListener() {
						@Override
						public void selectionChanged(SelectionChangedEvent event) {
							if (currentSelection != null) {
								if (txtName.getText().trim().isEmpty()){
									if (!currentSelection.isDefault()){
										//remove blank translations
										values.remove(currentSelection);
									}else{
										values.put(currentSelection, txtName.getText());		
									}
								}else{
									values.put(currentSelection, txtName.getText());
								}
								
							}
							currentSelection = langViewer.getCurrentSelection();
							String s = values.get(currentSelection);
							if (s == null){
								s = ""; //$NON-NLS-1$
							}
							txtName.setText(s);
						}
					});
			currentSelection = langViewer.getCurrentSelection();
		}
		Label lblNewLabel = new Label(parent, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText(Messages.NameKeyComposite_Name_Label);
		
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
					if (onChange != null){
						onChange.itemModified();
					}
				}
			});
		}
		txtName.setTextLimit(1024);
	
		/* Key */
		Label lblNewLabel_1 = new Label(parent, SWT.NONE);
		lblNewLabel_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_1.setText(Messages.NameKeyComposite_Key_Label);
		
		txtKey = new Text(parent, SWT.BORDER);
		txtKey.setEditable(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		if (canEdit){			
			cdKey = createDecoration(txtKey);
			cdKey.setDescriptionText(Messages.NameKeyComposite_Error_KeyNotEmpty);
			
			cdTxt = createDecoration(txtName);
			cdTxt.setDescriptionText(Messages.NameKeyComposite_Error_NameNotBlank);
			
			txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
			Button btnChangeKey = new Button(parent, SWT.NONE);
			btnChangeKey.setText(Messages.NameKeyComposite_ChangeKey_Button_Label);
			btnChangeKey.setToolTipText(Messages.NameKeyComposite_ChangeKey_Button_Tooltip);
			btnChangeKey.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {

					if (!MessageDialog
							.openConfirm(
									parent.getShell(),
									Messages.NameKeyComposite_ChangeKey_ConfirmDialog_Title,
									Messages.NameKeyComposite_ChangeKey_ConfirmDialog_Message)) {
						return;
					}
					KeyInputDialog id = new KeyInputDialog(parent.getShell(), txtKey.getText(), siblings);
					int ret = id.openNoWarning();
					if (ret != Window.CANCEL) {
						txtKey.setText(id.getValue());
						txtName.removeKeyListener(generateKeyListener);
					}
					
					if (onChange != null){
						onChange.itemModified();
					}
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
	 *   
	 * @return <code>true</code> if error on page; <code>false</code> otherwise
	 */
	public boolean validate(){
		boolean error = false;
		
		if (currentSelection != null ){
			values.put(currentSelection, txtName.getText());
		}
						
		String errormsg = NamedKeyItem.validateKey(txtKey.getText(), new ArrayList<NamedKeyItem>());
		if (errormsg != null){
			cdKey.setDescriptionText(errormsg);
			cdKey.show();
			error = true;
		}else{
			cdKey.hide();
		}
		boolean hide = true;
		for (Iterator<Entry<Language,String>> iterator = values.entrySet().iterator(); iterator.hasNext();) {
			Entry<Language, String> type = iterator.next();
			errormsg = validateName(type.getKey(), type.getValue());
			if (errormsg != null){
				cdTxt.setDescriptionText(errormsg);
				cdTxt.show();
				error = true;
				hide = false;
			}
		}
		if (hide){
			cdTxt.hide();
		}
		return error;
	}

	/**
	 * Validates the name for the given language. 
	 * Users can override if necessary.
	 * 
	 * @param l
	 * @param name
	 * @return
	 */
	public String validateName(Language l, String name){
		return DataModel.validateName(name, l);
	}
	
	public interface IChangeListener{
		public void itemModified();
	}
}

