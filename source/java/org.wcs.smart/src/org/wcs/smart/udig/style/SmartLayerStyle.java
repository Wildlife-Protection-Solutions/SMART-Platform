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
package org.wcs.smart.udig.style;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IMemento;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.StyleContent;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.util.SmartUtils;

/**
 * For tracking a SMART Saved style.  This style stores the uuid
 * of the saved style.
 * 
 * @author Emily
 *
 */
public class SmartLayerStyle extends StyleContent {

	public static final String STYLE_ID = "org.wcs.smart.udig.savedStyle"; //$NON-NLS-1$
	
	public SmartLayerStyle() {
		super(STYLE_ID);
	}

	@Override
	public Class<?> getStyleClass() {
		return byte[].class;
	}

	@Override
	public void save(IMemento memento, Object value) {
		byte[] uuid = (byte[]) value;
		memento.putString("uuid", SmartUtils.encodeHex(uuid)); //$NON-NLS-1$

	}

	@Override
	public Object load(IMemento memento) {
		try{
			byte[] uuid = SmartUtils.decodeHex(memento.getString("uuid")); //$NON-NLS-1$
			return uuid;
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return null;
	}

	@Override
	public Object load(URL url, IProgressMonitor monitor) throws IOException {
		return null;
	}

	@Override
	public Object createDefaultStyle(IGeoResource resource, Color colour,
			IProgressMonitor monitor) throws IOException {
		return null;
	}

}
