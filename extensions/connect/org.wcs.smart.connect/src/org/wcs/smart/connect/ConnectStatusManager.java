package org.wcs.smart.connect;

import java.util.ArrayList;
import java.util.List;

public enum ConnectStatusManager {

	INSTANCE;
	
	public enum ServerStatus{
		ERROR, 
		CONNECTING,
		DOWNLOADING,
		CHANGES,
		UPTODATE
	}
	
	private List<IConnectStatusListener> listeners = new ArrayList<IConnectStatusListener>();
	
	public void addListener(IConnectStatusListener listener){
		this.listeners.add(listener);
	}
	
	public void removeListener(IConnectStatusListener listener){
		this.listeners.remove(listener);
	}
	
	public void statusModified(ServerStatus newStatus, String statusMessage){
		for (IConnectStatusListener l : listeners){
			l.statusModified(newStatus, statusMessage);
		}
	}
	
}
