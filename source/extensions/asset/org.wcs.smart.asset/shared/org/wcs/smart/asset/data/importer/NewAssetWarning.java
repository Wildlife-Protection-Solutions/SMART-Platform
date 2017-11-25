package org.wcs.smart.asset.data.importer;

public class NewAssetWarning extends ActionableWarning{

	private String assetId;
	
	public NewAssetWarning(String message, String assetId) {
		super(message);
		this.assetId = assetId;
	}

	
	public String getAssetId() {
		return this.assetId;
	}
	

}
