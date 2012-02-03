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
package org.wcs.smart.patrol.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Patrol;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class StationComposite extends PatrolItemComposite{

	private ComboViewer stationList;

	/*
	 * Station/Team label provider
	 */
	private LabelProvider lblProvider = new LabelProvider(){
		public String getText(Object element) {
			if (element instanceof Station){
				return ((Station)element).getName();
			}
			return super.getText(element);
		}
	};
	
	/**
	 * 
	 */
	public StationComposite() {

	}

	public Composite createComponent(Composite parent, int style) {

		Composite center = new Composite(parent, SWT.NONE);
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		center.setLayout(new GridLayout(2, false));
		
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Station: ");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		stationList = new ComboViewer(center, SWT.DROP_DOWN | SWT.READ_ONLY);
		stationList.setContentProvider(ArrayContentProvider.getInstance());
		stationList.setLabelProvider(lblProvider);
		stationList.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		stationList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();	
			}
		});
		return center;
	}

	public void setValues(Patrol p, Session session) {
		List<? extends Object> stations = HibernateManager.getActiveStations(p.getConservationArea(), session);

		String none = "(None)";
		List<Object> stns = new ArrayList<Object>();
		stns.add(none);
		if (stations != null){
			stns.addAll(stations);
		}

		stationList.setInput(stns.toArray());
		if (p.getStation() != null){
			stationList.setSelection(new StructuredSelection(p.getStation()));
		}else{
			stationList.setSelection(new StructuredSelection(none));
		}

	}

	public void updatePatrol(Patrol p) {
		Object station = (Object)((IStructuredSelection)stationList.getSelection()).getFirstElement();
		if (station != null && station instanceof Station){
			p.setStation((Station)station);
		}else{
			p.setStation(null);
		}
	}


	/* (non-Javadoc)
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Patrol Station";
	}
}

