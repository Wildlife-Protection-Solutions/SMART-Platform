/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.patrol.report.parameter;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.birt.parameter.AbstractNamedSmartBirtParameter;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.patrol.model.IPatrolLabelProvider;
import org.wcs.smart.patrol.model.PatrolMandate;

/**
 * Patrol Mandate list BIRT Parameter
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class MandateParameter extends AbstractNamedSmartBirtParameter {

	public MandateParameter() {
	}

	@Override
	public String getName(Locale l) {
		return SmartContext.INSTANCE.getClass(IPatrolLabelProvider.class).getLabel(IPatrolLabelProvider.PATROLMANDATE_TABLENAME_KEY, l);
	}

	@Override
	public String getKey() {
		return "patrol:mandate"; //$NON-NLS-1$
	}

	@Override
	public List<String> getValues(Session session, Collection<ConservationArea> cas, Locale l) {
		return getValues(PatrolMandate.class, session, cas, l);
	}

}
