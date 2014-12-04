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

import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.StyleBlackboard;
import net.refractions.udig.project.internal.StyleEntry;
import net.refractions.udig.style.sld.SLD;
import net.refractions.udig.style.sld.editor.EditorPageManager;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

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
    public static final String STYLE_ID = "net.refractions.udig.style.sld"; //$NON-NLS-1$

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
        // the page to select by default
        String pageId = "simple"; //$NON-NLS-1$
        // the filter to apply, if defined
        // String[] displayedIds = null;

        try {
            if (SLD.POINT.supports(selectedLayer)) {
                Class< ? > pointClass = Class.forName("eu.udig.style.advanced.editorpages.SimplePointEditorPage"); //$NON-NLS-1$
                Field idField = pointClass.getField("ID"); //$NON-NLS-1$
                Object value = idField.get(null);
                pageId = value.toString();
            } else if (SLD.LINE.supports(selectedLayer)) {
                Class< ? > pointClass = Class.forName("eu.udig.style.advanced.editorpages.SimpleLineEditorPage"); //$NON-NLS-1$
                Field idField = pointClass.getField("ID"); //$NON-NLS-1$
                Object value = idField.get(null);
                pageId = value.toString();
            } else if (SLD.POLYGON.supports(selectedLayer)) {
                Class< ? > pointClass = Class.forName("eu.udig.style.advanced.editorpages.SimplePolygonEditorPage"); //$NON-NLS-1$
                Field idField = pointClass.getField("ID"); //$NON-NLS-1$
                Object value = idField.get(null);
                pageId = value.toString();
            } else if (selectedLayer.getGeoResource().getInfo(new NullProgressMonitor()).getDescription()
                    .equals("grassbinaryraster")) { //$NON-NLS-1$
                Class< ? > pointClass = Class.forName("eu.udig.style.jgrass.colors.JGrassRasterStyleEditorPage"); //$NON-NLS-1$
                Field idField = pointClass.getField("ID"); //$NON-NLS-1$
                Object value = idField.get(null);
                pageId = value.toString();
            }
        } catch (Exception e) {
            // fallback on simple
            pageId = "simple"; //$NON-NLS-1$
        }
        
        final EditorPageManager manager = EditorPageManager.loadManager(null, selectedLayer);

        SmartStyleEditorDialog dialog = SmartStyleEditorDialog.createSmartStyleDialog(shell, pageId, selectedLayer, manager);

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
