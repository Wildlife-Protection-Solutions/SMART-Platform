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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceSource;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.xml.external.IConvertedExtraData;
import org.wcs.smart.patrol.xml.model.ExtraDataDateKeyType;
import org.wcs.smart.patrol.xml.model.ExtraDataLabelKeyType;
import org.wcs.smart.patrol.xml.model.ExtraDataType;
import org.wcs.smart.patrol.xml.model.LabelType;

/**
 * Wrapper for intelligence extra-data conversion (from XML) result.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ConvertedIntelligenceExtraData implements IConvertedExtraData {

	private List<String> warnings = new ArrayList<String>();
	
	private List<Intelligence> motivationList = new ArrayList<Intelligence>();
	private List<Intelligence> reportedList = new ArrayList<Intelligence>();
	
	public ConvertedIntelligenceExtraData(List<ExtraDataType> extraDataList) {
		Intelligence intelligence;
		for (ExtraDataType extraDataType : extraDataList) {
			if(PatrolXmlExtraDataContribution.MOTIVATION_TYPE.equals(extraDataType.getType())) {
				intelligence = fetchReferedIntelligence(extraDataType, Messages.ConvertedIntelligenceExtraData_MotivationIntelligence);
				if (intelligence != null) {
					motivationList.add(intelligence);
				}
			} else if (PatrolXmlExtraDataContribution.REPORTED_TYPE.equals(extraDataType.getType())) {
				intelligence = fetchReferedIntelligence(extraDataType, Messages.ConvertedIntelligenceExtraData_ReportedIntelligence);
				if (intelligence != null && validateReportedIntelligence(intelligence)) {
					reportedList.add(intelligence);
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
		IntelligenceHibernateManager.savePatrolIntelligences(session, patrol, motivationList);
		for (Intelligence i : reportedList) {
			i.setPatrol(patrol);
			session.saveOrUpdate(i);
		}
		return true;
	}

	private Intelligence fetchReferedIntelligence(ExtraDataType dataType, String moduleMsgLabel) {
		moduleMsgLabel += " "; //$NON-NLS-1$
		//check date
		if (dataType.getLabelKey().isEmpty()) {
			warnings.add(moduleMsgLabel + Messages.ConvertedIntelligenceExtraData_NameKeyMissing);
			return null;
		}
		String name = null;
		Language language = SmartDB.getCurrentConservationArea().getDefaultLanguage();
		String codeWarnLabel = language.getCode();
		for (ExtraDataLabelKeyType labelKeyType : dataType.getLabelKey()) {
			if (PatrolXmlExtraDataContribution.NAME_KEY.equals(labelKeyType.getKey())) {
				name = findNameInLanguage(language, labelKeyType.getLabel());
				if (name == null) {
					language = SmartDB.getCurrentLanguage();
					if (!codeWarnLabel.equals(language.getCode())) {
						codeWarnLabel += "; " + language.getCode(); //$NON-NLS-1$
					}
					name = findNameInLanguage(language, labelKeyType.getLabel());
				}
			}
		}
		if (name == null) {
			warnings.add(moduleMsgLabel + MessageFormat.format(Messages.ConvertedIntelligenceExtraData_CannotDetermineName, codeWarnLabel));
			return null;
		}
		

		//check date
		if (dataType.getDateKey().isEmpty()) {
			warnings.add(moduleMsgLabel + MessageFormat.format(Messages.ConvertedIntelligenceExtraData_RecievedDateKeyMissing, name));
			return null;
		}
		Date receivedDate = null;
		for (ExtraDataDateKeyType dateType : dataType.getDateKey()) {
			if (PatrolXmlExtraDataContribution.RECEIVED_DATE_KEY.equals(dateType.getKey())) {
				XMLGregorianCalendar cal = dateType.getValue();
				if (cal != null) {
					receivedDate = cal.toGregorianCalendar().getTime();
					break;
				}
			}
		}
		if (receivedDate == null) {
			warnings.add(moduleMsgLabel + MessageFormat.format(Messages.ConvertedIntelligenceExtraData_CannotDetermineReceivedDate, name));
			return null;
		}
		
		//name and received date are known and contain valid data
		//try to fetch this intelligence from database
		Session session = HibernateManager.openSession();
		try {
			Query query = session.createQuery("SELECT i FROM Intelligence i, Label lbl WHERE i.receivedDate = :receivedDate AND lbl.id.element.uuid = i.uuid AND lbl.value = :name AND lbl.id.language = :language"); //$NON-NLS-1$
			query.setParameter("receivedDate", receivedDate); //$NON-NLS-1$
			query.setParameter("name", name); //$NON-NLS-1$
			query.setParameter("language", language); //$NON-NLS-1$
			@SuppressWarnings("unchecked")
			List<Intelligence> list = query.list();
			if (list.isEmpty()) {
				warnings.add(moduleMsgLabel + MessageFormat.format(Messages.ConvertedIntelligenceExtraData_IntelligenceNotFound, name, DateFormat.getDateInstance(DateFormat.LONG).format(receivedDate)));
				return null;
			}
			if (list.size() > 1) {
				warnings.add(moduleMsgLabel + MessageFormat.format(Messages.ConvertedIntelligenceExtraData_MultipleIntelligenceFound, name, DateFormat.getDateInstance(DateFormat.LONG).format(receivedDate)));
			}
			return list.get(0);
		} finally {
			session.close();
		}
	}

	private static String findNameInLanguage(Language language, List<LabelType> names) {
		String code = language.getCode();
		String name = null;
		for (LabelType labelType : names) {
			if (code.equals(labelType.getLanguageCode())) {
				name = labelType.getValue();
				break;
			}
		}
		return name;
	}
	
	private boolean validateReportedIntelligence(Intelligence intelligence) {
		if (IntelligenceSource.isPatrolSource(intelligence.getSource())) {
			if (intelligence.getPatrol() != null) {
				warnings.add(MessageFormat.format(Messages.ConvertedIntelligenceExtraData_IntelligencePatrolIsSet, intelligence.getName(), DateFormat.getDateInstance(DateFormat.LONG).format(intelligence.getReceivedDate())));
				return false;
			}
		} else {
			warnings.add(MessageFormat.format(Messages.ConvertedIntelligenceExtraData_NotPatrolReported, intelligence.getName(), DateFormat.getDateInstance(DateFormat.LONG).format(intelligence.getReceivedDate())));
			return false;
		}
		return true;
	}
	
}
