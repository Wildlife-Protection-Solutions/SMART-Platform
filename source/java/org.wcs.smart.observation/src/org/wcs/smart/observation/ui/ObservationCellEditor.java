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
package org.wcs.smart.observation.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.ui.input.ObservationWizard;
import org.wcs.smart.observation.ui.input.ObservationWizardDialog;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.TreeDropDown;

/**
 * Editor for editing the patrol observation table cell.
 * <p>Opens the patrol observation entry wizard.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ObservationCellEditor extends DialogCellEditor {

	private Text txtFilter;	//text box for typing initial entry
	private TreeDropDown treeDropDown;	//data model drop down
	private Button button;	//dialog-open button
	
	private Waypoint wp;	//waypoint being modified
	private ObservationWizardDialog dialog;

	private boolean fireModify = true;	//if txtFilter change events to be fired
	private boolean isEditable = false;	//if txtFilter is editable
	private boolean dialogOpen = false;	//if dialog is being opend
	
	private Listener focusListener;		//focus listener for determining when cell looses focus
	private Display focusDisplay;
	
	private List<Category> currentSelection = null;	//current catetory selected from tree drop down
	private List<Employee> observers;
	
	/**
	 * Job for loading data model for tree drop down
	 */
	private Job loadDataModel = new Job(Messages.ObservationCellEditor_LoadDataModel_JobName) {

		@Override
		public IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try {
				final DataModel dm = HibernateManager.loadDataModel(
						SmartDB.getCurrentConservationArea(), s);
				for (Category c : dm.getActiveCategories()) {
					visitCategory(c);
				}

				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (treeDropDown != null && !treeDropDown.getTreeViewer().getTree().isDisposed()){
							treeDropDown.getTreeViewer().setInput(dm);
							treeDropDown.setText(txtFilter.getText());
						}
					}
				});

			} finally {
				if (s.getTransaction().isActive()){
					s.getTransaction().rollback();
				}
				s.close();
			}
			return Status.OK_STATUS;
		}
		
		private void visitCategory(Category c){
			c.getFullCategoryName();
			for (Category child : c.getActiveChildren()){
				visitCategory(child);
			}
		}
	};
	
	/**
	 * Creates a new observation cell editor
	 * @param parent
	 */
	public ObservationCellEditor(Composite parent) {
		super(parent);
		super.addListener(new ICellEditorListener() {
			@Override
			public void editorValueChanged(boolean oldValidState, boolean newValidState) {
			}
			
			@Override
			public void cancelEditor() {
				//ensure tree is hidden
				if (treeDropDown != null && treeDropDown.isVisible()){
					treeDropDown.hide();
				}
				
			}
			
			@Override
			public void applyEditorValue() {
				//ensure tree is hiden
				if (treeDropDown != null && treeDropDown.isVisible()){
					treeDropDown.hide();
				}
			}
		});
		
		//initialize focus listener
		focusListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (!(event.widget == ObservationCellEditor.this.txtFilter || event.widget == ObservationCellEditor.this.button ||
						(ObservationCellEditor.this.treeDropDown != null && (event.widget == ObservationCellEditor.this.treeDropDown.getTreeViewer().getTree() || event.widget == ObservationCellEditor.this.treeDropDown.getTreeViewer().getControl().getParent()) ) ||
						dialogOpen
						)){
					fireCancelEditor();
				}
				
			}
		};		
	}
	
	/**
	 * Sets available observers
	 * 
	 * @param observers
	 */
	public void setObservers(List<Employee> observers){
		this.observers = observers;
	}

	/**
	 * @see org.eclipse.jface.viewers.CellEditor#activate()
	 */
	@Override
	public void activate(){
		currentSelection = null;
		super.activate();
		if (focusDisplay == null){
			focusDisplay = getControl().getShell().getDisplay();
		}
		focusDisplay.addFilter(SWT.FocusIn, focusListener);
	}
	
	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#deactivate()
	 */
	@Override
	public void deactivate(){
		super.deactivate();
		if (focusListener != null && focusDisplay!= null){
			focusDisplay.removeFilter(SWT.FocusIn, focusListener);
		}
	}
	
	/**
	 * @see org.eclipse.jface.viewers.CellEditor#dispose()
	 */
	@Override
	public void dispose(){
		if (focusListener != null && focusDisplay != null){
			focusDisplay.removeFilter(SWT.FocusIn, focusListener);
		}
		if (treeDropDown != null){
			treeDropDown.dispose();
		}
		super.dispose();
		
	}
	
	
	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#createButton(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Button createButton(final Composite parent) {
		button = super.createButton(parent);
		button.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				if (e.detail != SWT.TRAVERSE_TAB_PREVIOUS || !isEditable) {
					getControl().notifyListeners(SWT.Traverse, e);
				}
			}
		});
		return button;
	}

	/**
	 * Creates the controls used to show the value of this cell editor.
	 *
	 * @param cell
	 *            the control for this cell editor
	 * @return the underlying control
	 */
	protected Control createContents(final Composite cell) {
		this.txtFilter = new Text(cell, SWT.NONE);
		txtFilter.setFont(cell.getFont());
		txtFilter.setBackground(cell.getBackground());

		txtFilter.addListener(SWT.Traverse, new Listener() {
			@Override
			public void handleEvent(Event event) {
				
				if (event.detail != SWT.TRAVERSE_TAB_NEXT) {
					getControl().notifyListeners(SWT.Traverse, event);
				}
				if (event.detail == SWT.TRAVERSE_ESCAPE){
					fireCancelEditor();
				}
			}
		});

		txtFilter.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN){
					if (treeDropDown != null && treeDropDown.isVisible()){
						treeDropDown.setFocus();
					}
					e.doit = false;
				}else if (e.keyCode == SWT.CR &&
						treeDropDown != null && 
						treeDropDown.isVisible() && 
						!treeDropDown.getSelection().isEmpty()){
					openWithSelection(treeDropDown.getSelection());
				}
			}
		});
		
		txtFilter.addListener(SWT.Modify, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (!fireModify)
					return;
				if (treeDropDown == null){
					createTree();
				}
				if (treeDropDown != null && !treeDropDown.isVisible()) {
					//compute width
					Rectangle r = cell.getBounds();
					Point pnt = cell.getParent().toDisplay(r.x, r.y);
					Rectangle shellr = cell.getShell().getBounds();
					int	width = (shellr.width + shellr.x) - pnt.x - 20;
					if (width < r.width){
						width = r.width;
					}
					
					//show
					treeDropDown.positionAndShow(cell, width, null, new ISelectionListener() {

						@Override
						public void selectionChanged(IWorkbenchPart part,
								ISelection selection) {
							openWithSelection((IStructuredSelection)selection);
						}
					});
				}
				if (treeDropDown != null){
					treeDropDown.setText(txtFilter.getText());
				}
			}
		});
		return txtFilter;
	}

	/*
	 * opens the dialog with the current selection from the
	 * tree drop down
	 */
	private void openWithSelection(IStructuredSelection selection){
		currentSelection = null;
		if (selection != null && !selection.isEmpty()){
			currentSelection = new ArrayList<Category>();
			for (Iterator<?> iterator = ((IStructuredSelection)selection).iterator(); iterator.hasNext();) {
				Category category = (Category) iterator.next();
				currentSelection.add(category);
				
			}
		}
		button.notifyListeners(SWT.Selection, new Event());
		currentSelection = null;
	}	
	
	/**
	 * Creates the tree drop down
	 * @return
	 */
	private void createTree() {
		if (treeDropDown == null || treeDropDown.getTreeViewer().getTree().isDisposed()) {
			treeDropDown = new TreeDropDown(super.getControl().getShell());
			
			treeDropDown.getTreeViewer().setContentProvider(new DataModelContentProvider(true, true));
			treeDropDown.getTreeViewer().setLabelProvider(new DataModelLabelProvider());
			treeDropDown.getTreeViewer().getTree().addTraverseListener(new TraverseListener() {
				
				@Override
				public void keyTraversed(TraverseEvent e) {
					if (e.detail == SWT.TRAVERSE_ESCAPE){
						treeDropDown.hide();
						e.doit = false;
						doSetFocus();
					}
					
				}
			});
			loadDataModel.schedule();
			
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#updateContents(java.lang.Object)
	 */
	@Override
	protected void updateContents(Object value) {
		String text = Messages.ObservationCellEditor_NoObservations_Label;
		if (value != null && value instanceof Waypoint
				&& ((Waypoint) value).getObservations() != null
				&& ((Waypoint) value).getObservations().size() > 0) {
			text = ((Waypoint)value).getObservationsAsString();
		}
		fireModify = false;
		txtFilter.setText(text);
		fireModify = true;
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#doGetValue()
	 */
	@Override
	protected Object doGetValue() {
		return wp;
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#doSetFocus()
	 */
	@Override
	protected void doSetFocus() {
		if (isEditable) {
			txtFilter.setFocus();
			txtFilter.selectAll();
		} else {
			super.doSetFocus();
		}
	}

	/**
	 * Updates the size of the widget
	 */
	@Override
	public LayoutData getLayoutData() {
		LayoutData data = super.getLayoutData();
		data.minimumHeight = txtFilter.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		return data;
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#doSetValue(java.lang.Object)
	 */
	@Override
	protected void doSetValue(Object value) {
		if (value instanceof Waypoint) {
			this.wp = (Waypoint) value;
			this.isEditable = this.wp.getObservations() == null || this.wp.getObservations().size() == 0;
			txtFilter.setEditable(isEditable);
		}
		super.doSetValue(value);
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#openDialogBox(org.eclipse.swt.widgets.Control)
	 */
	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		dialogOpen = true;
		if (treeDropDown != null && treeDropDown.isVisible()){
			treeDropDown.hide();
		}
		Waypoint wp = (Waypoint) super.getValue();

		final ObservationWizard wizard = new ObservationWizard(wp, this.observers);
		if (currentSelection != null){
			wizard.setInitialCategories(currentSelection);
		}
		dialog = new ObservationWizardDialog(getControl().getShell(), wizard);
		wizard.setWizardDialog(dialog);
		try{
			if (dialog.open() == Window.CANCEL) {
				return null;
			}
			return wp;
		}finally{
			dialogOpen = false;
		}
		
	}

}
