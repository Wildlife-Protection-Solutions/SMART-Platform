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
package org.wcs.smart.map.internal.settings;

import java.util.HashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.locationtech.udig.core.internal.ExtensionPointProcessor;
import org.locationtech.udig.core.internal.ExtensionPointUtil;
import org.locationtech.udig.project.StyleContent;
import org.locationtech.udig.project.internal.ProjectPlugin;

/**
 * This class is responsible to create and maintian the set of {@link SyleContent}.
 * <p>
 * The StyleContent is used to transform the xml memento string to Style object.
 * </p>
 * @author Mauricio Pazos
 *
 */
public final class SyleContentFactory {
	
	/** cache of created StyleConten */
	private static HashMap<String, StyleContent> STYLE_TO_CONTENT = new HashMap<String, StyleContent>();
    

	/**
	 * Returns the {@link SyleContent} for the style
	 * 
	 * @param styleId id of style
	 * @return the  {@link SyleContent} required
	 */
    public static synchronized StyleContent getStyleContentFor( String styleId ) {
        // looks in local cache first
        StyleContent styleContent = STYLE_TO_CONTENT.get(styleId);
        if (styleContent == null) {
        	// needs create a new one
            loadStyleContent(styleId);
            styleContent = STYLE_TO_CONTENT.get(styleId);
        }
        return styleContent;
    }
    
    /**
     * Creates the  {@link SyleContent} for the required style 
     * @param styleId
     */
    private static void loadStyleContent( final String styleId ) {

        STYLE_TO_CONTENT.put(styleId, StyleContent.DEFAULT); // default to use of we cannot find a specific one
        ExtensionPointProcessor p = new ExtensionPointProcessor(){
            boolean found = false;
            public void process( IExtension extension, IConfigurationElement element )
                    throws Exception {
                if (!found && element.getAttribute("id").equals(styleId)) { //$NON-NLS-1$
                    found = true;
                    StyleContent styleContent = (StyleContent) element.createExecutableExtension("class"); //$NON-NLS-1$
                    STYLE_TO_CONTENT.put(styleId, styleContent);
                }
            }
        };
        ExtensionPointUtil.process(ProjectPlugin.getPlugin(), StyleContent.XPID, p);
    }
	

}
