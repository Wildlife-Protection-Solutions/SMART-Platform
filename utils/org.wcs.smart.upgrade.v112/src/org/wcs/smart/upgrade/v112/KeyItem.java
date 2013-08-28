package org.wcs.smart.upgrade.v112;


public class KeyItem {

	private byte[] uuid;
	private String oldKeyId;
	private String newKeyId;
	private byte[] cauuid;

	
	private byte[] parentItem;
	private String originalHkey;
	
	public KeyItem(byte[] uuid, String keyId, byte[] cauuid){
		this.uuid = uuid;
		this.oldKeyId = keyId;
		newKeyId = null;
		this.cauuid = cauuid;
	}
	
	public KeyItem(byte[] uuid, String keyId, byte[]cauuid, byte[] parentItem, String hkey){
		this(uuid, keyId, cauuid);
		this.parentItem = parentItem;
		this.originalHkey = hkey;
	}
	
	public byte[] getCaUuid(){
		return this.cauuid;
	}
	public byte[] getParentItem(){
		return this.parentItem;
	}
	
	public String getOriginalHkey(){
		return this.originalHkey;
	}
	public byte[] getUuid(){
		return this.uuid;
	}
	public void updateKey(String newKey){
		this.newKeyId = newKey;
	}
	
	public String getOriginalKey(){
		return this.oldKeyId;
	}
	
	public String getNewKey(){
		return this.newKeyId;
	}
	
	public String getCurrentKey(){
		if (this.newKeyId == null){
			return oldKeyId;
		}
		return newKeyId;
	}
	public boolean isChanged(){
		return this.newKeyId != null && !this.newKeyId.equals(oldKeyId);
	}
}
