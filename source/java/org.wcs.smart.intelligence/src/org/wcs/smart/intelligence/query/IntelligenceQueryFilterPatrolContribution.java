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

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.parser.IPatrolQueryOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOptionType;
import org.wcs.smart.query.ui.IQueryFilterPatrolContribution;

/**
 * Intelligence contribution for the Patrol section of a "Query Filter" view.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceQueryFilterPatrolContribution implements IQueryFilterPatrolContribution {

	@Override
	public List<IPatrolQueryOption> getOptions() {
		//TODO: implement real object!!!
		IPatrolQueryOption testOption = new IPatrolQueryOption() {

			@Override
			public boolean isEmployeeItem() {
				return false;
			}

			@Override
			public String getGuiName() {
				return "Intelligence";
			}

			@Override
			public String getKey() {
				return "intelligence";
			}

			@Override
			public String getColumnName() {
				return "intelligence";
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
			public Class<?> getSourceClass() {
				return IntelligenceQueryFilterPatrolContribution.class;
			}

			@Override
			public Image getImage() {
				return IntelligencePlugIn.getDefault().getImageRegistry().get(IntelligencePlugIn.INTELLIGENCE_ICON);
			}
			
			@Override
			public String getName(Session session, byte[] uuid) {
				return "fake";
			}

			@Override
			public String[] getNames(Session session, byte[] uuid) {
				return new String[] {"fake"};
			}

			@Override
			public Object getObject(Session session, byte[] uuid) {
				return null;
			}

			@Override
			public List<ListItem> getValues(Session session, String[] keys) {
				return null;
			}

			@Override
			public List<ListItem> getAllActiveValues(Session session) {
				return null;
			}
			
		};
		return Arrays.asList(testOption);
	}

}
