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
package org.wcs.smart.report.internal.ui.viewer;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.report.IReportListener;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportEventManager.EventType;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.export.ParameterCollecter;
import org.wcs.smart.report.model.Report;


/**
 * View to display the results of a report
 * @author egouge
 * @since 1.0.0
 */
public class ReportView implements IReportListener{

	/**
	 * Report view id
	 */
	public static final String ID = "org.wcs.smart.birt.ReportView"; //$NON-NLS-1$
	
	private Browser browser;
	private Report report;
	private HashMap<String, Object> selectedParams;

	@Inject private MPart part;

	Job reportRunner = new Job(Messages.ReportView_PreviewReportJobName){
		
		protected IStatus run(IProgressMonitor monitor) {
			try{
				IReportEngine engine = ReportEngineManager.getBirtReportEngine();
				final IReportRunnable design = engine.openReportDesign(report.getFullReportFilename().getAbsolutePath());
				
				try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
					HTMLRenderOption options = new HTMLRenderOption();
					options = new HTMLRenderOption( );
					options.setOutputStream(bos);
					options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
					options.setOutputFormat(HTMLRenderOption.HTML);
					IRunAndRenderTask task = engine.createRunAndRenderTask(design);
					try{
						task.setRenderOption(options);
						task.setParameterValues(selectedParams);
						task.run();
					}finally{
						task.close();
					}
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					if (browser.isDisposed()) return Status.CANCEL_STATUS;
					
					browser.getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							try {
								browser.setText(bos.toString("UTF8")); //$NON-NLS-1$
							} catch (UnsupportedEncodingException e) {
								throw new IllegalStateException(Messages.ReportView_UTF8NotSupported);
							}
						}});
				}
		} catch (Exception e) {
			ReportPlugIn.displayLog(MessageFormat.format(Messages.ReportView_RunReportError1, new Object[]{report.getName()}) + e.getLocalizedMessage(), e);
		}			
		return Status.OK_STATUS;
	}};
	
	@PreDestroy
	public void dispose(){
		ReportEventManager.getInstance().removeReportListener(this);
	}
	

	@PostConstruct
	public void createPartControl(Composite parent) {
		ReportEventManager.getInstance().addReportListener(this);
		
		parent.setLayout(new GridLayout());
	
		browser = new Browser( parent, SWT.NONE );
		
		GridData gd = new GridData( GridData.FILL_BOTH );
		gd.horizontalSpan = 2;
		browser.setLayoutData( gd );
		
		browser.addOpenWindowListener( new OpenWindowListener( ) {
			public void open( final WindowEvent event )
			{
				final Shell shell = new Shell( SWT.SHELL_TRIM | Window.getDefaultOrientation( ) );
				shell.setLayout( new FillLayout( ) );
				Browser browser = new Browser( shell, SWT.NONE );
				event.browser = browser;
				shell.open( );
			}
		});
	}

	@Focus
	public void setFocus() {
		browser.setFocus();
	}
	
	
	
	/**
	 * 
	 * @return the report associated with this view
	 */
	public Report getReport(){
		return this.report;
	}

	/**
	 * Re-runs the report
	 */
	public void refreshReport(boolean refreshParameters){
		try{
			previewReport(refreshParameters);
		}catch (Exception ex){
			ReportPlugIn.log(
					MessageFormat.format(Messages.ReportView_RunReportError1, new Object[]{report.getName()}) + ex.getLocalizedMessage(), ex);
		}
	}
		
	/**
	 * Sets the report to display in the review and runs the report; without 
	 * changing the report parameters.
	 * 
	 * @param report
	 */
	public void setReport(Report report){
		this.report = report;
		updateName();
		refreshReport(true);
	}
	
	/**
	 * Sets the report to display in the review and runs the report.
	 * @param report
	 */
	public void setReport(Report report, HashMap<String, Object> reportParamseters){
		this.report = report;
		updateName();
		this.selectedParams = reportParamseters;
		refreshReport(false);
	}
	
	/**
	 * Updates the report view tab name
	 * 
	 */
	private void updateName() {
		part.setLabel(report.getName());
	}
	
	private void previewReport(boolean refreshParameters) throws Exception {
		if (report == null){
			ReportPlugIn.displayLog(Messages.ReportView_ReportNotSelected, null);
			return; 
		}
		browser.setText(Messages.ReportView_Progress_RunningReport);
		if (refreshParameters){
			try{
				selectedParams  = getParameters();
				if (selectedParams == null){
					browser.setText(Messages.ReportView_CancelledReportMsg);
					return;
				}
			}catch (Exception ex){
				String error = Messages.ReportView_Error_GatheringParams;
				browser.setText(error+ ex.getLocalizedMessage());
				ReportPlugIn.log(error, ex);
				return;
			}
		}
		reportRunner.schedule();
		
	}
	
	private HashMap<String, Object> getParameters() throws Exception{
		ParameterCollecter paramCollector = new ParameterCollecter();	
		HashMap<String, Object> selectedParams = paramCollector.getParameters(new Report[]{report});
		return selectedParams;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.report.IReportListener#reportEvent(java.lang.Object, org.wcs.smart.report.ReportEventManager.EventType)
	 */
	@Override
	public void reportEvent(Object o, EventType eventType) {
		if (eventType == EventType.REPORT_DELETED){
			if (this.report.equals(o)){
				browser.getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						part.getContext().get(EPartService.class).hidePart(ReportView.this.part, true);
					}
					
				});
				
			}
		}
	}

	public static class ReportViewWrapper extends DIViewPart<ReportView>{
		public ReportViewWrapper(){
			super(ReportView.class);
		}
	}
}

