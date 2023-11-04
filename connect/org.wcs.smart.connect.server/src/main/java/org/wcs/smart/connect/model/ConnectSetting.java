package org.wcs.smart.connect.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="settings", schema="connect")
public class ConnectSetting {

	public static ConnectSetting.Setting[] DQ_SETTINGS = new ConnectSetting.Setting[] {
		Setting.DQ_SMART_MOBILE_PROCESSING,
		Setting.DQ_SMART_COLLECT_USEROPTION,
	};
	
	public static enum Setting{
		
		DQ_SMART_MOBILE_PROCESSING("connect.dataqueue.smartmobile.processing"), //$NON-NLS-1$
		DQ_SMART_COLLECT_USEROPTION("connect.dataqueue.smartcollect.useroption"); //$NON-NLS-1$
		
		public String key;
		
		Setting(String key) {
			this.key = key;
		}
	}
	
	public static enum SmartCollectUserOption{
		VALIDATE_REQUEUE("validaterequeue"), //$NON-NLS-1$
		LOAD("load"), //$NON-NLS-1$
		REQUEUE("requeue"), //$NON-NLS-1$
		DISCARD("discard"); //$NON-NLS-1$
	
		public String key;
		
		SmartCollectUserOption(String key){
			this.key = key;
		}
		
		public static SmartCollectUserOption findOption(String key) {
			for (SmartCollectUserOption op : SmartCollectUserOption.values()) {
				if (op.key.equalsIgnoreCase(key)) return op;				
			}
			return null;
		}
	}
	
	private String key;
	private String value;
	
	@Id
	public String getKey() {
		return this.key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
}
