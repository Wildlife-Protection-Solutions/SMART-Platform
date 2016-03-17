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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISourceProviderListener;
import org.osgi.service.event.Event;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.QuerySourceProvider;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.util.E3Utils;

/**
 * A view for building query definition.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryDefView  {

	/**
	 * View identifier
	 */
	public static final String ID = "org.wcs.smart.query.parts.definition"; //$NON-NLS-1$
	
	private QueryProxy current = null;	

	private Composite stackComp = null;
	private Composite emptyComp = null;
		
	private QueryDefPanel currentPanel;
	private HashMap<IQueryType, QueryDefPanel> definitionPanels = new HashMap<IQueryType, QueryDefPanel>();
	
	@Inject private MPart part;
	
	/* listener to update query definition when window changes */
	private IPartListener editorListener = new IPartListener() {
		
		@Override
		public void partVisible(MPart part) {
			Object lpart = E3Utils.getSourceObject(part);
			if (lpart instanceof IQueryEditor){
				IQueryEditor qpart = (IQueryEditor)lpart;
				if (qpart.getQueryProxy() != current){
					setQuery(qpart.getQueryProxy());
				}
			}
		}
		
		@Override
		public void partHidden(MPart part) {}
		
		@Override
		public void partDeactivated(MPart part) {}
		
		@Override
		public void partBroughtToTop(MPart part) {}
		
		@Override
		public void partActivated(MPart part) {
			Object lpart = E3Utils.getSourceObject(part);
			if (lpart instanceof IQueryEditor){
				IQueryEditor qpart = (IQueryEditor)lpart;
				qpart.validate();
				if (qpart.getQueryProxy() != current){
					setQuery(qpart.getQueryProxy());
				}
			}
		}
	};

	

	/**
	 * These events are fired when the query needs to be refreshed; perhaps a 
	 * system key was changed etc.  This is not to be fired when the user modifies the
	 * queries.
	 */
	private IQueryListener queryRefreshed = new QueryListenerAdapter() {
		@Override
		public void queryRefreshed(Query query) {
			if (current != null && query.equals(current.getQuery())){
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						refreshQuery();
					}});
				
			}
		}
		
		@Override
		public void queryModified(int eventType, Object object) {
			if (current == null) return;
			if (eventType == IQueryListener.QUERY_DEFINITION_MODIFIED && object instanceof Query && ((Query)object).equals(current.getQuery())){
				validate();
			}
		}
	};


	/**
	 * Creates new query definition view.
	 */
	public QueryDefView() {
			
	}
	
	public MPart getPart(){
		return part;
	}
	
	@Inject
	@Optional
	private void getClosed(@UIEventTopic(UIEvents.UIElement.TOPIC_WIDGET) Event e) {
		if (e.getProperty(UIEvents.EventTags.NEW_VALUE) == null){
			Object element = e.getProperty(UIEvents.EventTags.ELEMENT);
			if (element == null) return;
			if (!(element instanceof MPart)) return;
			
			Object src = E3Utils.getSourceObject((MPart)element); 
			if (src instanceof IQueryEditor){
				if (current == null || ((IQueryEditor)src).getQueryProxy().getQuery().equals(current.getQuery())){
					//closing the current active part
					setQuery(null);
				}
			}
		}
	}
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@PreDestroy
	public void dispose(EPartService pService) {
		if (editorListener != null){
			pService.removePartListener(editorListener);
		}
		for (QueryDefPanel pnl : definitionPanels.values()){
			pnl.dispose();
		}
		QueryEventManager.getInstance().removeListener(queryRefreshed);
	}
	
	/**
	 * Clears the current query
	 */
	public void clearQuery(){
		if (currentPanel != null){
			currentPanel.clear();
		}
		if (current != null){
			QueryEventManager.getInstance().fireQueryDefinitionModified(current.getQuery());
		}
	}


	/**
	 * Validates the current query
	 */
	public void validate(){
		if (currentPanel != null ) { 
			//validate the individual panels
			String error = current.getQueryType().validateQuery(currentPanel.getDefinitionPanels());
			
			//validate the entire query
			current.setValid(error);
			if (error != null){
				getSourceProvider().setQueryValid(false, error);
			}else{
				//update the query
				getSourceProvider().setQueryValid(true, null);
				current.getQueryType().updateQueryDefinition(current.getQuery(), currentPanel.getDefinitionPanels());				
			}
		}
	}
	
	/**
	 * Runs the current query
	 */
	public void runQuery(){
		validate();
		QueryEventManager.getInstance().fireRunQuery(current.getQuery());
		
	}
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@PostConstruct
	public void createPartControl(Composite parent, EPartService pService) {
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		
		stackComp = new Composite(parent, SWT.NONE);
		
		StackLayout layout = new StackLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		stackComp.setLayout(layout);
		stackComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));		
		emptyComp = new Composite(stackComp, SWT.NONE);
		emptyComp.setLayout(new GridLayout(1, false));
		
		((StackLayout)stackComp.getLayout()).topControl = emptyComp;
	
		addSourceListener();
		
		pService.addPartListener(editorListener);
		QueryEventManager.getInstance().addListener(queryRefreshed);
	}
	
	private QuerySourceProvider getSourceProvider(){
		return part.getContext().get(QuerySourceProvider.class);
	}
	
	
	private void addSourceListener() {
	
		
		getSourceProvider().addSourceProviderListener(new ISourceProviderListener() {

			@SuppressWarnings("rawtypes")
			@Override
			public void sourceChanged(int sourcePriority, String sourceName,
					Object sourceValue) {
				if (currentPanel == null)
					return;
				
				if (sourceName.equals(QuerySourceProvider.DEFINITION_ITEMS)) {
					String srcPanelId = (String) getSourceProvider().getCurrentState().get(QuerySourceProvider.DEFINITION_ITEMS_SRC);
					IStructuredSelection selection = (IStructuredSelection) sourceValue;
					
					boolean fireEvent = false;
					for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
						Object object = (Object) iterator.next();
						DropItem[] items = getDropItemFactory().generateDropItem(object, srcPanelId);
						if (items == null ) continue;
						for (int i = 0; i < items.length; i ++){
							if (items[i] != null){
								currentPanel.addItem(items[i], srcPanelId);
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
	public IDropItemFactory getDropItemFactory(){
		if (current != null){
			return current.getQueryType().getDropItemFactory();
		}
		return null;
	}
	
	
	/**
	 * Refreshes the current panel by clearing
	 * the content, re-initializing it, and re-validating it
	 * @param query
	 */
	private void refreshQuery(){
		try{
			currentPanel.clear();
			currentPanel.initItems(current);
			showCurrentPanel();
			validate();
		}catch (Exception ex){
			QueryPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	
	/**
	 * Sets the query and updates the
	 * query definition panel as required 
	 * @param query
	 */
	public void setQuery(QueryProxy query){
		if (current != null){
			currentPanel.saveItems(current);
			current = null;	//necessary so that clean doesn't fire events that cause current to get updated
			currentPanel.clear();
		}
		
		current = query;
		if (query != null){
			currentPanel = definitionPanels.get(query.getQueryType());
			
			if (currentPanel == null){
				currentPanel = new QueryDefPanel(query.getQueryType(), this);
				definitionPanels.put(query.getQueryType(), currentPanel);
			}
			
			if (currentPanel != null){
				current.setQueryDefinitionPanel(currentPanel);
				try{
					currentPanel.initItems(current);
				}catch (Exception ex){
					QueryPlugIn.displayLog(ex.getMessage(), ex);
				}
			}
		}else{
			currentPanel = null;
		}
		
		showCurrentPanel();
		if (currentPanel != null){
			validate();
		}
	}
	
	private void showCurrentPanel(){
		if (currentPanel == null){
			((StackLayout)stackComp.getLayout()).topControl = emptyComp;
		}else{
			Composite pnl = currentPanel.getComposite(stackComp);
			((StackLayout)stackComp.getLayout()).topControl = pnl;
			pnl.layout(true);
		}
		
		
		stackComp.layout();
		if (currentPanel != null){
			currentPanel.madeVisible();
		}
		
	}
	
	public Query getQuery(){
		return this.current.getQuery();
	}

	public QueryProxy getQueryProxy(){
		return this.current;
	}
	
	@Focus
	public void setFocus() {
		stackComp.setFocus();
	}
		
	public void fireQueryModifiedListeners(){
		QueryEventManager.getInstance().fireQueryDefinitionModified(current.getQuery());
	}
	
	
	
	public static class QueryDefViewWrapper extends DIViewPart<QueryDefView>{
		public QueryDefViewWrapper(){
			super(QueryDefView.class);
		}
	}
}
