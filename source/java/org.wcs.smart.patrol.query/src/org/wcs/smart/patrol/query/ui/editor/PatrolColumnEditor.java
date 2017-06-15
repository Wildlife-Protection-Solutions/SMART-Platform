
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
 */package org.wcs.smart.patrol.query.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.common.celleditor.ComboBoxViewerCellEditor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.common.ui.edit.AbstractQueryColumnEditor;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Column editor for the patrol columns in the query results table.
 * @author Emily
 *
 */
public class PatrolColumnEditor extends AbstractQueryColumnEditor {

	private ComboBoxViewerCellEditor stationCellEditor = null;
	private ComboBoxViewerCellEditor teamCellEditor = null;
	private ComboBoxViewerCellEditor mandateCellEditor = null;
	private ComboBoxViewerCellEditor transportCellEditor = null;
	
	private ComboBoxViewerCellEditor employeeCellEditor = null;
	
	
	public PatrolColumnEditor (ColumnViewer viewer, FixedQueryColumn queryColumn, IQueryEditor editor ){
		super(viewer, queryColumn, editor);
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		switch (((FixedQueryColumn)queryColumn).getColumn()) {
		case PATROL_ID:
			return getTextCellEditor();
		case PATROL_STATION:
			return getStationCellEditor();
		case PATROL_TEAM:
			return getTeamCellEditor();
		case PATROL_OBJETIVE:
			return getTextCellEditor();
		case PATROL_MANDATE:
			return getMandateCellEditor();
		case PATROL_ARMED:
			return getBooleanCellEditor(new LabelProvider(){
				public String getText(Object element){
					if (element instanceof Boolean){
						if ((Boolean)element){
							return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
						}else{
							return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
						}
					}
					return super.getText(element);
				}
			});
		case PATROL_LEG_ID:
			return getTextCellEditor();
			
		case PATROL_LEG_PILOT:
			if (!(element instanceof PatrolQueryResultItem)) return null;
			PatrolQueryResultItem pitem = (PatrolQueryResultItem)element;
			if (!pitem.getPatrolType().requiresPilot()) return null; // pilot not valid for type
		case PATROL_LEG_LEADER:
			if (!(element instanceof PatrolQueryResultItem)) return null;
			if (employeeCellEditor == null){
				employeeCellEditor = getDropDownEditor();
			}
			employeeCellEditor.setInput(new String[]{DialogConstants.LOADING_TEXT});
			PatrolQueryResultItem item = (PatrolQueryResultItem)element;
			Job j = new Job("load employees"){ //$NON-NLS-1$

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<Employee> members = new ArrayList<Employee>();
					Session s = HibernateManager.openSession();
					try{
						PatrolLeg pl = (PatrolLeg)s.get(PatrolLeg.class, item.getPatrolLegUuid());
						
						for(PatrolLegMember m : pl.getMembers()){
							SmartLabelProvider.getShortLabel(m.getMember());
							members.add(m.getMember());
						}
					}finally{
						s.close();
					}
					Display.getDefault().syncExec(()->{
						if (!employeeCellEditor.getControl().isDisposed()){
							employeeCellEditor.setInput(members);
							employeeCellEditor.recomputeSize();
						}
					});
					return Status.OK_STATUS;
				}
			};
			j.schedule();
			return employeeCellEditor;
		case TRANSPORT_TYPE:
			return getTransportCellEditor();
		default:
			return null;
		}
	}
	
	
	private ComboBoxViewerCellEditor getStationCellEditor(){
		if (stationCellEditor != null){
			return stationCellEditor;
		}
		stationCellEditor = getDropDownEditor();
				
		Job j = new Job("load list items"){ //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<Station> items = new ArrayList<>();
				Session s = HibernateManager.openSession();
				try{
					items.addAll(HibernateManager.getActiveStations(SmartDB.getCurrentConservationArea(), s));
				}finally{
					s.close();
				}
				Display.getDefault().syncExec(()->{
					if (!stationCellEditor.getControl().isDisposed()){
						stationCellEditor.setInput(items);
						stationCellEditor.recomputeSize();
					}
				});
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		return stationCellEditor;
	}
	
	private ComboBoxViewerCellEditor getTeamCellEditor(){
		if (teamCellEditor != null){
			return teamCellEditor;
		}
		teamCellEditor = getDropDownEditor();
				
		Job j = new Job("load list items"){ //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<Team> items = new ArrayList<>();
				Session s = HibernateManager.openSession();
				try{
					items.addAll(PatrolHibernateManager.getActiveTeams(SmartDB.getCurrentConservationArea(), s));
				}finally{
					s.close();
				}
				Display.getDefault().syncExec(()->{
					if (!teamCellEditor.getControl().isDisposed()){
						teamCellEditor.setInput(items);
						teamCellEditor.recomputeSize();
					}
				});
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		return teamCellEditor;
	}
	
	private ComboBoxViewerCellEditor getMandateCellEditor(){
		if (mandateCellEditor != null){
			return mandateCellEditor;
		}
		mandateCellEditor = getDropDownEditor();
				
		Job j = new Job("load list items"){ //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<PatrolMandate> items = new ArrayList<>();
				Session s = HibernateManager.openSession();
				try{
					items.addAll(PatrolHibernateManager.getActiveMandates(SmartDB.getCurrentConservationArea(), s));
				}finally{
					s.close();
				}
				Display.getDefault().syncExec(()->{
					if (!mandateCellEditor.getControl().isDisposed()){
						mandateCellEditor.setInput(items);
						mandateCellEditor.recomputeSize();
					}
				});
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		return mandateCellEditor;
	}
	
	private ComboBoxViewerCellEditor getTransportCellEditor(){
		if (transportCellEditor != null){
			return transportCellEditor;
		}
		transportCellEditor = getDropDownEditor();
				
		Job j = new Job("load list items"){ //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<PatrolTransportType> items = new ArrayList<>();
				Session s = HibernateManager.openSession();
				try{
					items.addAll(PatrolHibernateManager.getActivePatrolTransporationTypes(SmartDB.getCurrentConservationArea(), s));
				}finally{
					s.close();
				}
				Display.getDefault().syncExec(()->{
					if (!transportCellEditor.getControl().isDisposed()){
						transportCellEditor.setInput(items);
						transportCellEditor.recomputeSize();
					}
				});
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		return transportCellEditor;
	}
	
	private ComboBoxViewerCellEditor getDropDownEditor(){
		ComboBoxViewerCellEditor listCellEditor = new ComboBoxViewerCellEditor((Composite) getViewer().getControl(), SWT.READ_ONLY | SWT.DROP_DOWN);
		listCellEditor.setContentProvider(ArrayContentProvider.getInstance());
		listCellEditor.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof NamedItem){
					return ((NamedItem)element).getName();
				}else if (element instanceof Employee){
					return SmartLabelProvider.getShortLabel(((Employee) element));
				}
				return super.getText(element);
			}
		});
		listCellEditor.setInput(new Object[]{DialogConstants.LOADING_TEXT});
		return listCellEditor;
	}
}
