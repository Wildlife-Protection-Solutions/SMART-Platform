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
package org.wcs.smart.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Comparator for sorting Locale.  Languages without countries
 * appear first in alphebetical order, then languages
 * with countries.
 * 
 * @author Emily
 * @since 2.0.1
 *
 */
public class LocaleComparator implements Comparator<Locale> {
	
	@Override
	public int compare(Locale o1, Locale o2) {
		if (o1.getCountry().isEmpty() && !o2.getCountry().isEmpty()){
			return -1;
		}else if (!o1.getCountry().isEmpty() && o2.getCountry().isEmpty()){
			return 1;
		}
		if (o1.getDisplayLanguage().equals(o2.getDisplayLanguage())){
			return Collator.getInstance().compare(o1.getDisplayCountry(), o2.getDisplayCountry());
		}else{
			return Collator.getInstance().compare(o1.getDisplayLanguage(), o2.getDisplayLanguage());
		}
	}
}
