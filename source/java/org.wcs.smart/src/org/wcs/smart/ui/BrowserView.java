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

import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.E3Utils;

/**
 * Very basic web browser.
 * 
 * @author Emily
 *
 */
public class BrowserView {
	
	/**
	 * view id
	 */
	public static final String ID = "org.wcs.smart.view.browser"; //$NON-NLS-1$
	
	/**
	 * context variable to set to change the home button
	 */
	public static final String DEFAULT_URL = "org.wcs.smart.browser.home"; //$NON-NLS-1$
		
	@Inject private MPart part;
	
	private Browser browser ;
	private Text txt;
	private ToolItem btnBack , btnForward, btnSmartHome, btnRefresh;
	
	public BrowserView() {
	}

	@PreDestroy
	public void dispose(){
	}
	
	@PostConstruct
	public void createPartControl(final Composite parent) {
		part.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		
		Composite all = new Composite(parent, SWT.NONE);
		all.setLayout(new GridLayout());
		((GridLayout)all.getLayout()).marginHeight = 0;
		((GridLayout)all.getLayout()).marginWidth = 0;
		((GridLayout)all.getLayout()).horizontalSpacing = 0;
		((GridLayout)all.getLayout()).verticalSpacing = 0;
		all.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Composite buttons = new Composite(all, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		buttons.setLayout(new GridLayout(3, false));
		
		ToolBar toolBar2 = new ToolBar(buttons, SWT.FLAT);
		
		btnBack = new ToolItem(toolBar2, SWT.NONE);
		btnBack.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_BACKWARD));
		btnBack.setToolTipText(Messages.BrowserView_previoustooltip);
		btnBack.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browser.back();
			}	
		});
		btnForward = new ToolItem(toolBar2, SWT.NONE);
		btnForward.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_FORWARD));
		btnForward.setToolTipText(Messages.BrowserView_nexttooltip);
		btnForward.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browser.forward();
			}	
		});
		
		txt = new Text(buttons, SWT.BORDER);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txt.setEditable(false);
		
		ToolBar toolBar3 = new ToolBar(buttons, SWT.FLAT);
		
		btnRefresh = new ToolItem(toolBar3, SWT.NONE);
		btnRefresh.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_GO));
		btnRefresh.setToolTipText(Messages.BrowserView_refreshtooltip);
		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browser.refresh();
			}	
		});
		
		btnSmartHome = new ToolItem(toolBar3, SWT.PUSH);
		btnSmartHome.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_HOME));
		btnSmartHome.setToolTipText(Messages.BrowserView_hometooltip);
		btnSmartHome.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				goHome();
			}	
		});
		
		
		browser = new Browser(all, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txt.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.CR || e.character == SWT.KEYPAD_CR){
					browser.setUrl(txt.getText());
				}
			}
		});
		
		browser.addProgressListener(new ProgressListener() {
			@Override
			public void completed(ProgressEvent event) {
				btnRefresh.setEnabled(true);
			}
			
			@Override
			public void changed(ProgressEvent event) {
				if (event.total == 0) return;
				if (event.total == event.current) return;
				if (btnRefresh.isDisposed()) return;
				btnRefresh.setEnabled(false);
			}
		});
		
		browser.addTitleListener(new TitleListener() {
			@Override
			public void changed(TitleEvent event) {
				String title = event.title;
				if (title.length() > 30) title = title.substring(0, 30) + "..."; //$NON-NLS-1$
				part.setLabel(title);
				part.setTooltip(browser.getUrl());
			}
		});
		
		browser.addLocationListener(new LocationListener() {
			@Override
			public void changing(LocationEvent event) {
				changed(event);
			}
			
			@Override
			public void changed(LocationEvent event) {
				txt.setText(browser.getUrl());
				btnBack.setEnabled(browser.isBackEnabled());
				btnForward.setEnabled(browser.isForwardEnabled());
			}
		});
		
		browser.addOpenWindowListener(event -> {
			//when asked to open in a new window; open in a new part instead
			EPartService ePartService = part.getContext().get(EPartService.class);
			MPart newPart = ePartService.createPart(BrowserView.ID);
			this.part.getParent().getChildren().add(part);
			System.out.println(newPart.getElementId());
		    ePartService.showPart(newPart, PartState.ACTIVATE);	    
			BrowserView newView = (BrowserView) E3Utils.getSourceObject(newPart);
			newPart.getContext().set(DEFAULT_URL, part.getContext().get(DEFAULT_URL));
			event.browser = newView.browser;  			
		});
		
		btnBack.setEnabled(browser.isBackEnabled());
		btnForward.setEnabled(browser.isForwardEnabled());
	}

	public void goHome(){
		String home = (String) part.getContext().get(DEFAULT_URL);
		if (home != null){
			browser.setUrl(home);
		}else{
			browser.setUrl(""); //$NON-NLS-1$
		}
	}
	
	@Focus
	public void setFocus() {
		browser.setFocus();
	}

	
	public static class BrowserViewWrapper extends DIViewPart<BrowserView>{
		public BrowserViewWrapper(){
			super(BrowserView.class);
		}
	}
}
