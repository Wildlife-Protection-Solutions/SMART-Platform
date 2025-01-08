/*
 * Copyright (C) 2018 Wildlife Conservation Society
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
package org.wcs.smart.imageprocessor;

import java.util.List;

/**
 * Monitor for the processing of files
 * 
 * @author Emily
 *
 */
public interface IProcessingMonitor {

	/**
	 * Sets the items being processed
	 * @param items
	 */
	public default void setItems(List<ProcessingItem> items) {}
	
	/**
	 * Called when processing is complete
	 */
	public default void done() {}
	
	/**
	 * Called when processing begins
	 */
	public default void begin() {}
	
	/**
	 * Called when the status of an item has been updated
	 * @param item
	 */
	public default void update(ProcessingItem item) {}
	
	/**
	 * 
	 * @return if the processing should be cancelled or not
	 */
	public default boolean isCancelled() { return false; }
}
