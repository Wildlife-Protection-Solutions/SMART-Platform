package org.wcs.smart.connect;

import java.util.ArrayList;
import java.util.List;

public enum ConnectStatusManager {

	INSTANCE;
	
	public static final int CHECK_LOCAL_STATUS = 15 * 1000; //15 seconds
	
	public enum ServerStatus{
		ERROR, 
		CONNECTING,
		DOWNLOADING,
		CHANGES,
		UPTODATE
	}
	
	private List<IConnectStatusListener> serverListeners = new ArrayList<IConnectStatusListener>();
	private List<IConnectStatusListener> localListeners = new ArrayList<IConnectStatusListener>();
		
	public void addServerStatusListener(IConnectStatusListener listener){
		this.serverListeners.add(listener);
	}
	
	public void removeServerStatusListener(IConnectStatusListener listener){
		this.serverListeners.remove(listener);
	}
	
	public void serverStatusModified(ServerStatus newStatus, String statusMessage){
		for (IConnectStatusListener l : serverListeners){
			l.statusModified(newStatus, statusMessage);
		}
	}
	
	public void addLocalStatusListener(IConnectStatusListener listener){
		this.localListeners.add(listener);
	}
	
	public void removeLocalStatusListener(IConnectStatusListener listener){
		this.localListeners.remove(listener);
	}
	
	/**
	 * Fire this event when you know that the local status is changed and
	 * should be updated.  ServerStatus can be null the current status
	 * is not known.  In this case the database will be queried for the current
	 * status.
	 * 
	 */
	public void localStatusModified(ServerStatus newStatus, String statusMessage){
		for (IConnectStatusListener l : localListeners){
			l.statusModified(newStatus, statusMessage);
		}
	}
}
