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
package org.wcs.smart.i2.search;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.commons.codec.language.DoubleMetaphone;
/**
 * Fuzzy search functions for use in Derby
 * 
 * @author Emily
 *
 */
public class DerbyFuzzyFunctions {

	private static final DoubleMetaphone DOUBLE_METAPHONE = new DoubleMetaphone();
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+"); //$NON-NLS-1$
	
	/**
	 * Determines if one metaphone is contained within a set of metaphones represented
	 * by a single space deleminted string.
	 * @param string a single metaphone value
	 * @param metaphone a collection of metaphone values separated by a space (use doubleMetaphone
	 * function to generate this string).
	 * @return
	 */
	public static boolean metaphoneContains(String string, String metaphone){
		char[] c1 = string.toCharArray();
		char[] c2 = metaphone.toCharArray();
		
		int c1index = 0;
		boolean allmatch = true;
		for (int i = 0; i < c2.length; i ++){
			if (c2[i]==' '){
				if (allmatch && c1index == c1.length) return true;
				c1index = 0;
				if (i != c2.length-1) allmatch = true;
			}else{
				if (c1index >= c1.length){
					allmatch = false;
				}else if (c2[i] != c1[c1index]){
					allmatch = false;
				}
				c1index++;
			}
		}
		if (c1index == c1.length) return allmatch;
		return false;

	}
	
	/**
	 * Computes the double metaphone version of a string.  The string
	 * is split into words by whitespace, then both versions of the metaphone
	 * pattern are computed.  These are all put into a set then concatenated using
	 * a space to form a single string.  They are not ordered in any way. 
	 * @param string
	 * @return
	 */
	public static String doubleMetaphone(String string){
		if (string == null) return null;
		if (string.isEmpty()) return null;
		HashSet<String> found = new HashSet<String>();
		for (String x : SPLIT_PATTERN.split(string)){
			if (!x.trim().isEmpty()){
				String s1 = DOUBLE_METAPHONE.doubleMetaphone(x, false);
				String s2 = DOUBLE_METAPHONE.doubleMetaphone(x, true);
				if (!s1.isEmpty()){
					found.add(s1);
					found.add(s2);
				}
			}
		}
		if (found.isEmpty()) return null;
		StringBuilder sb = new StringBuilder();
		for(String x : found){
			sb.append(x);
			sb.append(" "); //$NON-NLS-1$
		}
		return sb.toString();
	}

}
