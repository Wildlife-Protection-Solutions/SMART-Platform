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
package org.wcs.smart.cybertracker.properties;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrolOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrolOption.PatrolMeta;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * The CyberTracker dialog for managing patrol meta screen
 * that will be shown in generated CyberTracker application
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class PatrolMetaConfigDialog extends AbstractPropertyJHeaderDialog {

	private PatrolMeta[] optionsToShow = {
			PatrolMeta.TYPE,
			PatrolMeta.TRANSPORT,
			PatrolMeta.ARMED,
			PatrolMeta.TEAM,
			PatrolMeta.STATION,
			PatrolMeta.MANDATE,
			PatrolMeta.OBJECTIVE,
			PatrolMeta.COMMENT,
			PatrolMeta.MEMBERS,
			PatrolMeta.LEADER,
			PatrolMeta.PILOT };

	private CyberTrackerProperties ctProperties;
	private List<PatrolType> patrolTypes;
	private List<Team> teams;
	private List<Station> stations;
	private List<PatrolMandate> mandates;
	private List<Employee> members;
	
	
	private TableViewer modelListViewer;

	private Composite infoInnerPanel;
	private Composite emptyComposite;

	private Map<PatrolMeta, Composite> screenComposites;
	
	public PatrolMetaConfigDialog() {
		super(Display.getDefault().getActiveShell(), "Patrol Metadata Data Collection Configuration");
		initData();
	}

	private void initData() {
		Session session = HibernateManager.openSession();
		try {
			ctProperties = CyberTrackerHibernateManager.getProperties(session);
			Map<PatrolMeta, CyberTrackerPatrolOption> options = ctProperties.getPatrolOptions();
			//creating missing options
			for (PatrolMeta meta : optionsToShow) {
				CyberTrackerPatrolOption cto = options.get(meta);
				if (cto == null) {
					cto = new CyberTrackerPatrolOption();
					cto.setType(meta);
					options.put(meta, cto);
				}
			}

			ConservationArea ca = SmartDB.getCurrentConservationArea();
			patrolTypes = PatrolHibernateManager.getActivePatrolTypes(ca, session);
			for (PatrolType type : patrolTypes) {
				type.getTransportTypes().size(); //load lazy items
			}
			teams = PatrolHibernateManager.getActiveTeams(ca, session);
			stations = PatrolHibernateManager.getActiveStations(ca, session);
			mandates = PatrolHibernateManager.getActiveMandates(ca, session);
			members = PatrolHibernateManager.getActiveEmployees(ca, session);
			Collections.sort(members, new Comparator<Employee>() {
				@Override
				public int compare(Employee e1, Employee e2) {
					return Collator.getInstance().compare(e1.getFullLabel(), e2.getFullLabel());
				}
			});
			
		} finally {
			session.close();
		}
		if (ctProperties == null)
			ctProperties = new CyberTrackerProperties();
		
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, true));

		modelListViewer = new TableViewer(container, SWT.V_SCROLL | SWT.H_SCROLL);
		modelListViewer.setLabelProvider(new PatrolMetaScreenLabelProvider());
		modelListViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelListViewer.setInput(optionsToShow);
		modelListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		modelListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateRightPanel();
			}
		});

		Composite rightPanel = new Composite(container, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		infoInnerPanel = new Composite(rightPanel, SWT.NONE);
		infoInnerPanel.setLayout(new StackLayout());
		infoInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		emptyComposite = new Composite(infoInnerPanel, SWT.NONE);
		emptyComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

		screenComposites = new HashMap<PatrolMeta, Composite>();
		Map<PatrolMeta, CyberTrackerPatrolOption> options = ctProperties.getPatrolOptions();

		Composite cmp = new TypeTransportScreenOptionComposite(infoInnerPanel, options.get(PatrolMeta.TYPE), options.get(PatrolMeta.TRANSPORT), patrolTypes);
		screenComposites.put(PatrolMeta.TYPE,      cmp);
		screenComposites.put(PatrolMeta.TRANSPORT, cmp);
		screenComposites.put(PatrolMeta.ARMED,     new ArmedScreenOptionComposite(infoInnerPanel, options.get(PatrolMeta.ARMED)));
		screenComposites.put(PatrolMeta.TEAM,      new DropdownScreenOptionComposite(infoInnerPanel, options.get(PatrolMeta.TEAM), teams));
		screenComposites.put(PatrolMeta.STATION,   new DropdownScreenOptionComposite(infoInnerPanel, options.get(PatrolMeta.STATION), stations));
		screenComposites.put(PatrolMeta.MANDATE,   new DropdownScreenOptionComposite(infoInnerPanel, options.get(PatrolMeta.MANDATE), mandates));
		
		return container;
	}

	private void updateRightPanel() {
		IStructuredSelection selection = (IStructuredSelection) modelListViewer.getSelection();
		Object obj = selection.getFirstElement();

		if (obj instanceof PatrolMeta) {
			PatrolMeta meta = (PatrolMeta) obj;
			((StackLayout)infoInnerPanel.getLayout()).topControl = screenComposites.get(meta);
		} else {
			((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
		}
		infoInnerPanel.layout();
	}

	@Override
	protected boolean performSave() {
		// TODO Auto-generated method stub
		return false;
	}

	private class PatrolMetaScreenLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof PatrolMeta) {
				PatrolMeta i = (PatrolMeta)element;
				return i.getGuiLabel();
			}
			return super.getText(element);
		}
	}
}
