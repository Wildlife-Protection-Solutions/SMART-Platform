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
package org.wcs.smart.query;


/**
 * A listener that listens for
 * changes to query folders.  This
 * include folders added, deleted and renamed
 * and queries added, deleted, and saved.
 * 
 * @author Emily
 * @since 1.0.0
 */
public interface IQueryFolderListener {
	/**
	 * A folder added event
	 */
	public static final int FOLDER_ADDED = 1;
	/**
	 * A folder renamed event
	 */
	public static final int FOLDER_RENAMED = 2;
	/**
	 * A folder deleted event
	 */
	public static final int FOLDER_DELETED = 3;
	
	/**
	 * A query added event
	 */
	public static final int QUERY_ADDED = 4;
	
	/**
	 * A query saved event
	 */
	public static final int QUERY_SAVED = 5;
	/**
	 * A query deleted event
	 */
	public static final int QUERY_DELETED = 6;
	
	/**
	 * Called when an event occured that effects
	 * a query folder.
	 *  
	 * @param eventType the type of event
	 * @param object the object either a QueryFolder or Query that
	 * was affect by the given event
	 */
	void folderChanged(int eventType, Object object);
	
}
