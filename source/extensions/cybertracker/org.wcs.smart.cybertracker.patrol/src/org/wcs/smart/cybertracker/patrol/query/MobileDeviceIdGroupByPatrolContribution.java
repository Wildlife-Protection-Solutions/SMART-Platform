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

import java.sql.SQLException;
import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.patrol.query.ext.IExtensionGroupBy;
import org.wcs.smart.patrol.query.ext.IExtensionGroupByViewer;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IGroupByViewer;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;

/**
 * SMART Mobile Device ID by contribution item.
 * 
 * @author Emily
 * @since 8.1.0
 *
 */
public class MobileDeviceIdGroupByPatrolContribution implements IExtensionGroupByViewer {
	
	private MobileDeviceIdPatrolQueryOption op = new MobileDeviceIdPatrolQueryOption();
	
	public MobileDeviceIdGroupByPatrolContribution() {
	}

	@Override
	public void addGroupBySql(IGroupBy groupBy, StringBuilder fromSql,
			StringBuilder groupBySql, StringBuilder groupByInnerSql,
			IValueItem value, ConservationAreaFilter caFilter, int itemCnt,
			IQueryEngine engine)
			throws SQLException {
		
		if (!(groupBy instanceof MobileDeviceIdParolGroupBy)){
			return;
		}
		
		String linkprefix = "did_" + itemCnt; //$NON-NLS-1$
		
		groupBySql.append("i_" + itemCnt); //$NON-NLS-1$
		groupByInnerSql.append(" CASE WHEN " + linkprefix + ".patrol_leg_uuid IS NULL "); //$NON-NLS-1$ //$NON-NLS-2$
		groupByInnerSql.append("THEN null "); //$NON-NLS-1$ 
		groupByInnerSql.append("else " + linkprefix + ".ct_device_id "); //$NON-NLS-1$ //$NON-NLS-2$
		groupByInnerSql.append("END as i_" + itemCnt); //$NON-NLS-1$
		fromSql.append(" LEFT JOIN "); //$NON-NLS-1$
		fromSql.append(" smart.ct_patrol_link " + linkprefix); //$NON-NLS-1$
		fromSql.append(" on "); //$NON-NLS-1$
		fromSql.append("temp.pl_uuid = " + linkprefix + ".patrol_leg_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		
	}


	@Override
	public String getName() {
		return op.getGuiName(Locale.getDefault());
	}


	@Override
	public DropItem asDropItem() {
		return new MobileDeviceIdGroupByDropItem();
	}


	@Override
	public Image getImage() {
		return CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_DEVICE16);
	}


	@Override
	public Class<? extends IExtensionGroupBy> getGroupByClass() {
		return MobileDeviceIdParolGroupBy.class;
	}


	@Override
	public DropItem[] getDropItems(IExtensionGroupBy groupBy, Session session) {
		if (!(groupBy instanceof MobileDeviceIdParolGroupBy)){
			return null;
		}
		DropItem di = new MobileDeviceIdGroupByDropItem();
		return new DropItem[]{di};
	}

	@Override
	public IGroupByViewer<? extends IGroupBy> createViewer(IGroupBy groupBy) {
		if (groupBy instanceof MobileDeviceIdParolGroupBy){
			return new MobileDeviceIdGroupByViewer((MobileDeviceIdParolGroupBy)groupBy);
		}
		return null;
	}

}
