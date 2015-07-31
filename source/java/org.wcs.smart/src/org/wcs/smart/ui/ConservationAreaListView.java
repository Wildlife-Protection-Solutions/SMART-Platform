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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.SmartPlugIn;
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
public class ConservationAreaListView {

	public static final String ID = "org.wcs.smart.query.conservationAreaList"; //$NON-NLS-1$
	
	private FormToolkit toolkit = null;
	private Label lblDefault = null;
	private Font boldFont = null;
	
	private Composite caComp = null;
	private Composite main = null;
	private ScrolledComposite scroll = null;
	
	@Inject private EPartService partService;
	@Inject private IEventBroker eventManager;
	
	public ConservationAreaListView() {
	}

	@PreDestroy
	public void dispose(){
		toolkit.dispose();
	}
	
	@PostConstruct
	public void createPartControl(final Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		
		
		scroll = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		toolkit.adapt(scroll);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		
		main = toolkit.createComposite(scroll,SWT.BORDER);
		scroll.setContent(main);
		
		main.setLayout(new GridLayout(1, false));
		
		Hyperlink link = toolkit.createHyperlink(main, Messages.ConservationAreaListView_ModifyLink, SWT.NONE);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				
				SelectCaDialog dialog = new SelectCaDialog(parent.getShell());
				if (dialog.open() == SelectCaDialog.OK){
					
					//close all active editors
					if (!partService.saveAll(true)){
						return;
					}
					for (MPart p : partService.getParts()){
						if (p.getObject() instanceof Persist){
							//we want to remove this when we hide it
							p.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);
							partService.hidePart(p);
						}
					}
					
					ConservationAreaConfiguration newConfig = dialog.getNewConfiguration();
					SmartDB.setConservationAreaConfiguration(newConfig.getEmployees().iterator().next(), 
							SmartDB.getCurrentConservationArea(), newConfig);
					eventManager.send(SmartDB.CCAA_CONFIGURATION_MODIFIED, newConfig);
					
					
					//the data model has changed
					DataModelManager.INSTANCE.fireChangeListeners();
					initCas();
				}
				
			}
		});
		
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
		caComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		initCas();
		
		Label ll = toolkit.createLabel(main, Messages.ConservationAreaListView_CaInfo, SWT.WRAP);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)ll.getLayoutData()).widthHint = 100;
		
		scroll.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scroll.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = scroll.getClientArea();
				scroll.setMinSize(main.computeSize(r.width-10, SWT.DEFAULT));
			}
		});
	}

	@Focus
	public void setFocus() {
		lblDefault.setFocus();
	}

	private void initCas(){
		for (Control kid : caComp.getChildren()){
			kid.dispose();
		}
		
		if (SmartDB.getConservationAreaConfiguration() != null){
			
			for(ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
				Composite c = toolkit.createComposite(caComp);
				c.setLayout(new GridLayout(2, false));
				((GridLayout)c.getLayout()).marginHeight = 0;
				((GridLayout)c.getLayout()).marginWidth = 0;
				((GridLayout)c.getLayout()).horizontalSpacing = 0;
				((GridLayout)c.getLayout()).verticalSpacing = 0;
				
				c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)c.getLayoutData()).horizontalIndent = 0;
				
				Label l = toolkit.createLabel(c,""); //$NON-NLS-1$
				l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BULLET_BLACK));
				l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
				
				l = toolkit.createLabel(c, ca.getNameLabel(), SWT.WRAP);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).widthHint = 90;
				
			}
		}
		caComp.layout();
		main.layout();
		scroll.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scroll.getParent().layout(true, true);

	}
	
	public static class ConservationAreaListViewWrapper extends DIViewPart<ConservationAreaListView>{
		public ConservationAreaListViewWrapper(){
			super(ConservationAreaListView.class);
		}
	}
}
