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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.importexport.QueryExportEngine;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;

/**
 * Wizard for collecting export format.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class RunExportFormatPage extends WizardPage {

	public static final String NAME = "FORMATPAGE"; //$NON-NLS-1$
	
	private Composite list;
	private ScrolledComposite form;
	
	private HashMap<Query, ComboViewer> query2format;
	private List<IDateFieldFilter> dateFilters;
	
	protected RunExportFormatPage() {
		super(NAME);
	}

	public RunExportWizard getWizardInternal() {
		return (RunExportWizard)getWizard();
	}
	
	public List<IDateFieldFilter> getDateFilters(){
		return this.dateFilters;
	}
	
	public HashMap<Query, ComboViewer> getFormats(){
		return this.query2format;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void createControl(Composite parent) {
	
		Composite core = new Composite(parent , SWT.NONE);
		core.setLayout(new GridLayout());
		((GridLayout)core.getLayout()).marginWidth = 0;
		((GridLayout)core.getLayout()).marginHeight = 0;
		
		form = new ScrolledComposite(core, SWT.V_SCROLL | SWT.H_SCROLL);
		form.setExpandHorizontal(true);
		form.setExpandVertical(true);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		list = new Composite(form, SWT.NONE);
		list.setLayout(new GridLayout(2, false));
		form.setContent(list);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		dateFilters = new ArrayList<>();
		
		query2format = new HashMap<>();
		
		for (Query q : getWizardInternal().getQueries()) {
			//classification
			Label querylabel = new Label(list, SWT.NONE);
			querylabel.setText(q.getName()); //+ " (" + QueryTypeManager.INSTANCE.findQueryType(q.getTypeKey()).getGuiName() + ")");
			querylabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));


			IQueryType type = QueryTypeManager.INSTANCE.findQueryType(q.getTypeKey());
			
			if (dateFilters.isEmpty()) {
				for (IDateFieldFilter i : type.getDateFilterOptions()) {
					dateFilters.add(i);
				}
			}else {
				Set<IDateFieldFilter> test = new HashSet<>();
				for (IDateFieldFilter i : type.getDateFilterOptions()) test.add(i);
				for (Iterator<IDateFieldFilter> iterator = dateFilters.iterator(); iterator.hasNext();) {
					IDateFieldFilter iDateFieldFilter = iterator.next();
					if (!test.contains(iDateFieldFilter)) iterator.remove();
				}
			}
			
			ComboViewer cmbViewer = new ComboViewer(list, SWT.READ_ONLY | SWT.DROP_DOWN);
			cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
			cmbViewer.setLabelProvider(new QueryExportLabelProvider());
			List<IQueryExporter> exporters = QueryExportEngine.getQueryExports(q);
			for (Iterator<IQueryExporter> iterator = exporters.iterator(); iterator.hasNext();) {
				IQueryExporter iQueryExporter = (IQueryExporter) iterator.next();
				if (iQueryExporter.getId().startsWith(IQueryExporter.QUERY_DEFINTION_EXPORTER_ID)) iterator.remove();
			}
			cmbViewer.setInput(exporters);
			if (!exporters.isEmpty()) cmbViewer.setSelection(new StructuredSelection(exporters.get(0)));
			query2format.put(q,cmbViewer);
			
			Menu m = new Menu(cmbViewer.getControl());
			MenuItem applyAll = new MenuItem(m, SWT.PUSH);
			applyAll.setText(Messages.RunExportFormatPage_ApplyAll);
			applyAll.addListener(SWT.Selection, e->{
				IQueryExporter exporter = (IQueryExporter) cmbViewer.getStructuredSelection().getFirstElement();
				
				for (ComboViewer c : query2format.values()) {
					if (c == cmbViewer) continue;
					List<IQueryExporter> items = (List<IQueryExporter>)c.getInput();
					for (IQueryExporter i : items) {
						if (i.getId().contentEquals(exporter.getId())) {
							c.setSelection(new StructuredSelection(i));
							break;
						}
					}
					
					
				}
			});
			cmbViewer.getControl().setMenu(m);
			
			
		}
		form.setMinSize(list.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setPageComplete(true);
		
		setControl(core);
		
		setTitle(Messages.RunExportFormatPage_PageTitle);
		setMessage(Messages.RunExportFormatPage_PageMessage);
	}

}
