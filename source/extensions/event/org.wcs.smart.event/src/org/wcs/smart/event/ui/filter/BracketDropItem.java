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
package org.wcs.smart.event.ui.filter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Bracket drop item.
 * @author Emily
 * @since 1.0.0
 */
public class BracketDropItem extends DropItem {

	
	/**
	 * Available bracket types
	 */
	public enum BracketType{
			OPEN,
			CLOSE};
	
	private BracketType order;
	private Label lbl;
	
	
	/**
	 * Creates a new bracket drop item
	 * @param parent parent composite
	 * @param target drop target
	 * @param order bracket type
	 */
	public BracketDropItem(BracketType order) {
		//super(parent, target);
		this.order = order;
		
	}

	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		if (order == BracketType.OPEN){
			return "("; //$NON-NLS-1$
		}else{
			return ")"; //$NON-NLS-1$
		}
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		return getText();
	}


	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		lbl = new Label(parent, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		initDrag(lbl);
		
		lbl.setText( formatStringForLabel(getText()));
	}


	/**
	 * Does nothing
	 */
	@Override
	public void initializeData(Object data) {
		
	}
}
