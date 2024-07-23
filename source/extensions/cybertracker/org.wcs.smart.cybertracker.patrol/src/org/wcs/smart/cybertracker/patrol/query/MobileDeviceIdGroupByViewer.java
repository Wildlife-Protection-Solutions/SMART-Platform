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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.SmartMobileDeviceManager;
import org.wcs.smart.cybertracker.model.SmartMobileDevice;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

/**
 * Group by query item for SMART Mobile device ids
 * 
 * @author Emily
 * @since 8.1.0
 *
 */
public class MobileDeviceIdGroupByViewer extends AbstractGroupByViewer<MobileDeviceIdParolGroupBy>{

	public MobileDeviceIdGroupByViewer(MobileDeviceIdParolGroupBy gb) {
		super(gb);
	}


	@Override
	public List<ListItem> getItems(Session session) {
		
		List<ConservationArea> cas = new ArrayList<>();
		if (SmartDB.isMultipleAnalysis()) {
			cas.addAll(SmartDB.getConservationAreaConfiguration().getConservationAreas());
		}else {
			cas.add(SmartDB.getCurrentConservationArea());
		}
		
		List<ListItem> items = new ArrayList<>();
		Set<String> deviceIds = new HashSet<>();
		for (ConservationArea ca : cas) {
			//get devices with aliases
			List<SmartMobileDevice> devices = SmartMobileDeviceManager.INSTANCE.getDevices(session, ca);
			for(SmartMobileDevice d : devices) {
				if (deviceIds.contains(d.getDeviceId())) continue;
				items.add(new ListItem(null, d.getName(), d.getDeviceId()));
				deviceIds.add(d.getDeviceId());
			}
			//get devices without aliases
			Set<String> x = SmartMobileDeviceManager.INSTANCE.getAllSystemDeviceIds(session, ca);
			x.removeAll(deviceIds);
			for (String deviceId : x) {
				items.add(new ListItem(null, deviceId, deviceId));
				deviceIds.add(deviceId);
			}
		}
		return items;
		
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		return new MobileDeviceIdGroupByDropItem();
	}


}
