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
package org.wcs.smart.event.internal;

import java.util.Locale;

import org.wcs.smart.event.IActionLabelProvider;
import org.wcs.smart.event.filter.Operator;

/**
 * Implementation of action label provider
 * 
 * @author Emily
 *
 */
public class ActionLabelProviderImpl implements IActionLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == Operator.AND) return Messages.ActionLabelProviderImpl_AndOperator;
		if (item == Operator.BETWEEN) return Messages.ActionLabelProviderImpl_BetweenOperator;
		if (item == Operator.BRACKETS) return "(  )"; //$NON-NLS-1$
		if (item == Operator.EQUALS) return "="; //$NON-NLS-1$
		if (item == Operator.GREATERTHAN) return ">"; //$NON-NLS-1$
		if (item == Operator.GREATERTHANEQUALS) return ">="; //$NON-NLS-1$
		if (item == Operator.LESSTHAN) return "<"; //$NON-NLS-1$
		if (item == Operator.LESSTHANEQUALS) return "<="; //$NON-NLS-1$
		if (item == Operator.NOT) return Messages.ActionLabelProviderImpl_NotOperator;
		if (item == Operator.NOTEQUALS) return "!="; //$NON-NLS-1$
		if (item == Operator.NOT_BETWEEN) return Messages.ActionLabelProviderImpl_NotBetweenOperator;
		if (item == Operator.OR) return Messages.ActionLabelProviderImpl_OrOperator;
		if (item == Operator.STR_CONTAINS) return Messages.ActionLabelProviderImpl_StrContainsOperator;
		if (item == Operator.STR_EQUALS) return Messages.ActionLabelProviderImpl_StrEqualsOperator;
		if (item == Operator.STR_NOTCONTAINS) return Messages.ActionLabelProviderImpl_StrNotContainsOperator;	
		return null;
	}
}
