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
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.export.IQueryExporter;
import org.wcs.smart.i2.query.export.IQueryExporter.ExportOption;

/**
 * Export query wizard.
 * 
 * @author Emily
 *
 */
public class ExportQueryWizard extends Wizard implements IPageChangingListener{
	
	public static final String LAST_DIR_KEY = "org.wcs.smart.i2.query.export.directory"; //$NON-NLS-1$

	private AbstractIntelQuery query;
	private IQueryResult queryResults;
	
	private QueryFormatPage page1;
	private QueryFormatOptionPage page2;
	private ExportQueryGeometryColumnPage page2a;
	
	private List<Projection> supportedProjections = null;
	private Projection defaultProjection = null;
	
	private List<IQueryColumn> queryColumns = null;
	
	public ExportQueryWizard(AbstractIntelQuery query, IQueryResult results) {
		this.query = query;
		this.queryResults = results;
		
		setWindowTitle(Messages.ExportQueryWizard_Title);
		setDialogSettings(Intelligence2PlugIn.getDefault().getDialogSettings());
		try(Session s = HibernateManager.openSession()){
			supportedProjections = HibernateManager.getCaProjectionList(s);
			defaultProjection = HibernateManager.getCurrentViewProjection(s);
		}
		if (results instanceof IPagedQueryResultSet) {
			queryColumns = ((IPagedQueryResultSet)results).getQueryColumns();
			
		}
		super.setNeedsProgressMonitor(true);
	}

	public List<IQueryColumn> getGeometryColumns(IQueryExporter exporter){
		if (queryColumns == null) return null;
		if (exporter == null) return null;
		
		List<IQueryColumn> geomColumns = queryColumns.stream()
				.filter(e-> e.getDataType().isGeometry())
				.collect(Collectors.toList());
		geomColumns .sort((a,b)->Collator.getInstance().compare(a.getColumnName(), b.getColumnName()));
		
		return geomColumns;
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
		
		page2a = new ExportQueryGeometryColumnPage();
		super.addPage(page2a);

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
	public AbstractIntelQuery getQuery(){
		return this.query;
	}
	
	public List<IQueryColumn> getGeometryColumnsForExport(){
		if (this.queryColumns == null) return null;
		if (!getQueryExporter().supportsOption(ExportOption.GEOMETRY_COLUMN)) return null;
		if (this.getGeometryColumns(getQueryExporter()).size() == 1) return  Collections.singletonList(this.getGeometryColumns(getQueryExporter()).get(0));
		return page2a.getGeometryColumns();	
	}
	
	/**
	 * Runs the export process
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		page1.performFinish();
		
		List<Path> exportFiles = new ArrayList<>();
		try {
			final IQueryExporter exporter = getQueryExporter();
			if (exporter == null){
				throw new Exception("No exporter selected"); //$NON-NLS-1$
			}
			
			HashMap<IQueryExporter.ExportOption, Object> options = page2.getOptions();
			if (options == null) options = new HashMap<>();
			final Path output = page2.getFile();
			
			if (getGeometryColumnsForExport() == null || getGeometryColumnsForExport().size() == 1) {
				getDialogSettings().put(LAST_DIR_KEY, output.getParent().toString());
			
				if (Files.exists(output)){
					if (!MessageDialog.openConfirm(getShell(), Messages.ExportQueryWizard_OverwriteDialogTitle, MessageFormat.format(Messages.ExportQueryWizard_OverwriteDialogMsg, output.toString()))){
						return false;
					}
				}else if (!Files.exists(output.getParent())){
					if (!MessageDialog.openConfirm(getShell(), Messages.ExportQueryWizard_CreateDialogTitle, MessageFormat.format(Messages.ExportQueryWizard_CreateDialogMessage, output.getParent().toString()))){
						return false;
					}
					Files.createDirectories(output.getParent());
				}
			}else if (getGeometryColumnsForExport() != null) {
				getDialogSettings().put(LAST_DIR_KEY, output.toString());
				
				if (!Files.exists(output)){
					if (!MessageDialog.openConfirm(getShell(), Messages.ExportQueryWizard_CreateDialogTitle, MessageFormat.format(Messages.ExportQueryWizard_CreateDialogMessage, output.getParent().toString()))){
						return false;
					}
					Files.createDirectories(output);
				}
			}
			final HashMap<IQueryExporter.ExportOption, Object> foptions = options;

				getContainer().run(false, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException, InterruptedException {
						if (getGeometryColumnsForExport() == null) {
							Collection<Path> results = exportQuery(monitor, output, exporter, foptions);
							if (results != null) exportFiles.addAll(results);
						}else {
							for (IQueryColumn qc : getGeometryColumnsForExport()) {
								foptions.put(IQueryExporter.ExportOption.GEOMETRY_COLUMN, qc);
								String name = URLUtils.cleanFilename(query.getName() + "_" + qc.getColumnName()); //$NON-NLS-1$
								name += "." + exporter.getExtension(); //$NON-NLS-1$
								Path output2 = output.resolve(name);
								if(Files.exists(output2)) {
									if (!MessageDialog.openConfirm(getShell(), Messages.ExportQueryWizard_OverwriteDialogTitle, MessageFormat.format(Messages.ExportQueryWizard_OverwriteDialogMsg, output2.toString()))){
										continue;
									}
								}
								Collection<Path> results = exportQuery(monitor, output2, exporter, foptions);
								if (results != null) exportFiles.addAll(results);
								
							}
						}
					}
				});
		} catch (Exception ex) {
			displayError(ex);
			return false;
		}
		if (!exportFiles.isEmpty()) {
			StringBuilder message = new StringBuilder();
			for (Path p : exportFiles) {
				if (message.length() != 0) {
					message.append(Messages.ExportQueryWizard_And);
				}
				message.append(p.toString());
			}
			MessageDialog.openInformation(getShell(), Messages.ExportQueryWizard_ExportDone, MessageFormat.format(Messages.ExportQueryWizard_ExportDoneMsg, message));
			return true;
		}					
		return false;
	}
	
	private void displayError(Exception ex){
		Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.ExportQueryWizard_ExportError, ex.getMessage()), ex);
	}
	/**
	 * Exports a single query to the selected format/file.
	 */
	private Collection<Path> exportQuery(IProgressMonitor monitor, Path output,
			IQueryExporter exporter, HashMap<IQueryExporter.ExportOption, Object> options){

		try(Session s = HibernateManager.openSession()){
			return exporter.exportQuery(s, queryResults, output, options);
		}catch (Exception ex){
			displayError(ex);
			return null;
		}
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getTargetPage() == page2){
			page2.initValues();
			page2.setPageComplete(true);
		}else if (event.getTargetPage() == page2a){
			page2a.initValues();				
		}
	}

}
