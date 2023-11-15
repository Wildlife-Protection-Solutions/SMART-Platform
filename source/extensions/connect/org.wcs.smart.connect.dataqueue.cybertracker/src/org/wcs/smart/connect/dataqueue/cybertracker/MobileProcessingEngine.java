package org.wcs.smart.connect.dataqueue.cybertracker;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public enum MobileProcessingEngine {
	
	INSTANCE;
	
	public static enum Option{
		SMART_MOBILE_DESKTOP_PROCESSING("org.wcs.smart.connect.dataqueue.smartmobile.desktopprocessing"); //$NON-NLS-1$
	
		public String key;
	
		private Option(String key) {
			this.key = key;
		}
	}
	
	private Boolean canProcessLocallyCache = null;
	
	public ConservationAreaProperty getOption(Option op, Session session) {
		return session.createQuery("FROM ConservationAreaProperty WHERE conservationArea = :ca and key = :key", ConservationAreaProperty.class) //$NON-NLS-1$
		.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
		.setParameter("key", op.key) //$NON-NLS-1$
		.uniqueResult();
	}
	public ConservationAreaProperty createOption(Option op, Session session) {
		ConservationAreaProperty prop = new ConservationAreaProperty();
		prop.setConservationArea(SmartDB.getCurrentConservationArea());
		prop.setKey(op.key);
		session.persist(prop);
		return prop;
	}
	 
	public Boolean getCanSmartMobileDesktopProcessing() {
		if (canProcessLocallyCache == null) {
			synchronized (MobileProcessingEngine.INSTANCE) {
				boolean value = Boolean.TRUE;
				
				try(Session s = HibernateManager.openSession()){
					ConservationAreaProperty prop = MobileProcessingEngine.INSTANCE.getOption(Option.SMART_MOBILE_DESKTOP_PROCESSING, s);
					
					if (prop == null) {
						value = Boolean.FALSE;
					}else if (prop.getValue() == null) {
						value = Boolean.FALSE;
					}else if (!prop.getValue().equalsIgnoreCase(Boolean.TRUE.toString())) {
						value=Boolean.FALSE;
					}
				}
				canProcessLocallyCache = value;
			}
		}
		if (!canProcessLocallyCache) return false;
		return true;
	}
	
	public void clearCachedValues() {
		canProcessLocallyCache = null;
	}
}
