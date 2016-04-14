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
package org.wcs.smart.report.birt.function;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionExecutor;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionFactory;
//import org.wcs.smart.hibernate.SmartDB;
//import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.report.execute.SmartTimezoneWrapper;

/**
 * Smart functions for BIRT reports
 * @author egouge
 * @since 1.0.0
 */
public class SmartCategoryFactory implements IScriptFunctionFactory {

	public static final String SMART_USER_FUNCTION = "SmartUser"; //$NON-NLS-1$
	public static final String CA_ID_FUNCTION = "ConservationAreaId"; //$NON-NLS-1$
	public static final String CA_NAME_FUNCTION = "ConservationAreaName"; //$NON-NLS-1$
	
	public SmartCategoryFactory() {
	}

	@Override
	public IScriptFunctionExecutor getFunctionExecutor(final String functionName)
			throws BirtException {
		
		return new IScriptFunctionExecutor() {
			@Override
			public Object execute(Object[] arguments, IScriptFunctionContext context)
					throws BirtException {
				
				SmartTimezoneWrapper ll = (SmartTimezoneWrapper) context.findProperty(IScriptFunctionContext.TIMEZONE);
				if (functionName.equals(SMART_USER_FUNCTION)){
					return ll.getUser();
				}else if (functionName.equals(CA_NAME_FUNCTION)){
					return ll.getConservationArea().getName();
				}else if (functionName.equals(CA_ID_FUNCTION)){
					return ll.getConservationArea().getId();
				}
				return null;
			}
		};
		
		
	}

}
