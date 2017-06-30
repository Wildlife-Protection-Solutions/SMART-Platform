/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.routine;

import java.util.List;
import java.util.Locale;

import org.wcs.smart.qa.model.QaError;

/**
 * Updates status to ignore.  This is applicable to
 * all data providers.
 * 
 * @author Emily
 *
 */
public class IgnoreAction implements IQaAction {

	public final static IgnoreAction INSTANCE = new IgnoreAction();
	
	private IgnoreAction(){	
	}
	
	@Override
	public void doAction(List<QaError> items) {
		for (QaError i : items){
			i.setStatus(QaError.Status.IGNORED);
		}
	}

	@Override
	public boolean supportsMultiple() {
		return true;
	}

	@Override
	public String getId() {
		return "org.wcs.smart.qa.action.ignore"; //$NON-NLS-1$
	}

	@Override
	public String getName(Locale l) {
		return "Ignore";
	}

}
