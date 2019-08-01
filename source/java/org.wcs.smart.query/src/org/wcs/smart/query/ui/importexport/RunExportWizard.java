/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.query.ui.importexport;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.importexport.ICsvQueryExporter;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;

import com.ibm.icu.text.MessageFormat;

/**
 * Wizard for running and exporting queries.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class RunExportWizard extends Wizard {

	private List<Query> queries;
	
	private RunExportDatePage datepage;
	private RunExportFormatPage formatpage;
	private RunExportOptionsPage oppage;
	
	
	public RunExportWizard(List<Query> queries) {
		super();
		this.queries = queries;
		
		setWindowTitle(Messages.RunExportWizard_WizardTitle);
	}
	
	
	@Override
	public boolean performFinish() {
		HashMap<Query, IQueryExporter> out = new HashMap<>();
		for (Entry<Query, ComboViewer> item : formatpage.getFormats().entrySet() ) {
			out.put(item.getKey(), (IQueryExporter) item.getValue().getStructuredSelection().getFirstElement());
		}
		DateFilter df = datepage.getDateFilters();
		
		HashMap<String, Object> exportOps = new HashMap<>();
		
		if (needsProjection()) {
			exportOps.put(IQueryExporter.PROJECTION_PARAM_KEY, oppage.getProjection());
		}
		if (needsDelimitier()) {
			exportOps.put(ICsvQueryExporter.DELIMITER_KEY, oppage.getDelimiter());
		}
		
		String t = oppage.getOutputDirectory();
		if (t.strip().isBlank()) {
			MessageDialog.openError(getShell(), Messages.RunExportWizard_ErrorTitle, Messages.RunExportWizard_InvalidDir);
			return false;
		}
		Path outdir = Paths.get(t);
		if (!Files.exists(outdir)) {
			try {
				Files.createDirectories(outdir);
			} catch (IOException ex) {
				QueryPlugIn.displayLog(MessageFormat.format(Messages.RunExportWizard_CouldNotCreateDir,outdir.toString(), ex.getMessage()), ex);
				return false;
			}
		}
		if (!Files.isDirectory(outdir)) {
			MessageDialog.openError(getShell(), Messages.RunExportWizard_ErrorTitle, Messages.RunExportWizard_InvalidDir);
			return false;
		}
		
		final Shell current = getShell();
		
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					SubMonitor mm = SubMonitor.convert(monitor);
					
					mm.beginTask(Messages.RunExportWizard_TaskName, out.size()*2);
					List<String> errors = new ArrayList<>();
					int done = 0;
					boolean cancelled = false;
					for (Entry<Query, IQueryExporter> item : out.entrySet() ) {
						mm.subTask(item.getKey().getName());
						
						item.getKey().setDateFilter(df);
						try(Session session = HibernateManager.openSession()){
							IQueryResult result = QueryExecutor.INSTANCE.executeQuery(item.getKey(), session, mm.split(1));
							item.getKey().setCachedResults(result);
						}catch (OperationCanceledException ex) {
							cancelled = true;
							break;
						}catch (Exception ex) {
							QueryPlugIn.log(ex.getMessage(),ex);
							errors.add(MessageFormat.format(Messages.RunExportWizard_RunError, item.getKey().getName(), ex.getMessage()));
							continue;
						}
						
						
						String fname = URLUtils.cleanFilename(item.getKey().getName() + "_" + item.getKey().getId()) + "." + item.getValue().getDefaultExtension(); //$NON-NLS-1$ //$NON-NLS-2$
						try {
							item.getValue().export(item.getKey(), item.getKey().getCachedResults(), outdir.resolve(fname).toFile(), exportOps, mm.split(1));
						}catch (OperationCanceledException ex) {
							cancelled = true;
							break;
						} catch (Exception ex) {
							QueryPlugIn.log(ex.getMessage(),ex);
							errors.add(MessageFormat.format(Messages.RunExportWizard_ExportError, item.getKey().getName(), ex.getMessage()));
							continue;
						}
						done++;
					}
					
					int fdone = done;
					boolean fc = cancelled;
					Display.getDefault().syncExec(()->{
						String message = MessageFormat.format(Messages.RunExportWizard_DoneMsg, fdone, out.size(), outdir.toString());
						if (fc) message = Messages.RunExportWizard_CancalledMsg + message;
						if (errors.isEmpty()) {
							MessageDialog.openInformation(current, Messages.RunExportWizard_DoneTitle, message);
							
						}else {
							WarningDialog wd = new WarningDialog(current, Messages.RunExportWizard_DoneTitle, message, errors);
							wd.open();
						}
					});
					
				}
			});
		}catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.RunExportWizard_DoneErrorMsg + ex.getMessage(), ex);
			return false;
		}
		
		return true;
	}
	
	
    @Override
	public void addPages() {
    	setNeedsProgressMonitor(true);
    	
    	formatpage = new RunExportFormatPage();
    	datepage = new RunExportDatePage();
    	oppage = new RunExportOptionsPage();
    	
    	addPage(formatpage);
    	addPage(datepage);
    	addPage(oppage);
    }

	public List<Query> getQueries(){
		return this.queries;
	}

	public boolean needsProjection() {
		for (ComboViewer cmb : formatpage.getFormats().values()) {
			if (((IQueryExporter)cmb.getStructuredSelection().getFirstElement()).supportsProjection()) return true;
		}
		return false;
	}
	
	public boolean needsDelimitier() {
		for (ComboViewer cmb : formatpage.getFormats().values()) {
			if (((IQueryExporter)cmb.getStructuredSelection().getFirstElement()) instanceof ICsvQueryExporter) return true;
		}
		return false;
	}
	
	public List<IDateFieldFilter> getDateFilters() {
		return formatpage.getDateFilters();
	}
}
