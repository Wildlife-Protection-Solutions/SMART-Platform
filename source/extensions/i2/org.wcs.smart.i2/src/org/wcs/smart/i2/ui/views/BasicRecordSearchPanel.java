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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.search.BasicRecordSearch;
import org.wcs.smart.i2.search.IntelRecordResult;
import org.wcs.smart.i2.search.IntelRecordSearchResultItem;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.DeleteRecordHandler;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.entity.exporter.RecordCsvExporter;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;
import org.wcs.smart.util.SmartUtils;

/**
 * Basic search panel for narrative view
 * 
 * @author Emily
 *
 */
public class BasicRecordSearchPanel extends Composite {

	private final Color LIST_HIGHLIGHT_COLOR = SmartUtils.getListHighlightColor(Display.getDefault());
	private final Color LIST_SELECTION_COLOR = SmartUtils.getListSelectedColor(Display.getDefault());
	
	private TableComboViewer cmbSource;
	private TableViewer tblResults;
	private FilterComposite txtNarrative;
	private FilterComposite txtSearch;
	private StyledText txtMatchString;
	
	private Label searchCount;
	private Label searchTime;
	
	private Pattern narrativePattern = null;
	
	private IEclipseContext context;
	
	public BasicRecordSearchPanel(Composite parent, FormToolkit toolkit, IEclipseContext context) {
		super(parent, SWT.NONE);
		this.context = context;
		createControls(toolkit);
		
		addListener(SWT.Dispose, e->{
			LIST_HIGHLIGHT_COLOR.dispose();
			LIST_SELECTION_COLOR.dispose();
		});
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
		cmbSource.getControl().setEnabled(IntelSecurityManager.INSTANCE.canViewRecords());
		
		toolkit.createLabel(top, Messages.BasicRecordSearchPanel_NarrativeLabel);
		
		txtNarrative = new FilterComposite(top, SWT.NONE);
		txtNarrative.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtNarrative.getControl().addListener(SWT.KeyDown, e->{
			if (e.character == SWT.CR){
				doSearch();
			}
		});
		txtNarrative.setEnabled(IntelSecurityManager.INSTANCE.canViewRecords());
		
		toolkit.createLabel(top, Messages.BasicRecordSearchPanel_TitleLabel);
		
		
		txtSearch = new FilterComposite(top, SWT.NONE);
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSearch.getControl().addListener(SWT.KeyDown, e->{
			if (e.character == SWT.CR){
				doSearch();
			}
		});
		txtSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewRecords());
		
		Composite btnComp = toolkit.createComposite(top);
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		btnComp.setLayout(new GridLayout(2, false));
		((GridLayout)btnComp.getLayout()).marginWidth  = 0;
		((GridLayout)btnComp.getLayout()).marginHeight  = 0;
		btnComp.setEnabled(IntelSecurityManager.INSTANCE.canViewRecords());
		
		Hyperlink btnExport = toolkit.createHyperlink(btnComp, Messages.BasicRecordSearchPanel_ExportLink, SWT.NONE);
		btnExport.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		btnExport.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				doExport();
			}
		});
		btnExport.setToolTipText(Messages.BasicRecordSearchPanel_ExportTooltip);
		btnExport.setEnabled(IntelSecurityManager.INSTANCE.canViewRecords());
		
		Button btnSearch = toolkit.createButton(btnComp, Messages.BasicRecordSearchPanel_SearchBtn,  SWT.PUSH);
		btnSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		btnSearch.addListener(SWT.Selection, e->doSearch());
		btnSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewRecords());
	
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
		
