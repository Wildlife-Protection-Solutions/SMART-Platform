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

import java.util.ArrayList;
import java.util.List;

import javax.measure.unit.Unit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IProjectionListener;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.ui.ProjectionLabelProvider;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Abstract grid definition panel.  Basic definition panel
 * for a gridded query.  Consists of a grid definition area and
 * a value drop area.
 * 
 * @author Emily
 *
 */
public abstract class AbstractGridDefinitionPanel implements IDefinitionPanel {

	private Composite main;
	private Label lblUnits;
	
	protected ComboViewer lstProjections;
	protected Text txtGridSize;
	protected ListDefinitionPanel lstValues;
	protected boolean isInitializing = false;
	
	protected QueryProxy currentQuery;
	
	private IProjectionListener projectionListener = new IProjectionListener() {
		@Override
		public void projectionsModified() {
			Job j = new Job("reload projections"){ //$NON-NLS-1$
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					loadProjections.schedule();
					Display.getDefault().asyncExec(new Runnable(){
						@Override
						public void run() {
							selectProjection(getDefaultProjection());
						}});
					
					return Status.OK_STATUS;
				}
			};
			j.setSystem(true);
			j.schedule();
		}
	};
	
	private Job loadProjections = new Job(Messages.GriddedValuePanel_LoadProjsJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			final List<Projection> projs = new ArrayList<Projection>();
			try{
				projs.addAll(HibernateManager.getCaProjectionList(s));
			}finally{
				try{
					s.getTransaction().commit();
				}catch (Exception ex){
					
				}
				s.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (!lstProjections.getControl().isDisposed()){
						lstProjections.setInput(projs.toArray(new Projection[projs.size()]));
					}
				}});
			return Status.OK_STATUS;
		}
		
		
	};
	
	public AbstractGridDefinitionPanel(){
	}
	
	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#getId()
	 */
	@Override
	public abstract String getId();

	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#getGuiName()
	 */
	@Override
	public abstract String getGuiName();

	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#validate()
	 */
	@Override
	public abstract String validate();
	
	
	/**
	 * adds the drop item to the value list
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#addItem(org.wcs.smart.query.ui.model.DropItem)
	 */
	@Override
	public void addItem(DropItem item) {
		lstValues.addItem(item);
	}

	/** removes the drop item from the value list
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#removeItem(org.wcs.smart.query.ui.model.DropItem)
	 */
	@Override
	public void removeItem(DropItem item) {
		lstValues.removeItem(item);

	}

	/**
	 * Re-draw the drop items
	 */
	@Override
	public void redraw(){
		lstValues.redraw();
	}

	
	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Composite createComposite(Composite parent) {
		main = new SashForm(parent, SWT.HORIZONTAL );
		
		Composite left = new Composite(main, SWT.BORDER);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		left.setLayout(gl);
		
		Composite leftInner = new Composite(left, SWT.NONE);
		leftInner.setLayout(new GridLayout(2, false));
		leftInner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblGridDef = new Label(leftInner, SWT.NONE);
		lblGridDef.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.GRID_ICON));
		
		Label lblDef = new Label(leftInner, SWT.NONE);
		lblDef.setText(Messages.GriddedValuePanel_GridDefLabel);
		lblDef.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblDef.setToolTipText(Messages.GriddedValuePanel_GridDefTooltip);
		

		Label lblSep = new Label(leftInner, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblSep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		Composite leftMain = new Composite(left, SWT.NONE);
		leftMain.setLayout(new GridLayout(3, false));
		leftMain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		leftMain.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		createProjection(leftMain);
		createGridSize(leftMain);
		createOrigin(leftMain);
		
		
		Composite right = new Composite(main, SWT.BORDER);
		GridLayout rgl = new GridLayout(2, false);
		rgl = new GridLayout(1, false);
		rgl.marginWidth = 0;
		rgl.marginHeight = 0;
		rgl.verticalSpacing = 0;
		rgl.horizontalSpacing = 0;
		right.setLayout(rgl);

		Composite rightInner = new Composite(right, SWT.NONE);
		gl = new GridLayout(2, false);
		rightInner.setLayout(gl);
		rightInner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblValues = new Label(rightInner, SWT.NONE);
		lblValues.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_ICON));
		
		lblValues = new Label(rightInner, SWT.NONE);
		lblValues.setText(Messages.GriddedValuePanel_GridValueLabel);
		lblValues.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblValues.setToolTipText(Messages.GriddedValuePanel_GridValueTooltip);
		
		lblSep = new Label(rightInner, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblSep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		lstValues = createValueListPanel();
		Composite comp = lstValues.createComposite(right);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		// Add listener for changes to projection list
		ConservationAreaManager.getInstance().addProjectListListener(projectionListener);
		main.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				ConservationAreaManager.getInstance().removeProjectionListnListener(projectionListener);
			}
		});
		return main;
	}

	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#dispose()
	 */
	@Override
	public void dispose() {
		lstValues.dispose();
		main.dispose();

	}

	/**
	 * Saves the value list drop items
	 */
	@Override
	public void saveItems(QueryProxy q) {
		lstValues.saveItems(q);
	}

	/**
	 * initializes the value list drop items
	 */
	@Override
	public void initItems(QueryProxy q) {
		isInitializing = true;
		try{
			this.currentQuery = q;
			lstValues.initItems(q);
		}finally{
			isInitializing = false;
		}
	}

	@Override
	public void clear() {
		lstValues.clear();
		
	}

	@Override
	public void finishDrag(DropItem item) {
		lstValues.finishDrag(item);
	}

	@Override
	public void fireQueryChangedListeners() {
		QueryEventManager.getInstance().fireQueryDefinitionModified(currentQuery.getQuery());
	}

	@Override
	public Composite getDropTargetComposite() {
		return main;
	}

	private void createOrigin(Composite parent){
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(Messages.GriddedValuePanel_GridOriginLabel);
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		lbl = new Label(parent, SWT.NONE);
		lbl.setText("(0, 0)"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2,1));
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
	}
	
	private void createGridSize(Composite parent){
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(Messages.GriddedValuePanel_GridSizeLabel);
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtGridSize = new Text(parent, SWT.BORDER);
		txtGridSize.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)txtGridSize.getLayoutData()).widthHint = 150;
		
		txtGridSize.setTextLimit(6);
		txtGridSize.setText("1"); //$NON-NLS-1$
		txtGridSize.addListener(SWT.Modify, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (isInitializing) return;
				fireQueryChangedListeners();
			}
		});
		
		
		lblUnits = new Label(parent, SWT.NONE);
		lblUnits.setText(Messages.GriddedValuePanel_UnknownUnitsLabel);
		lblUnits.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		
	}
	private void createProjection(Composite parent){
		
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(Messages.GriddedValuePanel_ProjectionsLabel);
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		lstProjections = new ComboViewer(parent,SWT.READ_ONLY | SWT.DROP_DOWN);
		lstProjections.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2,1));
		lstProjections.setLabelProvider(ProjectionLabelProvider.getInstance());
		lstProjections.setContentProvider(ArrayContentProvider.getInstance());
		lstProjections.setInput(new String[]{Messages.GriddedValuePanel_LoadingLabel});
		lstProjections.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!lstProjections.getSelection().isEmpty()){
					Object o = ((IStructuredSelection)lstProjections.getSelection()).getFirstElement();
					if (o instanceof Projection){
						lblUnits.setText(Messages.GriddedValuePanel_UknownProjectionLabel);
						try{
							//assume units of all axis are the same
							Unit<?> units = ReprojectUtils.stringToCrs(((Projection)o).getDefinition()).getCoordinateSystem().getAxis(0).getUnit();
							lblUnits.setText(units.toString());
						}catch (Exception ex){	
						}
					}
					
					if (isInitializing) return;
					fireQueryChangedListeners();
				}
				
			}
		});
		loadProjections.schedule();
	}
	
	
	/**
	 * 
	 * @return the grid size
	 */
	public double getGridSize(){
		return Double.parseDouble( txtGridSize.getText() );
	}
	
	/**
	 * 
	 * @return the crs 
	 */
	public CoordinateReferenceSystem getCrs() {
		if (lstProjections.getSelection().isEmpty()){
			return null;
		}
		Object o = ((IStructuredSelection)lstProjections.getSelection()).getFirstElement();
		if (o instanceof Projection){
			try {
				return ReprojectUtils.stringToCrs(((Projection)o).getDefinition());
			} catch (FactoryException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/*
	 * Selects the given projection
	 */
	protected void selectProjection(CoordinateReferenceSystem initialProjection){
		
		try {
			loadProjections.join();
			Projection[] values = (Projection[]) lstProjections.getInput();
			List<Projection> ps = new ArrayList<Projection>();
			Projection defaultP = null;
			for (int i = 0; i <values.length; i ++){
				if (values[i].getUuid() != null){
					//remove any custom projections
					ps.add(values[i]);
					if (values[i].getIsDefault()){
						defaultP = values[i];
					}
				}
			}
			if (defaultP==null && ps.size() > 0){
				defaultP=ps.get(0);
			}
			if (initialProjection != null ) {
				
				boolean found = false;
				//search for projection
				for (Projection p : ps){
					if (CRS.equalsIgnoreMetadata(ReprojectUtils.stringToCrs(p.getDefinition()), initialProjection)) {
						defaultP = p;
						found = true;
						break;
					}
				}
				if (!found) {
					//projection not in default list; add custom
					//projection for this query
					Projection p = new Projection();
					p.setDefinition(initialProjection.toWKT());
					defaultP = p;
					ps.add(p);
				}

			}
			lstProjections.setInput(ps.toArray(new Projection[ps.size()]));
			if (defaultP != null){
				lstProjections.setSelection(new StructuredSelection(defaultP));
			}
			lstProjections.refresh();
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.GriddedValuePanel_ProjectionParseError
					+ ex.getLocalizedMessage(), ex);
		}
	}
	
	/**
	 * The query part defined by the grid panel
	 */
	@Override
	public abstract String getQueryPart();

	/**
	 * Creates the value list panel
	 * @return
	 */
	public abstract ListDefinitionPanel createValueListPanel();
	
	/**
	 * Get the default projection to select
	 * @return
	 */
	public abstract CoordinateReferenceSystem getDefaultProjection();
}
