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
package org.wcs.smart.i2.ui.dialogs.query;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.export.IQueryExporter;

/**
 * Export query wizard.
 * 
 * @author Emily
 *
 */
public class ExportQueryWizard extends Wizard implements IPageChangingListener{
	
	public static final String LAST_DIR_KEY = "org.wcs.smart.i2.query.export.directory"; //$NON-NLS-1$

	private IntelRecordObservationQuery query;
	private IPagedQueryResultSet queryResults;
	
	private QueryFormatPage page1;
	private QueryFormatOptionPage page2;
	
	private List<Projection> supportedProjections = null;
	private Projection defaultProjection = null;
	
	@SuppressWarnings("unchecked")
	public ExportQueryWizard(IntelRecordObservationQuery query, IPagedQueryResultSet results) {
		this.query = query;
		this.queryResults = results;
		
		setWindowTitle("Export Query");
		setDialogSettings(Intelligence2PlugIn.getDefault().getDialogSettings());
		Session s = HibernateManager.openSession();
		try{
			supportedProjections = s.createCriteria(Projection.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
			defaultProjection = HibernateManager.getCurrentViewProjection(s);
		}finally{
			s.close();
		}
		super.setNeedsProgressMonitor(true);
	}


    /*
     * (non-Javadoc) Method declared on IWizard.
     */
    public boolean canFinish() {
    	if (getContainer().getCurrentPage() == page2 && page2.isPageComplete()){
    		return true;
    	}
    	return false;
    }
    
    public List<Projection> getSupportedProjections(){
    	return this.supportedProjections;
    }
    
    public Projection getDefaultProjection(){
    	return this.defaultProjection;
    }
    
	/**
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);

		page1 = new QueryFormatPage();
		super.addPage(page1);
		
		page2 = new QueryFormatOptionPage();
		super.addPage(page2);

	}
	/**
	 * @return the query exporter for the format selected
	 * on the first query page.
	 */
	public IQueryExporter getQueryExporter(){
		return page1.getQueryExporter();
	}
	
	/**
	 * The query to export or null if no specific query is active
	 * @return
	 */
	public IntelRecordObservationQuery getQuery(){
		return this.query;
	}
	
	/**
	 * Runs the export process
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		page1.performFinish();
		
		final boolean[] runOk = {false};
		try {
			final IQueryExporter exporter = getQueryExporter();
			if (exporter == null){
				throw new Exception("No exporter selected");
			}
			final HashMap<IQueryExporter.ExportOption, Object> options = page2.getOptions();
			final Path output = page2.getFile();
			getDialogSettings().put(LAST_DIR_KEY, output.getParent().toString());
			if (Files.exists(output)){
				if (!MessageDialog.openConfirm(getShell(), "Overwrite", MessageFormat.format("The files ''{0}'' exists.  Do you want to overwrite?", output.toString()))){
					return false;
				}
			}else if (!Files.exists(output.getParent())){
				if (!MessageDialog.openConfirm(getShell(), "Create Directory", MessageFormat.format("The directory ''{0}'' does not exist.  Do you want to create it?", output.getParent().toString()))){
					return false;
				}
				Files.createDirectories(output.getParent());
			}
			getContainer().run(false, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					runOk[0] =  exportQuery(monitor, output, exporter, options);
				}
			});
		} catch (Exception ex) {
			displayError(ex);
			return false;
		}
		return runOk[0];
	}
	
	private void displayError(Exception ex){
		Intelligence2PlugIn.displayLog(MessageFormat.format("Error exporting query results: {0}", ex.getMessage()), ex);
	}
	/**
	 * Exports a single query to the selected format/file.
	 */
	private boolean exportQuery(IProgressMonitor monitor, Path output,
			IQueryExporter exporter, HashMap<IQueryExporter.ExportOption, Object> options){

		Session s = HibernateManager.openSession();
		try{
			exporter.exportQuery(s, queryResults, output, options);
		}catch (Exception ex){
			displayError(ex);
			return false;
		}finally{
			s.close();
		}
		return true;
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getTargetPage() == page2){
			page2.initValues();
			page2.setPageComplete(true);
		}
	}

}
