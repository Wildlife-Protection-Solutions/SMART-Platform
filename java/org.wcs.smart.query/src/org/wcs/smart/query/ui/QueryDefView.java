package org.wcs.smart.query.ui;


import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.model.WaypointQuery;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropTargetPanel;

public class QueryDefView extends ViewPart {

	public static final String ID = "org.wcs.smart.query.ui.QueryDefView";
	
	
	private WaypointQuery current = null;
	private Text txtQueryDefinition;
	
	private IPartListener2 editorListener = new IPartListener2() {
		
		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			
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
		}
		
		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			
			if (partRef.getId().equals(QueryResultsEditor.ID) ){
				IWorkbenchPart part = partRef.getPart(false);
				if (part instanceof QueryResultsEditor){
					setQuery(((QueryResultsEditor)part).getQuery());
				}
				
				
			}
			
		}
	};;
	
	public QueryDefView() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(editorListener);
	}
	
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

	@Override
	public void createPartControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		txtQueryDefinition = new Text(main, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		txtQueryDefinition.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Button btnRefresh = new Button(main, SWT.PUSH);
		btnRefresh.setText("Run Query");
		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				current.setQueryFilter(txtQueryDefinition.getText());
				QueryEventManager.getInstance().fireQueryChangedListeners(current);
			}
		});
		
		createDragAndDropArea(main);
	}
	
	
	public void setQuery(WaypointQuery query){
		current = query;
		txtQueryDefinition.setText(query.getQueryFilter());
	}
	
	
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	
//	private Composite dropTarget = null;
//	private DropTarget dtarget  = null;
//	private ArrayList<Composite> order = new ArrayList<Composite>();
	
	private DropTargetPanel dropTarget;
	
	
	private Composite createDragAndDropArea(Composite parent){
		dropTarget = new DropTargetPanel();
		dropTarget.createComposite(parent);
		
		DropItem di1 = new DropItem(dropTarget.getComposite(), "Part 1");
		DropItem di2 = new DropItem(dropTarget.getComposite(), "Part 2");
		DropItem di3 = new DropItem(dropTarget.getComposite(), "Part 3");
		DropItem di4 = new DropItem(dropTarget.getComposite(), "A really long part");
		DropItem di5 = new DropItem(dropTarget.getComposite(), "Another really long part");
		DropItem di6 = new DropItem(dropTarget.getComposite(), "Yet another really really really really really long part");
		
		dropTarget.addElement(di1);
		dropTarget.addElement(di2);
		dropTarget.addElement(di3);
		dropTarget.addElement(di4);
		dropTarget.addElement(di5);
		dropTarget.addElement(di6);
		
		return dropTarget.getComposite();
		
	}
	
}
