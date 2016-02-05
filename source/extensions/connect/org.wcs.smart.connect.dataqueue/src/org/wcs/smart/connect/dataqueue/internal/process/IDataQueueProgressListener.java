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

import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;

/**
 * Listeners for listening to the state of processing of data queue 
 * items.  Listeners should be registered with the ProcessorManager
 * @author Emily
 *
 */
public interface IDataQueueProgressListener {
	
	/**
	 * Fired with the progress of an item is updated
	 * @param item  the item updated
	 * @param taskName current task name
	 * @param subTask current sub task
	 * @param totalWork total amount of work
	 * @param currentWork current amount of work completed
	 */
	public void progressUpdated(final LocalDataQueueItem item, String taskName, String subTask, int totalWork, int currentWork);

	/**
	 * Fired with the progress of an item is done.  This does not mean it
	 * was successful, it just means it's finished processing.
	 * @param item the item completed
	 */
	public void done(final LocalDataQueueItem item);

	/**
	 * Fired with the progress of an item is cancelled.
	 * @param item the item completed
	 */
	public void cancel(final LocalDataQueueItem item);
}
