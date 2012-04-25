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
package org.wcs.smart.query.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.model.WaypointQuery;
import org.wcs.smart.query.parser.internal.PatrolFilter.PatrolFilterOption;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.query.ui.formulaDnd.DropTargetPanel;
import org.wcs.smart.query.ui.queyfilter.QueryFilterContentProvider;

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
	public static final String ID = "org.wcs.smart.query.ui.QueryDefView";
	
	private WaypointQuery current = null;	
	private DropTargetPanel dropTarget;

	
	
	/* listener to update query definition when window changes */
	private IPartListener2 editorListener = new IPartListener2() {
		
		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			if (partRef.getId().equals(QueryResultsEditor.ID) ){
				IWorkbenchPart part = partRef.getPart(false);
				if (part instanceof QueryResultsEditor){
					setQuery(((QueryResultsEditor)part).getQuery());
					dropTarget.validate();
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
			if (partRef.getPage().findEditors(null, QueryResultsEditor.ID, IWorkbenchPage.MATCH_ID).length == 0){
				setQuery(null);	
			}
		}
		
		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			
		}
	};;
	
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
		if (dropTarget != null){
			dropTarget.dispose();
		}
	}
	
	/**
	 * Clears the current query
	 */
	public void clearQuery(){
		dropTarget.clear();
		QueryEventManager.getInstance().fireQueryChangedListeners(current);
	}

	/**
	 * @return the drop item factory for dropping items into the query
	 */
	public DropItemFactory getDropItemFactory(){
		return DropItemFactory.INSTANCE;
	}
	
	/**
	 * Runs the current query
	 */
	public void runQuery(){
		dropTarget.validate();
		QueryEventManager.getInstance().fireQueryRunListeners(current);
	}
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		
		main.setLayout(layout);
		main.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		// create drop area
		createDragAndDropArea(main);

		// add a listener for when items are added to the query
		ISourceProviderService service = (ISourceProviderService)getSite().getService(ISourceProviderService.class);
		SourceProvider provider = (SourceProvider) service.getSourceProvider(SourceProvider.SELECTED_FILTERS);
		provider.addSourceProviderListener(new ISourceProviderListener() {
			
			@SuppressWarnings("rawtypes")
			@Override
			public void sourceChanged(int sourcePriority, String sourceName,
					Object sourceValue) {
				if (sourceName == SourceProvider.SELECTED_FILTERS){
					IStructuredSelection selection = (IStructuredSelection)sourceValue;
					boolean fireEvent = false;
					for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
						Object type = (Object) iterator.next();
						if (type instanceof Category){
							DropItem it = getDropItemFactory().createCategoryDropItem((Category) type);
							dropTarget.addElement(it);
							fireEvent = true;
						}else if (type instanceof CategoryAttribute){
							DropItem it = getDropItemFactory().createAttributeDropItem((CategoryAttribute) type);
							if (it != null){
								dropTarget.addElement(it);
								fireEvent = true;
							}
						}else if (type instanceof PatrolFilterOption){
							DropItem it = getDropItemFactory().createPatrolDropItem((PatrolFilterOption)type);
							if (it != null){
								dropTarget.addElement(it);
								fireEvent = true;
							}
						}else if (type instanceof Attribute){
							DropItem it = getDropItemFactory().createAttributeDropItem((Attribute)type);
							if (it != null){
								dropTarget.addElement(it);
								fireEvent = true;
							}
						}else if (type instanceof QueryFilterContentProvider.OtherItems){
							DropItem[] its = getDropItemFactory().createOtherDropItem((QueryFilterContentProvider.OtherItems)type);
							if (its != null){
								for (int i = 0; i < its.length; i ++){
									dropTarget.addElement(its[i]);
									fireEvent = true;
								}
							}
						}
						
					}
					if (fireEvent){
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
	
	
	public void setQuery(WaypointQuery query){
		if (current != null){
			ArrayList<DropItem> items = new ArrayList<DropItem>();
			items.addAll(dropTarget.getItems());
			current.setDropItems(items);
			
			dropTarget.clear();
		}
		
		current = query;
		if (query != null){
			dropTarget.addElements(current.getDropItems());
		}else{
			dropTarget.clear();
		}
	}
	
	public WaypointQuery getQuery(){
		return this.current;
	}
	
	@Override
	public void setFocus() {
	}
	
	
	/**
	 * Creates the drag and drop are panel
	 * @param parent
	 * 
	 * @return
	 */
	private void createDragAndDropArea(Composite parent){
		SourceProvider provider = (SourceProvider) ((ISourceProviderService)getSite().getService(ISourceProviderService.class)).getSourceProvider(SourceProvider.QUERY_VALID);
		
		dropTarget = new DropTargetPanel(provider, this);
		dropTarget.createComposite(parent);
		
	}
	
	public void fireQueryModifiedListeners(){
		QueryEventManager.getInstance().fireQueryChangedListeners(current);
	}
	
}
