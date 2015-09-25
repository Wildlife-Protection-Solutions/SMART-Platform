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
package org.wcs.smart.dataentry;

import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttributeOption;

/**
 * Label provided for some of the configurable model options.
 * 
 * @author Emily
 *
 */
public enum CmAttributeOptionLabelProvider {
	INSTANCE;
	
	public String getGuiName(CmAttributeOption.EnterOnceType optiontype){
		switch(optiontype){
			case END: return Messages.CmAttributeOption_EnterOnceType_END;
			case NONE: return Messages.CmAttributeOption_EnterOnceType_NONE;
			case START: return Messages.CmAttributeOption_EnterOnceType_START;
		}
		return ""; //$NON-NLS-1$
	}

}
