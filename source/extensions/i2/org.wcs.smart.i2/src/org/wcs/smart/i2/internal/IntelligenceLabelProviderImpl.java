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
package org.wcs.smart.i2.internal;

import java.util.Locale;

import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;

/**
 * Desktop label provider for intelligence module.
 * 
 * @author Emily
 *
 */
public class IntelligenceLabelProviderImpl implements
		IIntelligenceLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof IAttributeType){
			IAttributeType type = (IAttributeType) item;
			
			if (type == IAttributeType.BOOLEAN){
				return "BOOLEAN";
			}else if (type == IAttributeType.DATE){
				return "DATE";
			}else if (type == IAttributeType.LIST){
				return "LIST";
			}else if (type == IAttributeType.NUMERIC){
				return "NUMERIC";
			}else if (type == IAttributeType.TEXT){
				return "TEXT";
			}
		}
		return null;
		
	}

}
