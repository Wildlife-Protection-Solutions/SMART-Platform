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
package org.wcs.smart.export.dialog;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;

/**
 * Comboviewer that displays a list of field delimiters
 * @author Emily
 *
 */
public class DelimiterCombo extends ComboViewer {

	public static enum Delimiter{
		
		COMMA(Messages.CsvFileComposite_comma, ','),
		SEMICOLON(Messages.CsvFileComposite_semicolon, ';'),
		COLON(Messages.CsvFileComposite_colon, ':'),
		TAB(Messages.CsvFileComposite_tab, '\t');
		
		public char value;
		public String name;
		private Delimiter(String name, char value){
			this.value = value;
			this.name = name;
		}
	}

	public DelimiterCombo(Composite parent, int style) {
		super(parent, style);

		setContentProvider(ArrayContentProvider.getInstance());
		setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof Delimiter){
					return ((Delimiter) element).name;
				}
				return null;
			}
		});
		setInput(Delimiter.values());
		getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		//default value
		String key = SmartPlugIn.getDefault().getDialogSettings().get(SmartPlugIn.DEFAULT_DELIMITER_KEY);
		if (key != null && key.length() == 1){
			char x = key.charAt(0);
			Delimiter defaultd = null;
			for (Delimiter d : Delimiter.values()){
				if (d.value == x){
					defaultd = d;
					break;
				}
			}
			if (defaultd != null){
				setSelection(new StructuredSelection(defaultd));
			}else{
				getCombo().setText(String.valueOf(x));
			}
		}else{
			setSelection(new StructuredSelection(Delimiter.COMMA));
		}
	}

	public char getDelimiter() throws Exception{
		Object x = ((IStructuredSelection)getSelection()).getFirstElement();
		if (x == null){
			//a custom value was entered
			x = getCombo().getText();
		}
		if (x instanceof Delimiter){
			return ((Delimiter) x).value;
		}else if (x instanceof String){
			if (((String) x).length() > 1){
				throw new Exception(MessageFormat.format(Messages.CsvFileComposite_InvalidDelimiter1, new Object[]{x}));
			}
			return ((String) x).charAt(0);
		}else{
			throw new Exception(MessageFormat.format(Messages.CsvFileComposite_InvalidDelimiter1, new Object[]{x.toString()}));
		}
	}
}
