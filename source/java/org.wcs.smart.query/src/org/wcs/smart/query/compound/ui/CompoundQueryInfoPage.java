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
package org.wcs.smart.query.compound.ui;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

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
import org.wcs.smart.query.compound.ui.QueryItem.Status;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.ui.QueryHeaderComposite;
import org.wcs.smart.query.ui.QueryPropertiesDialog;

/**
 * Compound query editor info page.  Lists all queries
 * selected and the date filters.
 * @author Emily
 *
 */
public class CompoundQueryInfoPage extends EditorPart  {

	private CompoundQueryEditor parentEditor;
	private FormToolkit toolkit;
	
	private Form frmQueryArea;
	private QueryHeaderComposite compQueryName;
	private Composite runQueryComp;
	
	private TableViewer resultsTable;
	private Hyperlink runQueryLink ;
	private Composite mainArea;
	
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
		layout.marginHeight = 5;
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
		
		// --- Stack Panel ---
				// Here we either show the table or the progress dialog
		Composite bits = toolkit.createComposite(frmQueryArea.getBody(), SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		bits.setLayout(gl);
		bits.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		runQueryComp = createRunQueryComp(bits, toolkit);
			
		mainArea = toolkit.createComposite(bits);
		
		mainArea.setLayout(new GridLayout());
		mainArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		resultsTable = new TableViewer(mainArea, SWT.BORDER | SWT.FULL_SELECTION);
		toolkit.adapt(resultsTable.getTable());
		resultsTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		resultsTable.setContentProvider(ArrayContentProvider.getInstance());
		resultsTable.getTable().setHeaderVisible(true);
		resultsTable.getTable().setLinesVisible(true);
		
		TableViewerColumn tableColumn = new TableViewerColumn(resultsTable, SWT.NONE);
		tableColumn.getColumn().setText(Messages.CompoundQueryInfoPage_QueryColumnName);
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
		tableColumn.getColumn().setText(Messages.CompoundQueryInfoPage_DateFilterColName);
		tableColumn.getColumn().setWidth( 200 );
		tableColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof QueryItem){
					DateFilter filter = ((QueryItem)element).getDateFilter();
					StringBuilder sb = new StringBuilder();
					
					sb.append(filter.getDateFilterOption().getGuiName(Locale.getDefault()));
					sb.append(" "); //$NON-NLS-1$
					sb.append(filter.getDateFilterOption().getLabel());
					sb.append(" - "); //$NON-NLS-1$
					sb.append(filter.getDateFieldOption().getGuiName(Locale.getDefault()));
					
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn progressColumn = new TableViewerColumn(resultsTable, SWT.NONE);
		progressColumn.getColumn().setText(Messages.CompoundQueryInfoPage_ResultsColName);
		progressColumn.getColumn().setWidth( 200 );
		progressColumn.setLabelProvider(new ColumnLabelProvider() {
			 //make sure you dispose these buttons when viewer input changes
			
            @Override
            public void update(ViewerCell cell) {
                TableItem item = (TableItem) cell.getItem();
//                
                if (item.getData() instanceof QueryItem){
                	QueryItem qi = (QueryItem) item.getData();
                	if (qi.getStatus() == Status.DONE){
                		
                		if (qi.getProgressBar() != null){
                			//dispose of progress bar
                			qi.getProgressBar().dispose();
                			qi.setProgressBar(null);
                		}
                		//create label widget
                		if (qi.getTotalWidget() == null){
                			Label l = new Label((Composite)cell.getViewerRow().getControl(), SWT.NONE);
                			l.setBackground(cell.getBackground());
                			qi.setTotalWidget(l);
                			qi.getTableEditor().setEditor(l, item, cell.getColumnIndex());
                			
                		}
                		
                		//configure table widget
                		if (qi.getStatus() == Status.DONE){
                			if (qi.getTotalCount() >= 0){
                				qi.getTotalWidget().setText(MessageFormat.format(Messages.CompoundQueryInfoPage_TotalMessage, qi.getTotalCount()));
                			}else{
                				qi.getTotalWidget().setText(Messages.CompoundQueryInfoPage_CompleteMessage);
                			}
                		}else if (qi.getStatus() == Status.ERROR){
                			qi.getTotalWidget().setText(MessageFormat.format(Messages.CompoundQueryInfoPage_ErrorMessage, qi.getErrorMessage()));
                		}
                	}else if (qi.getStatus() == Status.PROCESSING || qi.getStatus() == Status.UNKNOWN){
                		if (qi.getProgressBar() == null){
                			if (qi.getTotalWidget() != null){
                				qi.getTotalWidget().dispose();
                				qi.setTotalWidget(null);
                			}
                			ProgressBar pbar = new ProgressBar((Composite)cell.getViewerRow().getControl(), SWT.SMOOTH | SWT.HORIZONTAL);
                   			qi.setProgressBar(pbar);
                			if (qi.getTableEditor() == null){
                				TableEditor editor = new TableEditor(item.getParent());
                				editor.grabHorizontal  = true;
                				editor.grabVertical = true;
                				editor.layout();
                				qi.setTableEditor(editor);
                			}
                			qi.getTableEditor().setEditor(pbar, item, cell.getColumnIndex());
                		}
                	}
                }
            }
		});
		mainArea.setVisible(false);
	}
	
	public void refreshTable(){
		resultsTable.refresh();
	}
	
	public void clearTable(){
		disposeItems();
		resultsTable.setInput(new Object[]{});
		resultsTable.refresh();
	}
	
	private void disposeItems(){
		Object old = resultsTable.getInput();
		if (old instanceof List){
			List<?> oldlist = (List<?>)old;
			for (Object x : oldlist){
				if (x instanceof QueryItem){
					((QueryItem)x).dispose();
				}
			}
		}
	}
	public void setupTable(List<QueryItem> items){
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				if (!runQueryComp.isDisposed()){
					runQueryComp.dispose();
					mainArea.setVisible(true);
					mainArea.getParent().layout(true, true);
				}
				
				disposeItems();
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
	
	/**
	 * Creates the initial composite that prompts users
	 * to run query.
	 * 
	 * @param parent
	 * @return
	 */
	private Composite createRunQueryComp(Composite parent, FormToolkit toolkit){
		Composite main = toolkit.createComposite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		runQueryLink = toolkit.createHyperlink(main, Messages.QueryEditorTableContent_RunQueryLink, SWT.NONE);
		runQueryLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				parentEditor.executeQuery();
			}
		});
		return main;
	}
	
	
	
}