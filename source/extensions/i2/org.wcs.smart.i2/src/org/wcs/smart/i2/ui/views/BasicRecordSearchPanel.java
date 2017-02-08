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
package org.wcs.smart.i2.ui.views;

import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.search.BasicRecordSearch;
import org.wcs.smart.i2.search.IntelRecordResult;
import org.wcs.smart.i2.search.IntelRecordSearchResultItem;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Basic search panel for narrative view
 * 
 * @author Emily
 *
 */
public class BasicRecordSearchPanel extends Composite {

	private TableComboViewer cmbSource;
	private TableViewer tblResults;
	private FilterComposite txtNarrative;
	private FilterComposite txtSearch;
	
	private Label searchCount;
	private Label searchTime;
	
	private List<IntelRecordSource> sources = null;
	
	public BasicRecordSearchPanel(Composite parent) {
		super(parent, SWT.NONE);
		
		createControls();
	}
	
	private void createControls(){
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		createSearchPart(this);
		Label l = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createSearchResults(this);
		
		refreshSource();
		
	}
	
	private void createSearchPart(Composite parent){
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		((GridLayout)top.getLayout()).marginWidth = 0;
//		((GridLayout)top.getLayout()).marginHeight = 0;
		
		Label l = new Label(top, SWT.NONE);
		l.setText("Source:");
		
		cmbSource = new TableComboViewer(top, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		cmbSource.setContentProvider(ArrayContentProvider.getInstance());
		cmbSource.setLabelProvider(new RecordSourceLabelProvider());
		cmbSource.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(top, SWT.NONE);
		l.setText("Narrative:");
		
		txtNarrative = new FilterComposite(top, SWT.NONE);
		txtNarrative.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		l = new Label(top, SWT.NONE);
		l.setText("Title:");
		
		//TODO: make return run the search
		txtSearch = new FilterComposite(top, SWT.NONE);
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Button btnSearch = new Button(top, SWT.NONE);
		btnSearch.setText("Search");
		btnSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false, 2, 1));
		btnSearch.addListener(SWT.Selection, e->doSearch());
	}

	private Text txtMatchString;
	
	private void createSearchResults(Composite parent){
		Composite results = new Composite(parent, SWT.NONE);
		results.setLayout(new GridLayout());
		results.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		searchCount = new Label(results, SWT.NONE);
		searchCount.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		tblResults = new TableViewer(results, SWT.FULL_SELECTION | SWT.BORDER);
		tblResults.getTable().setLinesVisible(false);
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		tblResults.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblResults.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection) tblResults.getSelection();
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					Object item = (Object) iterator.next();
					if (item instanceof IntelRecordSearchResultItem){
						RecordEditorInput in = new RecordEditorInput(null, ((IntelRecordSearchResultItem) item).getRecordUuid(), null, null);
						(new OpenRecordHandler()).openRecord(in, false);
					}
					
				}
				
			}
		});
		
		TableViewerColumn statusColumn = new TableViewerColumn(tblResults, SWT.NONE);
		statusColumn.getColumn().setWidth(27);
		statusColumn.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public Image getImage(Object element){
				if (element instanceof IntelRecordSearchResultItem){
					switch(((IntelRecordSearchResultItem) element).getStatus()){
					case COMPLETE:
						return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_SRC_DONE);
					case NEW:
						return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_SRC_NEW);
					case PROCESSING:
						return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_SRC_IP);
					}
				}
				return super.getImage(element);
			}
		});
		
		TableViewerColumn nameColumn = new TableViewerColumn(tblResults, SWT.NONE);
		nameColumn.getColumn().setWidth(250);
		nameColumn.setLabelProvider(new ColumnLabelProvider(){
				HashMap<UUID, Image> images = new HashMap<UUID, Image>();
				
				@Override
				public void dispose(){
					for (Image i : images.values()) i.dispose();
					images.clear();
				}
				@Override
				public String getText(Object element){
					if (element instanceof IntelRecordSearchResultItem){
						return ((IntelRecordSearchResultItem) element).getTitle();
					}
					return super.getText(element);
				}
				@Override
				public Image getImage(Object element){
					if (element instanceof IntelRecordSearchResultItem){
						UUID srcUuid = ((IntelRecordSearchResultItem) element).getRecordSourceUuid();
						Image i = images.get(srcUuid);
						if (i == null){
							if (sources != null){
								for (IntelRecordSource s : sources){
									if (s.getUuid().equals(srcUuid)){
										try{
											BufferedImage image = s.getIconAsImage();
											if (image != null){
												i = AWTSWTImageUtils.convertToSWTImage(image);
												images.put(srcUuid, i);
											}
										}catch (Exception ex){
										}
										break;
									}
								}
							}
						}
						return i;
					}
					return super.getImage(element);
				}
		});
		
		tblResults.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				// TODO Auto-generated method stub
				Object x = ((IStructuredSelection)event.getSelection()).getFirstElement();
				String value = null;
				if (x != null && x instanceof IntelRecordSearchResultItem){
					value = ((IntelRecordSearchResultItem)x).getLocalMatch();
					
				}
				txtMatchString.setText(value == null ? "" : value );
			}
		});
		
		
		searchTime = new Label(results, SWT.NONE);
		searchTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		txtMatchString = new Text(results, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		txtMatchString.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
	}
	
	private void doSearch(){
		String narrative = txtNarrative.getPatternFilter() == null ? null : txtNarrative.getPatternFilter().trim();
		if (narrative != null && narrative.isEmpty()) narrative = null;
		String title = txtSearch.getPatternFilter() == null ? null : txtSearch.getPatternFilter().trim();
		if (title != null && title.isEmpty()) title = null;
		
		Object selection = ((IStructuredSelection)cmbSource.getSelection()).getFirstElement();
		IntelRecordSource source = null;
		if (selection instanceof IntelRecordSource) source = (IntelRecordSource)selection;
		
		final BasicRecordSearch search = new BasicRecordSearch(source, narrative, title);
	
		Job searchJob = new Job("search"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				IntelRecordResult result = null;
				Session s = HibernateManager.openSession();
				try{
					result = search.doSearch(s, monitor);
				}finally{
					s.close();
				}
				IntelRecordResult fresult = result;
				Display.getDefault().syncExec(()->{
					tblResults.setInput(fresult.getResults());
					searchCount.setText(MessageFormat.format("{0} of {1}", fresult.getResults().size(), fresult.getTotalMatched()));
					searchTime.setText(MessageFormat.format("{0} seconds", fresult.getTotalTime() / Math.pow(10, 9)));
				});
				return Status.OK_STATUS;
			}
		};
		searchJob.schedule();
	}
	
	public void refreshSource(){
		cmbSource.setInput(new String[]{DialogConstants.LOADING_TEXT});
		refreshSource.setSystem(true);
		refreshSource.schedule();
	}
	
	private Job refreshSource = new Job("refresh source"){

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> sources = new ArrayList<>();
			sources.add("");
			Session s = HibernateManager.openSession();
			try{
				List<IntelRecordSource> srcs = s.createCriteria(IntelRecordSource.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.list();
					srcs.forEach(r->{
					r.getName();
					r.getIcon();
				});
				sources.addAll(srcs);
				BasicRecordSearchPanel.this.sources = srcs;
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(()->{
					cmbSource.setInput(sources);
			});
			return Status.OK_STATUS;
		}
		
	};
}
