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
package org.wcs.smart.upgrade.v112;

import java.io.File;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;


/**
 * General utility functions.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SmartUtils {

	public static final String INVALID_FILENAME_CHARS_PATTERN = "[\"',/:;<>\\\\|]"; //$NON-NLS-1$
	
	/**
	 * Various pre-defined regex expressions
	 * for validating strings in smart.
	 */
	public static enum RegExLevel{
		/**
		 * Allows only letters and digits (a-Z and 0-9)
		 */
		ALLOWED_CHARS_SIMPLE_REGEX( "[^\\p{L}\\p{M}\\p{Nd}-]"),  //\p{L} - letters \p{Nd} - digits	is a digit zero through nine //$NON-NLS-1$
		/**
		 * Allows chars, digits, - _ and :
		 */
		ALLOWED_CHARS_MED_REGEX("[^^\\p{L}\\p{M}\\p{Nd}-:_]" ), //$NON-NLS-1$
		/**
		 * Allows chars, digits, _ : & and spaces
		 */
		ALLOWED_CHARS_COMPLEX_REGEX("[^^\\p{L}\\p{M}\\p{Nd}- :_&'<>,\\(\\)\\.\\#;\\/]"); //$NON-NLS-1$
		
		public final String regex;
	
		RegExLevel(String regex){
			this.regex = regex;
		}
	}
	

	
	/**
	 * <p>
	 * Strips double quotes off the beginning and end of the string
	 * only if they exist in both places.  Examples:</p>
	 * <p>"abc" -> abc</p>
	 * <p>abc" -> abc"</p>
	 * <p>"abc -> "abc</p>
	 * <p>""abc"" -> "abc"</p>
	 *  
	 * @param str 
	 * @return string with quotes removed
	 */
	public static String stripQuotes(String str){
		if (str == null) return str;
		if (str.charAt(0)=='"' && str.charAt(str.length()-1) == '"'){
			return str.substring(1, str.length() - 1);
		}
		return str;
		
	}
	
	/**
	 * Checks a string:
	 * <ol>
	 * <li> only contains letters number and a few other common characters like spaces and _ - : & 
	 * <li>is between 1-64 chars long 
	 * </ol>
	 * @param str - the string which to validate
	 * @param level the regex level to match to
	 * @param maxChars the maximum number of characters in the string
	 * @param minChar the minimum number of characters
	 * @return <code>true</code> if matches the required parameters, <code>false</code> otherwise 
	 */
	public static Boolean isSimpleString (String str, RegExLevel level, 
			Integer maxChars, Integer minChar){
		Pattern p = Pattern.compile( level.regex , Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(str);
		boolean b = m.find();
		if (b || str.length() > maxChars || str.length() < minChar ){
			return false;
		}else{ 
			return true;
		}		
	}

	/**
	 * Determines if a string matches the given requirements and is
	 * at least one character long.
	 * 
	 * @param str the string to match
	 * @param level the regexlevel to match it to
	 * @param maxChars the maximum number of characters
	 * @return <code>true</code> if meets the above requirements, <code>false</code> otherwise
	 */
	public static Boolean isSimpleString (String str, RegExLevel level, Integer maxChars){
		return isSimpleString(str, level, maxChars,1);
	}
	
	/**
	 * Determines if a string is at least one character long,
	 * no longer than 64 characters, and matches the 
	 * ALLOWED_CHARS_MED_REGEX expression.
	 * 
	 * @param str the string to match
	 * @return <code>true</code> if meets the above requirements, <code>false</code> otherwise
	 */
	public static Boolean isSimpleString (String str){
		return isSimpleString(str, SmartUtils.RegExLevel.ALLOWED_CHARS_MED_REGEX, 64,1);
	}

	/**
	 * Determines if a string is at least one character long,
	 * no longer than 64 characters, and macthes the 
	 * provided expression.
	 * 
	 * @param str the string to match
	 * @param level the regex level to match to
	 * @return <code>true</code> if meets the above requirements, <code>false</code> otherwise
	 */
	public static Boolean isSimpleString (String str, RegExLevel level){
		return isSimpleString(str, level, 64, 1);
	}
	
	/**
	 * Counts all the files in a directory including
	 * all sub-directories.
	 * 
	 * @param directory the directory to start at
	 * @return the total number of files
	 */
	public static int countFiles(File directory){
		int count = 0;
		for (File f : directory.listFiles()){
			if (f.isFile()){
				count++;
			}else{
				count += countFiles(f);
			}
		}
		return count;
	}

	
	/**
	 * Determine if the given file is a zip file or not.
	 * 
	 * @param file the file to check
	 * @return <code>true</code> if file is zip file, <code>false</code> otherwise
	 */
	public static boolean isZip(File file){
		try{
			ZipFile zout = new ZipFile(file);
			zout.entries();
			zout.close();
			return true;
		}catch (Exception ex){
			
		}
		return false;
		
	}
	
	/**
	 * Formats a string for placement in an swt label.
	 * <p>
	 * Replaces all & with &&
	 * </p>
	 * @param text
	 * @return
	 */
	public static String formatStringForLabel(String text){
		return text.replaceAll("&", "&&");  //$NON-NLS-1$//$NON-NLS-2$
	}
	
	
	/**
	 * Converts a local string stored as lang_Country
	 * to a Locale object.
	 * 
	 * @param s
	 * @return
	 */
	public static Locale stringToLocale(String s)
	{
	    String l = ""; //$NON-NLS-1$
	    String c = ""; //$NON-NLS-1$
		StringTokenizer tempStringTokenizer = new StringTokenizer(s.trim(),"_"); //$NON-NLS-1$
	    if(tempStringTokenizer.hasMoreTokens())
	    	l = (String) tempStringTokenizer.nextElement();
	    
	    if(tempStringTokenizer.hasMoreTokens())
	    	c = (String) tempStringTokenizer.nextElement();
	    return new Locale(l,c);
	}
	
	/**
	 * Converts a local string stored as lang_Country
	 * to a Locale object.
	 * 
	 * @param s
	 * @return
	 */
	public static String localeToString(Locale l)
	{
	    String key = l.getLanguage();
	    if (!l.getCountry().isEmpty()){
	    	key += "_" + l.getCountry(); //$NON-NLS-1$
	    }
	    return key.trim();
	}
	
	
	
	
}
