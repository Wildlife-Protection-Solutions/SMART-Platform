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
package org.wcs.smart.common.celleditor;

import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;


/**
 * Cell editor that ensure input is double value
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DoubleCellEditor extends TextCellEditor {
	
	private boolean canNull = false;
	
	/**
	 * <p>If the value is allowed to be null then
	 * the cell editor allows empty values to be 
	 * maintained.  Otherwise users cannot enter
	 * and empty value.</p>
	 * Creates a new cell editor
	 * @param parent parent 
	 * @param canNull if the value can be null 
	 */
	public DoubleCellEditor(Composite parent, boolean canNull){
		this(parent);
		this.canNull = canNull;
	}
	
	/**
	 * Creates a new cell editor that cannot be null;
	 * 
	 * @param composite
	 */
	public DoubleCellEditor(Composite composite) {
		super(composite);
		setValidator(new ICellEditorValidator() {
			public String isValid(Object object) {
				if (object == null || object instanceof Double || object instanceof Float) {
					return null;
				} else {
					String string = (String) object;
					if (canNull && string.trim().length() == 0){
						return null;
					}
					try {
						Double.parseDouble(string);
						return null;
					} catch (NumberFormatException exception) {
						return exception.getLocalizedMessage();
					}
				}
			}
		});
	}

	@Override
	public Object doGetValue() {
		if (super.doGetValue() == null || ((String)super.doGetValue()).trim().length() == 0 ){
			return null;
		}
		return Double.parseDouble((String) super.doGetValue());
	}

	@Override
	public void doSetValue(Object value) {
		if (value == null){
			super.doSetValue(""); //$NON-NLS-1$
		}else{
			super.doSetValue(value.toString());
		}
	}
	
	@Override
	public LayoutData getLayoutData() {
		LayoutData data = super.getLayoutData();
		data.minimumHeight = text.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		return data;
	}
}
