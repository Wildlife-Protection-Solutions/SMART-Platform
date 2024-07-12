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
package org.wcs.smart.i2.patrol.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.patrol.internal.Messages;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.patrol.query.ui.IPatrolOptionData;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Patrol option data for intelligence filter.
 * 
 * @author Emily
 *
 */
public class PatrolProfileRecordPatrolData implements IPatrolOptionData {
	

	public static final ListItem ANY_ITEM = 
			new ListItem(UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR), 
					Messages.PatrolProfileRecordPatrolData_AnyOption);
	
	public PatrolProfileRecordPatrolData(){
	}
	
	@Override
	public List<ListItem> getListValues(Session session, String[] keys) {
		return getListValues(session);
	}

	@Override
	public List<ListItem> getListValues(Session session) {
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		
		List<IntelProfile> profiles = ProfilesManager.INSTANCE.getProfiles(session, true);
		List<IntelProfile> viewable = new ArrayList<>();
		for (IntelProfile p : profiles) {
			if (IntelSecurityManager.INSTANCE.canViewRecords(p)) viewable.add(p);
			
		}
		if (!viewable.isEmpty()) {
			List<IntelRecord> records = session.createQuery("FROM IntelRecord WHERE conservationArea = :ca and profile in (:profiles)",IntelRecord.class) //$NON-NLS-1$
					.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.setParameter("profiles", viewable) //$NON-NLS-1$
					.list();
			
			for (IntelRecord i : records) {
				items.add(new ListItem(i.getUuid(), i.getTitle()));
			}
		}
		Collections.sort(items);
		items.add(0,ANY_ITEM);
		return items;
	}

	@Override
	public ListItem getDefaultListItem() {
		return ANY_ITEM;
	}

	@Override
	public boolean isDependOnQueryConfiguration() {
		return false;
	}

}
