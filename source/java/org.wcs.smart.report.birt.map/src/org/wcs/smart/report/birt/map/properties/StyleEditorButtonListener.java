
/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
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

import java.io.File;
import java.text.MessageFormat;

import org.locationtech.udig.project.internal.SetDefaultStyleProcessor;
import org.locationtech.udig.style.internal.StyleLayer;
import org.locationtech.udig.style.sld.editor.ExportSLD;
import org.locationtech.udig.style.sld.editor.ImportSLD;
import org.locationtech.udig.style.sld.editor.StyleEditorDialog;
import org.locationtech.udig.ui.graphics.SLDs;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.wcs.smart.report.birt.map.internal.Messages;

/**
 * Copied from uDig; customized for our SMARTStyleEditorDialog
 */
class StyleEditorButtonListener implements Listener {

    /** StyleEditorButtonListener styleEditorDialog field */
    private final ReportSmartStyleEditorDialog styleEditorDialog;

    /**
     * @param styleEditorDialog
     */
    StyleEditorButtonListener( ReportSmartStyleEditorDialog styleEditorDialog ) {
        this.styleEditorDialog = styleEditorDialog;
    }

    /**
     * Will dispatch the even to the correct method (doApply, doRevert, etc...).
     */
    public void handleEvent( Event event ) {
        
        int buttonId = (Integer) event.widget.getData();
        
        switch( buttonId ) {
        case StyleEditorDialog.IMPORT_ID:
            doImport();
            break;
        case StyleEditorDialog.EXPORT_ID:
            doExport();
            break;
        case StyleEditorDialog.DEFAULTS_ID:
            doDefaults();
            break;
        case StyleEditorDialog.APPLY_ID:
            doApply();
            break;
        case StyleEditorDialog.REVERT_ID:
            doRevert();
            break;
        case StyleEditorDialog.OK_ID:
            if( doApply() ){
            	this.styleEditorDialog.setReturnCode(Window.OK);
                this.styleEditorDialog.close();
            }
            break;
        case StyleEditorDialog.CANCEL_ID:
        	this.styleEditorDialog.setReturnCode(Window.CANCEL);
            this.styleEditorDialog.close();
            break;

        default:
            break;
        }
        
    }

    private boolean doApply() {
        if( this.styleEditorDialog.getCurrentPage() == null){
            return false;
        }
        if (this.styleEditorDialog.getCurrentPage().performApply()) {
            this.styleEditorDialog.setExitButtonState();
            this.styleEditorDialog.getSelectedLayer().apply();
            return true;
        }
        return false;
    }
    private void doDefaults() {
            StyleLayer layer = styleEditorDialog.getSelectedLayer();
            layer.getStyleBlackboard().clear();
            SetDefaultStyleProcessor p = new SetDefaultStyleProcessor(layer.getGeoResource(), layer);
            p.run();
            
            this.styleEditorDialog.getSelectedLayer().apply();
            this.styleEditorDialog.getSelectedLayer().getMap().getRenderManager().refresh(this.styleEditorDialog.getSelectedLayer(), null);
            this.styleEditorDialog.setExitButtonState();
            this.styleEditorDialog.getCurrentPage().refresh();
    }
    
    private void doRevert() {
        //store the old sld
        //StyledLayerDescriptor oldSLD = this.styleEditorDialog.getSLD();
        
        //return to the blackboard state before we loaded the dialog
        this.styleEditorDialog.getSelectedLayer().revertAll();
        this.styleEditorDialog.getSelectedLayer().apply();
        if (this.styleEditorDialog.getSelectedLayer().getMap().getRenderManager() != null){
        	this.styleEditorDialog.getSelectedLayer().getMap().getRenderManager().refresh(this.styleEditorDialog.getSelectedLayer(), null);
        }
        
        //move listeners to new sld
        //StyledLayerDescriptor newSLD = this.styleEditorDialog.getSLD();
        this.styleEditorDialog.setExitButtonState();
        
        // TODO: update button states, page updates
        this.styleEditorDialog.getCurrentPage().refresh();
    }
    
    private void doImport() {
        ImportSLD importe = new ImportSLD();
        StyledLayerDescriptor sld = null;
        File file = importe.promptFile(Display.getDefault(), sld);
        if (file != null) {
            try {
                sld = (StyledLayerDescriptor) importe.importFrom(file, null);
            } catch (Exception e1) {
                MessageBox mb = new MessageBox(this.styleEditorDialog.getShell(), SWT.ICON_ERROR | SWT.OK);
                mb.setMessage(MessageFormat.format(Messages.StyleEditorButtonListener_UnableToImportSld, e1.getLocalizedMessage())); 
                mb.open();
                throw (RuntimeException) new RuntimeException().initCause(e1);
            }
        }
        if (sld != null) {
            Style newStyle = SLDs.getDefaultStyle(sld);
            // TODO: assert there is only 1 style
            this.styleEditorDialog.setStyle(newStyle);
            //refresh the page (there's a new SLD in town)
            this.styleEditorDialog.getCurrentPage().refresh();
        }
    }
    
    private void doExport() {
        StyledLayerDescriptor sld = this.styleEditorDialog.getSLD();
        ExportSLD export = new ExportSLD();
        File file = export.promptFile(Display.getDefault(), sld);
        if (file != null) {
            try {
                export.exportTo(sld, file, null);
            } catch (Exception e1) {
                // TODO Handle Exception
                throw (RuntimeException) new RuntimeException().initCause(e1);
            }
        }
    }
    
}