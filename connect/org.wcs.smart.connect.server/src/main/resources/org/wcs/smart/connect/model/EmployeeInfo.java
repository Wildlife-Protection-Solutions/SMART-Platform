package org.wcs.smart.connect.model;

import java.util.ArrayList;
import java.util.UUID;

public class EmployeeInfo {
	private UUID uuid;
	private UUID caUuid;
	
	private String caLabel;
	
	private String id;
	private String givenName;
	private String familyName;

	private char gender;
	private String smartUserId;
	private String smartPassword;
	private String userLevelKey;
	private ArrayList<SimpleConservationAreaList> allCasUserIsIn;
	
	
	public EmployeeInfo(String smartUserId, String caLabel, UUID caUuid) {
		setCaLabel(caLabel);
		setSmartUserId(smartUserId);
		setCaUuid(caUuid);
	}

	public EmployeeInfo() {
	}

	public UUID getUuid() {
		return uuid;
	}
	
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public UUID getCaUuid() {
		return caUuid;
	}
	public void setCaUuid(UUID caUuid) {
		this.caUuid = caUuid;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getGivenName() {
		return givenName;
	}
	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}
	public String getFamilyName() {
		return familyName;
	}
	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public char getGender() {
		return gender;
	}
	public void setGender(char gender) {
		this.gender = gender;
	}
	public String getSmartUserId() {
		return smartUserId;
	}
	public void setSmartUserId(String smartUserId) {
		this.smartUserId = smartUserId;
	}
	public String getSmartPassword() {
		return smartPassword;
	}
	public void setSmartPassword(String smartPassword) {
		this.smartPassword = smartPassword;
	}
	public String getUserLevelKey() {
		return userLevelKey;
	}
	public void setUserLevelKey(String userLevelKey) {
		this.userLevelKey = userLevelKey;
	}
	public String getCaLabel() {
		return caLabel;
	}
	public void setCaLabel(String caLabel) {
		this.caLabel = caLabel;
	}

	public ArrayList<SimpleConservationAreaList> getAllCasUserIsIn() {
		return allCasUserIsIn;
	}

	public void setAllCasUserIsIn(ArrayList<SimpleConservationAreaList> allCasUserIsIn) {
		this.allCasUserIsIn = allCasUserIsIn;
	}
	
}
