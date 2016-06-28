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
package org.wcs.smart.incident.ui.newwizard;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Incident location composite.
 * @author Emily
 *
 */
public class LocationComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.location"; //$NON-NLS-1$

	private Projection currentProjection;
	private Projection dbProjection;

	private ComboViewer cmbProjection;
	private Text txtX;
	private Text txtY;
	
	@Override
	public String validate() {
		if (txtX.getText().trim().isEmpty()){
			return Messages.LocationComposite_XRequired;
		}
		if (txtY.getText().trim().isEmpty()){
			return Messages.LocationComposite_YRequired;
		}
		try{
			Double.parseDouble(txtX.getText());
		}catch (Exception ex){
			return Messages.LocationComposite_XNumberRequired;
		}
		try{
			Double.parseDouble(txtY.getText());
		}catch (Exception ex){
			return Messages.LocationComposite_YNumberRequired;
		}
		
		double x = Double.parseDouble(txtX.getText());
		double y = Double.parseDouble(txtY.getText());
		Projection proj = (Projection) ((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
		try{
			ReprojectUtils.reproject(x, y, ReprojectUtils.stringToCrs(proj.getDefinition()), GeometryUtils.SMART_CRS);
		}catch (Exception ex){
			return Messages.LocationComposite_CoordinatesNotValid + ex.getMessage();
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout(2, false));
		
		Label l = new Label(item, SWT.NONE);
		l.setText(Messages.LocationComposite_ProjectionLabel);
		cmbProjection = new ComboViewer(item, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbProjection.setContentProvider(ArrayContentProvider.getInstance());
		cmbProjection.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof Projection){
					return ((Projection)element).getName();
				}
				return super.getText(element);
			}
		});
		cmbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbProjection.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				transformInput();
				currentProjection = (Projection)((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
				fireChange(new Event());	
			}
		});
		
		l = new Label(item, SWT.NONE);
		l.setText(Messages.LocationComposite_xLabel);
		txtX = new Text(item, SWT.BORDER);
		txtX.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(item, SWT.NONE);
		l.setText(Messages.LocationComposite_yLabel);
		txtY = new Text(item, SWT.BORDER);
		txtY.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Link lnkMap  = new Link(item, SWT.NONE);
		lnkMap.setText("<a>" + Messages.LocationComposite_SelectOnMap + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnkMap.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectOnMap();
			}
		});
		lnkMap.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false,2,1));
		return item;
	}

	private void transformInput() {
		Projection target = (Projection)((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
		transformInput(currentProjection, target);
	}

	private void transformInput(final Projection source, final Projection target) {
		try {
			//reproject
			Point point = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(Double.parseDouble(txtX.getText()),Double.parseDouble(txtY.getText())));
			Point p = (Point) JTS.transform(point, CRS.findMathTransform(
					ReprojectUtils.stringToCrs(source.getDefinition()),
					ReprojectUtils.stringToCrs(target.getDefinition())));

			txtX.setText(String.valueOf(p.getX()));
			txtY.setText(String.valueOf(p.getY()));
		} catch (Exception ex) {
			//nothing
		}
	}
	
	@Override
	public void updateIncident(Waypoint incident) {
		try{
			Coordinate z = getPoint();
			incident.setX(z.x);
			incident.setY(z.y);
		}catch (Exception ex){
			//this error should be caught in the validate function
			IncidentPlugIn.log(Messages.LocationComposite_Error1, ex);
		}
	}

	private Coordinate getPoint() throws Exception{
		double x = Double.parseDouble(txtX.getText());
		double y = Double.parseDouble(txtY.getText());
		Projection proj = (Projection) ((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
		Coordinate z = ReprojectUtils.reproject(x, y, 
				ReprojectUtils.stringToCrs(proj.getDefinition()), GeometryUtils.SMART_CRS);
		return z;
	}
	
	@Override
	public void initFields(Waypoint incident, Session session) {
		initializing = true;
		try{
			List<Projection> projs = HibernateManager.getCaProjectionList(session);
			for(Projection p : projs){
				try{
					if (CRS.equalsIgnoreMetadata(ReprojectUtils.stringToCrs(p.getDefinition()), GeometryUtils.SMART_CRS)){
						dbProjection = p;
						break;
					}
				}catch (Exception ex){
					IncidentPlugIn.log(Messages.LocationComposite_Error2, ex);
				}
			}
			if (dbProjection == null){
				dbProjection = new Projection();
				dbProjection.setDefinition(GeometryUtils.SMART_CRS.toWKT());
				projs.add(dbProjection);
			}
			cmbProjection.setInput(projs);
			if (incident.getX() != null){
				txtX.setText(String.valueOf(incident.getX()));
			}
			if (incident.getY() != null){
				txtY.setText(String.valueOf(incident.getY()));
			}
		
			currentProjection = dbProjection;
			Projection viewPrj = ObservationHibernateManager.getCurrentViewProjection(session);
			if (viewPrj == null) {
				viewPrj = currentProjection;
			}
			cmbProjection.setSelection(new StructuredSelection(viewPrj)); //transformation will be done by change listener
		}finally{
			initializing = false;
		}
	}
	
	@Override
	public String getName() {
		return Messages.LocationComposite_Name;
	}

	@Override
	public String getDescription() {
		return Messages.LocationComposite_Description;
	}

	
	private void selectOnMap(){
		MapDialog md = new MapDialog(txtX.getShell());
		
		try{
			Coordinate z = getPoint();
			if (z != null){
				md.setInitPoint(z.x, z.y);
			}
		}catch (Exception ex){}
		
		if (md.open() == MapDialog.OK){
			if (md.getPoint() != null){
				txtX.setText(String.valueOf(md.getPoint().getX()));
				txtY.setText(String.valueOf(md.getPoint().getY()));
				transformInput(dbProjection, currentProjection);
			}
		}
	}
	
	
}
