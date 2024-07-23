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

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.SmartMobileDeviceManager;
import org.wcs.smart.cybertracker.model.SmartMobileDevice;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.ui.IPatrolOptionData;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

/**
 * SMART Mobile Device ID filter option data.
 * 
 * @author Emily
 * @since 8.1.0
 *
 */
public class MobileDeviceIdPatrolQueryOptionData implements IPatrolOptionData{

	public static final ListItem ANY_DEVICE_ITEM = new ListItem(null,Messages.MobileDeviceIdPatrolQueryOptionData_AnyOption,MobileDeviceIdPatrolQueryFilter.ANY_KEY); 
	
	public MobileDeviceIdPatrolQueryOptionData(){	
	}

	@Override
	public List<ListItem> getListValues(Session session, String[] keys) {
		return getListValues(session);
	}

	@Override
	public List<ListItem> getListValues(Session session) {
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		
		Set<String> deviceIds = new HashSet<>();
		
		List<SmartMobileDevice> devices = SmartMobileDeviceManager.INSTANCE.getDevices(session, ca);
		for(SmartMobileDevice d : devices) {
			items.add(new ListItem(null, d.getName(), d.getDeviceId()));
			deviceIds.add(d.getDeviceId());
		}
		
		Set<String> x = SmartMobileDeviceManager.INSTANCE.getAllSystemDeviceIds(session, ca);
		x.removeAll(deviceIds);
		for (String deviceId : x) {
			items.add(new ListItem(null, deviceId, deviceId));
		}
		
		items.sort((a,b)->Collator.getInstance().compare(a.getName().toUpperCase(), b.getName().toUpperCase()));
		items.add(0,ANY_DEVICE_ITEM);
		return items;
	}

	public ListItem getDefaultListItem() {
		return ANY_DEVICE_ITEM;
	}

	@Override
	public boolean isDependOnQueryConfiguration() {
		return false;
	}

}
