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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.patrol.query.model.IExtensionOption;
import org.wcs.smart.patrol.query.parser.IExtensionGroupBy;
import org.wcs.smart.patrol.query.parser.IGroupByPatrolContribution;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;

/**
 * Intelligence patrol gorup by contribution item.
 * 
 * @author Emily
 *
 */
public class IntelligenceGroupByPatrolContribution implements
		IGroupByPatrolContribution {

	public static final List<ListItem> SUPPORTEDVALUES = new ArrayList<ListItem>();
	static{
		SUPPORTEDVALUES.add(new ListItem(null, Messages.IntelligenceGroupByPatrolContribution_MotivatedByIntelligenceLabel, "m")); //$NON-NLS-1$
		SUPPORTEDVALUES.add(new ListItem(null, Messages.IntelligenceGroupByPatrolContribution_NotMotivatedByIntelligenceLabel, "nm")); //$NON-NLS-1$
	}
	
	private static final IExtensionOption GBOPTION = new IExtensionOption() {
		
		@Override
		public String getName() {
			return Messages.IntelligenceGroupByPatrolContribution_ContributionName;
		}
		
		@Override
		public Image getImage() {
			return IntelligencePlugIn.getDefault().getImageRegistry().get(IntelligencePlugIn.INTELLIGENCE_ICON);
		}
		
		@Override
		public DropItem asDropItem() {
			return new IntelligenceGroupByDropItem();
		}
	};
	
	
	
	public IntelligenceGroupByPatrolContribution() {
	}

	
	/**
	 * Supports group bys of the form "patrol:contribution:intelligence"
	 */
	@Override
	public IExtensionGroupBy createGroupBy(String key) {
		String[] bits = key.split(":"); //$NON-NLS-1$
		if (bits[2].equals("intelligence")){ //$NON-NLS-1$
			return new PatrolIntelligenceGroupBy();
		}
		return null;
	}

	@Override
	public IExtensionOption getOption() {
		return GBOPTION;
	}

	@Override
	public void addGroupBySql(IGroupBy groupBy, StringBuilder fromSql,
			StringBuilder groupBySql, StringBuilder groupByInnerSql,
			IValueItem value, ConservationAreaFilter caFilter, int itemCnt,
			IQueryEngine engine)
			throws SQLException {
		
		if (!(groupBy instanceof PatrolIntelligenceGroupBy)){
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

}
