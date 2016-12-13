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
package org.wcs.smart.patrol.ui;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.PatrolItemComposite;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Patrol Item composite for selecting patrol station.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class StationComposite extends PatrolItemComposite{

	private ComboViewer stationList;

	/*
	 * Station label provider
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

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {

		Composite center = new Composite(parent, SWT.NONE);
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		center.setLayout(new GridLayout(2, false));
		
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.StationComposite_Station_Label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		stationList = new ComboViewer(center, SWT.DROP_DOWN | SWT.READ_ONLY);
		stationList.setContentProvider(ArrayContentProvider.getInstance());
		stationList.setLabelProvider(lblProvider);
		stationList.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)stationList.getCombo().getLayoutData()).widthHint = 100;
		stationList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();	
			}
		});
		return center;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(Patrol p, Session session) {
		List<? extends Object> stations = HibernateManager.getActiveStations(p.getConservationArea(), session);

		setInput(stations, p.getStation());
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public boolean updatePatrol(Patrol p, Session session) {
		Object station = (Object)((IStructuredSelection)stationList.getSelection()).getFirstElement();
		if (station != null && station instanceof Station){
			p.setStation((Station)station);
		}else{
			p.setStation(null);
		}
		return true;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getSelectedStation(org.wcs.smart.patrol.model.Patrol)
	 */
	public Station getSelectedStation() {
		Object station = (Object)((IStructuredSelection)stationList.getSelection()).getFirstElement();
		if (station != null && station instanceof Station){
			return((Station)station);
		}else{
			return null;
		}
	}
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getSelectedTeam()
	 */

	public void setSelectedStation(Station stn) {
		stationList.setSelection(new StructuredSelection(stn));
	}	


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return Messages.StationComposite_Title;
	}
	
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_STATION;
	}

	public void setInput(List<? extends Object> stations, Station station) {
		String none = Messages.StationComposite_NoStation_Label;
		List<Object> stns = new ArrayList<Object>();
		stns.add(none);
		if (stations != null){
			Collections.sort(stations, new Comparator<Object>(){
				@Override
				public int compare(Object o1, Object o2) {
					return Collator.getInstance().compare(((Station)o1).getName(), ((Station)o2).getName());
			}});
			
			stns.addAll(stations);
		}
		
		stationList.setInput(stns.toArray());
		if (station != null){
			stationList.setSelection(new StructuredSelection(station));
		}else{
			stationList.setSelection(new StructuredSelection(none));
		}
		
	}
	public ComboViewer getViewer(){
		return stationList;
	}
}

