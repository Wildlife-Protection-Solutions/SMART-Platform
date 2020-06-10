package org.wcs.smart.smartcollect.model;

import java.util.UUID;

public class SmartCollectUser {

	public enum State{
		BLACKLISTED,
		VALIDATED,
		NEW,
		VALIDATION_PENDING,
	}
	
	private String source;
	private State state;
	private UUID uuid;
	
	public static boolean isEmailSource(String source) {
		if (source == null) return false;
		return  source.matches("\\S+@\\S+\\.\\S+");
	}
	
	public UUID getUuid() {
		return this.uuid;
	}
	
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
	public String getSource() {
		return this.source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	public State getState() {
		return this.state;
	}
	
	public void setState(State state) {
		this.state = state;
	}
}
