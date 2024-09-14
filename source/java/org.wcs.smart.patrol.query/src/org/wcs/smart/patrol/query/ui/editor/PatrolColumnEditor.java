
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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
import org.wcs.smart.patrol.query.model.IPatrolQueryResultItem;
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
	
	private HashMap<UUID, Boolean> pTypePilot = null;
	
	//cache link between patrol types and pilot
	//users will have to re-run query in order for these to be
	//reset if they change
	private Job initpatrolTypes = new Job("initialize patrol types") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				List<PatrolTransportType> types = session.createQuery("FROM PatrolTransportType WHERE conservationArea = :ca", PatrolTransportType.class) //$NON-NLS-1$
						.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.list();
				types.forEach(e->pTypePilot.put(e.getUuid(),e.getRequiresPilot()));
			}
			return Status.OK_STATUS;
		}
		
	};
	
	public PatrolColumnEditor (ColumnViewer viewer, FixedQueryColumn queryColumn, IQueryEditor editor ){
		super(viewer, queryColumn, editor);
		
		if ( queryColumn.getColumn() == FixedQueryColumn.FixedColumns.PATROL_LEG_PILOT) {
			pTypePilot = new HashMap<>();
			initpatrolTypes.schedule();
		}
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
			if (!(element instanceof IPatrolQueryResultItem)) return null;
			IPatrolQueryResultItem pitem = (IPatrolQueryResultItem)element;
			
			if (!pTypePilot.containsKey(pitem.getPatrolTransportTypeUuid())) return null;
			if (!pTypePilot.get(pitem.getPatrolTransportTypeUuid())) return null; // pilot not valid for type
			
			
		case PATROL_LEG_LEADER:
			if (!(element instanceof IPatrolQueryResultItem)) return null;
			if (employeeCellEditor == null){
				employeeCellEditor = getDropDownEditor();
			}
			employeeCellEditor.setInput(new String[]{DialogConstants.LOADING_TEXT});
			IPatrolQueryResultItem item = (IPatrolQueryResultItem)element;
			Job j = new Job("load employees"){ //$NON-NLS-1$

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<Employee> members = new ArrayList<Employee>();
					try(Session s = HibernateManager.openSession()){
						PatrolLeg pl = (PatrolLeg)s.get(PatrolLeg.class, item.getPatrolLegUuid());
						
						for(PatrolLegMember m : pl.getMembers()){
							SmartLabelProvider.getShortLabel(m.getMember());
							members.add(m.getMember());
						}
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
			
			if (!(element instanceof IPatrolQueryResultItem)) return null;
			IPatrolQueryResultItem ppitem = (IPatrolQueryResultItem)element;
			return getTransportCellEditor(ppitem.getPatrolTypeUuid());
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
				try(Session s = HibernateManager.openSession()){
					items.addAll(HibernateManager.getActiveStations(SmartDB.getCurrentConservationArea(), s));
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
				try(Session s = HibernateManager.openSession()){
					items.addAll(PatrolHibernateManager.getActiveTeams(SmartDB.getCurrentConservationArea(), s));
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
				try(Session s = HibernateManager.openSession()){
					items.addAll(PatrolHibernateManager.getActiveMandates(SmartDB.getCurrentConservationArea(), s));
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
	
	private ComboBoxViewerCellEditor getTransportCellEditor(UUID patrolTypeUuid){
		if (transportCellEditor == null){
			transportCellEditor = getDropDownEditor();
		}
				
		UUID temp = (UUID) transportCellEditor.getControl().getData("LASTUUID"); //$NON-NLS-1$
		transportCellEditor.getControl().setData("LASTUUID", patrolTypeUuid); //$NON-NLS-1$
		
		if (!patrolTypeUuid.equals(temp)) {
			transportCellEditor.setInput(new String[] {DialogConstants.LOADING_TEXT});
			Job j = new Job("load list items"){ //$NON-NLS-1$
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					final List<PatrolTransportType> items = new ArrayList<>();
					try(Session s = HibernateManager.openSession()){
						items.addAll(s.createQuery("FROM PatrolTransportType WHERE patrolType.uuid = :uuid and conservationArea = :ca and isActive", PatrolTransportType.class) //$NON-NLS-1$
								.setParameter("uuid", patrolTypeUuid) //$NON-NLS-1$
								.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.list());
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
		}
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
