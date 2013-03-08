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
package org.wcs.smart.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.ViewPart;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.hibernate.ConservationAreaConfiguration;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
/**
 * View that displays a list of current active conservation 
 * areas.
 * <p>To be used with cross-ca analysis</p>
 * 
 * @author Emily
 *
 */
public class ConservationAreaListView extends ViewPart {

	public static final String ID = "org.wcs.smart.query.conservationAreaList"; //$NON-NLS-1$
	
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Label lblDefault = null;
	private Font boldFont = null;
	
	private Composite caComp = null;
	private Composite parent = null;
	
	public ConservationAreaListView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;
		Composite main = toolkit.createComposite(parent);
		
		main.setLayout(new GridLayout(1, false));
		lblDefault = toolkit.createLabel(main, Messages.ConservationAreaListView_CaLabel, SWT.NONE);
		
		FontData fd = lblDefault.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(Display.getCurrent(), fd);
		
		lblDefault.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblDefault.getLayoutData()).widthHint = 100;
		lblDefault.setFont(boldFont);
		lblDefault.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (boldFont != null){
					boldFont.dispose();
					boldFont = null;
				}
			}
		});
		caComp = toolkit.createComposite(main, SWT.NONE);
		caComp.setLayout(new GridLayout());
		
		initCas();
		
		Hyperlink link = toolkit.createHyperlink(main, Messages.ConservationAreaListView_ModifyLink, SWT.NONE);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				
				SelectCaDialog dialog = new SelectCaDialog(getSite().getShell());
				if (dialog.open() == SelectCaDialog.OK){
					
					//close all active editors 
					if (!PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(true)){
						return;
					}
					
					ConservationAreaConfiguration newConfig = dialog.getNewConfiguration();

					SmartDB.setConservationAreaConfiguration(newConfig);
					//the data model has changed
					DataModelManager.getInstance().fireChangeListeners();
					initCas();
				}
				
			}
		});
		
		Label ll = toolkit.createLabel(main, Messages.ConservationAreaListView_CaInfo, SWT.WRAP);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)ll.getLayoutData()).widthHint = 100;
	}

	@Override
	public void setFocus() {
		lblDefault.setFocus();
	}

	private void initCas(){
		for (Control kid : caComp.getChildren()){
			kid.dispose();
		}
		
		if (SmartDB.getConservationAreaConfiguration() != null){
			
			for(ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
				Label l = toolkit.createLabel(caComp, ca.getNameLabel());
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).horizontalIndent = 10;
			}
		}
		caComp.layout();
		
		parent.layout(true, true);
	}
}
