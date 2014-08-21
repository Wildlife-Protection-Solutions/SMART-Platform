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
package org.wcs.smart.query.model.filter;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.query.ui.model.impl.ObserverDropItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Observer query filter.
 * 
 * <p>Form: 'wpnobs:observer  equals "<HEX_UUID>"'</p>
 * 
 * @author Emily
 *
 */
public class ObserverFilter implements IFilter {

	public static ObserverFilter createFilter(String value){
		return new ObserverFilter(value);
	}
	
	private String value;
	
	public ObserverFilter(String value){
		this.value = SmartUtils.stripQuotes(value);
	}

	@Override
	public String asString() {
		return "wpnobs:observer " + Operator.STR_EQUALS.asSmartValue() + " \"" + value + "\"";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);

	}
	
	public String getValue(){
		return this.value;
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		Employee e = (Employee) session.load(Employee.class, SmartUtils.decodeHex(value));
		DropItem di;
		if (e == null){
			di = new ErrorDropItem(MessageFormat.format(Messages.ObserverFilter_EmployeeNotFound, new Object[]{value}));
		}else{
			e.getUuid();
			e.getFullLabel();
			
			di = new ObserverDropItem();
			di.initializeData(e);
		}
		return new DropItem[]{di};
	}

}
