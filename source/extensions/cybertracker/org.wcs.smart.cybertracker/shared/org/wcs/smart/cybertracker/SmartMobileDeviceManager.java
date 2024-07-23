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
package org.wcs.smart.cybertracker;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.model.SmartMobileDevice;

/**
 * @since 8.1.0
 */
public enum SmartMobileDeviceManager {
	
	INSTANCE;
	
	public static final Object DEFAULT_NAME = new Object();
	
	private volatile List<ISmartMobileDeviceIdProvider> providers = null;;

	
	/**
	 * Finds the device with the given deviceId;
	 * 
	 * @param session
	 * @param deviceId
	 * @param ca
	 * @return
	 */
	public SmartMobileDevice findDevice(Session session, String deviceId, ConservationArea ca) {
		SmartMobileDevice device = session.createQuery("FROM SmartMobileDevice where deviceId = :id", SmartMobileDevice.class) //$NON-NLS-1$
				.setParameter("id", deviceId) //$NON-NLS-1$
				.uniqueResult();
		return device;
	}
	
	/**
	 * Loads the devices defined in the smartmobiledevice table. 
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public List<SmartMobileDevice> getDevices(Session session, ConservationArea ca) {
		return 	session.createQuery(" FROM SmartMobileDevice sm WHERE sm.conservationArea = :ca", SmartMobileDevice.class) //$NON-NLS-1$
				.setParameter("ca", ca) //$NON-NLS-1$
				.list();
	}
	
	/**
	 * Create missing alaises for devices AND saves them to the database.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public void createMissingDevices(Session session, ConservationArea ca, Locale l) throws Exception{
		session.beginTransaction();
		try {
			Set<String> deviceIds = getAllSystemDeviceIds(session, ca);
			
			List<SmartMobileDevice> all = getDevices(session, ca);
			
			for (SmartMobileDevice d : all) {
				deviceIds.remove(d.getDeviceId());
			}
			for (String s : deviceIds) {
				SmartMobileDevice newd = new SmartMobileDevice();
				newd.setDeviceId(s);
				newd.setConservationArea(ca);
				newd.setName(generateDeviceName(all, l));
				
				session.persist(newd);
				all.add(newd);
			}
			session.getTransaction().commit();
		}catch (Exception ex) {
			session.getTransaction().rollback();
			throw ex;
		}
	}
	
	/**
	 * Generates a device name unique from the list of devices provided
	 * 
	 * @param devices
	 * @return
	 */
	public String generateDeviceName(List<SmartMobileDevice> devices, Locale l) {
			
		Set<String> used = devices.stream().map(e->e.getName()).collect(Collectors.toSet());

		String prefix = SmartContext.INSTANCE.getClass(ICyberTrackerLabelProvider.class).getLabel(DEFAULT_NAME, l);
		int number = devices.size() + 1;
		String thisname = MessageFormat.format(prefix, number);
		while(used.contains(thisname)) {
			number++;
			thisname = MessageFormat.format(prefix, number);
		}
		return thisname;
	}
	
	/**
	 * Gets all the system devices used in SMART. There are provided by
	 * plugins implementing the ISmartMobiledeviceIDProvide class.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public Set<String> getAllSystemDeviceIds(Session session, ConservationArea ca) {
		Set<String> allIds = new HashSet<>();
		
		for (ISmartMobileDeviceIdProvider providers : getDeviceIdProviders()) {
			allIds.addAll(providers.getDeviceIds(session, ca));
		}
		return allIds;
	}
	
	/**
	 * Find all contributions registered in the system
	 * @return
	 */
	public List<ISmartMobileDeviceIdProvider> getDeviceIdProviders(){
		if (providers != null) return providers;
		
		synchronized (this) {
			if (providers != null) return providers;
		
			List<ISmartMobileDeviceIdProvider>  items = new ArrayList<>();
			IExtensionRegistry registry = RegistryFactory.getRegistry();
			if (registry == null) return Collections.emptyList();
			
			IExtensionPoint pnt = registry.getExtensionPoint(ISmartMobileDeviceIdProvider.EXT_ID);
			IConfigurationElement[] config = pnt.getConfigurationElements();

			for (IConfigurationElement e : config) {
				if (e.getName().equals(ISmartMobileDeviceIdProvider.EXT_NAME)) {
					try {
						ISmartMobileDeviceIdProvider ext = (ISmartMobileDeviceIdProvider) e.createExecutableExtension("provider"); //$NON-NLS-1$
						items.add(ext);
					}catch (Exception ex){
						Logger.getLogger(SmartMobileDeviceManager.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
					}
				}
			}
			
			this.providers = items;
		}
		return providers;
	}
	
}
