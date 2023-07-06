package org.wcs.smart.observation.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


@Entity
@Table(name="data_link", schema="smart")
public class DataLink extends UuidItem{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String dataType;
	private UUID providerId;
	private UUID smartId;
	private LocalDateTime lastUpdated;
	private ConservationArea ca;

	public DataLink() {
		modified();
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	@Column(name="data_type")
	public String getDataType() {
		return this.dataType;
	}
	
	public void setDataType(String dataType) {
		this.dataType = dataType;
		modified();
	}
	
	@Column(name="provider_id")
	public UUID getProviderId() {
		return this.providerId;
	}
	
	public void setProviderId(UUID provider) {
		this.providerId = provider;
	}
	
	@Column(name="smart_id")
	public UUID getSmartId() {
		return this.smartId;
	}
	
	public void setSmartId(UUID smartId) {
		this.smartId = smartId;
		modified();
	}
	
	@Column(name="last_modified")
	public LocalDateTime getLastModified() {
		return this.lastUpdated;
	}
	
	public void setLastModified(LocalDateTime lastModified) {
		this.lastUpdated = lastModified;
	}
	
	@Transient
	private void modified() {
		this.lastUpdated = LocalDateTime.now();
	}
	
	/*
	 * We don't use this on the desktop version of SMART however
	 * it is used on Connect.  This prevents sync conflicts (the same
	 * data being deleted on server and desktop). Most data
	 * processing is supposed to be happening on connect so this
	 * should be ok.
	 */
	public static void cleanUp(IExtensionRegistry registry, Session session) throws Exception{

		if (registry == null) throw new Exception("Extension Registry cannot be null"); //$NON-NLS-1$
		
		List<String[]> linktypes = new ArrayList<>();
		IExtensionPoint pnt = registry.getExtensionPoint("org.wcs.smart.datalink.type"); //$NON-NLS-1$
		if (pnt == null) throw new Exception("Extension point org.wcs.smart.datalink.type not found"); //$NON-NLS-1$
		
		IConfigurationElement[] config = pnt.getConfigurationElements();
		for (IConfigurationElement e : config) { 
			linktypes.add(new String[] { e.getAttribute("type_key"), e.getAttribute("hibernate_class")} ); //$NON-NLS-1$ //$NON-NLS-2$			
		}
		
		for (String[] links : linktypes) {
			String type = links[0];
			String clazz = links[1];
			
			StringBuilder sb = new StringBuilder();
			sb.append("DELETE FROM DataLink"); //$NON-NLS-1$
			sb.append(" WHERE dataType = :type" ); //$NON-NLS-1$
			sb.append(" AND smartId NOT IN ( " ); //$NON-NLS-1$
			sb.append(" SELECT uuid FROM " ); //$NON-NLS-1$
			sb.append(clazz);
			sb.append(" ) "); //$NON-NLS-1$
			
			session.createMutationQuery(sb.toString() )
				.setParameter("type", type) //$NON-NLS-1$
				.executeUpdate();
		}
	}
}
