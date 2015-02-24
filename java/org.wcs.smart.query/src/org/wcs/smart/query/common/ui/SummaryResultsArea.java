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
package org.wcs.smart.query.common.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.ProgressAreaComposite;

/**
 * Area for displaying summary results.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SummaryResultsArea extends Composite{

	private Composite stackComposite;
	private ProgressAreaComposite progressComp ;
	private Composite runQueryComp;
	private SummaryEditor editor;
	private Composite tableComp ;
	private SummaryResultTable resultsTable;
	
	private Hyperlink runQueryLink;
	private FormToolkit toolkit;
	
	
	
	public SummaryResultsArea(Composite parentArea, SummaryEditor parentEditor){
		super(parentArea, SWT.NONE);
		this.editor = parentEditor;
		
		toolkit = new FormToolkit(parentArea.getDisplay());
		super.addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});
		toolkit.adapt(this);
		
		createTable(toolkit);
	}
	
	public void createTable(FormToolkit toolkit){
		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = gl.marginWidth = gl.horizontalSpacing = gl.verticalSpacing = 0;
		this.setLayout(gl);
		// --- Stack Panel ---
		// Here we either show the table or the progress dialog
		stackComposite = toolkit.createComposite(this);
		stackComposite.setLayout(new StackLayout());
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		runQueryComp = createRunQueryComp(stackComposite, toolkit);
		tableComp = createTableResultsComposite(stackComposite, toolkit);
		progressComp = new ProgressAreaComposite(stackComposite);
		progressComp.adapt(toolkit);
		
		((StackLayout) stackComposite.getLayout()).topControl = runQueryComp;
	}
	
	private Composite createTableResultsComposite(Composite parent, FormToolkit toolkit){
		Composite tableArea = toolkit.createComposite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = gl.marginHeight = gl.verticalSpacing = gl.horizontalSpacing = 0;
		tableArea.setLayout(gl);
		tableArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		return tableArea;
	}
	
	/**
	 * Creates the initial composite that prompts users
	 * to run query.
	 * 
	 * @param parent
	 * @return
	 */
	private Composite createRunQueryComp(Composite parent, FormToolkit toolkit){
		Composite main = toolkit.createComposite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		runQueryLink = toolkit.createHyperlink(main, Messages.SummaryResultsArea_RunSummaryLink, SWT.NONE);
		runQueryLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				editor.refreshQuery();
			}
		});
		return main;
	}
	
	
	public void showProgressArea(){
		((StackLayout)stackComposite.getLayout()).topControl = progressComp;
		stackComposite.layout();
		
	}
	
	/**
	 * if results are null we show cancelled message
	 * @param results
	 */
	public void updateAndShowTable(final SummaryQueryResult results){
		tableComp.getDisplay().asyncExec(new Runnable(){
			@Override
			public void run() {
				if (results == null){
					showProgressArea();
					progressComp.showCancelled();
				}else{
					((StackLayout)stackComposite.getLayout()).topControl = tableComp;
					stackComposite.layout();
				
					if (resultsTable != null){
						resultsTable.dispose();
						resultsTable = null;
					}
					resultsTable = new SummaryResultTable(tableComp, results, toolkit);
					resultsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					tableComp.layout();
				}
			}
		});
	}
	
	public IProgressMonitor createProgressMonitor(){
		return progressComp.createProgressMonitor();
	}
	
	
}
