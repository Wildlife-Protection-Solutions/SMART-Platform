package org.wcs.smart.connect.dataqueue.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;

public enum ConnectDataQueue {

	INSTANCE;
	
	public List<DataQueueItem> getQueuedItems(SmartConnect connect, ConservationArea ca){
		ResteasyClient client = connect.getClient();
		
		ResteasyWebTarget target = client.target(connect.getServer().getServerUrl() + "/api");
		DataQueueApi simple = target.proxy(DataQueueApi.class);
		
		
		return simple.getItems(ca.getUuid().toString(), DataQueueApi.ServerStatus.QUEUED.name());
		
	}

	public void updateStatus(SmartConnect connect, LocalDataQueueItem item, DataQueueApi.ServerStatus newStatus){
		ResteasyClient client = connect.getClient();
		
		ResteasyWebTarget target = client.target(connect.getServer().getServerUrl() + "/api");
		DataQueueApi simple = target.proxy(DataQueueApi.class);
		
		simple.updateStatus(item.getServerItemUuid().toString(), newStatus.name());
	}
	
	public String getFileDownloadUrl(SmartConnect connect, LocalDataQueueItem item) throws MalformedURLException{
		String downloadUrl = connect.getServer().getServerUrl() + "/api/" + DataQueueApi.DATAQUEUE_PATH + "/items/" + item.getServerItemUuid().toString() + "/file";
		return downloadUrl;
	}
}
