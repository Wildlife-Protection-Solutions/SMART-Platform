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
package org.wcs.smart.dataentry.dialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for changing the name of
 * any NamedItem.
 * <p>Displays to the user a dialog with a language combo
 * and text box.</p>
 * 
 * @author egouge
 *
 */
public class TranslateConfigurableModelItemDialog extends SmartStyledTitleDialog {

	private TableViewer nameTable;
	protected NamedItem item;
	protected Map<Language, org.wcs.smart.ca.Label> input;
	
	protected boolean isDirty = false;
	
	/**
	 * @param parentShell parent shell
	 * @param item item to update
	 */
	public TranslateConfigurableModelItemDialog(Shell parentShell, NamedItem item) {
		super(parentShell);

		this.item = item;
		input = new HashMap<Language, org.wcs.smart.ca.Label>();
		for (Language l : SmartDB.getCurrentConservationArea().getLanguages()){
			org.wcs.smart.ca.Label copy = new org.wcs.smart.ca.Label();
			copy.setValue(item.findName(l));
			copy.setLanguage(l);
			input.put(l, copy);
		}
		
//		//sort so default language is first in the list
//		Collections.sort(input, new Comparator<org.wcs.smart.ca.Label>() {
//			@Override
//			public int compare(org.wcs.smart.ca.Label l0, org.wcs.smart.ca.Label l1) {
//				if (l0.getLanguage().isDefault()){
//					return -1;
//				}
//				return Collator.getInstance().compare(l0.getLanguage().getDisplayName(), l1.getLanguage().getDisplayName());
//			}
//		});;
		
	}

	protected boolean validate(){
		boolean ok = true;
		setErrorMessage(null);
		for (org.wcs.smart.ca.Label lbl : input.values()){
		
			if (!SmartUtils.isSimpleString(lbl.getValue(),
						SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX,
						org.wcs.smart.ca.Label.MAX_LENGTH, 0)) {

					setErrorMessage(MessageFormat
							.format(Messages.TranslateConfigurableModelItemDialog_InvalidLabel,
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
	protected void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.OK_ID).setText(DialogConstants.SAVE_TEXT);
	}
	
	@Override
	public boolean close(){
		getButtonBar().setFocus();
		
		if (isDirty){
			MessageDialog md = new MessageDialog(getShell(), 
					Messages.TranslateConfigurableModelItemDialog_SaveTitle, 
					null, 
					Messages.TranslateConfigurableModelItemDialog_SaveMessage, 
					MessageDialog.QUESTION_WITH_CANCEL, 
					new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			
			int ret = md.open();
			if (ret == 2){
				//cancel
				return false;
			}else if (ret == 0){
				//yes
				if (save()){
					setReturnCode(IDialogConstants.OK_ID);
					return super.close();
				}else{
					return false;
				}
			}
		}
		return super.close();
	}
	
	
	protected boolean save(){
		if (!validate()){
			return false;
		}
		
			
		// update item object 
		for (Entry<Language, Label> e : input.entrySet()) {
			org.wcs.smart.ca.Label lbl = null;
			for (org.wcs.smart.ca.Label tmp : this.item.getNames()){
				if (tmp.getLanguage().equals(e.getKey())){
					lbl = tmp;
					break;
				}
			}
			if (lbl != null){
				lbl.setValue(e.getValue().getValue());
			}else{
				org.wcs.smart.ca.Label l = new org.wcs.smart.ca.Label();
				l.setElement(item);
				l.setLanguage(e.getKey());
				l.setValue(e.getValue().getValue());
				this.item.getNames().add(l);
				lbl = l;
			}
			if (lbl.getLanguage().equals(SmartDB.getCurrentLanguage())){
				this.item.setName(lbl.getValue());
			}
		}
		
		List<org.wcs.smart.ca.Label> toRemove = new ArrayList<org.wcs.smart.ca.Label>();
		for (org.wcs.smart.ca.Label l : this.item.getNames()){
			if (!input.containsKey(l.getLanguage())) toRemove.add(l);
		}
		this.item.getNames().removeAll(toRemove);
		setDirty(false);
		return true;
	}
	
	@Override
	protected void okPressed() {
		if (save()){
			super.okPressed();
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		super.getShell().setText(Messages.TranslateConfigurableModelItemDialog_Title);
		setTitle(getShell().getText());
		super.setMessage(Messages.TranslateConfigurableModelItemDialog_Message);
		
		Composite composite = (Composite) super.createDialogArea(parent);
		composite = new Composite(composite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout());
		
		nameTable = new TableViewer(composite, SWT.FULL_SELECTION | SWT.BORDER);
		nameTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		nameTable.setContentProvider(ArrayContentProvider.getInstance());
		nameTable.getTable().setHeaderVisible(true);
		nameTable.getTable().setLinesVisible(true);
		
		TableViewerColumn colLang = new TableViewerColumn(nameTable, SWT.NONE);
		colLang.getColumn().setWidth(100);
		colLang.getColumn().setText(Messages.TranslateConfigurableModelItemDialog_LanguageColumn);
		colLang.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					return ((Language) element).getDisplayName();
				}
			  	return super.getText(element);
			}

		});
		
		TableViewerColumn colName = new TableViewerColumn(nameTable, SWT.NONE);
		colName.getColumn().setWidth(400);
		colName.getColumn().setText(Messages.TranslateConfigurableModelItemDialog_ConfiguredNameColumn);
		colName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					Label ll = input.get((Language)element);
					
					if (ll != null && !ll.getValue().isBlank()) return ll.getValue();
					
					if (item instanceof CmNode) {
						if (((CmNode)item).getCategory() != null) {
							String label = ((CmNode)item).getCategory().findNameNull( (Language)element );
							if (label != null) return label;
						}
					}else if (item instanceof CmAttribute) {
						if (((CmAttribute)item).getAttribute() != null) {
							String label = ((CmAttribute)item).getAttribute().findNameNull( (Language)element );
							if (label != null) return label;
						}
					}
					return ""; //$NON-NLS-1$
				}
			  	return super.getText(element);
			}
			
