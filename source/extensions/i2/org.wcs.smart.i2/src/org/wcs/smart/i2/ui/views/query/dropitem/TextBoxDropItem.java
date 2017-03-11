/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views.query.dropitem;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.i2.query.Operator;

/**
 * Drop item that displays a text box where the user can enter 
 * a string or number.
 * 
 * @author Emily
 *
 */
public class TextBoxDropItem extends DropItem {

	public enum InputType{TEXT,NUMERIC};
	
	private String name;
	private String queryKeyPart;
	
	private InputType type;
	
	private Text value;
	private ComboViewer operators;
	
	private Operator currentOperator;
	private String currentValue;
	
	/**
	 * Creates a new are drop item that has 
	 * single text field label
	 * 
	 */
	public TextBoxDropItem(String name, String queryKeyPart, InputType type){
		this.name = name;
		this.queryKeyPart = queryKeyPart;
		this.type = type;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return name;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		if (type == InputType.TEXT){
			String strValue = value.getText().replaceAll("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
			return queryKeyPart + " " + ((Operator) ((IStructuredSelection)operators.getSelection()).getFirstElement()).getKey() + " \"" + strValue + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else{
			return queryKeyPart + " " + ((Operator) ((IStructuredSelection)operators.getSelection()).getFirstElement()).getKey() + " " + value.getText(); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void setInitialValue(Operator op, String data){
		this.currentOperator = op;
		this.currentValue = data;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText( formatStringForLabel(getText()));
		initDrag(lbl);

		operators = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		operators.setContentProvider(ArrayContentProvider.getInstance());
		operators.setLabelProvider(new OperatorLabelProvider());
		if (type == InputType.NUMERIC) {
			operators.setInput(Operator.NUMERIC_OPS);
		} else if (type == InputType.TEXT) {
			operators.setInput(Operator.STRING_OPS);
		}
		FontData fd = (operators.getControl().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		Font smallerFont = new Font(Display.getCurrent(), fd);
		operators.getControl().setFont(smallerFont);
		operators.getControl().addListener(SWT.Dispose, e->smallerFont.dispose());
		
		operators.getControl().addListener(SWT.Modify, e->{
			Operator current = (Operator) ((IStructuredSelection)operators.getSelection()).getFirstElement();
			if (current != null && !current.equals(currentOperator)){
				currentOperator = current;
				queryChanged();
			}
		});		

		value = new Text(main, SWT.BORDER);
		value.addListener(SWT.Modify, e->{
			queryChanged();
		});

		GridData gd = new GridData();
		gd.minimumWidth = 50;
		gd.widthHint = 100;
		value.setLayoutData(gd);

		if (currentOperator != null){
			operators.setSelection(new StructuredSelection(currentOperator));
		}else{
			operators.setSelection(new StructuredSelection(((Object[])operators.getInput())[0]));
		}
		if (currentValue != null){
			value.setText(currentValue);
		}
		
	}

}
