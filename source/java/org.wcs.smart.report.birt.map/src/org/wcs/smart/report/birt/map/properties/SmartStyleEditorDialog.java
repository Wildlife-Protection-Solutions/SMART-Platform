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

import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.style.sld.IStyleEditorPageContainer;
import net.refractions.udig.style.sld.editor.EditorPageManager;
import net.refractions.udig.style.sld.editor.StyleEditorDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.geotools.util.NullProgressListener;
import org.opengis.util.ProgressListener;
import org.wcs.smart.report.birt.map.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Extension of the StyleEditorDialog customized for our purposes in the BIRT
 * report map style editor.  In this case we have removed the apply button,
 * and ensured the ok and cancel buttons set the return code of the window. 
 */
public class SmartStyleEditorDialog extends StyleEditorDialog implements IStyleEditorPageContainer {
    
	  public static final SmartStyleEditorDialog createSmartStyleDialog( Shell shell, final String pageId,Layer selectedLayer, EditorPageManager manager ) {
	        final SmartStyleEditorDialog dialog;

	        Shell parentShell = shell;
	        if (parentShell == null) {
	            // Determine a decent parent shell.
	            final IWorkbench workbench = PlatformUI.getWorkbench();
	            final IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
	            if (workbenchWindow != null) {
	                parentShell = workbenchWindow.getShell();
	            } else {
	                parentShell = null;
	            }
	        }

	        dialog = new SmartStyleEditorDialog(parentShell, manager);
	        dialog.setSelectedNode(pageId);
	        dialog.setSelectedLayer(selectedLayer);
	        dialog.create();
	        dialog.getShell().setText(Messages.SmartStyleEditorDialog_Title); 
	        dialog.filteredTree.getFilterCombo().setEnabled(true); // allow filtering

	        if (pageId != null) {
	            dialog.findNodeMatching(pageId);
	        }
	        return dialog;
	    }

	  
    

    /**
     * Creates a new dialog under the control of the given manager manager.
     * 
     * @param parentShell the parent shell
     * @param manager the preference manager
     */
    protected SmartStyleEditorDialog( Shell parentShell, EditorPageManager manager ) {
        super(parentShell, manager);
    }

    public ProgressListener getProgressListener() {
        ProgressListener cancelProgress = new NullProgressListener();
        return cancelProgress ;
    }
    
    @Override
    protected Control createButtonBar( Composite parent ) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        composite.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        // add import/export buttons
        addImportExportButtons(composite);

        // add apply/revert/close buttons
        addOkCancelRevertApplyButtons(parent, composite);

        return composite;
    }

    private void addOkCancelRevertApplyButtons( Composite parent, Composite composite ) {
        GridLayout layout;
        GridData data;
        Composite compRight = new Composite(composite, SWT.NONE);
        layout = new GridLayout(0, true); // columns are set at end because createButton sets them but we want 2x2
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        compRight.setLayout(layout);
        data = new GridData(SWT.END, SWT.CENTER, true, false);
        compRight.setLayoutData(data);
        compRight.setFont(parent.getFont());

        StyleEditorButtonListener listener = new StyleEditorButtonListener(this);
        
        Button revertButton = createButton(compRight, REVERT_ID, Messages.SmartStyleEditorDialog_RevertButton, false); 
        revertButton.setEnabled(false);
        revertButton.addListener(SWT.Selection, listener);
        
        Button applyButton = createButton(compRight, APPLY_ID, Messages.SmartStyleEditorDialog_ApplyButton, false); 
        applyButton.setEnabled(false);
        applyButton.addListener(SWT.Selection, new StyleEditorButtonListener(this));
        
        Button closeButton = createButton(compRight, CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false); 
        closeButton.setEnabled(true);
        closeButton.addListener(SWT.Selection, listener);
        
        Button okButton = createButton(compRight, OK_ID,
                IDialogConstants.OK_LABEL, false); 
        okButton.setEnabled(true);
        okButton.addListener(SWT.Selection, listener);
        
        layout.numColumns=2;
    }

    protected void setReturnCode(int code){
    	super.setReturnCode(code);
    }
    
    private void addImportExportButtons( Composite composite ) {
        GridLayout layout;
        Composite compLeft = new Composite(composite, SWT.NONE);
        layout = new GridLayout(0, true); // columns are incremented by createButton
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        compLeft.setLayout(layout);
        compLeft.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));

        Button importButton = createButton(compLeft, IMPORT_ID,
                DialogConstants.IMPORT_BUTTON_TEXT, false); 
        importButton.setEnabled(false);
        importButton.addListener(SWT.Selection, new StyleEditorButtonListener(this));
        Button exportButton = createButton(compLeft, EXPORT_ID,
        		DialogConstants.EXPORT_BUTTON_TEXT, false); 
        exportButton.setEnabled(false);
        exportButton.addListener(SWT.Selection, new StyleEditorButtonListener(this));
    }
    
    @Override
    public void setExitButtonState() {
    	getButton(APPLY_ID).setEnabled(true);
    }
    
    @Override
    public void updateButtons() {
        getButton(IMPORT_ID).setEnabled(true);
        getButton(EXPORT_ID).setEnabled(true);
        getButton(REVERT_ID).setEnabled(true);
        getButton(APPLY_ID).setEnabled(true);
        getButton(OK_ID).setEnabled(true);
        getButton(CANCEL_ID).setEnabled(true);
    }
}
