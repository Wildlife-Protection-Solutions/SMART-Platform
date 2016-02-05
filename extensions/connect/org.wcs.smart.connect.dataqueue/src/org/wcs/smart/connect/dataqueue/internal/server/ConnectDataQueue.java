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
package org.wcs.smart.connect.dataqueue.internal.server;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.model.ConnectServerOption;

/**
 * Interface for interacting with the SMART Connect Server data queue.
 * @author Emily
 *
 */
public enum ConnectDataQueue {

	INSTANCE;
	
	/**
	 * Gets all items on the server that have a status of QUEUED
	 * @param connect
	 * @param ca
	 * @return
	 */
	public List<DataQueueItem> getQueuedItems(SmartConnect connect, ConservationArea ca){
		ResteasyClient client = connect.getClient();
		
		ResteasyWebTarget target = client.target(connect.getServer().getServerUrl() + SmartConnect.API_URL);
		DataQueueApi simple = target.proxy(DataQueueApi.class);
		
		
		return simple.getItems(ca.getUuid().toString(), DataQueueApi.ServerStatus.QUEUED.name());
	}

	/**
	 * Updates the status of an data queue item on the server.
	 * 
	 * @param connect
	 * @param item
	 * @param newStatus
	 */
	public void updateStatus(SmartConnect connect, LocalDataQueueItem item, DataQueueApi.ServerStatus newStatus) throws Exception{

		int numRetry = ConnectServerOption.ConnectionOption.MAX_RETRY_DOWNLOAD.getIntegerValue(connect.getServer());
		long delay = ConnectServerOption.ConnectionOption.RETY_WAIT_TIME.getIntegerValue(connect.getServer());
		int retryCnt = 0;
		Exception lastException = null;
		while(retryCnt < numRetry){
			try{
				ResteasyClient client = connect.getClient();
				ResteasyWebTarget target = client.target(connect.getServer().getServerUrl() + SmartConnect.API_URL);
				DataQueueApi simple = target.proxy(DataQueueApi.class);
				simple.updateStatus(item.getServerItemUuid().toString(), newStatus.name());
				return;
			}catch (WebApplicationException ex){
				throw ex;
			}catch (Exception ex){
				//try again; server may be down temporarily
				lastException = ex;
			}
			retryCnt ++;
			if (retryCnt < numRetry){
				Thread.sleep(delay);
			}
		}
		throw new Exception(MessageFormat.format("Unable to update server status after {0} attempts.",retryCnt) + lastException.getMessage(), lastException);
		
	}
	
	/**
	 * Downloads the file associated with a data queue item.
	 * @param connect
	 * @param item
	 * @return
	 * @throws MalformedURLException
	 */
	public String getFileDownloadUrl(SmartConnect connect, LocalDataQueueItem item) throws MalformedURLException{
		String downloadUrl = connect.getServer().getServerUrl() + SmartConnect.API_URL + "/" + DataQueueApi.DATAQUEUE_PATH + "/items/" + item.getServerItemUuid().toString() + "/file";
		return downloadUrl;
	}
}
