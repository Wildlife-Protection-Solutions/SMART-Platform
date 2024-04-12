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

import java.sql.SQLException;
import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.patrol.query.ext.IExtensionGroupBy;
import org.wcs.smart.patrol.query.ext.IExtensionGroupByViewer;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IGroupByViewer;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Intelligence patrol gorup by contribution item.
 * 
 * @author Emily
 *
 */
public class IntelligenceGroupByPatrolContribution implements IExtensionGroupByViewer {
	
	private IntelligencePatrolQueryOption op = new IntelligencePatrolQueryOption();
	public IntelligenceGroupByPatrolContribution() {
	}

	@Override
	public void addGroupBySql(IGroupBy groupBy, StringBuilder fromSql,
			StringBuilder groupBySql, StringBuilder groupByInnerSql,
			IValueItem value, ConservationAreaFilter caFilter, int itemCnt,
			IQueryEngine engine)
			throws SQLException {
		
		if (!(groupBy instanceof IntelligencePatrolGroupBy)){
			return;
		}
		
		String intelPrefix = "intel_" + itemCnt; //$NON-NLS-1$
		groupBySql.append("i_" + itemCnt); //$NON-NLS-1$
		groupByInnerSql.append(" CASE WHEN " + intelPrefix + ".patrol_uuid IS NULL THEN 'nm' else 'm' END as i_" + itemCnt); //$NON-NLS-1$ //$NON-NLS-2$
		fromSql.append(" LEFT JOIN "); //$NON-NLS-1$
		fromSql.append(" smart.patrol_intelligence " + intelPrefix); //$NON-NLS-1$
		fromSql.append(" on "); //$NON-NLS-1$
		fromSql.append("temp.p_uuid = " + intelPrefix + ".patrol_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		
	}


	@Override
	public String getName() {
		return op.getGuiName(Locale.getDefault());
	}


	@Override
	public DropItem asDropItem() {
		return new IntelligenceGroupByDropItem();
	}


	@Override
	public Image getImage() {
		return IntelligencePlugIn.getDefault().getImageRegistry().get(IntelligencePlugIn.INTELLIGENCE_ICON);
	}


	@Override
	public Class<? extends IExtensionGroupBy> getGroupByClass() {
		return IntelligencePatrolGroupBy.class;
	}


	@Override
	public DropItem[] getDropItems(IExtensionGroupBy groupBy, Session session) {
		if (!(groupBy instanceof IntelligencePatrolGroupBy)){
			return null;
		}
		DropItem di = new IntelligenceGroupByDropItem();
		return new DropItem[]{di};
	}

	@Override
	public IGroupByViewer<? extends IGroupBy> createViewer(IGroupBy groupBy) {
		if (groupBy instanceof IntelligencePatrolGroupBy){
			return new PatrolIntelligenceGroupByViewer((IntelligencePatrolGroupBy)groupBy);
		}
		return null;
	}

}
