/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.connect;

import java.util.ArrayList;
import java.util.List;

/**
 * Connect server status manager.
 * @author Emily
 *
 */
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
