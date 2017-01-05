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

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;

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
		if (item instanceof AttributeType){
			AttributeType type = (AttributeType) item;
			
			if (type == AttributeType.BOOLEAN){
				return "BOOLEAN";
			}else if (type == AttributeType.DATE){
				return "DATE";
			}else if (type == AttributeType.LIST){
				return "LIST";
			}else if (type == AttributeType.NUMERIC){
				return "NUMERIC";
			}else if (type == AttributeType.TEXT){
				return "TEXT";
			}
		}
		
		if (item == IntelWorkingSetCategory.ENTITY) return "Entities";
		if (item == IntelWorkingSetCategory.RECORD) return "Records";
		if (item == IntelWorkingSetCategory.QUERIES) return "Queries";
		
		return null;
		
	}
	
	public Image getImage(Object item){

		if (item == IntelWorkingSetCategory.RECORD){
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD);
		}else if (item == IntelWorkingSetCategory.ENTITY){
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY);
		}
		return null;
	}

}
