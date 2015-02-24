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
package org.wcs.smart.report.birt.map.properties;

import java.lang.reflect.Field;
import java.util.Iterator;

import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.StyleEntry;
import org.locationtech.udig.style.advanced.editorpages.SimpleLineEditorPage;
import org.locationtech.udig.style.advanced.editorpages.SimplePointEditorPage;
import org.locationtech.udig.style.advanced.editorpages.SimplePolygonEditorPage;
import org.locationtech.udig.style.sld.SLD;
import org.locationtech.udig.style.sld.editor.EditorPageManager;
import org.opengis.coverage.grid.GridCoverage;
import org.wcs.smart.util.MapStyleUtil;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.geotools.coverage.grid.GridCoverage2D;

/**
 * 
 * Opens the style editor page
 * @author Emily
 *
 */
public class SmartOpenStyleEditorAction {

    public static final String ATT_ID = "id"; //$NON-NLS-1$
    public static final String ATT_CLASS = "class"; //$NON-NLS-1$
    public static final String ATT_LABEL = "label"; //$NON-NLS-1$
    public static final String ATT_REQUIRES = "requires"; //$NON-NLS-1$
    public static final String STYLE_ID = "org.locationtech.udig.style.sld"; //$NON-NLS-1$

    private Layer selectedLayer;
    private StyleBlackboard updatedBlackboard = null;

    /**
     * Create a new <code>OpenPreferenceAction</code>
     * This default constructor allows the the action to be called from the welcome page.
     */
    public SmartOpenStyleEditorAction(Layer layer) {
    	selectedLayer = layer;
    }


    public void run( ) {
        Shell shell = Display.getDefault().getActiveShell();
        
        String pageId = MapStyleUtil.findInitialStylePageId(selectedLayer);
        final EditorPageManager manager = MapStyleUtil.createEditorPageManager(selectedLayer);

        ReportSmartStyleEditorDialog dialog = ReportSmartStyleEditorDialog.createSmartStyleDialog(shell, pageId, selectedLayer, manager);

        if (dialog.open() == Window.OK){
        	updatedBlackboard = ProjectFactory.eINSTANCE.createStyleBlackboard();
        	updatedBlackboard.clear();
        	//add styles
            for( Iterator< ? > itr = dialog.getSelectedLayer().getStyleBlackboard().getContent().iterator(); itr.hasNext(); ) {
                StyleEntry entry = (StyleEntry) itr.next();
                if (entry.getStyle() != null) {
                	//this is key; we do this to ensure the style is correctly applied
                    updatedBlackboard.put(entry.getID(), entry.getStyle());
                }
            }
        }
    }
    
    /**
     * @return true if user pressed ok; otherwise false
     */
    public StyleBlackboard getBlackboard(){
    	return updatedBlackboard;
    }

}
