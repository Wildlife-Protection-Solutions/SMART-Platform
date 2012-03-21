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
package org.wcs.smart;

import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * This workbench advisor creates the window advisor, and specifies the
 * perspective id for the initial window.
 */
public class SmartWorkbenchAdvisor extends WorkbenchAdvisor {

	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		super.initialize(configurer);

		// don't save and restore workbench state
		configurer.setSaveAndRestore(false);
	}

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new SmartWorkbenchWindowAdvisor(configurer);
	}

	public String getInitialWindowPerspectiveId() {
		return DefaultPerspective.ID;
	}

	
//	@Override
//	public ContributionComparator getComparatorFor(String contributionType) {
//		if (contributionType.equals("property")){
//			return new ConservationPageComparator();
//		}
//		return new ContributionComparator();
//	}
}
//
///**
// * Comparator for ordering the conservation area property pages.
// *
// */
//class ConservationPageComparator extends ContributionComparator {
//	public int category(IComparableContribution c) {
//
//		if (c instanceof PreferenceNode) {
//			String id = ((PreferenceNode) c).getId();
//			if (CaPropertyPage.ID.equals(id)) {
//				return 1;
//			} else if (StationListPropertyPage.ID.equals(id)) {
//				return 2;
//			} else if (AgencyRankPropertyPage.ID.equals(id)) {
//				return 3;
//			} else if (EmployeePropertyPage.ID.equals(id)) {
//				return 4;
//			} else {
//				return super.category(c);
//			}
//		} else {
//			return super.category(c);
//		}
//	}
//}
