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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.style.sld.editor.EditorNode;
import org.locationtech.udig.style.sld.editor.EditorPageManager;
import org.wcs.smart.SmartPlugIn;

/**
 * Manager for style editor page
 * 
 * @author Emily
 *
 */
public class StyleEditorPageManager {

	/**
	 * List of styles to exclude from the smart style
	 * editor
	 */
	private final static Collection<String> EXCLUDED_STYLES = 
			Arrays.asList("cache",  //$NON-NLS-1$
					"filter",  //$NON-NLS-1$
					"cache-gridcoverage",  //$NON-NLS-1$
					"org.locationtech.udig.style.advanced.editorpages.CoverageColorMaskStyleEditorPage" //$NON-NLS-1$
			);
	
	/**
	 * Creates an editor manage for a give layer style
	 * @param selectedLayer
	 * @return
	 */
	public static EditorPageManager createEditorPageManager(ILayer selectedLayer){
		EditorPageManager mgr = EditorPageManager.loadManager(SmartPlugIn.getDefault(), selectedLayer);
		
		List<EditorNode> toRemove = new ArrayList<EditorNode>();
		for (EditorNode en : mgr.getRootSubNodes()){
			if (EXCLUDED_STYLES.contains(en.getId())){
				toRemove.add(en);
			}
		}
		
		for (EditorNode en: toRemove){
			mgr.remove(en);
		}
		return mgr;
	}
}
