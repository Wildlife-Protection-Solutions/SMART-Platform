/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import javax.servlet.ServletRequest;
import javax.ws.rs.core.HttpHeaders;

/**
 * A collection of utility functions
 * 
 * @author Emily
 *
 */
public class SmartUtils {

	private static final char[] ALPHACHARS = "0123456789abcdefghiljklmnopqrstuvwxyz".toCharArray(); //$NON-NLS-1$
	public static final SimpleDateFormat DT_FORMAT = new SimpleDateFormat("yyyy-MM-dd H:m:s"); //$NON-NLS-1$
	public static final SimpleDateFormat D_FORMAT = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
	
	/**
	 * Finds the locale provided in the given headers.  Returns
	 * english locale if no locale found in headers.
	 * 
	 * @param headers
	 * @return
	 */
	public static Locale getRequestLocale(HttpHeaders headers){
		if (headers.getAcceptableLanguages().size() != 0){
			return headers.getAcceptableLanguages().get(0);
		}else{
			//return default en locale
			return Locale.ENGLISH;
		}
		
	}
	/**
	 * Finds the request locale.
	 * 
	 * @param request
	 * @return
	 */
	public static Locale getRequestLocale(ServletRequest request){
		return request.getLocale();
	}
	
	/**
	 * Generates a random string with the given length
	 * @param length
	 * @return
	 */
	public static String randomString(int length){
		char[] buf = new char[length];
		Random r = new Random();
		for (int i = 0; i < length; i ++){
			buf[i] = ALPHACHARS[r.nextInt(ALPHACHARS.length)];
		}
		return new String(buf);
	}
	
	/**
	 * Parses a date string.  Assumes the string is provided as either
	 * DT_FORMAT or D_FORMAT.
	 *  
	 * @param dateString
	 * @return
	 * @throws Exception
	 */
	public static Date parseDate(String dateString) throws Exception{
		try{
			return DT_FORMAT.parse(dateString);
		}catch (Exception ex){
		}
		return D_FORMAT.parse(dateString);
	}
	
	/**
	 * removes all non alphanumeric characters, 0_.from a string and replaces
	 * them with _
	 * @param filename
	 * @return
	 */
	public static String cleanFileName(String filename){
		return filename.replaceAll("[^a-zA-Z0-9-_\\.]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
