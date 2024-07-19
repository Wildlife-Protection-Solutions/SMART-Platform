package org.wcs.smart.cybertracker;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.model.SmartMobileDevice;


public enum SmartMobileDeviceManager {
	
	INSTANCE;
	
	private volatile List<ISmartMobileDeviceIdProvider> providers = null;;

	public SmartMobileDevice findOrCreateDeviceAlias(Session session, UUID deviceId, ConservationArea ca) {
		SmartMobileDevice device = session.createQuery("FROM SmartMobileDevice where deviceId = :id", SmartMobileDevice.class) //$NON-NLS-1$
				.setParameter("id", deviceId) //$NON-NLS-1$
				.uniqueResult();
		
		if (device != null) return device;
		
		device = new SmartMobileDevice();
		device.setDeviceId(deviceId.toString());
		device.setConservationArea(ca);
		device.setName(generateDeviceName(session, ca));
		
		session.persist(device);
		
		return device;		
	}
	
	public List<SmartMobileDevice> getDevices(Session session, ConservationArea ca) {
		return 	session.createQuery(" FROM SmartMobileDevice sm WHERE sm.conservationArea = :ca", SmartMobileDevice.class)
				.setParameter("ca", ca)
				.list();
	}
	
	/**
	 * Create missing alaises for devices AND saves them to the database.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public void createMissingDevices(Session session, ConservationArea ca) throws Exception{
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
				newd.setName(generateDeviceName(all));
				
				session.persist(newd);
				all.add(newd);
			}
			session.getTransaction().commit();
		}catch (Exception ex) {
			session.getTransaction().rollback();
			throw ex;
		}
	}
	
	private String generateDeviceName(Session session, ConservationArea ca) {
		List<SmartMobileDevice> names = getDevices(session, ca); 
		return generateDeviceName(names);
	}
	
	public String generateDeviceName(List<SmartMobileDevice> devices) {
			
		Set<String> used = devices.stream().map(e->e.getName()).collect(Collectors.toSet());
		
		int number = devices.size() + 1;
		String thisname = MessageFormat.format("Device {0}", number);
		while(used.contains(thisname)) {
			number++;
			thisname = MessageFormat.format("Device {0}", number);
		}
		return thisname;
	}
	
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
			
			if (Platform.getExtensionRegistry() != null) {
				IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(ISmartMobileDeviceIdProvider.EXT_ID);
				try {
					for (IConfigurationElement e : config) {
						if (e.getName().equals(ISmartMobileDeviceIdProvider.EXT_NAME)) {
							ISmartMobileDeviceIdProvider ext = (ISmartMobileDeviceIdProvider) e.createExecutableExtension("provider"); //$NON-NLS-1$
							items.add(ext);
						}
					}
				}catch (Exception ex){
					SmartPlugIn.displayLog("Error loading device id providers", ex);
				}
			}
			this.providers = items;
		}
		return providers;
	}
	
}
