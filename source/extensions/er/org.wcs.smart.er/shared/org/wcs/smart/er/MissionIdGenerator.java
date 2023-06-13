/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.er;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.er.model.IErLabelProvider;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.hibernate.QueryFactory;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Mission/Survey ID Generator
 * 
 * @author Emily
 *
 */
public enum MissionIdGenerator {
	
	INSTANCE;
	
	private static NumberFormat MISSION_ID_FORMATTER = new DecimalFormat("000000"); //$NON-NLS-1$

	/**
	 * Computes the next mission id; requires the survey & surveyDesign to
	 * be set so conservation area id can be determined. 
	 * 
	 * @param mission mission 
	 * @param s active session 
	 * 
	 * @return next id for given mission
	 */
	public String generateMissionId(Mission mission,Session s) {
		
		StringBuilder sb = new StringBuilder();

		if (mission.getSurvey() != null && mission.getSurvey().getSurveyDesign() != null) {
			sb.append(mission.getSurvey().getSurveyDesign().getConservationArea().getId());
		}

		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<Mission> c = cb.createQuery(Mission.class);
		Root<Mission> from = c.from(Mission.class);
		c.where(cb.like(from.get("id"), sb.toString() + "%")); //$NON-NLS-1$ //$NON-NLS-2$
		c.orderBy(cb.desc(from.get("id"))); //$NON-NLS-1$
		List<Mission> results = s.createQuery(c).getResultList();
		
		long idNumber = 0;
		for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
			Mission m =  (Mission) iterator.next();
			String localId = m.getId(); 
			try {
				int offset = localId.lastIndexOf('_') + 1;
				idNumber = Integer.parseInt(localId.substring(offset));
				break;
			} catch (Exception ex) {
				// not of the form CAID_# skip this one
			}
		}
		sb.append("_"); //$NON-NLS-1$
		//sb.append("M"); //$NON-NLS-1$  //not so good for non-english language installs. Just going to leave it without a prefix I guess.
		idNumber = (idNumber + 1) % 1000000;
		if (idNumber <= 0) {
			idNumber = 1;
		}
		sb.append(MISSION_ID_FORMATTER.format(idNumber));

		return sb.toString();
	}
	
	public String generateSurveyId(Survey survey, Session s, Locale l) {
		if (survey.getSurveyDesign() == null) return SmartContext.INSTANCE.getClass(IErLabelProvider.class).getLabel(IErLabelProvider.SURVEY_NAME, l);
		
		Long cnt = QueryFactory.buildCountQuery(s, Survey.class, 
				new Object[] {"surveyDesign", survey.getSurveyDesign()}); //$NON-NLS-1$
		cnt++;
		return MessageFormat.format("{0} {1}",SmartContext.INSTANCE.getClass(IErLabelProvider.class).getLabel(IErLabelProvider.SURVEY_NAME, l), cnt); //$NON-NLS-1$
		
	}
}
