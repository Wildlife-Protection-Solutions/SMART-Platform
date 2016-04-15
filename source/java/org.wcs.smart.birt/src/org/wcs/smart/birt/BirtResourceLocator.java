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
package org.wcs.smart.birt;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.eclipse.birt.report.model.api.IResourceLocator;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.util.ResourceLocatorImpl;
import org.wcs.smart.SmartContext;

/**
 * Extension of BIRT Resource Locator to also check the smart connect
 * filestore directory for the resource.
 * 
 * @author Emily
 *
 */
public class BirtResourceLocator extends ResourceLocatorImpl {
	
	public static final BirtResourceLocator INSTANCE = new BirtResourceLocator();
	
	private BirtResourceLocator(){
		
	}
	
	@Override
	public URL findResource( ModuleHandle moduleHandle, String fileName,
			int type, Map appContext ){
		
		URL url = super.findResource(moduleHandle, fileName, type, appContext);
		if (url != null) return url;
		
		if (type != IResourceLocator.MESSAGE_FILE){
			File f = new File(SmartContext.INSTANCE.getFilestoreLocation(), fileName);
			if (f.exists()){
				try {
					return f.toURI().toURL();
				} catch (MalformedURLException e) {
					Activator.log(e);
				}
			}
		}
		return null;
	}
}
