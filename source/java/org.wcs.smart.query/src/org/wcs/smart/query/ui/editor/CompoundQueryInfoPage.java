package org.wcs.smart.query.ui.editor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.QueryHeaderComposite;
import org.wcs.smart.query.ui.QueryPropertiesDialog;

public class CompoundQueryInfoPage extends EditorPart  {

	private CompoundQueryEditor parentEditor;
	private FormToolkit toolkit;
	
	private Form frmQueryArea;
	private QueryHeaderComposite compQueryName;
	
	private TableViewer resultsTable;
	
	/**
	 * Creates new editor page
	 * @param parent
	 */
	public CompoundQueryInfoPage(CompoundQueryEditor parent) {
		this.parentEditor = parent;
	}


	
	/**
	 * Does nothing.
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void dispose(){
		super.dispose();
		if (this.toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
	}
	
	/**
	 * Does nothing.
	 */
	@Override
	public void doSaveAs() {
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
	}
	
	/**
	 * Initializes the entire page including the title
	 * @param all
	 */
	public void initPage(){
		updateQueryName();
//		content.initValues(parentEditor.getQueryInternal());
	}
	
	
	/**
	 * @return <code>false</code>
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		return false;
	}

	/** 
	 * @return <code>false</code>
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	
	/**
	 * validates the date filter
	 */
	public void validate(){
	}
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		parent.setLayout(layout);
		
		createContent(parent, toolkit);
	}
	
	
	@Override
	public void setFocus() {
		compQueryName.setFocus();
	}
	
	/**
	 * update name field
	 */
	public void updateQueryName(){
		Query query = parentEditor.getQueryProxy().getQuery();
		compQueryName.setText(query.getName(), query.getId(),
				QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey()).getGuiName());
	}
	
	/**
	 * Creates the main content area 
	 * @param parent
	 */
	private void createContent(Composite parent, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(parent, SWT.NONE);

		toolkit.paintBordersFor(container);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		frmQueryArea = toolkit.createForm(container);
		frmQueryArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,1, 1));
		
		layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		frmQueryArea.getBody().setLayout(layout);
		
		
		createNameHeader(frmQueryArea.getBody(), toolkit);
		
		// --- Query Properties ----
		Composite queryProp = toolkit.createComposite(frmQueryArea.getBody(), SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 10;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginRight = 5;
		layout.marginLeft = 5;
		queryProp.setLayout(layout);
		queryProp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Hyperlink editQueryProp = toolkit.createHyperlink(queryProp, Messages.QueryEditorTableContent_QueryPropertiesLable,SWT.NONE);
		editQueryProp.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
		editQueryProp.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				QueryPropertiesDialog dialog = new QueryPropertiesDialog(
						parentEditor.getSite().getShell(), parentEditor.getQueryProxy().getQuery());
				if (dialog.open() == Window.OK){
					parentEditor.queryPropertiesChange();
				}
			}
		});
		
		
		Composite mainArea = toolkit.createComposite(frmQueryArea.getBody());
		
		mainArea.setLayout(new GridLayout());
		mainArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		resultsTable = new TableViewer(mainArea, SWT.BORDER | SWT.FULL_SELECTION);
		toolkit.adapt(resultsTable.getTable());
		resultsTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		resultsTable.setContentProvider(ArrayContentProvider.getInstance());
		resultsTable.getTable().setHeaderVisible(true);
		resultsTable.getTable().setLinesVisible(true);
		
		TableViewerColumn tableColumn = new TableViewerColumn(resultsTable, SWT.NONE);
		tableColumn.getColumn().setText("Query");
		tableColumn.getColumn().setWidth( 200 );
		tableColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof QueryItem){
					return ((QueryItem)element).getQueryName();
				}
				return super.getText(element);
			}
		});
		
		tableColumn = new TableViewerColumn(resultsTable, SWT.NONE);
		tableColumn.getColumn().setText("Date Field");
		tableColumn.getColumn().setWidth( 200 );
		tableColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof QueryItem){
					return ((QueryItem)element).getDateFilter().asString();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn progressColumn = new TableViewerColumn(resultsTable, SWT.NONE);
		progressColumn.getColumn().setText("Progress/Results");
		progressColumn.getColumn().setWidth( 200 );
		progressColumn.setLabelProvider(new ColumnLabelProvider() {
			 //make sure you dispose these buttons when viewer input changes
			
            @Override
            public void update(ViewerCell cell) {
                TableItem item = (TableItem) cell.getItem();
//                
                if (item.getData() instanceof QueryItem){
                	QueryItem qi = (QueryItem) item.getData();
                	if (qi.gettotalCnt() != null){
                		if (qi.getProgressBar() != null){
                			qi.getProgressBar().dispose();
                			qi.setProgressBar(null);
                		}
                		if (qi.getTotalWidget() == null){
                			Label l = new Label((Composite)cell.getViewerRow().getControl(), SWT.NONE);
                			qi.setTotalWidget(l);
                			
                			qi.getTableEditor().setEditor(l, item, cell.getColumnIndex());
                		}
                		if (qi.gettotalCnt() >= 0){
                			qi.getTotalWidget().setText("Total Records Returned:" + qi.gettotalCnt());
                		}else{
                			qi.getTotalWidget().setText("ERROR");
                		}
                	}else{
                		if (qi.getProgressBar() == null){
                			ProgressBar pbar = new ProgressBar((Composite)cell.getViewerRow().getControl(), SWT.SMOOTH | SWT.HORIZONTAL);
                   			qi.setProgressBar(pbar);
                			
                			TableEditor editor = new TableEditor(item.getParent());
		                    editor.grabHorizontal  = true;
		                    editor.grabVertical = true;
		                    editor.setEditor(pbar, item, cell.getColumnIndex());
		                    editor.layout();
		                    qi.setTableEditor(editor);
                		}
                	}
                }
            }
		});
	}
	
	public void refreshTable(){
		resultsTable.refresh();
	}
	
	public void clearTable(){
		resultsTable.setInput(new Object[]{});
		resultsTable.refresh();
	}
	public void setupTable(List<QueryItem> items){
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				resultsTable.setInput(items);
				resultsTable.refresh();		
			}
			
		});
		
		
	}
	private void createNameHeader(Composite main, FormToolkit toolkit) {
		compQueryName = new QueryHeaderComposite(main, toolkit, frmQueryArea.getFont(), frmQueryArea.getForeground());
		compQueryName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		compQueryName.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				parentEditor.getQueryProxy().getQuery().setName(event.text);
				parentEditor.setDirty(true);
			}});
	}
	
	
}