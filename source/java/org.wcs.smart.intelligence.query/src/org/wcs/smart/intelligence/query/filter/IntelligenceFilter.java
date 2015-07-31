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
package org.wcs.smart.intelligence.query.filter;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.intelligence.query.ui.dropitem.IntelligenceDropItemFactory;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.util.SharedUtils;

/**
 * Intelligence item filter.
 * 
 * @author Emily
 *
 */
public class IntelligenceFilter implements IFilter {

	/**
	 * Creates new filter.
	 * 
	 * @param key
	 * @param operator
	 * @param value
	 * @return
	 */
	public static IntelligenceFilter parseIntelligenceFilter(String key, Operator operator, String value) {
		IntelligenceFilterOption op = null;
		for (IntelligenceFilterOption option : IntelligenceFilterOption.values()){
			if (option.getKey().equals(key.toLowerCase())){
				op = option;
				break;
			}
		}
		if (op == null){
			throw new RuntimeException(MessageFormat.format(Messages.IntelligenceFilter_InvalidFilterOption, key));
		}
		value = SharedUtils.stripQuotes(value);
		return new IntelligenceFilter(op, operator, value);
	}
	
	private IntelligenceFilterOption op = null;
	private Operator operator = null;
	private String value = null;
	
	public IntelligenceFilter(IntelligenceFilterOption key, Operator operator, String value) {
		this.op = key;
		this.operator = operator;
		this.value = value;
	}
	
	/**
	 * 
	 * @return the filter option
	 */
	public IntelligenceFilterOption getFilterOption(){
		return this.op;
	}
	
	/**
	 * 
	 * @return the filter operator
	 */
	public Operator getOperator(){
		if (operator == null){
			return Operator.STR_EQUALS;
		}
		return operator;
	}
	
	/**
	 * 
	 * @return the filter value 
	 */
	public String getValue(){
		return this.value;
	}
	
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(op.getKey());
		sb.append(" "); //$NON-NLS-1$
		sb.append(getOperator().asSmartValue());
		sb.append(" "); //$NON-NLS-1$
		sb.append("\""); //$NON-NLS-1$
		sb.append(value);
		sb.append("\""); //$NON-NLS-1$
		return sb.toString();
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		DropItem di = IntelligenceDropItemFactory.INSTANCE.createDropItem(op);
		di.initializeData(new Object[]{operator, value});
		return new DropItem[]{di};
	}

}
