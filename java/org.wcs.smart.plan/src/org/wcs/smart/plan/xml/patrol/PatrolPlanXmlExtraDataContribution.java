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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.xml.external.IConvertedExtraData;
import org.wcs.smart.patrol.xml.external.IXmlExtraDataContribution;
import org.wcs.smart.patrol.xml.model.ExtraDataStringKeyType;
import org.wcs.smart.patrol.xml.model.ExtraDataType;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.model.Plan;

/**
 * Plan contribution for Patrol module to provide ability to 
 * Export/Import to/from XML file plan related data.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolPlanXmlExtraDataContribution implements IXmlExtraDataContribution {

	static final String PLAN_TYPE = "plan"; //$NON-NLS-1$

	static final String PLAN_ID_KEY = "id"; //$NON-NLS-1$

	@Override
	public List<ExtraDataType> exportData(Patrol patrol) throws Exception {
		Session session = HibernateManager.openSession();
		try {
			List<ExtraDataType> result = new ArrayList<ExtraDataType>();
			//plan to which the patrol belong
			Plan plan = PlanHibernateManager.getPlanForPatrol(patrol, session);
			if (plan != null) {
				ExtraDataType planData = new ExtraDataType();
				planData.setType(PLAN_TYPE);
				
				ExtraDataStringKeyType idKey = new ExtraDataStringKeyType();
				idKey.setKey(PLAN_ID_KEY);
				idKey.setValue(plan.getId());
				planData.getStringKey().add(idKey);
				
				result.add(planData);
			}
			return result;
		} finally {
			session.close();
		}
	}

	@Override
	public IConvertedExtraData fromXml(List<ExtraDataType> extraDataList) {
		return new ConvertedPlanExtraData(extraDataList);
	}
}
