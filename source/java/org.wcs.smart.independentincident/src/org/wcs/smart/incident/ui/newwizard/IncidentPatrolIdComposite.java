/*
 * Copyright (C) 2023 Wildlife Conservation Society
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.model.IncidentWaypoint;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.internal.ui.views.IPatrolFilteringView;
import org.wcs.smart.patrol.internal.ui.views.PatrolFilterDialog;
import org.wcs.smart.patrol.internal.ui.views.PatrolTreeLabelProvider;
import org.wcs.smart.patrol.internal.ui.views.PatrolViewFilter;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Incident distance/direction composite.
 * @author Emily
 *
 */
public class IncidentPatrolIdComposite extends AbstractIncidentComposite implements IPatrolFilteringView{

	private static final String NONE_OP = Messages.IncidentPatrolIdComposite_NONE_OP;

	public static final String ID = "incident.patrolid"; //$NON-NLS-1$
	
	private PatrolViewFilter currentFilter = PatrolViewFilter.newInstance();

	private TableViewer cmbPatrol;

	private PatrolEditorInput currentValue = null;
	
	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout());
		
		Link filterLink = new Link(item, SWT.NONE);
		filterLink.setText(Messages.IncidentPatrolIdComposite_ChangeFilter);
		
		filterLink.addListener(SWT.Selection, event->(new PatrolFilterDialog(parent.getShell(), IncidentPatrolIdComposite.this)).open());
		
		Composite main = new Composite(item, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new TableColumnLayout());
		((GridData)main.getLayoutData()).heightHint = 200;
		
		cmbPatrol = new TableViewer(main, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		cmbPatrol.setContentProvider(ArrayContentProvider.getInstance());
		cmbPatrol.addSelectionChangedListener(e->fireChange(new Event()));
		TableViewerColumn col1 = new TableViewerColumn(cmbPatrol, SWT.NONE);
		col1.setLabelProvider(new PatrolTreeLabelProvider());

		((TableColumnLayout)main.getLayout()).setColumnData(col1.getColumn(), new ColumnWeightData(1));

		
		this.updateContent();
		
		return item;
	}

	@Override
	public void updateIncident(Waypoint incident) {}
	
	@Override
	public void afterSave(Waypoint incident, Session session) {
		if (session == null) return;
		
		IncidentWaypoint iw = session.get(IncidentWaypoint.class, incident.getUuid());
		
		Object newValue = cmbPatrol.getStructuredSelection().getFirstElement();
		
		if (newValue == null || !(newValue instanceof PatrolEditorInput)) {
			if (iw != null) session.delete(iw);
		}else {
			PatrolEditorInput in = (PatrolEditorInput)newValue;
			if (iw == null || !in.getUuid().equals(iw.getPatrol().getUuid())) {
				if (iw != null) session.remove(iw);

				Patrol patrol = session.get(Patrol.class, in.getUuid());
				iw = new IncidentWaypoint();
				iw.setPatrol(patrol);
				iw.setWaypoint(incident);
				
				session.persist(iw);
			}
		}
		
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		IncidentWaypoint iw = session.get(IncidentWaypoint.class, incident.getUuid());
		
		if (iw == null) {
			currentValue = null;
			cmbPatrol.setSelection(new StructuredSelection(NONE_OP));
		}else {
			currentValue = new PatrolEditorInput(iw.getPatrol().getUuid(), iw.getPatrol().getId(), iw.getPatrol().getPatrolType(), iw.getPatrol().getStartDate(), iw.getPatrol().getEndDate());
			cmbPatrol.setSelection(new StructuredSelection(currentValue));
		}
		
	}
	
	@Override
	public String getName() {
		return Messages.IncidentPatrolIdComposite_Title;
	}

	@Override
	public String getDescription() {
		return Messages.IncidentPatrolIdComposite_Message;
	}

	@Override
	public void updateContent() {
		cmbPatrol.setInput(DialogConstants.LOADING_TEXT);
		try(Session session = HibernateManager.openSession()){
			List<Object> all = new ArrayList<>();
			
			all.add(NONE_OP);
			
			List<?> items = currentFilter.buildQuery(session).list();
			for (Object x : items) {
				Object[] data = (Object[])x;
				all.add(new PatrolEditorInput((UUID)data[0], (String)data[1], 
						(PatrolType.Type)data[2], (LocalDate)data[3], (LocalDate)data[4]));

			}
			if (all.contains(currentValue)) all.add(currentValue);
			
			cmbPatrol.setInput(all);
			if (currentValue != null) {
				cmbPatrol.setSelection(new StructuredSelection(currentValue));
			}else {
				cmbPatrol.setSelection(new StructuredSelection(NONE_OP));
			}
			
		}
		cmbPatrol.refresh();
		cmbPatrol.getControl().getParent().getParent().layout(true,  true);
	}

	@Override
	public PatrolViewFilter getFilter() {
		return currentFilter;
	}

}
