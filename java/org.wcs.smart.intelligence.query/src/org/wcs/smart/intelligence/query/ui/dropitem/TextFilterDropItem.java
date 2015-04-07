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
package org.wcs.smart.intelligence.query.ui.dropitem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Intelligence text filter drop items.  This is used for Name and Description
 * filters.
 * 
 * @author Emily
 *
 */
public class TextFilterDropItem extends DropItem{

	public IntelligenceFilterOption filter;

	private String currentValue = null;
	private String currentOp = null;	
	private Label lblAttribute;
	private Text value;
	private Combo operators;
	
	private Font smallerFont;
	
	public TextFilterDropItem(IntelligenceFilterOption filter){
		if (filter != IntelligenceFilterOption.NAME &&
			filter != IntelligenceFilterOption.DESCRIPTION ) {
			throw new IllegalStateException("Intelligence filter " + filter.getKey() + " not text type."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.filter = filter;
	}
	
	@Override
	public String getText() {
		return filter.getGuiName();
	}
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append(filter.getKey());
		sb.append(currentOp);
		sb.append(" \""); //$NON-NLS-1$
		sb.append(currentValue);
		sb.append("\" "); //$NON-NLS-1$
		return sb.toString();
	}
	@Override
	public void initializeData(Object data) {
		if (data != null && data instanceof Object[]){
			Object[] initd = (Object[])data;
			this.currentOp = ((Operator)initd[0]).asSmartValue();
			this.currentValue = (String)initd[1];
		}
	}
	
	@Override
	public void dispose(){
		smallerFont.dispose();
		super.dispose();
	}
	
	
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);
	
		operators = new Combo(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		operators.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (currentOp != null
						&& currentOp.equals(operators.getText())) {
					// no change
				} else {
					currentOp = operators.getText();
					queryChanged();
				}
			}
		});
		
		FontData fd = (operators.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		operators.setFont(smallerFont);

		value = new Text(main, SWT.BORDER);
		value.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (currentValue != null
						&& currentValue.equals(value.getText())) {
					// nothing changed
				} else {
					queryChanged();
					value.setToolTipText(value.getText());
					currentValue = value.getText();
			}
			}
		});
		
		GridData gd = new GridData();
		gd.minimumWidth = 50;
		gd.widthHint = 100;
		value.setLayoutData(gd);

		Operator[] options = Operator.STRING_OPS;
		int index = 01;
		for (int i = 0; i < options.length; i++) {
			operators.add(options[i].getGuiValue());
			if (currentOp != null
					&& currentOp.equals(options[i].getGuiValue())) {
				index = i;
			}
		}
		operators.select(index);
		
		initDrag(main);
		initDrag(lblAttribute);
		
		lblAttribute.setText(formatStringForLabel(getText()));
		
		if (currentValue != null){
			if (value != null){
				value.setText(currentValue);
			}
		}
	}
	
}
