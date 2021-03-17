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
package org.wcs.smart.connect.hibernate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Supports urls of the form "platform:/plugin/<pluginid>/<resource>".
 * 
 * @author Emily
 *
 */
public class PlatformUrlHandler extends URLStreamHandler{

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		return new URLConnection(u) {

		    @Override
		    public void connect() throws IOException {
		        // nothing to be done
		    }

		    @Override
		    public InputStream getInputStream() {
		    	if (!getURL().getProtocol().equalsIgnoreCase("platform")) return null; //$NON-NLS-1$
		    	
		    	String path = getURL().getPath();
		    	if (!path.startsWith("/plugin/")) return null; //$NON-NLS-1$

		    	int index = path.indexOf('/', "/plugin/".length()); //$NON-NLS-1$
		    	path = path.substring(index);
		    	InputStream ss = getClass().getClassLoader().getResourceAsStream(path);
		    	return ss;
		    }
		};
		
	}

}
