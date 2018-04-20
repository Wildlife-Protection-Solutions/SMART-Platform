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
package org.wcs.smart.p2;

/**
 * An advisor plugins can install to prevent the 
 * installation or uninstallation of plugins
 * 
 * @author Emily
 *
 */
public interface IPluginAdvisor {

	public static final String EXTENSION_ID = "org.wcs.smart.p2.pluginadvisor"; //$NON-NLS-1$
	
	/**
	 * 
	 * @return a string describing why cannot uninstall plugin or null if you can uninstall plugin
	 */
	default public String canUninstall() { return null; }
	
	/**
	 * 
	 * @return a string describes why cannot install plugin or null if you can install plugin
	 */
	default public String canInstall() { return null; }
}
