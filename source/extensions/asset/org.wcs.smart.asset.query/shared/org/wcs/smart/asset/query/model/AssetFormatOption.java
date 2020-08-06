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
package org.wcs.smart.asset.query.model;

import java.util.Locale;
import java.util.function.Function;

import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.IAssetLabelProvider;
import org.wcs.smart.asset.ui.IQueryAssetLabelProvider;

/**
 * Options for formatting asset time related summary values.
 * 
 * @author Emily
 *
 */
public enum AssetFormatOption {

	DAYS_HOUR("dayshour"), //$NON-NLS-1$
	SECOND("second"); //$NON-NLS-1$
	
	String key;		//unique key
	
	AssetFormatOption(String queryKey){
		this.key = queryKey;
	}
	
	public String getGuiName(Locale l){
		return SmartContext.INSTANCE.getClass(IQueryAssetLabelProvider.class).getLabel(this, l);
	}
	
	public String getKey() {
		return this.key;
	}
	
	public Function<Double, String> getFormatter(Locale l){
		if (this == SECOND) return null;
		
		return timeInSeconds->{
			
			if (timeInSeconds == null) return ""; //$NON-NLS-1$
			return ((IAssetLabelProvider)SmartContext.INSTANCE.getClass(IAssetLabelProvider.class)).formatTime(timeInSeconds, l);
		};
		
	}
}
