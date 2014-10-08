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
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;

/**
 * Sampling unit attribute delete advisor.  Cannot delete
 * sampling unit attribute if it is used by a survey design.
 * @author Emily
 *
 */
public class SamplingUnitAttributeDeleteAdvisor implements IDeleteAdvisor {

	@SuppressWarnings("unchecked")
	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof SamplingUnitAttribute)){
			return Messages.MissionAttributeListItemDeleteAdvisor_InvalidObject;
		}
		
		SamplingUnitAttribute sua = (SamplingUnitAttribute)object;
		
		//find missions which use this attribute
		List<SurveyDesignSamplingUnitAttribute> designs = session.createCriteria(SurveyDesignSamplingUnitAttribute.class)
				.add(Restrictions.eq("id.samplingUnitAttribute", sua)).list(); //$NON-NLS-1$
		if (designs.size() == 0){
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		for (SurveyDesignSamplingUnitAttribute d : designs){
			sb.append(d.getSurveyDesign().getName() + ", "); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		return  MessageFormat.format(
				Messages.SamplingUnitAttributeDeleteAdvisor_CannotDeleteAttribute,
				 new Object[]{sb.toString()});
	}

}
