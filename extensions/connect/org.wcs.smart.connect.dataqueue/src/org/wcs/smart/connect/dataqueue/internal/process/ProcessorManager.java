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
package org.wcs.smart.connect.dataqueue.internal.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;

/**
 * Manages data queue item processing jobs and associated
 * status.
 * 
 * @author Emily
 *
 */
public enum ProcessorManager {
	
	INSTANCE;
	
	// static list of available processors
	private List<IItemProcessor> processors = null;
	//list of progress listeners
	private List<IDataQueueProgressListener> listeners = new ArrayList<IDataQueueProgressListener>();
	
	public synchronized List<IItemProcessor> getProcessors(){
		if (processors != null) return processors;
		
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		ArrayList<IItemProcessor> items = new ArrayList<IItemProcessor>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IItemProcessor.EXTENSION_ID);
		for (IConfigurationElement e : config) {
			try{
				items.add((IItemProcessor)e.createExecutableExtension("class")); //$NON-NLS-1$
			}catch (Exception ex){
				ConnectDataQueuePlugin.log(ex.getMessage(), ex);
			}
		}
		processors = items;
		return processors;
	}
	
	/**
	 * Schedules a job to process the data queue.
	 * 
	 * @param connect
	 */
	public void processDataQueue(SmartConnect connect){
		DataQueueItemProcessor job = new DataQueueItemProcessor(connect, new DataQueueProcessMonitor());
		job.setRule(DataQueueItemProcessor.MUTEX);
		job.schedule();
	}
	
	/**
	 * Adds a queue item processor listener for listening to the state
	 * of the processing of data queue items.
	 * @param listener
	 */
	public void addListener(IDataQueueProgressListener listener){
		listeners.add(listener);
	}
	/**
	 * Removes a processing listener
	 * @param listener
	 */
	public void removeListener(IDataQueueProgressListener listener){
		listeners.remove(listener);
	}
	
	/**
	 * Process listeners, calling progress updated event for given queue item 
	 * @param item
	 * @param taskName
	 * @param subTask
	 * @param totalWork
	 * @param currentWork
	 */
	void progressUpdated(final LocalDataQueueItem item, String taskName, 
			String subTask, int totalWork, int currentWork) {
		for (IDataQueueProgressListener l : listeners){
			l.progressUpdated(item, taskName, subTask, totalWork, currentWork);
		}
	}

	/**
	 * Process listeners, calling done event for given
	 * queue item
	 * @param item
	 */
	void done(final LocalDataQueueItem item) {
		for (IDataQueueProgressListener l : listeners){
			l.done(item);
		}
	}

	/**
	 * Process listeners, calling cancelled event for given
	 * queue item
	 * @param item
	 */
	void cancel(final LocalDataQueueItem item) {
		for (IDataQueueProgressListener l : listeners){
			l.cancel(item);
		}
	}
	
}
