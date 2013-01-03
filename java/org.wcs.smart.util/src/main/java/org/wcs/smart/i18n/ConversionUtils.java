package org.wcs.smart.i18n;

public class ConversionUtils {

	/**    
     * prefix of ascii string of native character    
     */     
    private static String PREFIX = "\\u";   
	
	public static String asciiToNative(String x){
		StringBuilder sb = new StringBuilder();
		int begin = 0;
		int index = x.indexOf(PREFIX);
		while(index > 0){
			sb.append(x.substring(begin, index));
			sb.append(asciiToChar(x.substring(index, index+6)));
			begin = index + 6;
			index = x.indexOf(PREFIX, begin);
		}
		sb.append(x.substring(begin));
		return sb.toString();
		
	}
	
	private static char asciiToChar(String x){
		if (x.length() != 6){
			throw new IllegalArgumentException("Invalid ascii character string - not 6 characters");
		}
		if (!x.startsWith(PREFIX)){
			throw new IllegalArgumentException("Invalid ascii character string - does not start with " + PREFIX);
		}
		String tmp = x.substring(2, 4);
		int code = Integer.parseInt(tmp, 16) << 8;
		tmp = x.substring(4,6);
		code += Integer.parseInt(tmp, 16);
		return (char)code;
	}

	

	public static String nativeToAscii(String x){
		StringBuilder sb = new StringBuilder();
		char[] chars = x.toCharArray();
		for (int i = 0; i < chars.length; i ++){
			sb.append(charToAscii(chars[i]));
		}
		return sb.toString();	
	}
	
	private static String charToAscii(char x){
		if (x <= 127){
			return Character.toString(x);
		}
		StringBuilder sb = new StringBuilder();  
        sb.append(PREFIX);      
        int code = (x >> 8);      
        String tmp = Integer.toHexString(code);      
        if (tmp.length() == 1) {      
            sb.append("0");      
        }      
        sb.append(tmp);      
        code = (x & 0xFF);      
        tmp = Integer.toHexString(code);      
        if (tmp.length() == 1) {      
            sb.append("0");      
        }      
        sb.append(tmp);      
        return sb.toString();      
	}
}
