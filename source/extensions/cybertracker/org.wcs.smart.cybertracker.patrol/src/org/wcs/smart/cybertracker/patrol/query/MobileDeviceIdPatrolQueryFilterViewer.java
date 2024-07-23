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

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.SmartMobileDeviceManager;
import org.wcs.smart.cybertracker.model.SmartMobileDevice;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.ext.IExtensionFilter;
import org.wcs.smart.patrol.query.ext.IExtensionFilterViewer;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.util.SharedUtils;


/**
 * SMART Mobile Device Id Filter contribution viewer
 * 
 * @author egouge
 * @since 8.1.0
 */
public class MobileDeviceIdPatrolQueryFilterViewer implements IExtensionFilterViewer {

	private MobileDeviceIdPatrolQueryOption option = new MobileDeviceIdPatrolQueryOption();

	
	@Override
	public String asSql(IQueryEngine engine, Session session, IFilter filter){
		if (!(filter instanceof MobileDeviceIdPatrolQueryFilter qfilter)){
			return null;
		}
		
		String prefix = engine.tablePrefix(qfilter.getOption().getPatrolAttributeClass());
		String deviceId = SharedUtils.stripQuotes((String)qfilter.getValue());
		if (deviceId.equals(MobileDeviceIdPatrolQueryFilter.ANY_KEY)) {
			//any option
			return "EXISTS (SELECT * FROM smart.ct_patrol_link l WHERE l.patrol_leg_uuid = "+prefix+".uuid)";  //$NON-NLS-1$ //$NON-NLS-2$ 
		}else {
			return "EXISTS (SELECT * FROM smart.ct_patrol_link l WHERE l.ct_device_id = '" + deviceId + "' and l.patrol_leg_uuid = "+prefix+".uuid)";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	@Override
	public String getName() {
		return option.getGuiName(Locale.getDefault());
	}

	@Override
	public DropItem asDropItem() {
		DropItem it = PatrolDropItemFactory.INSTANCE.createPatrolFilterDropItem(option);
		it.initializeData(new Object[]{new MobileDeviceIdPatrolQueryOptionData()});
		return it;
	}



	@Override
	public Image getImage() {
		return CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_DEVICE16);
	}



	@Override
	public Class<? extends IExtensionFilter> getFilterClass() {
		return MobileDeviceIdPatrolQueryFilter.class;
	}



	@Override
	public DropItem[] getDropItems(IFilter filter, Session session) {
		if (!(filter instanceof MobileDeviceIdPatrolQueryFilter)){
			return null;
		}
		MobileDeviceIdPatrolQueryFilter f = (MobileDeviceIdPatrolQueryFilter)filter;
		
		DropItem it = PatrolDropItemFactory.INSTANCE.createPatrolFilterDropItem(option);
		
		String id = SharedUtils.stripQuotes((String)f.getValue());
		ListItem listItem = null;
		
		if (f.isAnyDevice()) {
			listItem = MobileDeviceIdPatrolQueryOptionData.ANY_DEVICE_ITEM;
		}else {
			SmartMobileDevice device = SmartMobileDeviceManager.INSTANCE.findDevice(session, id, SmartDB.getCurrentConservationArea());
			if (device == null) {
				listItem = new ListItem(null, id, id);
			}else {
				listItem = new ListItem(null, device.getName(), device.getDeviceId());
			}
		}
			
		it.initializeData(new Object[]{new MobileDeviceIdPatrolQueryOptionData(), listItem});		
		return new DropItem[]{it};
	}
}
