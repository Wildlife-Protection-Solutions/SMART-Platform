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
package org.wcs.smart.query.ui.definition;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.ui.IQueryEditor;
import org.wcs.smart.query.ui.SourceProvider;
import org.wcs.smart.query.ui.SourceProvider.QueryDropType;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;

/**
 * A view for building query definition.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryDefView extends ViewPart {

	/**
	 * View identifier
	 */
	public static final String ID = "org.wcs.smart.query.ui.QueryDefView"; //$NON-NLS-1$
	
	private Query current = null;	

	private HashMap<Query.QueryType, QueryDefinitionComposite>
		definitionComposites = new HashMap<Query.QueryType, QueryDefinitionComposite>();

	private Composite stackComp = null;
	private Composite emptyComp = null;
	
	private QueryDefinitionComposite currentPanel;
	
	/* listener to update query definition when window changes */
	private IPartListener2 editorListener = new IPartListener2() {
		
		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (part instanceof IQueryEditor){
				Query q =((IQueryEditor)part).getQuery();
				if (q != current){
					setQuery(q);
				}
				if (currentPanel != null){
					currentPanel.validate();
				}
			}
		}
		
		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			try{
				IEditorReference[] editors = partRef.getPage().getEditorReferences();
				boolean hasQueryEditor= false;
				for (int i = 0; i < editors.length; i ++){
					if (QueryInput.class.isAssignableFrom(editors[i].getEditorInput().getClass() )){
						hasQueryEditor = true;
						break;
					}
				}
				if (!hasQueryEditor){
					setQuery(null);	
				}
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.QueryDefView_CloseError + ex.getLocalizedMessage(), ex);
			}
		}
		
		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (part instanceof IQueryEditor){
				Query q =((IQueryEditor)part).getQuery();
				if (q != current){
					setQuery(q);
				}
				if (currentPanel != null){
					currentPanel.validate();
				}
			}
		}
	};

	
	/**
	 * Creates new query definition view.
	 */
	public QueryDefView() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(editorListener);
	}
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (editorListener != null){
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(editorListener);
		}
		for (QueryDefinitionComposite comp : definitionComposites.values()){
			comp.dispose();
		}
	}
	
	/**
	 * Clears the current query
	 */
	public void clearQuery(){
		currentPanel.clear();
		QueryEventManager.getInstance().fireQueryChangedListeners(current);
	}


	/**
	 * Validates the current query
	 */
	public void validate(){
		currentPanel.validate();
	}
	
	/**
	 * Runs the current query
	 */
	public void runQuery(){
		currentPanel.validate();
		QueryEventManager.getInstance().fireQueryRunListeners(current);
		
	}
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		stackComp = new Composite(parent, SWT.BORDER);
		
		StackLayout layout = new StackLayout();
		layout.marginHeight = 0;
		layout.marginWidth=0;
		stackComp.setLayout(layout);
		stackComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));		
		emptyComp = new Composite(stackComp, SWT.NONE);
		emptyComp.setLayout(new GridLayout(1, false));
		
		ObservationQueryDefinitionComposite wp = new ObservationQueryDefinitionComposite(stackComp, this);
		definitionComposites.put(QueryType.OBSERVATION, wp);
		definitionComposites.put(QueryType.PATROL, wp);	//patrols are the same
		SummaryQueryDefinitionComposite sum = new SummaryQueryDefinitionComposite(stackComp, this);
		definitionComposites.put(QueryType.SUMMARY, sum);
		GriddedQueryDefinitionComposite grid = new GriddedQueryDefinitionComposite(stackComp, this);
		definitionComposites.put(QueryType.GRIDDED, grid);
		
		
		((StackLayout)stackComp.getLayout()).topControl = emptyComp;
	
		addSourceListener();
	}
	
	private void addSourceListener() {
		// add a listener for when items are added to the query
		ISourceProviderService service = (ISourceProviderService) getSite()
				.getService(ISourceProviderService.class);
		final SourceProvider provider = (SourceProvider) service
				.getSourceProvider(SourceProvider.SELECTED_FILTERS);
		
		
		provider.addSourceProviderListener(new ISourceProviderListener() {

			@SuppressWarnings("rawtypes")
			@Override
			public void sourceChanged(int sourcePriority, String sourceName,
					Object sourceValue) {
				if (currentPanel == null)
					return;
				
				
				if (sourceName.equals(SourceProvider.SELECTED_FILTERS)) {
					QueryDropType dropType = (QueryDropType)provider.getCurrentState().get(SourceProvider.QUERY_DROP_TYPE);
					IStructuredSelection selection = (IStructuredSelection) sourceValue;
					boolean fireEvent = false;
					
					for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
						Object object = (Object) iterator.next();
						DropItem[] items = getDropItemFactory().createDropItem(object, current.getType(), dropType);
						if (items == null ) continue;
						for (int i = 0; i < items.length; i ++){
							if (items[i] != null){
								currentPanel.addItem(items[i]);
								fireEvent = true;
							}
						}
					}
					if (fireEvent) {
						fireQueryModifiedListeners();
					}
				}

			}

			@SuppressWarnings("rawtypes")
			@Override
			public void sourceChanged(int sourcePriority, Map sourceValuesByName) {

			}
		});
	}

	
	
	
	/**
	 * @return the drop item factory for dropping items into the query
	 */
	public DropItemFactory getDropItemFactory(){
		return DropItemFactory.INSTANCE;
	}
	
	/**
	 * Sets the query and updates the
	 * query definition panel as required 
	 * @param query
	 */
	public void setQuery(Query query){
		if (current != null){
			currentPanel.saveItems();
			current = null;	//necessary so that clean doesn't fire events that cause current to get updated
			currentPanel.clear();
		}
		
		current = query;
		if (query != null){
			currentPanel = definitionComposites.get(current.getType());
			currentPanel.init();
		}else{
			currentPanel = null;
		}
		showCurrentPanel();
	}
	
	private void showCurrentPanel(){
		if (currentPanel == null){
			((StackLayout)stackComp.getLayout()).topControl = emptyComp;
		}else{
			((StackLayout)stackComp.getLayout()).topControl = currentPanel;
		}
		stackComp.layout();
		if (currentPanel != null){
			currentPanel.visible();
		}
	}
	
	public Query getQuery(){
		return this.current;
	}
	
	@Override
	public void setFocus() {
		stackComp.setFocus();
	}
		
	public void fireQueryModifiedListeners(){
		QueryEventManager.getInstance().fireQueryChangedListeners(current);
	}
	
}
