/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.record;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.ObservationDialog;
import org.wcs.smart.i2.ui.ObservationTreeViewer;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Observations table list for record editor summary page
 * @author Emily
 *
 */
public class RecordLocationSummaryComposite extends Composite{

	private RecordEditor editor;
	private ObservationTreeViewer observationViewer;
	
	public RecordLocationSummaryComposite(Composite parent, FormToolkit toolkit, RecordEditor editor){
		super(parent, SWT.NONE);
		this.editor = editor;
		setLayout(new GridLayout());
		
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		observationViewer = new ObservationTreeViewer(this, SWT.FULL_SELECTION);
		observationViewer.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		observationViewer.getViewer().getTree().setHeaderVisible(false);
		
		Menu mnu = new Menu(observationViewer.getViewer().getControl());
		
		MenuItem showMap = new MenuItem(mnu, SWT.PUSH);
		showMap.setText(Messages.RecordLocationSummaryComposite_ShowOnMap);
		showMap.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON));
		showMap.addListener(SWT.Selection, e->{
			Object element = observationViewer.getViewer().getStructuredSelection().getFirstElement();
			IntelLocation loc = null;
			if (element instanceof IntelObservation) {
				loc = ((IntelObservation)element).getLocation();
			}else if (element instanceof IntelObservationAttribute) {
				loc = ((IntelObservationAttribute)element).getObservation().getLocation();
			}
			if (loc != null) {
				editor.getMapPage().selectLocation(loc);
				editor.setActiveEditor(editor.getMapPage());
			}
		});
		
		if (IntelSecurityManager.INSTANCE.canEditRecord( editor.getInputInternal().getRecordProfileUuid() )){
			MenuItem editItem = new MenuItem(mnu, SWT.PUSH);
			editItem.setText(DialogConstants.EDIT_BUTTON_TEXT);
			editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			editItem.addListener(SWT.Selection, e->{
				Object element = observationViewer.getViewer().getStructuredSelection().getFirstElement();
				IntelObservation loc = null;
				if (element instanceof IntelObservation) {
					loc = ((IntelObservation)element);
				}else if (element instanceof IntelObservationAttribute) {
					loc = ((IntelObservationAttribute)element).getObservation();
				}
				if (loc != null) {
					ObservationDialog dialog = new ObservationDialog(getShell(), loc);
					if (dialog.open() == Window.OK){
						editor.setDirty(true);
						editor.getMapPage().refreshLocationTable();
						init();
					}
				}
			});
			editItem.setEnabled(false);
			
			mnu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
					editItem.setEnabled( editor.getEditMode() ); 
				}
				
				@Override
				public void menuHidden(MenuEvent e) {}
			});
		}
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem miExpandAll = new MenuItem(mnu, SWT.PUSH);
		miExpandAll.setText(Messages.RecordLocationSummaryComposite_ExpandAll);
		miExpandAll.addListener(SWT.Selection, e->observationViewer.getViewer().expandAll());
		
		MenuItem miCollapseAll = new MenuItem(mnu, SWT.PUSH);
		miCollapseAll.setText(Messages.RecordLocationSummaryComposite_CollapseAll);
		miCollapseAll.addListener(SWT.Selection, e->observationViewer.getViewer().collapseAll());
		
		observationViewer.getViewer().getControl().setMenu(mnu);
		
		
	}
	
	public void init(){
		Object[] expanded = observationViewer.getViewer().getExpandedElements();
		
		observationViewer.setInput(editor.getRecord());
		observationViewer.getViewer().expandAll();
		for (int i = 0, n = observationViewer.getViewer().getTree().getColumnCount(); i < n; i++)
            observationViewer.getViewer().getTree().getColumn(i).pack();
		
		if (expanded == null || expanded.length == 0) {
			observationViewer.getViewer().expandAll();
		}else {
			observationViewer.getViewer().setExpandedElements(expanded);
		}
	}
	
	public void refresh(){
	}

}