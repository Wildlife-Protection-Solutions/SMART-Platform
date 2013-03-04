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
package org.wcs.smart.intelligence.query;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.query.parser.AbstractEmptyPatrolQueryOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOptionType;

/**
 * Intelligence option to contribute to Patrol Query Filter
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligencePatrolQueryOption extends AbstractEmptyPatrolQueryOption {
	
	public static final String KEY = BOOLEAN_CONTRIBUTION_KEY_PREFIX + "intelligence"; //$NON-NLS-1$

	@Override
	public String getGuiName() {
		return Messages.IntelligencePatrolQueryOption_Name;
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
		return PatrolQueryOptionType.BOOLEAN;
	}

	@Override
	public Class<?> getPatrolAttributeClass() {
		return Patrol.class;
	}

	@Override
	public Image getImage() {
		return IntelligencePlugIn.getDefault().getImageRegistry().get(IntelligencePlugIn.INTELLIGENCE_ICON);
	}
	
}
