package org.wcs.smart.i2.search;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.commons.codec.language.DoubleMetaphone;

public class DerbyFuzzyFunctions {

	private static final DoubleMetaphone DOUBLE_METAPHONE = new DoubleMetaphone();
	
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
	
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");
	
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
			sb.append(" ");
		}
		return sb.toString();
	}

	public final static void main (String args[]){
//		String ss = "NPF";
//		String s = "NPF KNTR RNFL TMNF KN0R SNSP N ";
//		System.out.println(metaphoneContains(ss, s));
		System.out.println(doubleMetaphone("pyroantimonic swaimous recompression unglutinate stomatocace tragicomic"));
		System.out.println(doubleMetaphone("impaternate overcluster"));
		
	}
}
