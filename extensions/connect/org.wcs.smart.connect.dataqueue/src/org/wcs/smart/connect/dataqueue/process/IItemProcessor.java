package org.wcs.smart.connect.dataqueue.process;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;

public interface IItemProcessor {

	public boolean canProcess(DataQueueItem.Type type);
	
	public void process(DataQueueItem item, IProgressMonitor monitor) throws Exception;
}
