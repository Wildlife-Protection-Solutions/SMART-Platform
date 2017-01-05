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
package org.wcs.smart.i2.ui.views.query;

import java.util.Locale;

import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextOperatorDropItem;

/**
 * Tree filter item that represents operators
 * @author Emily
 *
 */
public class OperatorTreeFilterItem extends BasicTreeFilterItem{

	private Operator op;
	
	public OperatorTreeFilterItem(Operator op){
		super(op.getLabel(Locale.getDefault()));
		this.op = op;
	}
	
	@Override
	public DropItem[] asDropItem() {
		if (op == Operator.BRACKETS){
			return new DropItem[]{ new TextOperatorDropItem(Operator.BRACKET_OPEN), new TextOperatorDropItem(Operator.BRACKET_CLOSE)};
		}
		return new DropItem[]{new TextOperatorDropItem(op)};
	}
}