//		TableViewerColumn statusColumn = new TableViewerColumn(tblResults, SWT.NONE);
		
		tblResults.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) { return ""; }; //$NON-NLS-1$
			@Override
			public Image getImage(Object element) { return null; };
		});
		
		tblResults.setInput(new String[]{DialogConstants.LOADING_TEXT});
		if (!IntelSecurityManager.INSTANCE.canViewRecords()) {
			tblResults.setInput(new String[] {Messages.BasicRecordSearchPanel_unauthorized});
		}
		final RecordsViewLabelProvider lblprovider = new RecordsViewLabelProvider(true);
		tblResults.getControl().addListener(SWT.MeasureItem, new Listener() {
	 		public void handleEvent(Event event) {
	 			TableItem item = (TableItem)event.item;
	 			Image trailingImage = lblprovider.getImage(item.getData());
	 			String txt = lblprovider.getText(item.getData());
	 			int width = 0;
	 			int height = 0;
	 			if (trailingImage != null) {
	 				width += trailingImage.getBounds().width;
	 				height = trailingImage.getBounds().height;
	 			}
	 			width += event.gc.stringExtent(txt).x + 1;
	 			height = Math.max(height,  event.gc.stringExtent(txt).y);
	 			event.width = width;
	 			event.height = height;
	 		}
	 	});
	 	
		tblResults.getControl().addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event event) {
				TableItem item = (TableItem) event.item;
				Image trailingImage = lblprovider.getImage(item.getData());
				int offset = 0;
				Color c = event.gc.getBackground();
				if (trailingImage != null) {
					int x = event.x + event.width;
					int itemHeight = tblResults.getTable().getItemHeight();
					int imageHeight = trailingImage.getBounds().height;
					int y = event.y + (itemHeight - imageHeight) / 2;
					event.gc.drawImage(trailingImage, x, y);
					offset = x + trailingImage.getBounds().width;
				}
				if ((event.detail & SWT.SELECTED) == SWT.SELECTED) {
					c = LIST_SELECTION_COLOR;
				}else if ( (event.detail & SWT.HOT) == SWT.HOT) {
					c = LIST_HIGHLIGHT_COLOR;
				}
				String text = lblprovider.getText(item.getData());
				event.gc.setBackground(c);
				event.gc.drawText(text, offset+1, event.y+1);
				
			}
		});
		
		tblResults.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)event.getSelection()).getFirstElement();
				
				if (x != null && x instanceof IntelRecordSearchResultItem){
					searchJobRecordUuid = ((IntelRecordSearchResultItem)x).getRecordUuid();
					hightlightSearchResults.setSystem(true);
					hightlightSearchResults.cancel();
					hightlightSearchResults.schedule();
					
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
						selection.add(new RecordEditorInput(x.getTitle(), x.getRecordUuid(), null, x.getRecordSource().getUuid(), x.getStatus()));
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
		if (IntelSecurityManager.INSTANCE.canEditWorkingSet()){
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
							toDelete.add(new RecordEditorInput(item.getTitle(), item.getRecordUuid(), null, item.getRecordSource().getUuid(), null));
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
				RecordEditorInput e = new RecordEditorInput(null, ((IntelRecordSearchResultItem) x).getRecordUuid(), null, ((IntelRecordSearchResultItem) x).getRecordSource().getUuid(), null);
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
		
		narrativePattern = null;
		if (narrative != null && !narrative.isEmpty()){
			narrativePattern = Pattern.compile(narrative);
		}
		
		Job searchJob = new Job(Messages.BasicRecordSearchPanel_SearchJobName){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				IntelRecordResult result = null;
				try(Session s = HibernateManager.openSession()){
					result = search.doSearch(s, monitor);
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
		refreshSourcesJob.setSystem(true);
		refreshSourcesJob.schedule();
	}

	private Job refreshSourcesJob = new Job("refresh source list") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> srcs = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				srcs.addAll(QueryFactory.buildQuery(session, IntelRecordSource.class,"conservationArea", SmartDB.getCurrentConservationArea()).getResultList()); //$NON-NLS-1$
				srcs.forEach(e->((IntelRecordSource)e).getName());
			}
			srcs.add(0, ""); //$NON-NLS-1$
			Display.getDefault().syncExec(()->{
				cmbSource.setInput(srcs);
			});
			return Status.OK_STATUS;
		}
		
	};
		
	/*
	 * Highlight search results
	 */
	private UUID searchJobRecordUuid;
	private Job hightlightSearchResults = new Job("highlight search results") { //$NON-NLS-1$
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			UUID uuid = searchJobRecordUuid;
			if (uuid == null || narrativePattern == null) {
				clearAndHide();
				return Status.OK_STATUS;
			}
			
			String narrative = null;
			try(Session session = HibernateManager.openSession()){
				IntelRecord record = session.get(IntelRecord.class, uuid);
				if (record != null) {
					narrative = record.getDescription();
				}
			}
			if (narrative == null || monitor.isCanceled()) {
				clearAndHide();
				return Status.OK_STATUS;
			}
			
			//perform match
			StringBuilder localMatch = new StringBuilder();
			List<int[]> matchRanges = new ArrayList<>();
			if (narrativePattern != null){
				Matcher m = narrativePattern.matcher(narrative.toLowerCase());
				while(m.find()){
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					int sIndex = m.start();
					int eIndex = m.end();
					int offset = 150;
					
					int start = sIndex - offset; 
					if (start < 0) start = 0;
					int rangeStart = sIndex - start;
					
					int end = eIndex + offset;
					if (end > narrative.length()) end = narrative.length();
					
					
					rangeStart = rangeStart + 3 + localMatch.length();
					int rangeEnd = rangeStart + (eIndex - sIndex);
					
					matchRanges.add(new int[]{rangeStart, rangeEnd});
					localMatch.append("..." + narrative.substring(start, end) + "..."); //$NON-NLS-1$ //$NON-NLS-2$
					localMatch.append("\n\n"); //$NON-NLS-1$
				}
			}
			
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			
			Display.getDefault().syncExec(()->{
				if (monitor.isCanceled()) return ;				
				if(localMatch.length() > 0){
					String value = localMatch.toString();
					txtMatchString.setText( value );
					if (matchRanges != null){
						StyleRange[] styles = new StyleRange[matchRanges.size()];
						int index = 0;
						for (int[] range : matchRanges){
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
					clearAndHide();
				}
			});
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;			
			return Status.OK_STATUS;
		}
		
		private void clearAndHide() {
			Display.getDefault().syncExec(()->{
				txtMatchString.setText(""); //$NON-NLS-1$
				txtMatchString.setStyleRange(null);
				((GridData)txtMatchString.getLayoutData()).heightHint = 0;
				txtMatchString.getParent().layout(true, true);
				txtMatchString.setVisible(false);
			});
		}
	};
}
