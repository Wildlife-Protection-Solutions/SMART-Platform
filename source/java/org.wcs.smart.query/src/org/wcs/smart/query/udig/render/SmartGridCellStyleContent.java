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
package org.wcs.smart.query.udig.render;

import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import javax.xml.transform.TransformerException;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.StyleContent;
import net.refractions.udig.style.sld.SLDPlugin;
import net.refractions.udig.ui.graphics.SLDs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IMemento;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.styling.SLDParser;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.StyleFactory;
import org.opengis.style.Style;

public class SmartGridCellStyleContent extends StyleContent {
	
    /** factory used to create style content * */
	private static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());
    
	public static final String STYLE_ID = "org.wcs.smart.query.grid.style.border"; //$NON-NLS-1$
	
	public SmartGridCellStyleContent() {
		super(STYLE_ID);
	}

	@Override
	public Class<?> getStyleClass() {
		return Style.class;
	}

	@Override
	public void save(IMemento memento, Object value) {
		Style style = (Style) value;

		// serialize out the style objects
		SLDTransformer sldWriter = new SLDTransformer();
		String out = ""; //$NON-NLS-1$
		try {
			out = sldWriter.transform(style);
		} catch (TransformerException e) {
			SLDPlugin.log("SLDTransformer failed", e); //$NON-NLS-1$
			e.printStackTrace();
		} catch (Exception e) {
			SLDPlugin.log("SLDTransformer failed", e); //$NON-NLS-1$
		}
		memento.putTextData(out);
		memento.putString("type", "SLDStyle"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putString("version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Object load(IMemento momento) {
		// parse the sld object
        if( momento.getTextData()==null )
            return null;
        StringReader reader = new StringReader(momento.getTextData());
        SLDParser sldParser = new SLDParser(getStyleFactory(), reader);

        Style[] parsed = sldParser.readXML();
        if (parsed != null && parsed.length > 0)
            return parsed[0];

        return null;
	}

	@Override
	public Object load(URL url, IProgressMonitor monitor) throws IOException {
		return parse(url);
	}
	
	public static StyleFactory getStyleFactory() {
		return styleFactory;
	}

	public static Style parse(URL url) throws IOException {
		return SLDs.parseStyle(url);
	}

	@Override
	public Object createDefaultStyle(IGeoResource resource, Color colour,
			IProgressMonitor monitor) throws IOException {
		return null;
	}

}
