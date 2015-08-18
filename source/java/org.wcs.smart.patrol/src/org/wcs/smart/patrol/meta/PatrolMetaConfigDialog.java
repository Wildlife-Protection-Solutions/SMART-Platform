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
package org.wcs.smart.patrol.meta;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.ScreenOption;
import org.wcs.smart.patrol.model.ScreenOption.ScreenOptionMeta;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * The CyberTracker dialog for managing patrol meta screen
 * that will be shown in generated CyberTracker application
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class PatrolMetaConfigDialog extends AbstractPropertyJHeaderDialog {

	private ScreenOptionMeta[] optionsToShow = {
			ScreenOptionMeta.TYPE,
			ScreenOptionMeta.TRANSPORT,
			ScreenOptionMeta.ARMED,
			ScreenOptionMeta.TEAM,
			ScreenOptionMeta.STATION,
			ScreenOptionMeta.MANDATE,
			ScreenOptionMeta.OBJECTIVE,
			ScreenOptionMeta.COMMENT,
			ScreenOptionMeta.MEMBERS,
			ScreenOptionMeta.LEADER,
			ScreenOptionMeta.PILOT };

	private Map<ScreenOptionMeta, ScreenOption> options;
	
	private List<PatrolType> patrolTypes;
	private List<Team> teams;
	private List<Station> stations;
	private List<PatrolMandate> mandates;
	private List<Employee> members;
	
	private TableViewer modelListViewer;

	private Composite infoInnerPanel;
	private Composite emptyComposite;

	private Map<ScreenOptionMeta, Composite> screenComposites;
	
	public PatrolMetaConfigDialog(Shell shell) {
		super(shell, Messages.PatrolMetaConfigDialog_ShellTitle);
		initData();
	}

	private void initData() {
		Session session = getSession();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		options = PatrolHibernateManager.getScreenOptions(ca, session);
		//creating missing options
		for (ScreenOptionMeta meta : optionsToShow) {
			ScreenOption cto = options.get(meta);
			if (cto == null) {
				cto = new ScreenOption();
				cto.setConservationArea(ca);
				cto.setType(meta);
				options.put(meta, cto);
			}
		}

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
				return Collator.getInstance().compare(SmartLabelProvider.getFullLabel(e1), SmartLabelProvider.getFullLabel(e2));
			}
		});
	}

	@Override
	protected Composite createContent(Composite parent) {
		
		SashForm container = new SashForm(parent, SWT.HORIZONTAL);

		modelListViewer = new TableViewer(container, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
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

		IScreenOptionChangeListener listener = new IScreenOptionChangeListener() {
			@Override
			public void screenOptionChanged() {
				setChangesMade(true);
				StackLayout stackLayout = ((StackLayout)infoInnerPanel.getLayout());
				updateMessage((Composite)stackLayout.topControl);
			}
		};
		
		screenComposites = new HashMap<ScreenOptionMeta, Composite>();

		ScreenOptionComposite soc  = new TypeTransportScreenOptionComposite(infoInnerPanel, options.get(ScreenOptionMeta.TYPE), options.get(ScreenOptionMeta.TRANSPORT), patrolTypes);
		soc.addScreenOptionListener(listener);
		screenComposites.put(ScreenOptionMeta.TYPE,      soc);
		screenComposites.put(ScreenOptionMeta.TRANSPORT, soc);
		
		soc = new ArmedScreenOptionComposite(infoInnerPanel, options.get(ScreenOptionMeta.ARMED));
		soc.addScreenOptionListener(listener);
		screenComposites.put(ScreenOptionMeta.ARMED, soc);

		soc = new DropdownScreenOptionComposite(infoInnerPanel, options.get(ScreenOptionMeta.TEAM), teams);
		soc.addScreenOptionListener(listener);
		screenComposites.put(ScreenOptionMeta.TEAM, soc);

		soc = new DropdownScreenOptionComposite(infoInnerPanel, options.get(ScreenOptionMeta.STATION), stations);
		soc.addScreenOptionListener(listener);
		screenComposites.put(ScreenOptionMeta.STATION, soc);

		soc = new DropdownScreenOptionComposite(infoInnerPanel, options.get(ScreenOptionMeta.MANDATE), mandates);
		soc.addScreenOptionListener(listener);
		screenComposites.put(ScreenOptionMeta.MANDATE, soc);
		
		soc = new TextScreenOptionComposite(infoInnerPanel, options.get(ScreenOptionMeta.OBJECTIVE));
		soc.addScreenOptionListener(listener);
		screenComposites.put(ScreenOptionMeta.OBJECTIVE, soc);

		soc = new TextScreenOptionComposite(infoInnerPanel, options.get(ScreenOptionMeta.COMMENT));
		soc.addScreenOptionListener(listener);
		screenComposites.put(ScreenOptionMeta.COMMENT, soc);

		soc = new EmployeeScreenOptionComposite(infoInnerPanel, options, members);
		soc.addScreenOptionListener(listener);
		screenComposites.put(ScreenOptionMeta.MEMBERS, soc);
		screenComposites.put(ScreenOptionMeta.LEADER, soc);
		screenComposites.put(ScreenOptionMeta.PILOT, soc);
		
		container.setWeights(new int[]{40,60});
		
		setTitle(Messages.PatrolMetaConfigDialog_DialogTitle);
		setMessage(Messages.PatrolMetaConfigDialog_DialogMessage);
		return container;
	}

	private void updateRightPanel() {
		IStructuredSelection selection = (IStructuredSelection) modelListViewer.getSelection();
		Object obj = selection.getFirstElement();

		StackLayout stackLayout = ((StackLayout)infoInnerPanel.getLayout());
		if (obj instanceof ScreenOptionMeta) {
			ScreenOptionMeta meta = (ScreenOptionMeta) obj;
			Composite cmp = screenComposites.get(meta);
			updateMessage(cmp);
			stackLayout.topControl = cmp;
		} else {
			stackLayout.topControl = emptyComposite;
		}
		infoInnerPanel.layout();
	}

	private void updateMessage(Composite editComposite) {
		if (editComposite instanceof ScreenOptionComposite) {
			ScreenOptionComposite soc = (ScreenOptionComposite) editComposite;
			String msg = soc.validate();
			if (msg != null) {
				setMessage(msg, IMessageProvider.ERROR);
				return;
			}
		}
		setMessage(Messages.PatrolMetaConfigDialog_DialogMessage);
	}
	
	private boolean validate() {
		for (ScreenOptionMeta option : optionsToShow) {
			Composite cmp = screenComposites.get(option);
			if (cmp instanceof ScreenOptionComposite) {
				ScreenOptionComposite soc = (ScreenOptionComposite) cmp;
				if (soc.validate() != null) {
					modelListViewer.setSelection(new StructuredSelection(option));
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	protected boolean performSave() {
		if (!validate())
			return false;
		
		Session session = getSession();
		session.beginTransaction();
		try {
			for (ScreenOption so : options.values()) {
				session.saveOrUpdate(so);
			}
			session.getTransaction().commit();
			setChangesMade(false);
			return true;
		} catch (Exception ex) {
			session.getTransaction().rollback();
			SmartPatrolPlugIn.displayLog(Messages.PatrolMetaConfigDialog_ErrorMessage + "\n"+ ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return false;
		}
	}

	private class PatrolMetaScreenLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof ScreenOptionMeta) {
				ScreenOptionMeta i = (ScreenOptionMeta)element;
				return org.wcs.smart.patrol.ui.LabelConstants.getLabel(i);
			}
			return super.getText(element);
		}
	}
}
