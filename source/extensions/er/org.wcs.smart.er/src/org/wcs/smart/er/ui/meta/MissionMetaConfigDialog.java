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
package org.wcs.smart.er.ui.meta;

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
import org.wcs.smart.dataentry.meta.IScreenOptionChangeListener;
import org.wcs.smart.dataentry.meta.MetaConfigDialog;
import org.wcs.smart.dataentry.meta.ScreenOptionComposite;
import org.wcs.smart.dataentry.meta.TextScreenOptionComposite;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * The dialog for managing mission meta screen. This can allow to configure
 * what screens will be display in some generated application (e.g. CyberTracker).
 * 
 * @author elitvin
 * @since 3.3.0
 */
public class MissionMetaConfigDialog extends MetaConfigDialog<MissionScreenOptionMeta> {

	private MissionScreenOptionMeta[] optionsToShow = {
			MissionScreenOptionMeta.MEMBERS,
			MissionScreenOptionMeta.LEADER,
			MissionScreenOptionMeta.COMMENT
			};

	private Map<MissionScreenOptionMeta, ScreenOption> options;
	
	private LabelProvider metaScreenLabelProvider = new MissionMetaScreenLabelProvider();
	
	private List<Employee> members;
	
	public MissionMetaConfigDialog(Shell shell) {
		super(shell, Messages.MissionMetaConfigDialog_Title, Messages.MissionMetaConfigDialog_Message);
		initData();
	}

	private void initData() {
		Session session = getSession();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		options = SurveyHibernateManager.getMissionScreenOptions(ca, session);
		//creating missing options
		for (MissionScreenOptionMeta meta : optionsToShow) {
			ScreenOption cto = options.get(meta);
			if (cto == null) {
				cto = new ScreenOption();
				cto.setConservationArea(ca);
				cto.setResource(MissionScreenOptionMeta.MISSION_RESOURCE_ID);
				cto.setType(meta.name());
				options.put(meta, cto);
			}
		}

		members = HibernateManager.getActiveEmployees(ca, session);
		Collections.sort(members, new Comparator<Employee>() {
			@Override
			public int compare(Employee e1, Employee e2) {
				return Collator.getInstance().compare(e1.getFullLabel(), e2.getFullLabel());
			}
		});
	}
	
	@Override
	protected Map<MissionScreenOptionMeta, ScreenOption> getOptionsMap() {
		return options;
	}

	@Override
	protected MissionScreenOptionMeta[] getOptionsToShow() {
		return optionsToShow;
	}

	@Override
	protected Map<MissionScreenOptionMeta, ScreenOptionComposite> buildOptionComposites(Composite infoInnerPanel, IScreenOptionChangeListener listener) {
		Map<MissionScreenOptionMeta, ScreenOptionComposite> screenComposites = new HashMap<MissionScreenOptionMeta, ScreenOptionComposite>();
		ScreenOptionComposite soc;

		soc = new TextScreenOptionComposite(infoInnerPanel, options.get(MissionScreenOptionMeta.COMMENT), metaScreenLabelProvider.getText(MissionScreenOptionMeta.COMMENT));
		soc.addScreenOptionListener(listener);
		screenComposites.put(MissionScreenOptionMeta.COMMENT, soc);

		soc = new MembersScreenOptionComposite(infoInnerPanel, options, members);
		soc.addScreenOptionListener(listener);
		screenComposites.put(MissionScreenOptionMeta.MEMBERS, soc);
		screenComposites.put(MissionScreenOptionMeta.LEADER, soc);
		
		return screenComposites;
	}

	@Override
	protected LabelProvider getMetaTypeLabelProvider() {
		return metaScreenLabelProvider;
	}

	private class MissionMetaScreenLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof MissionScreenOptionMeta) {
				MissionScreenOptionMeta i = (MissionScreenOptionMeta)element;
				return SurveyMetaLabelUtil.getLabel(i);
			}
			return super.getText(element);
		}
	}
}
