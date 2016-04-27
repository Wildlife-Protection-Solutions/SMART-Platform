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
 * Conservation area actions.  This actions controls which users can view,delete and update
 * a set of Conservation areas.
 * 
 * @author Emily
 *
 */
public class CaAction implements ISmartConnectAction{

	public static final String VIEWCA_KEY = "viewca"; //$NON-NLS-1$
	public static final String DELETECA_KEY = "deleteca"; //$NON-NLS-1$
	public static final String UPDATECA_KEY = "updateca"; //$NON-NLS-1$
	public static final String ADDCA_KEY = "addca"; //$NON-NLS-1$
	
	@Override
	public String getActionName(String actionKey, Locale l) {
		if (actionKey.equals(VIEWCA_KEY)){
			return Messages.getString("CaAction.ViewCaPermission", l); //$NON-NLS-1$
		}else if (actionKey.equals(DELETECA_KEY)){
			return Messages.getString("CaAction.DeleteCaPermission", l); //$NON-NLS-1$
		}else if (actionKey.equals(UPDATECA_KEY)){
			return Messages.getString("CaAction.UpdateCaPermission", l); //$NON-NLS-1$
		}else if (actionKey.equals(ADDCA_KEY)){
			return Messages.getString("CaAction.AddCaPermission", l); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public String[] getActionKeys() {
		return new String[]{ADDCA_KEY, VIEWCA_KEY, UPDATECA_KEY, DELETECA_KEY};
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ResourceOption> getResourceOptions(String actionKey, Session s, Locale l) {
		if (actionKey.equals(ADDCA_KEY)){
			return null;
		}
		List<ResourceOption> ops = new ArrayList<ResourceOption>();
		ResourceOption ro = new ResourceOption(Messages.getString("CaAction.AllCas", l), null); //$NON-NLS-1$
		ops.add(ro);
		
		List<ConservationAreaInfo> info = s.createCriteria(ConservationAreaInfo.class).list();
		for (ConservationAreaInfo i : info){
			ro = new ResourceOption(i.getLabel(), i.getUuid());
			ops.add(ro);
		}
		
		return ops;
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
