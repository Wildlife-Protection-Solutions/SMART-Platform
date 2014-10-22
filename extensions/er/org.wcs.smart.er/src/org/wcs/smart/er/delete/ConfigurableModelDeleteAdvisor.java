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
package org.wcs.smart.er.delete;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Delete advisor for deleting configurable model.
 * 
 * @author Emily
 *
 */
public class ConfigurableModelDeleteAdvisor implements IDeleteAdvisor {

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof ConfigurableModel)){
			return Messages.ConfigurableModelDeleteAdvisor_InvalidType;
		}
		ConfigurableModel cm = (ConfigurableModel)object;
		
		//check survey design for references
		Long cmCnt = (Long) session.createCriteria(SurveyDesign.class)
				.add(Restrictions.eq("configurableModel", cm)) //$NON-NLS-1$
				.setProjection(Projections.rowCount())
				.list().get(0);

		if (cmCnt > 0){
			return MessageFormat.format(
					Messages.ConfigurableModelDeleteAdvisor_SurveyCnt, new Object[]{cmCnt});
		}
		return null;
	}

}
