/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol.query;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.cybertracker.patrol.model.IPatrolCyberTrackerLabelProvider;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.query.model.AbstractEmptyPatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;

/**
 * SMART Mobile device ID option to contribute to Patrol Query Filter
 * 
 * @author egouge
 * @since 8.1.0
 */
public class MobileDeviceIdPatrolQueryOption extends AbstractEmptyPatrolQueryOption {

	public static final String KEY = STRING_CONTRIBUTION_KEY_PREFIX + "smdeviceid"; //$NON-NLS-1$

	@Override
	public String getGuiName(Locale l) {
		return SmartContext.INSTANCE.getClass(IPatrolCyberTrackerLabelProvider.class).getLabel(KEY, l);
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getColumnName() {
		//This value is not user for this particular contribution 
		//as there is no corresponding column in patrol table
		return ""; //$NON-NLS-1$
	}

	@Override
	public PatrolQueryOptionType getType() {
		return PatrolQueryOptionType.KEY;
	}

	@Override
	public Class<?> getPatrolAttributeClass() {
		return PatrolLeg.class;
	}

	
}
