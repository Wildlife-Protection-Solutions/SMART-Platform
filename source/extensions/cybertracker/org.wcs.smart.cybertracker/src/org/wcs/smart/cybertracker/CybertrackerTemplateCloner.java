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
package org.wcs.smart.cybertracker;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ConfigurableModelCtPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.QueryFactory;
/**
 * Clones the cybertracker properties when creating
 * a new conservation area from a template.
 * 
 * @author Emily
 *
 */
public class CybertrackerTemplateCloner implements
		IConservationAreaTemplateCloner {

	public CybertrackerTemplateCloner() {
	}

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.CybertrackerTemplateCloner_CloningCtProfiles,2);
		
		List<CyberTrackerPropertiesProfile> profiles = 
				QueryFactory.buildQuery(engine.getSession(), CyberTrackerPropertiesProfile.class, "conservationArea", engine.getTemplateCa()).getResultList(); //$NON-NLS-1$
		
		SubMonitor sub = progress.split(1);
		sub.setWorkRemaining(profiles.size());
		for (CyberTrackerPropertiesProfile p : profiles) {
			progress.subTask(MessageFormat.format(Messages.CybertrackerTemplateCloner_Copying, p.getName()));

			CyberTrackerPropertiesProfile clone = new CyberTrackerPropertiesProfile();
			clone.setConservationArea(engine.getNewCa());
			engine.copyLabels(p, clone);
			clone.setDefault(p.isDefault());
			engine.getSession().persist(clone);
			engine.getSession().flush();
			engine.addConservationItemMapping(p, clone);
			
			for (CyberTrackerPropertiesProfileOption templateOption : p.getOptions().values()) {
				CyberTrackerPropertiesProfileOption newOption = new CyberTrackerPropertiesProfileOption();
				newOption.setProfile(clone);
				newOption.setOptionId(templateOption.getOptionId());
				newOption.setDoubleValue(templateOption.getDoubleValue());
				newOption.setIntegerValue(templateOption.getIntegerValue());
				newOption.setStringValue(templateOption.getStringValue());

				engine.getSession().persist(newOption);
				engine.getSession().flush();
			}
			
			cloneCmProfileMappings(engine, p, clone);
			
			sub.worked(1);
		}
		
		progress.subTask(Messages.CybertrackerTemplateCloner_CloningCtOptions);

		List<CyberTrackerPropertiesOption> list = 
				QueryFactory.buildQuery(engine.getSession(), CyberTrackerPropertiesOption.class, "conservationArea", engine.getTemplateCa()).getResultList(); //$NON-NLS-1$
		sub = progress.split(1);
		sub.setWorkRemaining(list.size());
		for (CyberTrackerPropertiesOption templateOption : list) {
			CyberTrackerPropertiesOption newOption = new CyberTrackerPropertiesOption();
			newOption.setConservationArea(engine.getNewCa());
			newOption.setOptionId(templateOption.getOptionId());
			newOption.setDoubleValue(templateOption.getDoubleValue());
			newOption.setIntegerValue(templateOption.getIntegerValue());
			newOption.setStringValue(templateOption.getStringValue());

			engine.getSession().persist(newOption);
			engine.getSession().flush();
			sub.worked(1);
		}
		
	}
	
	private void cloneCmProfileMappings(ConservationAreaClonerEngine engine, CyberTrackerPropertiesProfile sourceProfile, CyberTrackerPropertiesProfile clonedProfile) throws Exception {
		List<ConfigurableModelCtPropertiesProfile> toClone = 
				QueryFactory.buildQuery(engine.getSession(), ConfigurableModelCtPropertiesProfile.class, "profile", sourceProfile).getResultList(); //$NON-NLS-1$
		for (ConfigurableModelCtPropertiesProfile cmctp : toClone) {
			ConfigurableModelCtPropertiesProfile clone = new ConfigurableModelCtPropertiesProfile();
			clone.setModel((ConfigurableModel)engine.getNewConservationItem(cmctp.getModel()));
			clone.setProfile(clonedProfile);
			engine.getSession().persist(clone);
		}
		engine.getSession().flush();
	}

}