			@Override
			public Color getForeground(Object element) {
				if (element instanceof Language){
					Label ll = input.get((Language)element);
					if (ll == null || ll.getValue().isBlank()) return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				}
				return null;
				
			}
		});
		
		colName.setEditingSupport(new EditingSupport(nameTable) {
			private TextCellEditor editor =  new TextCellEditor(nameTable.getTable());
		
			@Override
			protected void setValue(Object element, Object value) {
				Language lang = (Language)element;
				String newValue = (String)value;
				
				if (newValue.trim().length() == 0){
					input.remove(lang);
				}else {
					if (input.containsKey(lang)) {
						input.get(lang).setValue(newValue);
					}else {
						Label newLabel = new Label();
						newLabel.setElement(item);
						newLabel.setValue(newValue);
						newLabel.setLanguage(lang);
						input.put(lang, newLabel);
					}
				}				
				nameTable.refresh();
				setDirty(true);
			}
			
			@Override
			protected Object getValue(Object element) {
				Label l = input.get((Language)element);
				if (l == null) return ""; //$NON-NLS-1$
				return l.getValue();
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		if (item instanceof CmNode && (((CmNode)item).getCategory() != null)) {
			colName.getColumn().setWidth(200);
			TableViewerColumn dmName = new TableViewerColumn(nameTable, SWT.NONE);
			dmName.getColumn().setWidth(200);
			dmName.getColumn().setText(Messages.TranslateConfigurableModelItemDialog_DataModelNameColumn);
			dmName.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof Language){
						Language l = (Language)element;
						
						if (item instanceof CmNode && ((CmNode)item).getCategory() != null) {
							String ll = ((CmNode)item).getCategory().findNameNull(l);
							if (ll != null) return ll;
						}else if (item instanceof CmAttribute && ((CmAttribute)item).getAttribute() != null) {
							String ll = ((CmAttribute)item).getAttribute().findNameNull(l);
							if (ll != null) return ll;
						}
						return ""; //$NON-NLS-1$
					}
				  	return super.getText(element);
				}
			});
		}
		
		nameTable.setInput(SmartDB.getCurrentConservationArea().getLanguages());
		return composite;
	}

	protected boolean isResizable() {
		return true;
	}
	
	protected void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		validate();
		//getButton(IDialogConstants.CANCEL_ID).setEnabled(isDirty);
	}
	
}
