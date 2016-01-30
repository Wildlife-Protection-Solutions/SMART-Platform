package org.wcs.smart.connect.dataqueue.process;

import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;

public interface IProgressMonitorListener {

	public void progressUpdated(LocalDataQueueItem item, String taskName, String subTask, int totalWork, int currentWork);
	
	public void done(LocalDataQueueItem item);
	
	public void cancel(LocalDataQueueItem item);
}
