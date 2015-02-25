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

import java.util.Iterator;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.StyleEntry;
import org.locationtech.udig.style.sld.editor.EditorPageManager;
import org.wcs.smart.udig.style.StyleManager;

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
        
        String pageId = StyleManager.INSTANCE.findInitialStylePageId(selectedLayer);
        final EditorPageManager manager = StyleManager.INSTANCE.createEditorPageManager(selectedLayer);

        ReportSmartStyleEditorDialog dialog = ReportSmartStyleEditorDialog.createSmartStyleDialog(shell, pageId, selectedLayer, manager);

        if (dialog.open() == Window.OK){
        	updatedBlackboard = ProjectFactory.eINSTANCE.createStyleBlackboard();
        	updatedBlackboard.clear();      	
        	updatedBlackboard.addAll(dialog.getSelectedLayer().getStyleBlackboard());
        }
    }
    
    /**
     * @return true if user pressed ok; otherwise false
     */
    public StyleBlackboard getBlackboard(){
    	return updatedBlackboard;
    }

}
