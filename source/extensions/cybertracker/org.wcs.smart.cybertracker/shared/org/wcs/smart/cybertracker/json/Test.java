package org.wcs.smart.cybertracker.json;

import com.ibm.icu.text.MessageFormat;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Integer x = 10;
		if (x instanceof Number) System.out.println(((Number)x).intValue());
		
		Double y = 10.0;
		if (y instanceof Number) System.out.println(((Number)y).intValue());
		
		Float z = 34.2f;
		if (z instanceof Number) System.out.println(((Number)z).intValue());
		
		Long a = 342l;
		if (a instanceof Number) System.out.println(((Number)a).intValue());
		
		String bits[] = {"a", "b"};
		System.out.println(MessageFormat.format("This {0} is {1}", bits));
	}

}
