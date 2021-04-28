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
package org.wcs.smart.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Util for manipulations with colors
 *  
 * @author elitvin
 * @since 6.0.0
 *
 */
public class ColorUtil {

	/**
	 * Convert {@link Color} into hex string representation (e.g. "#FFFFFF")
	 * 
	 */
	public static String color2HexStr(RGB color) {
		if (color == null) {
			return null;
		}
		String hex = String.format("#%02X%02X%02X", color.red, color.green, color.blue); //$NON-NLS-1$
		return hex;
	}
	
	/**
	 * Convert string representation into {@link Color}
	 * 
	 * @param colorStr e.g. "#FFFFFF"
	 * @return color
	 */
	public static Color hex2Color(String colorStr) {
		if (colorStr == null || colorStr.isEmpty()) {
			return null;
		}
		int r = Integer.valueOf(colorStr.substring(1, 3), 16);
		int g = Integer.valueOf(colorStr.substring(3, 5), 16);
		int b = Integer.valueOf(colorStr.substring(5, 7), 16);
	    return new Color(Display.getCurrent(), r ,g, b);
	}

}
