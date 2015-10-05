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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.dataentry.meta.DropdownScreenOptionComposite;
import org.wcs.smart.dataentry.meta.IScreenOptionChangeListener;
import org.wcs.smart.dataentry.meta.MetaConfigDialog;
import org.wcs.smart.dataentry.meta.ScreenOptionComposite;
import org.wcs.smart.dataentry.meta.TextScreenOptionComposite;
import org.wcs.smart.dataentry.meta.YesNoScreenOptionComposite;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * The CyberTracker dialog for managing patrol meta screen
 * that will be shown in generated CyberTracker application
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class PatrolMetaConfigDialog extends MetaConfigDialog<PatrolScreenOptionMeta> {

	private PatrolScreenOptionMeta[] optionsToShow = {
			PatrolScreenOptionMeta.TYPE,
			PatrolScreenOptionMeta.TRANSPORT,
			PatrolScreenOptionMeta.ARMED,
			PatrolScreenOptionMeta.TEAM,
			PatrolScreenOptionMeta.STATION,
			PatrolScreenOptionMeta.MANDATE,
			PatrolScreenOptionMeta.OBJECTIVE,
			PatrolScreenOptionMeta.COMMENT,
			PatrolScreenOptionMeta.MEMBERS,
			PatrolScreenOptionMeta.LEADER,
			PatrolScreenOptionMeta.PILOT };

	private Map<PatrolScreenOptionMeta, ScreenOption> options;
	
	private LabelProvider metaScreenLabelProvider = new PatrolMetaScreenLabelProvider();
	
	private List<PatrolType> patrolTypes;
	private List<Team> teams;
	private List<Station> stations;
	private List<PatrolMandate> mandates;
	private List<Employee> members;
	
	public PatrolMetaConfigDialog(Shell shell) {
		super(shell, Messages.PatrolMetaConfigDialog_ShellTitle, Messages.PatrolMetaConfigDialog_DialogMessage);
		initData();
	}

	private void initData() {
		Session session = getSession();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		options = PatrolHibernateManager.getScreenOptions(ca, session);
		//creating missing options
		for (PatrolScreenOptionMeta meta : optionsToShow) {
			ScreenOption cto = options.get(meta);
			if (cto == null) {
				cto = new ScreenOption();
				cto.setConservationArea(ca);
				cto.setResource(PatrolScreenOptionMeta.PATROL_RESOURCE_ID);
				cto.setType(meta.name());
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
	protected Map<PatrolScreenOptionMeta, ScreenOption> getOptionsMap() {
		return options;
	}

	@Override
	protected PatrolScreenOptionMeta[] getOptionsToShow() {
		return optionsToShow;
	}

	@Override
	protected Map<PatrolScreenOptionMeta, ScreenOptionComposite> buildOptionComposites(Composite infoInnerPanel, IScreenOptionChangeListener listener) {
		Map<PatrolScreenOptionMeta, ScreenOptionComposite> screenComposites = new HashMap<PatrolScreenOptionMeta, ScreenOptionComposite>();

		ScreenOptionComposite soc  = new TypeTransportScreenOptionComposite(infoInnerPanel, options.get(PatrolScreenOptionMeta.TYPE), options.get(PatrolScreenOptionMeta.TRANSPORT), patrolTypes);
		soc.addScreenOptionListener(listener);
		screenComposites.put(PatrolScreenOptionMeta.TYPE,      soc);
		screenComposites.put(PatrolScreenOptionMeta.TRANSPORT, soc);
		
		soc = new YesNoScreenOptionComposite(infoInnerPanel, options.get(PatrolScreenOptionMeta.ARMED), metaScreenLabelProvider.getText(PatrolScreenOptionMeta.ARMED));
		soc.addScreenOptionListener(listener);
		screenComposites.put(PatrolScreenOptionMeta.ARMED, soc);

		soc = new DropdownScreenOptionComposite(infoInnerPanel, options.get(PatrolScreenOptionMeta.TEAM), metaScreenLabelProvider.getText(PatrolScreenOptionMeta.TEAM), teams);
		soc.addScreenOptionListener(listener);
		screenComposites.put(PatrolScreenOptionMeta.TEAM, soc);

		soc = new DropdownScreenOptionComposite(infoInnerPanel, options.get(PatrolScreenOptionMeta.STATION), metaScreenLabelProvider.getText(PatrolScreenOptionMeta.STATION), stations);
		soc.addScreenOptionListener(listener);
		screenComposites.put(PatrolScreenOptionMeta.STATION, soc);

		soc = new DropdownScreenOptionComposite(infoInnerPanel, options.get(PatrolScreenOptionMeta.MANDATE), metaScreenLabelProvider.getText(PatrolScreenOptionMeta.MANDATE), mandates);
		soc.addScreenOptionListener(listener);
		screenComposites.put(PatrolScreenOptionMeta.MANDATE, soc);
		
		soc = new TextScreenOptionComposite(infoInnerPanel, options.get(PatrolScreenOptionMeta.OBJECTIVE), metaScreenLabelProvider.getText(PatrolScreenOptionMeta.OBJECTIVE));
		soc.addScreenOptionListener(listener);
		screenComposites.put(PatrolScreenOptionMeta.OBJECTIVE, soc);

		soc = new TextScreenOptionComposite(infoInnerPanel, options.get(PatrolScreenOptionMeta.COMMENT), metaScreenLabelProvider.getText(PatrolScreenOptionMeta.COMMENT));
		soc.addScreenOptionListener(listener);
		screenComposites.put(PatrolScreenOptionMeta.COMMENT, soc);

		soc = new EmployeeScreenOptionComposite(infoInnerPanel, options, members);
		soc.addScreenOptionListener(listener);
		screenComposites.put(PatrolScreenOptionMeta.MEMBERS, soc);
		screenComposites.put(PatrolScreenOptionMeta.LEADER, soc);
		screenComposites.put(PatrolScreenOptionMeta.PILOT, soc);
		
		return screenComposites;
	}

	@Override
	protected LabelProvider getMetaTypeLabelProvider() {
		return metaScreenLabelProvider;
	}

	private class PatrolMetaScreenLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof PatrolScreenOptionMeta) {
				PatrolScreenOptionMeta i = (PatrolScreenOptionMeta)element;
				return LabelConstants.getLabel(i);
			}
			return super.getText(element);
		}
	}
}
