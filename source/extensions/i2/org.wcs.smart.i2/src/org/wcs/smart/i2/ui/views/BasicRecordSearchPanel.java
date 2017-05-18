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
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.search.BasicRecordSearch;
import org.wcs.smart.i2.search.IntelRecordResult;
import org.wcs.smart.i2.search.IntelRecordSearchResultItem;
import org.wcs.smart.i2.ui.DeleteRecordHandler;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.entity.exporter.RecordCsvExporter;
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
	private StyledText txtMatchString;
	
	private Label searchCount;
	private Label searchTime;
	
	private IEclipseContext context;
	
	private List<IntelRecordSource> sources = null;
	
	public BasicRecordSearchPanel(Composite parent, FormToolkit toolkit, IEclipseContext context) {
		super(parent, SWT.NONE);
		this.context = context;
		createControls(toolkit);
	}
	
	private void createControls(FormToolkit toolkit){
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		createSearchPart(this, toolkit);
		Label l = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createSearchResults(this);
		
		refreshSource();
		
	}
	
	private void createSearchPart(Composite parent,FormToolkit toolkit){
		Composite top = toolkit.createComposite(parent);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		((GridLayout)top.getLayout()).marginWidth = 0;
//		((GridLayout)top.getLayout()).marginHeight = 0;
		
		toolkit.createLabel(top, Messages.BasicRecordSearchPanel_SourceLabel);
		
		cmbSource = new TableComboViewer(top, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		cmbSource.setContentProvider(ArrayContentProvider.getInstance());
		cmbSource.setLabelProvider(new RecordSourceLabelProvider());
		cmbSource.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbSource.getControl().addListener(SWT.KeyDown, e->{
			if (e.character == SWT.CR){
				doSearch();
			}
		});
		
		toolkit.createLabel(top, Messages.BasicRecordSearchPanel_NarrativeLabel);
		
		txtNarrative = new FilterComposite(top, SWT.NONE);
		txtNarrative.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtNarrative.getControl().addListener(SWT.KeyDown, e->{
			if (e.character == SWT.CR){
				doSearch();
			}
		});

		toolkit.createLabel(top, Messages.BasicRecordSearchPanel_TitleLabel);
		
		
		txtSearch = new FilterComposite(top, SWT.NONE);
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSearch.getControl().addListener(SWT.KeyDown, e->{
			if (e.character == SWT.CR){
				doSearch();
			}
		});
		
		Composite btnComp = toolkit.createComposite(top);
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		btnComp.setLayout(new GridLayout(2, false));
		((GridLayout)btnComp.getLayout()).marginWidth  = 0;
		((GridLayout)btnComp.getLayout()).marginHeight  = 0;
		
		Hyperlink btnExport = toolkit.createHyperlink(btnComp, Messages.BasicRecordSearchPanel_ExportLink, SWT.NONE);
		btnExport.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		btnExport.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				doExport();
			}
		});
		btnExport.setToolTipText(Messages.BasicRecordSearchPanel_ExportTooltip);
		
		Button btnSearch = toolkit.createButton(btnComp, Messages.BasicRecordSearchPanel_SearchBtn,  SWT.PUSH);
		btnSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		btnSearch.addListener(SWT.Selection, e->doSearch());
	
	}

	private void createSearchResults(Composite parent){
		Composite results = new Composite(parent, SWT.NONE);
		results.setLayout(new GridLayout());
		results.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		searchCount = new Label(results, SWT.NONE);
		searchCount.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		tblResults = new TableViewer(results, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		tblResults.getTable().setLinesVisible(false);
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		tblResults.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblResults.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openSelection();
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
				Object x = ((IStructuredSelection)event.getSelection()).getFirstElement();
				String value = null;
				int[][] ranges = null;
				if (x != null && x instanceof IntelRecordSearchResultItem){
					value = ((IntelRecordSearchResultItem)x).getLocalMatch();
					ranges = ((IntelRecordSearchResultItem)x).getMatchRanges();
					
				}
				if(value != null){
					txtMatchString.setText( value );
					if (ranges != null){
						StyleRange[] styles = new StyleRange[ranges.length];
						int index = 0;
						for (int[] range : ranges){
							StyleRange r = new StyleRange(range[0], range[1] - range[0], getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT), getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
							r.fontStyle = SWT.BOLD;
							styles[index++] = r;
						}
						txtMatchString.setStyleRanges(styles);
					}
					
					if (!txtMatchString.isVisible()){
						((GridData)txtMatchString.getLayoutData()).heightHint = (int)(tblResults.getControl().getBounds().height / 3.0);
						txtMatchString.getParent().layout(true, true);
						txtMatchString.setVisible(true);
					}
				}else{
					txtMatchString.setText(""); //$NON-NLS-1$
					txtMatchString.setStyleRange(null);
					((GridData)txtMatchString.getLayoutData()).heightHint = 0;
					txtMatchString.getParent().layout(true, true);
					txtMatchString.setVisible(false);
				}
			}
		});
		tblResults.addDragSupport(DND.DROP_LINK,new Transfer[]{IntelRecordSelectionTransfer.getTransfer()}, new DragSourceAdapter(){
			private ISelection getSelection(){
				List<RecordEditorInput> selection = new ArrayList<>();
				IStructuredSelection sel = (IStructuredSelection) tblResults.getSelection();
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					Object o= iterator.next();
					if (o instanceof IntelRecordSearchResultItem){
						IntelRecordSearchResultItem x = (IntelRecordSearchResultItem) o;
						selection.add(new RecordEditorInput(x.getTitle(), x.getRecordUuid(), null, x.getRecordSourceUuid(), x.getStatus()));
					}
					
				}
				return new StructuredSelection(selection);
			}
			@Override
			public void dragStart(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(getSelection());				
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (IntelRecordSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = getSelection();
				}
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(null);
			}
		});
		
		searchTime = new Label(results, SWT.NONE);
		searchTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		txtMatchString = new StyledText (results, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		txtMatchString.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtMatchString.getLayoutData()).heightHint = 0;
		txtMatchString.setVisible(false);
		
		
		Menu mnu = new Menu(tblResults.getControl());
		
		final MenuItem open = new MenuItem(mnu, SWT.PUSH);
		open.setText(Messages.BasicRecordSearchPanel_OpenMenuItem);
		open.addListener(SWT.Selection, e->openSelection());
		
		MenuItem ws = null;
		if (IntelSecurityManager.INSTANCE.canViewWorkingSets()){
				ws = new MenuItem(mnu, SWT.PUSH);
				ws.setText(Messages.BasicRecordSearchPanel_AddToWsMenuItem);
				ws.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
				ws.addListener(SWT.Selection, e->addToWorkingset());
		}
		final MenuItem wsItem = ws;
		
		MenuItem delete = null;
		if (IntelSecurityManager.INSTANCE.canDeleteRecord()){
			delete = new MenuItem(mnu, SWT.PUSH);
			delete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			delete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			delete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					List<Object> toDelete = new ArrayList<>();
					for (Iterator<?> iterator = ((IStructuredSelection)tblResults.getSelection()).iterator(); iterator.hasNext();) {
						Object x = (Object) iterator.next();
						if (x instanceof IntelRecordSearchResultItem){
							IntelRecordSearchResultItem item = (IntelRecordSearchResultItem) x;
							toDelete.add(new RecordEditorInput(item.getTitle(), item.getRecordUuid(), null, item.getRecordSourceUuid(), null));
						}
					}
					if ((new DeleteRecordHandler()).deleteRecords(toDelete, context)){
						doSearch();
					}
				}
			});
		}
		final MenuItem miDelete = delete;
		tblResults.getControl().setMenu(mnu);
		
		tblResults.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)event.getSelection()).getFirstElement();
				open.setEnabled(x != null && x instanceof IntelRecordSearchResultItem);
			}
		});
		
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				if (wsItem != null) wsItem.setEnabled(open.isEnabled() && WorkingSetManager.INSTANCE.isSet());
				if (miDelete != null) miDelete.setEnabled(open.isEnabled());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		
	}
	
	private void addToWorkingset(){
		List<RecordEditorInput> toAdd = new ArrayList<RecordEditorInput>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblResults.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();	
			if (x instanceof IntelRecordSearchResultItem){
				RecordEditorInput e = new RecordEditorInput(null, ((IntelRecordSearchResultItem) x).getRecordUuid(), null, ((IntelRecordSearchResultItem) x).getRecordSourceUuid(), null);
				toAdd.add(e);
			}
		}
		WorkingSetManager.INSTANCE.addRecordInputToActiveWorkingSetRecord(toAdd, context);
	}
	
	private void openSelection(){
		IStructuredSelection sel = (IStructuredSelection) tblResults.getSelection();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof IntelRecordSearchResultItem){
				RecordEditorInput in = new RecordEditorInput(null, ((IntelRecordSearchResultItem) item).getRecordUuid(), null, null, ((IntelRecordSearchResultItem)item).getStatus());
				(new OpenRecordHandler()).openRecord(in, false);
			}
			
		}
	}
	
	private void doExport(){
		List<UUID> toExport = new ArrayList<UUID>();
		List<?> sel = (List<?>) tblResults.getInput();
		if (sel == null) return;
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof IntelRecordSearchResultItem){
				toExport.add(((IntelRecordSearchResultItem) item).getRecordUuid());
			}
		}
		if (toExport.isEmpty()) return;
		
		RecordCsvExporter exporter = new RecordCsvExporter(toExport);
		CsvExportDialog dialog = new CsvExportDialog(getShell(), exporter.createExportConfiguration());
		dialog.open();		
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
	
		Job searchJob = new Job(Messages.BasicRecordSearchPanel_SearchJobName){
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
					searchCount.setText(MessageFormat.format(Messages.BasicRecordSearchPanel_SearchResultCntLabel, fresult.getResults().size(), fresult.getTotalMatched()));
					searchTime.setText(MessageFormat.format(Messages.BasicRecordSearchPanel_SearchResultTimeLabel, fresult.getTotalTime() / Math.pow(10, 9)));
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
	
	private Job refreshSource = new Job(Messages.BasicRecordSearchPanel_RefershSourceJobName){

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> sources = new ArrayList<>();
			sources.add(""); //$NON-NLS-1$
			Session s = HibernateManager.openSession();
			try{
				List<IntelRecordSource> srcs = s.createCriteria(IntelRecordSource.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
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
