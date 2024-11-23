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

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.PatrolItemComposite;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.ui.NamedIconItemLabelProvider;

/**
 * Patrol Item composite for selecting patrol station.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class StationComposite extends PatrolItemComposite{

	private TableViewer stationList;
	private LabelProvider lblProvider = new NamedIconItemLabelProvider();
	
	
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
		center.setLayout(new GridLayout());
		center.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)center.getLayout()).marginWidth = 0;
		((GridLayout)center.getLayout()).marginHeight = 0;
		
		Composite table = new Composite(center, SWT.NONE);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setLayout(new TableColumnLayout());
		((GridData)table.getLayoutData()).heightHint = 100;

		stationList = new TableViewer(table, SWT.BORDER | SWT.SINGLE);
		stationList.setContentProvider(ArrayContentProvider.getInstance());
		stationList.setLabelProvider(lblProvider);
		stationList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)stationList.getControl().getLayoutData()).widthHint = 100;
		stationList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();	
			}
		});
		((TableColumnLayout)table.getLayout()).setColumnData(
				new TableColumn(stationList.getTable(), SWT.NONE),
	            new ColumnWeightData(100));
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
		return LabelConstants.STATION_NAME;
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
		stationList.getControl().getParent().layout();
		
	}
	public TableViewer getViewer(){
		return stationList;
	}
}

