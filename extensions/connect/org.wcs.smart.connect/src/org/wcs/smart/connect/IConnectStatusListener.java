package org.wcs.smart.connect;

public interface IConnectStatusListener {

	/**
	 * Fired when the server status had been modified.
	 * 
	 * @param status current status
	 * @param message current status message or null if no message
	 */
	public void statusModified(ConnectStatusManager.ServerStatus status, String message);
	
}
