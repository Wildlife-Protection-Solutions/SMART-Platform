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
package org.wcs.smart.ui.ca.datamodel.dropitem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.filter.Operator;

/**
 * Boolean operator (and/or) drop item.
 * @author Emily
 * @since 1.0.0
 */
public class BooleanOpDropItem extends DropItem {
	
	private Combo operator;
	private Font smallerFont;
	private String currentSelection = null;
	
	private static Operator[] operators = new Operator[]{Operator.AND, Operator.OR};
	
	/**
	 * Creates a new drop item.
	 * @param parent parent composite
	 * @param target drop panel target
	 */
	public BooleanOpDropItem() {
	}
	
	/**
	 * @param data - a string value representing the selected operator gui<br>
	 * see {@link Operator.AND} or {@link Operator.OR} <br>
	 * Should be the smart value
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeData(Object operatorValue){
		if( operatorValue instanceof Operator ) {
			currentSelection = ((Operator)operatorValue).asSmartValue();
		}else {
			currentSelection = (String) operatorValue;
		}
	}
	

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return operators[operator.getSelectionIndex()].getGuiValue();
	}
	
	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
		}
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		//return operators[operator.getSelectionIndex()].asSql();
		return currentSelection;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		operator = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		for (int i = 0; i < operators.length; i ++){
			operator.add(operators[i].getGuiValue());
		}
		
		FontData fd = (operator.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		operator.setFont(smallerFont);
		operator.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		operator.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!currentSelection.equals(operators[operator.getSelectionIndex()].asSmartValue())){
					currentSelection = operators[operator.getSelectionIndex()].asSmartValue();
					queryChanged();
				}
			}
		});
		initDrag(operator);
		
		
		if (currentSelection != null){
			Operator op = Operator.parseOperator(currentSelection);
			if (op != null){
				operator.setText( formatStringForLabel(op.getGuiValue()));	
				currentSelection = op.asSmartValue();
			}else{
				operator.select(0);
				currentSelection = operators[operator.getSelectionIndex()].asSmartValue();
			}
			
		}else{
			operator.select(0);
			currentSelection = operators[operator.getSelectionIndex()].asSmartValue();
		}
	}
	
}
