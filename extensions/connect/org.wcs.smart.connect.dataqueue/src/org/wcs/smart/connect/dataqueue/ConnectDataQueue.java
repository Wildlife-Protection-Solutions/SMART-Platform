package org.wcs.smart.connect.dataqueue;

import java.util.List;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;

public enum ConnectDataQueue {

	INSTANCE;
	
	public List<DataQueueItem> getQueuedItems(SmartConnect connect, ConservationArea ca){
		ResteasyClient client = connect.getClient();
		
		ResteasyWebTarget target = client.target(connect.getServer().getServerUrl() + "/api");
		DataQueueApi simple = target.proxy(DataQueueApi.class);
		
		
		return simple.getItems(ca.getUuid().toString(), DataQueueApi.SERVER_STATUS_PROCESSING);
		
	}

}
