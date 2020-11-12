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
package org.wcs.smart.plan.xml.patrol;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Synchronization;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.xml.external.IConvertedExtraData;
import org.wcs.smart.patrol.xml.model.ExtraDataStringKeyType;
import org.wcs.smart.patrol.xml.model.ExtraDataType;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.PatrolPlan;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.xml.PlanFromXml;

/**
 * Wrapper for plan extra-data conversion (from XML) result.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ConvertedPlanExtraData implements IConvertedExtraData {

	private List<String> warnings = new ArrayList<String>();

	private String planId;
	private Path tempDir;
	
	public ConvertedPlanExtraData(List<ExtraDataType> extraDataList, Path tempDir) {
		boolean found = false;
		planId = null;
		this.tempDir = tempDir;
		
		for (ExtraDataType extraDataType : extraDataList) {
			if(PatrolPlanXmlExtraDataContribution.PLAN_TYPE.equals(extraDataType.getType())) {
				if (found) {
					//if we are here this means that XML contains 2+ plans in extra data,
					//which is invalid as only one plan is allowed
					warnings.add(Messages.ConvertedPlanExtraData_ExtraRecordFound);
					break;
				}
				found = true;
				if (extraDataType.getStringKey().size() > 1) {
					warnings.add(Messages.ConvertedPlanExtraData_ExtraKeyFound);
					
				}
				for (ExtraDataStringKeyType eds : extraDataType.getStringKey()) {
					if (PatrolPlanXmlExtraDataContribution.PLAN_ID_KEY.equals(eds.getKey())) {
						planId = eds.getValue();
						break;
					}
				}
			}
		}
		
	}

	@Override
	public List<String> getWarnings() {
		return warnings;
	}

	@Override
	public boolean saveInTransaction(Session session, Patrol patrol) {
		
		//check plan file
		if (tempDir == null) return true; //no file
		Path planFile = tempDir.resolve(PatrolPlanXmlExtraDataContribution.PLAN_FILENAME);
		Plan plan = null;
		if (Files.exists(planFile)) {
			PlanFromXml xml = new PlanFromXml();
			xml.setAllDuplicateMessage(Messages.ConvertedPlanExtraData_AllPlansFound);

			try {
				if (xml.convertPlan(planFile)) {
					xml.doSave(session);
					plan = xml.getRootPlan();
					
					session.getTransaction().registerSynchronization(new Synchronization() {
						@Override
						public void beforeCompletion() {
						}
						
						@Override
						public void afterCompletion(int arg0) {
							xml.fireEvents();
						}
					});
				}
			}catch (Exception ex) {
				SmartPlanPlugIn.displayLog(Messages.ConvertedPlanExtraData_PlanImportError + ex.getMessage(), ex);
			}
		}
		
		//check plan id
		if (plan == null && planId != null) {
			plan = fetchReferedPlan(planId);
		}
		
		if (plan != null) {
			PatrolPlan pp = new PatrolPlan();
			pp.setPatrol(patrol);
			pp.setPlan(plan);
			session.saveOrUpdate(pp);
		}
		return true;
	}

	private Plan fetchReferedPlan(String id) {
		if (id == null) {
			warnings.add(Messages.ConvertedPlanExtraData_NoPlanReference);
			return null;
		}
		
		try(Session session = HibernateManager.openSession()) {
			List<Plan> plans = PlanHibernateManager.getPlansById(session, SmartDB.getCurrentConservationArea(), id);
			if (plans.isEmpty()) {
				warnings.add(MessageFormat.format(Messages.ConvertedPlanExtraData_PlanNotFound, id));
				return null;
			}
			if (plans.size() > 1) {
				warnings.add(MessageFormat.format(Messages.ConvertedPlanExtraData_MultiplePlansFound, id));
			}
			return plans.get(0);
		}
	}
}
