package org.wcs.smart.i2.migrate.intelligence;

import java.util.UUID;

import org.wcs.smart.ca.ConservationArea;

public class IntelligenceSource {

	private UUID uuid;
	private String name;
	private String key;
	
	private ConservationArea ca;
	
	public IntelligenceSource(ConservationArea ca, UUID uuid, String name, String key) {
		this.ca = ca;
		this.uuid = uuid;
		this.name = name;
		this.key = key;
	}
	
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	public UUID getUuid() {
		return this.uuid;
	}
	public String getName() {
		return this.name;
	}
	public String getKey() {
		return this.key;
	}
}
