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
package org.wcs.smart.query.ui.queryfilter;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.ISourceProviderService;
import org.hibernate.Session;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.PatrolQueryOptions;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.query.ui.QueryLayoutManager;
import org.wcs.smart.query.ui.SourceProvider;
import org.wcs.smart.query.ui.SourceProvider.QueryPartPanelType;

/**
 * A view that display the query filter options.
 * <p>
 * The Source Provider is updated when a query filter
 * option is chosen.
 * </p>
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryFilterView extends ViewPart {

	

	public static final String ID ="org.wcs.smart.query.ui.QueryFilter"; //$NON-NLS-1$

	private Composite main;
	
	private Label defaultFilter;
	
	public QueryFilterView() {

	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		Composite outer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 2;
		layout.verticalSpacing = 2;
		layout.marginWidth = 3;
		layout.marginHeight = 3;		
		outer.setLayout(layout);
		outer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		main = new Composite(outer, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		StackLayout stack = new StackLayout();
		stack.marginHeight = stack.marginWidth = 0;
		main.setLayout(stack);
		
		defaultFilter = new Label(main, SWT.NONE);
		defaultFilter.setText("No filter options for current query type.");

		ISourceProviderService service = (ISourceProviderService)getSite().getService(ISourceProviderService.class);
		SourceProvider provider = (SourceProvider) service.getSourceProvider(SourceProvider.QUERY_DROP_TYPE);
		provider.addSourceProviderListener(new ISourceProviderListener() {
			
			@Override
			public void sourceChanged(int sourcePriority, String sourceName,
					Object sourceValue) {
				if (sourceName.equals(SourceProvider.QUERY_DROP_TYPE)){
					AbstractQueryItemPanel panel = QueryLayoutManager.getInstance().getFilterPanel((QueryPartPanelType)sourceValue);
					if (panel != null){
						((StackLayout)main.getLayout()).topControl = panel.getPanel(main);
					}else{
						((StackLayout)main.getLayout()).topControl = defaultFilter;
					}
					main.layout();
				}
			}
			
			@SuppressWarnings("rawtypes")
			@Override
			public void sourceChanged(int sourcePriority, Map sourceValuesByName) {				
			}
		});
	}

	@Override
	public void dispose(){
		super.dispose();
	}
	
	@Override
	public void setFocus() {
		main.setFocus();
	}


}
