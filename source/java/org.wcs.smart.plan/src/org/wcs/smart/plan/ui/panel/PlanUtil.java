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

import java.time.LocalDate;

import org.wcs.smart.plan.model.Plan;

/**
 * Utility class for {@link Plan} object
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PlanUtil {

	/**
	 * Checks if plans from/to dates are in range of parents from/to dates
	 * @param plan
	 * @param parent
	 * @return boolean
	 */
	public static boolean isDatesInParentRange(Plan plan, Plan parent) {
		if (plan == null || parent == null) {
			return true;
		}

		LocalDate start = plan.getStartDate();
		if (start == null) {
			return true; //nothing to validate, data incomplete
		}
		LocalDate end = plan.getEndDate() != null ? plan.getEndDate() : start;
		
		LocalDate parentStart = parent.getStartDate();
		LocalDate parentEnd = parent.getEndDate() != null ? parent.getEndDate() : parentStart;

		return start.compareTo(parentStart) >= 0 && end.compareTo(parentEnd) <= 0;
	}

	/**
	 * Shifts plan dates to fit in range of parents from/to dates
	 * @param plan
	 * @param parent
	 */
	public static void fitDatesInParentRange(Plan plan, Plan parent) {
		if (plan == null || parent == null) {
			return;
		}

		LocalDate parentStart = parent.getStartDate();
		LocalDate parentEnd = parent.getEndDate() != null ? parent.getEndDate() : parentStart;

		LocalDate start = plan.getStartDate();
		LocalDate end = plan.getEndDate() != null ? plan.getEndDate() : start;

		if (start != null) {
			//both dates must be not later than parent end date
			if (start.compareTo(parentEnd) > 0) {
				start = parentEnd;
			} 
			if (end.compareTo(parentEnd) > 0) {
				end = parentEnd;
			}
			//both dates must be not earlier than parent start date
			if (start.compareTo(parentStart) < 0) {
				start = parentStart;
			} 
			if (end.compareTo(parentStart) < 0) {
				end = parentStart;
			}
			
			plan.setStartDate(start);
			plan.setEndDate(end);
		}
	}
	
}
