package org.wcs.smart.connect.dataqueue.internal.process;


public interface IDataQueueListener {

	/**
	 * Called when items are added or removed from the local data
	 * queue.  Is not called when the status of items is not 
	 * modified.
	 */
	public void dataQueueModified();
}
