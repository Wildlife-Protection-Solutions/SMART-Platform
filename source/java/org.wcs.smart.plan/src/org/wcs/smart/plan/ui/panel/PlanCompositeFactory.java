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
package org.wcs.smart.plan.ui.panel;

import org.eclipse.swt.widgets.Composite;


/**
 * IntelligenceCompositeFactory
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PlanCompositeFactory {

	private static PlanCompositeFactory instance;
	
	private PlanCompositeFactory() {}

	public static PlanCompositeFactory getInstance() {
		if (instance == null) {
			instance = new PlanCompositeFactory();
		}
		return instance;
	}

	public PlanComposite createComposite(Composite parent, int style, PanelType type) {
		switch (type) {
		case STARTDATE:    return new PlanDatesComposite(parent, style);
		case ENDDATE:    return new PlanDatesComposite(parent, style);
		case TARGETS:    return new PlanTargetComposite(parent, style);
		case PLANID:    return new PlanIdNameDescComposite(parent, style, true);
		case TYPE:    return new PlanTypeEmployeesComposite(parent, style);
		case STATION:    return new PlanStationTeamComposite(parent, style);
		case PLANPARENTID:    return new PlanParentIdComposite(parent, style);
		
		default: throw new UnsupportedOperationException(type + "is not supported"); //$NON-NLS-1$
		}
	}

	public String getTitle(PanelType type) {
		switch (type) {
		case STARTDATE:    return "Plan Start Date";
		case ENDDATE:    return "Plan End Date";
		case TARGETS:    return "Plan Targets";
		case PLANID:    return "Plan ID, Name, Desc";
		case TYPE: return "Plan Type";
		case STATION: return "Plan Station";
		case PLANPARENTID: return "Parent ID";
		default: throw new UnsupportedOperationException(type + "is not supported"); //$NON-NLS-1$
		}
	}

	/**
	 * The supported panels.
	 */
	public enum PanelType {
		PLANID,
		TYPE,
		PLANPARENTID,
		STATION,
		STARTDATE,
		ENDDATE,
		TARGETS;
	}

}
