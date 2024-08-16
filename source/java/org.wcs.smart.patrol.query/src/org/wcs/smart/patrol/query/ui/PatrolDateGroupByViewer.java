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
package org.wcs.smart.patrol.query.ui;

import java.util.List;
import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.patrol.query.model.PatrolEndMonthDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolEndQuarterDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolStartMonthDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolStartQuarterDateGroupBy;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.filter.date.DateGroupByViewer;
import org.wcs.smart.query.model.filter.date.QuarterDateGroupBy;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

/**
 * Group by viewer for patrol specific date group bys
 * @since 8.1.0
 */
public class PatrolDateGroupByViewer extends DateGroupByViewer {

	public PatrolDateGroupByViewer(DateGroupBy gb) {
		super(gb);
	}

	@Override
	public List<ListItem> getItems(Session session) {
		if (groupBy.getOption() instanceof PatrolStartMonthDateGroupBy || 
				groupBy.getOption() instanceof PatrolEndMonthDateGroupBy ) {
			return getMonthItems(session, groupBy.getDateFilter());
		} else if (groupBy.getOption() instanceof PatrolStartQuarterDateGroupBy || 
				groupBy.getOption() instanceof PatrolEndQuarterDateGroupBy ) {
			return getQuarterItems(session, groupBy.getDateFilter());
		}
		return super.getItems(session);
	}

	public String getText() {
		if (groupBy.getOption() instanceof PatrolStartMonthDateGroupBy || 
				groupBy.getOption() instanceof PatrolEndMonthDateGroupBy ||
				groupBy.getOption() instanceof PatrolStartQuarterDateGroupBy || 
				groupBy.getOption() instanceof PatrolEndQuarterDateGroupBy) {
			return SmartContext.INSTANCE.getClass(IQueryPatrolLabelProvider.class).getLabel(
					getGroupBy().getOption(),
					Locale.getDefault());			
		}
		return super.getText();
	}
	
	public Image getImage() {
		
		if (groupBy.getOption() instanceof PatrolStartMonthDateGroupBy ||
				groupBy.getOption() instanceof PatrolEndMonthDateGroupBy 
				) {
			return QueryPlugIn.getDefault().getImageRegistry()
					.get(QueryPlugIn.CALENDAR_MONTH_ICON);		
		}else if (groupBy.getOption() instanceof PatrolStartQuarterDateGroupBy ||
				groupBy.getOption() instanceof PatrolEndQuarterDateGroupBy) {
		
			return QueryPlugIn.getDefault().getImageRegistry()
					.get(QueryPlugIn.CALENDAR_QUARTER_ICON);
		}
		return super.getImage();
	}

	
	@Override
	public DropItem asDropItem(Session session) throws Exception {
		return PatrolDropItemFactory.INSTANCE.createDateGroupByDropItem(groupBy.getOption());
	}

}