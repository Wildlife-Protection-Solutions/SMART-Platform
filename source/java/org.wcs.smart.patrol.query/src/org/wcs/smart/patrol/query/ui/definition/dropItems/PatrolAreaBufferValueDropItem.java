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
package org.wcs.smart.patrol.query.ui.definition.dropItems;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;

/**
 * Patrol value drop item
 * @author egouge
 * @since 1.0.0
 */
public class PatrolAreaBufferValueDropItem extends AbstractValueDropItem{
	
	protected PatrolValueOption item;
	protected String guiLabel;
	
	protected Text txtBuffer;
	private double initValue = 10.0;
	
	/**
	 * Creates a new drop item
	 * @param item
	 */
	public PatrolAreaBufferValueDropItem(PatrolValueOption item){
		super(true);
		this.item = item;
		this.guiLabel = item.getGuiName(Locale.getDefault());
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#getValueText()
	 */
	@Override
	public String getValueText() {
		return this.guiLabel;
	}
	
	public void updateGuiName(String newName){
		this.guiLabel = newName;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#getValueQueryPart()
	 */
	@Override
	public String getValueQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("patrol:sum:"); //$NON-NLS-1$
		sb.append(item.getKeyPart());
		sb.append(":"); //$NON-NLS-1$
		sb.append(txtBuffer.getText());
		return sb.toString();
		
	}


	@Override
	protected void createValueComposite(Composite parent) {
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		
		Label lbl = new Label(temp, SWT.NONE);
		lbl.setText( formatStringForLabel(item.getGuiName(Locale.getDefault())));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		initDrag(lbl);
		
		lbl = new Label(temp, SWT.NONE);
		lbl.setText(Messages.PatrolAreaBufferValueDropItem_BufferText);			
		lbl.setToolTipText(Messages.PatrolAreaBufferValueDropItem_buffertooltip2);
		
		txtBuffer = new Text(temp, SWT.BORDER);
		txtBuffer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtBuffer.setText(String.valueOf( initValue));
		txtBuffer.addModifyListener(m->{
			try {
				initValue = Double.parseDouble(txtBuffer.getText());
			}catch (Exception ex) {}
						
			super.queryChanged();
		});
		initDrag(lbl);
		initDrag(temp);
	}

	/**
	 * Does nothing
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#initializeValueData(java.lang.Object)
	 */
	@Override
	protected void initializeValueData(Object data) {
		if (data instanceof Double) {
			initValue = (Double)data;
			if (txtBuffer != null) txtBuffer.setText(String.valueOf( initValue));
		}
	}

}
