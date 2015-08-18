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
package org.wcs.smart.intelligence.xml.patrol;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.xml.external.IConvertedExtraData;
import org.wcs.smart.patrol.xml.external.IXmlExtraDataContribution;
import org.wcs.smart.patrol.xml.model.ExtraDataDateKeyType;
import org.wcs.smart.patrol.xml.model.ExtraDataLabelKeyType;
import org.wcs.smart.patrol.xml.model.ExtraDataType;
import org.wcs.smart.patrol.xml.model.LabelType;
import org.wcs.smart.util.SmartUtils;

/**
 * Intelligence contribution for Patrol module to provide ability to 
 * Export/Import to/from XML file intelligence related data.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolXmlExtraDataContribution implements IXmlExtraDataContribution {
	
	static final String MOTIVATION_TYPE = "motivation_intelligence"; //$NON-NLS-1$

	static final String REPORTED_TYPE = "reported_intelligence"; //$NON-NLS-1$

	static final String NAME_KEY = "name"; //$NON-NLS-1$
	static final String RECEIVED_DATE_KEY = "receivedDate"; //$NON-NLS-1$
	
	@Override
	public List<ExtraDataType> exportData(Patrol patrol) throws Exception {
		Session session = HibernateManager.openSession();
		try {
			List<ExtraDataType> result = new ArrayList<ExtraDataType>();
			//motivation intelligence
			List<Intelligence> motivationList = IntelligenceHibernateManager.getMotivatedIntelligences(patrol, session);
			for (Intelligence intelligence : motivationList) {
				ExtraDataType data = fromIntelligence(intelligence);
				data.setType(MOTIVATION_TYPE);
				result.add(data);
			}

			//reported intelligence
			List<Intelligence> reportedList = IntelligenceHibernateManager.getReportedIntelligences(patrol, session);
			for (Intelligence intelligence : reportedList) {
				ExtraDataType data = fromIntelligence(intelligence);
				data.setType(REPORTED_TYPE);
				result.add(data);
			}

			return result;
		} finally {
			session.close();
		}
	}

	private ExtraDataType fromIntelligence(Intelligence intelligence) throws Exception {
		ExtraDataType data = new ExtraDataType();

		ExtraDataLabelKeyType nameKey = new ExtraDataLabelKeyType();
		for (Label label : intelligence.getNames()) {
			LabelType labelType = new LabelType();
			labelType.setLanguageCode(label.getLanguage().getCode());
			labelType.setValue(label.getValue());
			nameKey.getLabel().add(labelType);
		}
		nameKey.setKey(NAME_KEY);
		data.getLabelKey().add(nameKey);
		
		ExtraDataDateKeyType recDateKey = new ExtraDataDateKeyType();
		recDateKey.setKey(RECEIVED_DATE_KEY);
		recDateKey.setValue(SmartUtils.toXmlDate(intelligence.getReceivedDate()));
		data.getDateKey().add(recDateKey);
		
		return data;
	}
	
	@Override
	public IConvertedExtraData fromXml(List<ExtraDataType> extraDataList) {
		return new ConvertedIntelligenceExtraData(extraDataList);
	}
}
