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
package org.wcs.smart.patrol;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;

/**
 * Template cloner than copies patrol information from the
 * template conservation area to the new conservation area.
 * <p>Data copied includes: teams, mandates, options, types
 * and transport types.</p>
 * 
 * @author Emily
 *
 */
public class PatrolTemplateCloner implements
		IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.PatrolTemplateCloner_ProgressPatrols, 3);
		try{
			//	need to clone: team, mandate,  types, transport types
			monitor.subTask(Messages.PatrolTemplateCloner_ProgressCopyMandates);
			cloneMandates(engine);
			monitor.worked(1);
			monitor.subTask(Messages.PatrolTemplateCloner_ProgressCopyTeams);
			cloneTeams(engine);
			monitor.worked(1);
			monitor.subTask(Messages.PatrolTemplateCloner_ProgressCopyTypes);
			clonePatrolTypes(engine);
			monitor.worked(1);
		}finally{
			monitor.done();
		}
	}

	/*
	 * clone patrol mandates
	 */
	private void cloneMandates(ConservationAreaClonerEngine engine){
		List<PatrolMandate> mandates = PatrolHibernateManager.getMandates(engine.getTemplateCa(), engine.getSession());
		for (PatrolMandate m : mandates){
			PatrolMandate clone = new PatrolMandate();
			clone.setConservationArea(engine.getNewCa());
			clone.setIsActive(m.getIsActive());
			clone.setKeyId(m.getKeyId());
			engine.copyLabels(m, clone);
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(m,clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone patrol teams
	 */
	private void cloneTeams(ConservationAreaClonerEngine engine) throws Exception{
		List<Team> teams = PatrolHibernateManager.getTeams(engine.getTemplateCa(), engine.getSession());
		for (Team team : teams){
			Team clone = new Team();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(team.getKeyId());
			clone.setIsActive(team.getIsActive());
			if (team.getMandate() != null){
				clone.setMandate((PatrolMandate)engine.getNewConservationItem(team.getMandate()));
			}
			engine.copyLabels(team, clone);
			engine.copyDescriptions(team, clone);
			engine.addConservationItemMapping(team, clone);
			engine.getSession().save(clone);
		}
		engine.getSession().flush();
	}


	/*
	 * clone patrol types & transport types
	 */
	private void clonePatrolTypes(ConservationAreaClonerEngine engine){
		@SuppressWarnings("unchecked")
		List<PatrolType> types = engine.getSession().createCriteria(PatrolType.class).add(Restrictions.eq("id.conservationArea", engine.getTemplateCa())).list(); //$NON-NLS-1$
		engine.getSession().flush();
		
		for (PatrolType t : types){
			PatrolType newt = new PatrolType();
			newt.setConservationArea(engine.getNewCa());
			newt.setIsActive(t.getIsActive());
			newt.setMaxSpeed(t.getMaxSpeed());
			newt.setType(t.getType());
			newt.setTransportTypes(new ArrayList<PatrolTransportType>());
			
			engine.getSession().save(newt);
			engine.getSession().flush();
			
			for (PatrolTransportType pt : t.getTransportTypes()){
				PatrolTransportType clone = new PatrolTransportType();
				clone.setConservationArea(engine.getNewCa());
				clone.setIsActive(pt.getIsActive());
				clone.setKeyId(pt.getKeyId());
				clone.setPatrolType(newt.getType());
				engine.copyLabels(pt, clone);				
				newt.getTransportTypes().add(clone);
				
				engine.getSession().save(clone);					
				engine.addConservationItemMapping(pt, clone);
			}
			
		}
		engine.getSession().flush();
	}
	
}
