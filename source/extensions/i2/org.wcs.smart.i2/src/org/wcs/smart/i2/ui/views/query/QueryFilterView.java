package org.wcs.smart.i2.ui.views.query;

import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ui.editors.query.IntelQueryEditor;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.util.E3Utils;

public class QueryFilterView {
	public static final String ID = "org.wcs.smart.i2.ui.view.query.filter"; //$NON-NLS-1$

	@Inject
	private IEclipseContext context;
	
	private TreeViewer filterTree = null;
	private Job refreshJob;
	public QueryFilterView() {
		super();
	}

	@PostConstruct
	public void createPartControl(final Composite parent) {
		
		ConservationAreaManager.getInstance().addAreaChangeListener(new IAreaModifiedListener() {
			
			@Override
			public void areasUpdated(AreaType type) {
				// TODO Auto-generated method stub
				
			}
		});
		
		parent.setLayout(new GridLayout());
		
		ToolBar tb = new ToolBar(parent, SWT.FLAT);
		ToolItem refreshItem = new ToolItem(tb, SWT.PUSH);
		refreshItem.setToolTipText("refresh tree");
		refreshItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_REFRESH));
		refreshItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshView();
			}
		});
		filterTree = new TreeViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		filterTree.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filterTree.setLabelProvider(new FilterTreeLabelProvider());
		filterTree.setContentProvider(new FilterTreeContentProvider());
		
		filterTree.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				//TODO: clean this up
				IntelQueryEditor addTo = null;
				for (MPart part : context.get(EPartService.class).getParts()){
					if (part.isVisible()){
						Object item = E3Utils.getSourceObject(part);
						if (item instanceof IntelQueryEditor){
							addTo = (IntelQueryEditor) item;
							break;
						}
					}
				}
				
				IStructuredSelection selection = (IStructuredSelection) filterTree.getSelection();
				for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
					Object element = (Object) iterator.next();
					if (element instanceof FilterItem){
						DropItem[] di = ((FilterItem) element).asDropItem();
						if (di == null) continue;
						addTo.addDropItems(di);
						
					}
					
				}
			}
		});
		refreshJob = new LoadFilterOptions(filterTree);
	}

	public void refreshView(){	
		filterTree.setInput(null);
		refreshJob.schedule();
	}

	
	
	@Focus
	public void setFocus() {
//		lstInProgress.getControl().setFocus();
	}

	@PreDestroy
	public void dispose() {
	}
	
	public static class QueryFilterViewWrapper extends DIViewPart<QueryFilterView>{
		public QueryFilterViewWrapper() {
			super(QueryFilterView.class);
		}
	}

	
}