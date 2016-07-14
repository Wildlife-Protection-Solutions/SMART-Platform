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
package org.wcs.smart.connect.cybertracker;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.ConnectAlert;
import org.wcs.smart.connect.cybertracker.model.ConnectCtProperties;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
/**
 * Clones the Connect for CyberTracker data when creating
 * a new conservation area from a template.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectCtTemplateCloner implements IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		@SuppressWarnings("unchecked")
		List<ConfigurableModel> cmList = engine.getSession().createCriteria(ConfigurableModel.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).list(); //$NON-NLS-1$
		monitor.beginTask(Messages.ConnectCtTemplateCloner_TaskName, cmList.size());
		
		for (ConfigurableModel cm : cmList) {
			monitor.subTask(MessageFormat.format(Messages.ConnectCtTemplateCloner_CloneAlerts, cm.getName()));
			cloneAlertsForCm(engine, cm, monitor);
			cloneConnectCtProperties(engine, cm, monitor);
			monitor.worked(1);
		}
	}

	private void cloneAlertsForCm(ConservationAreaClonerEngine engine, ConfigurableModel cm, IProgressMonitor monitor) throws Exception {
		@SuppressWarnings("unchecked")
		List<ConnectAlert> alerts = engine.getSession().createCriteria(ConnectAlert.class).add(Restrictions.eq("model", cm)).list(); //$NON-NLS-1$
		if (!alerts.isEmpty()) {
			ConfigurableModel cmClone = (ConfigurableModel) engine.getNewConservationItem(cm);
			for (ConnectAlert a : alerts) {
				ConnectAlert clone = new ConnectAlert();
				clone.setModel(cmClone);
				clone.setAttrubute((CmAttribute)engine.getNewConservationItem(a.getAttrubute()));
				clone.setAlertItem(engine.getNewConservationItem(a.getAlertItem()));
				clone.setType(a.getType());
				clone.setLevel(a.getLevel());

				engine.getSession().save(clone);
			}
			engine.getSession().flush();
		}
	}

	private void cloneConnectCtProperties(ConservationAreaClonerEngine engine, ConfigurableModel cm, IProgressMonitor monitor) {
		@SuppressWarnings("unchecked")
		List<ConnectCtProperties> props = engine.getSession().createCriteria(ConnectCtProperties.class).add(Restrictions.eq("model", cm)).list(); //$NON-NLS-1$
		if (!props.isEmpty()) {
			ConfigurableModel cmClone = (ConfigurableModel) engine.getNewConservationItem(cm);
			for (ConnectCtProperties p : props) {
				ConnectCtProperties clone = new ConnectCtProperties();
				clone.setModel(cmClone);
				clone.setPingFrequency(p.getPingFrequency());
				clone.setDataFrequency(p.getDataFrequency());
				clone.setPingType(p.getPingType());
				engine.getSession().save(clone);
			}
			engine.getSession().flush();
		}
	}

}
