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
package org.wcs.smart.changetracking;

import org.hibernate.Session;

/**
 * Replication/Changelog event handler executed when
 * replication is enabled or disabled.
 * 
 * @author Emily
 *
 */
public interface IReplicationEventHandler {

	public static final String EXTENSION_ID = "org.wcs.smart.changelog.handler"; //$NON-NLS-1$
	
	/**
	 * 
	 * @param watcher
	 * @param session session with open transaction that is committed when complete
	 */
	public void replicationEnabled(IFileStoreWatcher watcher, Session session);
	
	/**
	 * 
	 * @param watcher
	 * @param session session with open transaction that is committed when complete
	 */
	public void replicationDisabled(IFileStoreWatcher watcher, Session session);
}
