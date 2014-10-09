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
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;

/**
 * Delete advisor for sampling unit attribute list items.
 * <p>Validates that the list item is not referenced in any sampling unit.</p>
 * @author Emily
 *
 */
public class SamplingUnitAttributeListItemDeleteAdvisor implements IDeleteAdvisor {

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof SamplingUnitAttributeListItem)){
			return Messages.SamplingUnitAttributeListItemDeleteAdvisor_InvalidObjectError;
		}
		
		SamplingUnitAttributeListItem ma = (SamplingUnitAttributeListItem)object;
		
		//find missions which use this attribute
		@SuppressWarnings("unchecked")
		List<SamplingUnitAttributeValue> suattributes = session.createCriteria(SamplingUnitAttributeValue.class)
				.add(Restrictions.eq("attributeListItem", ma)).list(); //$NON-NLS-1$
		if (suattributes.size() == 0){
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		for (SamplingUnitAttributeValue d : suattributes){
			sb.append(d.getSamplingUnit().getId() + " [" + d.getSamplingUnit().getSurveyDesign().getName() + "], "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		return  MessageFormat.format(
				Messages.SamplingUnitAttributeListItemDeleteAdvisor_CannotDelete, new Object[]{sb.toString()});
		
	}

}
