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
package org.wcs.smart.ui;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageLabelProvider;
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
public class TranslateSimpleListItemDialog extends TitleAreaDialog {

	private TableViewer tblViewer;
	protected NamedItem item;
	protected List<org.wcs.smart.ca.Label> input;
	
	protected boolean isDirty = false;
	
	/**
	 * @param parentShell parent shell
	 * @param item item to update
	 */
	public TranslateSimpleListItemDialog(Shell parentShell, NamedItem item) {
		super(parentShell);

		this.item = item;
		input = new ArrayList<org.wcs.smart.ca.Label>(SmartDB.getCurrentConservationArea().getLanguages().size());
		for (Language l : SmartDB.getCurrentConservationArea().getLanguages()){
			org.wcs.smart.ca.Label copy = new org.wcs.smart.ca.Label();
			copy.setValue(item.findName(l));
			copy.setLanguage(l);
			input.add(copy);
		}
		
		//sort so default language is first in the list
		Collections.sort(input, new Comparator<org.wcs.smart.ca.Label>() {
			@Override
			public int compare(org.wcs.smart.ca.Label l0, org.wcs.smart.ca.Label l1) {
				if (l0.getLanguage().isDefault()){
					return -1;
				}
				return Collator.getInstance().compare(l0.getLanguage().getDisplayName(), l1.getLanguage().getDisplayName());
			}
		});;
		
	}

	protected boolean validate(){
		boolean ok = true;
		setErrorMessage(null);
		for (org.wcs.smart.ca.Label lbl : input){
		
			if (lbl.getLanguage().isDefault() && lbl.getValue().length() == 0){
		
				setErrorMessage(MessageFormat.format(Messages.TranslateSimpleListItemDialog_Error_LabelRequired, new Object[]{SmartDB.getCurrentConservationArea().getDefaultLanguage().getDisplayName()}));
				ok = false;
				break;
			}
			if (!SmartUtils.isSimpleString(lbl.getValue(),
						SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX,
						org.wcs.smart.ca.Label.MAX_LENGTH, 0)) {

					setErrorMessage(MessageFormat
							.format(Messages.TranslateSimpleListItemDialog_Error_InvalidLabel,
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
			MessageDialog md = new MessageDialog(getShell(), Messages.AbstractPropertyJHeaderDialog_ConfirmSave_DialogTitle, null, Messages.AbstractPropertyJHeaderDialog_ConfirmSave_DialogMessage, MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
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
		
		HashMap<Language, String> values = new HashMap<Language, String>();
		for(org.wcs.smart.ca.Label lbl : input){
			if (lbl.getValue().length() > 0){
				values.put(lbl.getLanguage(), lbl.getValue());
			}
		}
				
		// update item object 
		for (Entry<Language, String> e : values.entrySet()) {
			org.wcs.smart.ca.Label lbl = null;
			for (org.wcs.smart.ca.Label tmp : this.item.getNames()){
				if (tmp.getLanguage().equals(e.getKey())){
					lbl = tmp;
					break;
				}
			}
			if (lbl != null){
				lbl.setValue(e.getValue());
			}else{
				org.wcs.smart.ca.Label l = new org.wcs.smart.ca.Label();
				l.setElement(item);
				l.setLanguage(e.getKey());
				l.setValue(e.getValue());
				this.item.getNames().add(l);
				lbl = l;
			}
			if (lbl.getLanguage().equals(SmartDB.getCurrentLanguage())){
				this.item.setName(lbl.getValue());
			}
		}
		
		List<org.wcs.smart.ca.Label> toRemove = new ArrayList<org.wcs.smart.ca.Label>();
		for (org.wcs.smart.ca.Label l : this.item.getNames()){
			String key = values.get(l.getLanguage());
			if (key == null){
				toRemove.add(l);
			}
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
		super.getShell().setText(Messages.TranslateSimpleListItemDialog_DialogTitle);
		super.setMessage(Messages.TranslateSimpleListItemDialog_DialogMessage);
		setTitle(Messages.TranslateSimpleListItemDialog_DialogTitle);
		
		Composite composite = (Composite) super.createDialogArea(parent);
		composite = new Composite(composite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		composite.setLayout(new GridLayout(2, false));
		composite.setLayout(new TableColumnLayout());
		
		tblViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
		tblViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		TableViewerColumn viewerColumn = new TableViewerColumn(tblViewer,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		TableColumnLayout layout = (TableColumnLayout) tblViewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(34,ColumnWeightData.MINIMUM_WIDTH, true));
		column.setText(Messages.TranslateSimpleListItemDialog_LanguageLabel);
		column.setResizable(true);
		column.setMoveable(true);
		viewerColumn.setLabelProvider(new ColumnLabelProvider(){
			LanguageLabelProvider ll = new LanguageLabelProvider();
			@Override
			public String getText(Object element) {
				return ll.getText(((org.wcs.smart.ca.Label)element).getLanguage());
			}
			 
		});
		
		viewerColumn = new TableViewerColumn(tblViewer,SWT.NONE);
		column = viewerColumn.getColumn();
		layout = (TableColumnLayout) tblViewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(66,ColumnWeightData.MINIMUM_WIDTH, true));
		column.setText(Messages.TranslateSimpleListItemDialog_NameLabel);
		column.setResizable(true);
		column.setMoveable(true);
		viewerColumn.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element) {
				String x = ((org.wcs.smart.ca.Label)element).getValue();
				if (x == null){
					return ""; //$NON-NLS-1$
				}
				return x;
			}			 
		});
		viewerColumn.setEditingSupport(new TextTableEditor(tblViewer));

		tblViewer.setContentProvider(ArrayContentProvider.getInstance());
		tblViewer.setInput(input);
		tblViewer.getTable().setHeaderVisible(true);
		tblViewer.getTable().setLinesVisible(true);
		
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
	
	/**
	 * Agency table editor 
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	private class TextTableEditor extends EditingSupport{

		private TableViewer viewer;
		
		TextTableEditor(TableViewer  viewer) {
			super(viewer);
			this.viewer = viewer;
		}
		
		@Override
		protected void setValue(Object element, Object value) {
			String newValue = (String)value;
			String oldValue = ((org.wcs.smart.ca.Label)element).getValue();
			if (newValue.equals(oldValue)){
				//nothing to update
				return;
			}
			((org.wcs.smart.ca.Label)element).setValue((String)value);
			viewer.refresh();
			TranslateSimpleListItemDialog.this.validate();
			TranslateSimpleListItemDialog.this.setDirty(true);
		}
		
		@Override
		protected Object getValue(Object element) {
			return ((org.wcs.smart.ca.Label)element).getValue();
		}
		
		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(viewer.getTable());
		}
		
		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}
}
