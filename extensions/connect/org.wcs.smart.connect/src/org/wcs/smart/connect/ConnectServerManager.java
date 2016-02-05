package org.wcs.smart.connect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

public enum ConnectServerManager {
	INSTANCE;
	
	
	private List<IConnectServerEventHandler> listeners = new ArrayList<IConnectServerEventHandler>();
	
	public void addHandler(IConnectServerEventHandler listener){
		listeners.add(listener);
	}
	
	public void removeHandler(IConnectServerEventHandler listener){
		listeners.remove(listener);
	}
	
	public void runAfterDeleteHandlers(Session session) throws Exception{
		for(IConnectServerEventHandler l : listeners){
			l.beforeDelete(session);
		}
	}
	
	
	public interface IConnectServerEventHandler{
		/**
		 * Called before the server is deleted.
		 * @param session database session in active transaction
		 */
		public void beforeDelete(Session session) throws Exception;
	}
}
