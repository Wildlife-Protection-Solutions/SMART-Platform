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
package org.wcs.smart;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.List;

/**
 * Tools to support additional locales in addition to 
 * defaults provided by java.
 * 
 * @author Emily
 *
 */
public class Localization {

	private static final String PROPERTIES = "/properties/locales.properties"; //$NON-NLS-1$
	
	/**
	 * Returns all locals supported by the smart language
	 * @return
	 */
	public static Locale[] getSupportedLocals(){
		Locale[] defaults = Locale.getAvailableLocales();
		List<Locale> allLocales = new ArrayList<Locale>();
		for (Locale d : defaults){
			if (!d.toString().isEmpty()){
				allLocales.add(d);
			}
		}
		try{
			for (Locale l : readLocaleSupport()){
				allLocales.add(l);
			}
		}catch (Exception ex){
			SmartPlugIn.log("Could not read additional from properties files (properties/locales.properties).", ex); //$NON-NLS-1$
		}
		
		return allLocales.toArray(new Locale[allLocales.size()]);
	}
	
	private static Locale[] readLocaleSupport() throws Exception{
		ArrayList<Locale> adds = new ArrayList<Locale>();
		Properties prop = new Properties();
		prop.load(Localization.class.getResourceAsStream(PROPERTIES));
		for (Object x: prop.keySet()){
			String ll = (String) x;
			adds.add(new Locale(ll));
		}
		return adds.toArray(new Locale[adds.size()]);
		
	}
}
