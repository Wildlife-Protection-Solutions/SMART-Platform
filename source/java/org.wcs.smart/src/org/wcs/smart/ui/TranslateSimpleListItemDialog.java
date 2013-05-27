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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for changing the name of
 * any SimpleListItem.
 * <p>Displays to the user a dialog with a language combo
 * and text box.</p>
 * 
 * @author egouge
 *
 */
public class TranslateSimpleListItemDialog extends TitleAreaDialog {

	private TableViewer tblViewer;
	private SimpleListItem item;
	private org.wcs.smart.ca.Label[] input;
	

	/**
	 * @param parentShell parent shell
	 * @param item item to update
	 */
	public TranslateSimpleListItemDialog(Shell parentShell, SimpleListItem item) {
		super(parentShell);

		this.item = item;
		input = new org.wcs.smart.ca.Label[SmartDB.getCurrentConservationArea().getLanguages().size()];
		int i = 0;
		for (Language l : SmartDB.getCurrentConservationArea().getLanguages()){
			org.wcs.smart.ca.Label copy = new org.wcs.smart.ca.Label();
			copy.setValue(item.findName(l));
			copy.setLanguage(l);
			input[i++] = copy;
		}
		
	}

	private boolean validate(){
		boolean ok = true;
		setErrorMessage(null);
		for(int i = 0 ; i < input.length;i++){
			org.wcs.smart.ca.Label lbl = input[i];
		
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
			btn.setEnabled(ok);
		}
		return ok;
	}
	
	protected void okPressed() {
		if (!validate()){
			return ;
		}
		HashMap<Language, String> values = new HashMap<Language, String>();
		for (int i = 0; i < input.length;i++){
			if (((org.wcs.smart.ca.Label)input[i]).getValue().length() > 0){
				values.put(((org.wcs.smart.ca.Label)input[i]).getLanguage(), ((org.wcs.smart.ca.Label)input[i]).getValue());
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
		
		super.okPressed();
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
			@Override
			public String getText(Object element) {
				String x = ((org.wcs.smart.ca.Label)element).getLanguage().getDisplayName();
				if (x == null){
					return ""; //$NON-NLS-1$
				}
				return x;
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
			((org.wcs.smart.ca.Label)element).setValue((String)value);
			viewer.refresh();
			TranslateSimpleListItemDialog.this.validate();
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
