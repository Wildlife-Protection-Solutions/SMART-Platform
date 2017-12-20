package org.wcs.smart.asset.data.importer;

public class NewLocationWarning extends ActionableWarning{

	private String locationId;
	
	public NewLocationWarning(String message, String locationId) {
		super(message);
		this.locationId = locationId;
	}

	
	public String getLocationId() {
		return this.locationId;
	}
	

}
