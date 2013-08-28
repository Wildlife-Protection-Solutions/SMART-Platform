package org.wcs.smart.upgrade.v112;

import java.util.ArrayList;
import java.util.List;

public class KeyItem {

	private byte[] uuid;
	private String oldKeyId;
	private String newKeyId;
	private byte[] cauuid;
	
	private List<KeyName> names;
	
	private byte[] parentItem;
	private String originalHkey;
	
	public KeyItem(byte[] uuid, String keyId, byte[] cauuid){
		this.uuid = uuid;
		this.oldKeyId = keyId;
		newKeyId = null;
		names = new ArrayList<KeyName>();
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
	
	public void addName(byte[] language, String name){
		names.add(new KeyName(language, name));
	}
	
	public List<KeyName> getNames(){
		return names;
	}
	
	class KeyName{
		private byte[] language;
		private String name;
		private String newName;
		
		public KeyName(byte[] language, String name){
			this.language = language;
			this.name = name;
			this.newName = null;
		}
		
		public void updateName(String newName){
			this.newName = newName;
		}
		public boolean isChanged(){
			return this.newName != null;
		}
		public String getOldName(){
			return this.name;
		}
		public String getNewName(){
			return this.newName;
		}
		public byte[] getLanguage(){
			return this.language;
		}
	}
}
