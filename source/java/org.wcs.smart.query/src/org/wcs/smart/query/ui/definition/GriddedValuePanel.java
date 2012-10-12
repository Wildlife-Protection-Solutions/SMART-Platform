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
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.ListDropTargetPanel;
import org.wcs.smart.ui.ProjectionLabelProvider;

/**
 * Query definition panel for gridded queries.
 * Collects the grid information and the 
 * value to compute.
 * 
 * @author Jeff
 *
 */
public class GriddedValuePanel {

	private ComboViewer lstProjections;
	private Text txtGridSize;
	private ListDropTargetPanel lstValues;
	private boolean isInitializing = false;
	private Label lblUnits;
	private QueryDefView parentView;
	
	private Job loadProjections = new Job("load projections"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			final List<Projection> projs = new ArrayList<Projection>();
			try{
				projs.addAll(HibernateManager.getCaProjectinList(s));
			}finally{
				try{
					s.getTransaction().commit();
				}catch (Exception ex){
					
				}
				s.close();
			}
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					if (!lstProjections.getControl().isDisposed()){
						lstProjections.setInput(projs.toArray(new Projection[projs.size()]));
					}
				}});
			return Status.OK_STATUS;
		}
		
		
	};
	/**
	 * Clears values
	 */
	public void clear() {
		lstValues.clear();		
	}

	/**
	 * @return the value part of the gridded query
	 */
	public String getQueryString() {
		return  lstValues.getQueryString();
	}

	/**
	 * Initializes the panel with the query values
	 * @param query
	 */
	public void init(GriddedQuery query) {
		isInitializing = true;
		lstValues.addElements(query.getValueDropItems());
		txtGridSize.setText(Double.toString(query.getGridSize()));
		
		try {
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
			if (query.getCoordinateReferenceSystem() != null) {
				
				boolean found = false;
				//search for projection
				for (Projection p : ps){
					if (CRS.equalsIgnoreMetadata(p.getCrs(),
							query.getCoordinateReferenceSystem())) {
						defaultP = p;
						found = true;
						break;
					}
				}
				if (!found) {
					//projection not in default list; add custom
					//projection for this query
					Projection p = new Projection();
					p.setCrs(query.getCoordinateReferenceSystem());
					defaultP = p;
					ps.add(p);
				}

			}
			lstProjections.setInput(ps.toArray(new Projection[ps.size()]));
			lstProjections.setSelection(new StructuredSelection(defaultP));
			lstProjections.refresh();
		} catch (Exception ex) {
			QueryPlugIn.displayLog("Error parsing projection information. "
					+ ex.getMessage(), ex);
		}
		isInitializing = false;
	}

	/**
	 * Saves drop items to query
	 * @param query
	 */
	public void saveDropItems(GriddedQuery query) {
		ArrayList<DropItem> items = new ArrayList<DropItem>();
		items.addAll(lstValues.getItems());
		query.setValueDropItems(items);	
	}

	/**
	 * Adds drop item to values
	 * @param item
	 */
	public void addItem(DropItem item) {
		lstValues.addElement(item);
	}

	public Composite createComposite(Composite parent, QueryDefView parentView) {
		this.parentView  = parentView;
		SashForm main = new SashForm(parent, SWT.HORIZONTAL );
		
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
		lblGridDef.setImage(JFaceResources.getImageRegistry().get(QueryPlugIn.GRID_ICON));
		
		Label lblDef = new Label(leftInner, SWT.NONE);
		lblDef.setText("Grid Definition");
		lblDef.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblDef.setToolTipText("Define the grid here.");
		
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
		
		Label lblValues = new Label(rightInner, SWT.NONE);
		lblValues.setImage(JFaceResources.getImageRegistry().get(QueryPlugIn.VALUE_ICON));
		
		lblValues = new Label(rightInner, SWT.NONE);
		lblValues.setText("Grid Value");
		lblValues.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblValues.setToolTipText("Add values to compute here from the 'Value Options' section of the Query Filters tree.");
		
		lstValues = new ListDropTargetPanel(parentView, false);
		Composite comp = lstValues.createComposite(right);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return main;
	}

	private void createOrigin(Composite parent){
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Grid Origin:");
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		lbl = new Label(parent, SWT.NONE);
		lbl.setText("(0, 0)");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2,1));
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
	}
	private void createGridSize(Composite parent){
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Grid Size:");
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtGridSize = new Text(parent, SWT.BORDER);
		txtGridSize.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)txtGridSize.getLayoutData()).widthHint = 150;
		
		txtGridSize.setTextLimit(6);
		txtGridSize.setText("1");
		txtGridSize.addListener(SWT.Modify, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (isInitializing) return;
				parentView.validate();
				parentView.fireQueryModifiedListeners();
			}
		});
		
		
		lblUnits = new Label(parent, SWT.NONE);
		lblUnits.setText("unknown");
		lblUnits.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		
	}
	private void createProjection(Composite parent){
		
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Projection:");
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		lstProjections = new ComboViewer(parent,SWT.READ_ONLY | SWT.DROP_DOWN);
		lstProjections.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2,1));
		lstProjections.setLabelProvider(ProjectionLabelProvider.getInstance());
		lstProjections.setContentProvider(ArrayContentProvider.getInstance());
		lstProjections.setInput(new String[]{"Loading"});
		lstProjections.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!lstProjections.getSelection().isEmpty()){
					Object o = ((IStructuredSelection)lstProjections.getSelection()).getFirstElement();
					if (o instanceof Projection){
						try{
							//assume units of all axis are the same
							Unit<?> units = ((Projection)o).getCrs().getCoordinateSystem().getAxis(0).getUnit();
							lblUnits.setText(units.toString());
						}catch (Exception ex){
							lblUnits.setText("unknown");	
						}
						
					}else{
						lblUnits.setText("unknown");
					}
					
					if (isInitializing) return;
					parentView.validate();
					parentView.fireQueryModifiedListeners();
				}
				
			}
		});
		loadProjections.schedule();
	}
	
	
	/**
	 * 
	 * @return the grid size
	 */
	protected double getGridSize(){
		return Double.parseDouble( txtGridSize.getText() );
	}
	
	/**
	 * 
	 * @return the crs 
	 */
	protected CoordinateReferenceSystem getCrs() throws Exception{
		if (lstProjections.getSelection().isEmpty()){
			return null;
		}
		Object o = ((IStructuredSelection)lstProjections.getSelection()).getFirstElement();
		if (o instanceof Projection){
			return ((Projection)o).getCrs();
		}
		return null;
	}
}
