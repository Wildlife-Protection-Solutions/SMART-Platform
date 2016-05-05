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
package org.wcs.smart.connect.cybertracker;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.ConnectAlert;
import org.wcs.smart.connect.cybertracker.util.CmElementsVisitor;
import org.wcs.smart.connect.cybertracker.util.CmElementsVisitor.IElementVisitHandler;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * "Connect for CyberTracker" related database functions.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectCtHibernateManager {
	
	/**
	 * Fetches a list of {@link ConnectAlert} for {@link ConfigurableModel}
	 * 
	 * @param cm {@link ConfigurableModel} for which alerts are retrieved
	 * @param s session
	 * @return List<ConnectAlert>
	 */
	public static List<ConnectAlert> getConnectAlerts(ConfigurableModel cm, Session s, boolean replaceLazyItems) {
		try {
			@SuppressWarnings("unchecked")
			List<ConnectAlert> items = s.createCriteria(ConnectAlert.class)
					.add(Restrictions.eq("model", cm)).list();  //$NON-NLS-1$
			if (replaceLazyItems) {
				//replace lazy items with real
				CmElementsMap map = new CmElementsMap(cm);
				for (ConnectAlert a : items) {
					a.setModel(cm);
					if (a.getAttrubute() != null) {
						a.setAttrubute(map.getCmAttribute(a.getAttrubute().getUuid()));
					}
					if (a.getAlertItem() != null) {
						a.setAlertItem(map.getUuidItem(a.getAlertItem().getUuid()));
					}
				}
			}
			return items;
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.ConnectCtHibernateManager_LoadAlertsError, ex);
			return Collections.emptyList();
		}
	}

	/**
	 * Map that allows to quickly find any element in {@link ConfigurableModel} based on its {@link UUID}.
	 * NOTE: Only maps objects that have UUID. 
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	private static class CmElementsMap {

		private Map<UUID, UuidItem> map;

		public CmElementsMap(ConfigurableModel model) {
			map = new HashMap<>();
			if (model.getUuid() == null) {
				return;
			}
			map.put(model.getUuid(), model);
			CmElementsVisitor visitor = new CmElementsVisitor();
			visitor.visit(model, new IElementVisitHandler() {
				@Override
				public void handle(UuidItem item) {
					if (item.getUuid() != null) {
						map.put(item.getUuid(), item);
					}
				}
			});
		}

		public UuidItem getUuidItem(UUID uuid) {
			if (uuid == null) {
				return null;
			}
			UuidItem obj = map.get(uuid);
			if (obj == null) {
				//just some development validation
				SmartPlugIn.log("Unexpected item requested from a configurable model map: " + uuid, null); //$NON-NLS-1$
			}
			return obj;
		}
		
		public CmAttribute getCmAttribute(UUID uuid) {
			UuidItem obj = getUuidItem(uuid);
			return obj instanceof CmAttribute ? (CmAttribute) obj : null;
		}
	}	
}
