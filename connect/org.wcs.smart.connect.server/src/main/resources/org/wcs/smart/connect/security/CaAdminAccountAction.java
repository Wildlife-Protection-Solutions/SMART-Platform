/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;

/**
 * Ca Administrator actions.  This action allows a user to do most things associated with a particular CA: view/edit/delete alerts, view/run reports & queries, 
 * create and edit users whose permissions only affect that same CA
 * 
 * @author Jeff
 *
 */



public class CaAdminAccountAction implements ISmartConnectAction{

	public static final String KEY = "caadmin"; //$NON-NLS-1$
	
	@Override
	public String getActionName(String actionKey, Locale l) {
		if (actionKey.equals(KEY)){
			return Messages.getString("CaAccountAction.AdminPermission", l); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public String[] getActionKeys() {
		return new String[]{KEY};
	}
	
	@Override
	public String[] getCaAdminAccessibleActionKeys(){
		return new String[]{};
	}

	@Override
	public List<ResourceOption> getResourceOptions(String actionKey, Session s, Locale l) {
		List<ResourceOption> ops = new ArrayList<ResourceOption>();
		ResourceOption ro;
		
		List<ConservationAreaInfo> info = s.createCriteria(ConservationAreaInfo.class).list();
		for (ConservationAreaInfo i : info){
			ro = new ResourceOption(i.getLabel(), i.getUuid());
			ops.add(ro);
		}
		return ops;
	}
	
	@Override
	public List<ResourceOption> getResourceOptionsForCas(String actionKey, Session s, Locale l, List<UUID> uuidList) {
		return null;	
	}

	@Override
	public String getResourceName(UUID resource, Session s, Locale l) {
		if (resource == null) return Messages.getString("CaAction.AllCas", l); //$NON-NLS-1$
		ConservationAreaInfo info = (ConservationAreaInfo) s.createCriteria(ConservationAreaInfo.class)
				.add(Restrictions.eq("uuid", resource)) //$NON-NLS-1$
				.uniqueResult();
		if (info == null) return resource.toString();
		return info.getLabel();
	}



}


