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

import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityPart;

/**
 * Simple utility methods for e4/e3 compatibility layer
 * @author Emily
 *
 */
//E3
public class E3Utils {

	/**
	 * Gets the source object for a part.  This goes one level before the compatibilitypart of
	 * parts which are part of the compatibility layer; and gets components of DIViewParts.
	 * @param part
	 * @return
	 */
	public static Object getSourceObject(MPart part) {
		if (part == null)
			return null;
		Object x = part.getObject();
		if (x == null)
			return null;
		if (x instanceof CompatibilityPart) {
			x = ((CompatibilityPart) x).getPart();
		}
		if (x instanceof DIViewPart){
			x = ((DIViewPart<?>)x).getComponent();
		}
		return x;
	}
	
	/**
	 * checks the element id against "org.eclipse.e4.ui.compatibility.editor" 
	 * @param element
	 * @return
	 */
	public static boolean isCompatibilityEditor(MUIElement element){
		return "org.eclipse.e4.ui.compatibility.editor".equals(element.getElementId()); //$NON-NLS-1$
	}
	
	/**
	 * determines if the current element is the e3 editor area or any of
	 * it's parents are the e3 editor area
	 * @param current
	 * @return
	 */
	public static boolean isEditorArea(MUIElement current){
		MUIElement parent = current;
		while(parent != null){
			if ("org.eclipse.ui.editorss".equals(parent.getElementId())){ //$NON-NLS-1$
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}
}
