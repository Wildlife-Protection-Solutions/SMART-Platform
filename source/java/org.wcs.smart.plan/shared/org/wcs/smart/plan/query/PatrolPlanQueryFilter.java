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
package org.wcs.smart.plan.query;

import org.wcs.smart.patrol.query.parser.IExtensionFilter;
import org.wcs.smart.patrol.query.parser.IPatrolQueryOption;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;

/**
 * "Patrol is a part of a plan" Query Filter
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolPlanQueryFilter implements IExtensionFilter {

	private IPatrolQueryOption option;
	private Operator op;	
	private Object value;

	public PatrolPlanQueryFilter(IPatrolQueryOption option, Operator op, Object value) {
		this.option = option;
		this.op = op;
		this.value = value;
	}

	@Override
	public String asString() {
		return "patrol:" + option.getKey() + " " + op.asSmartValue() + " " + value;  //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$
	}

	
	public boolean isAnyPlan(String v) {
		return v == null || v.isEmpty();
	}
	
	public IPatrolQueryOption getOption(){
		return this.option;
	}
	
	public Object getValue(){
		return this.value;
	}
	

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}
	
}
