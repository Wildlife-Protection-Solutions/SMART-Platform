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
package org.wcs.smart.intelligence;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.changetracking.IFileStoreWatcher;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.Intelligence;


/**
 * Event handler that tells the file store watcher to ignore informat data files.
 * 
 * @author Emily
 *
 */
public class ReplicationEventHandler implements org.wcs.smart.changetracking.IReplicationEventHandler {

	@SuppressWarnings("unchecked")
	@Override
	public void replicationEnabled(IFileStoreWatcher watcher, Session session) {
		Query query = session.createQuery("from ConservationArea ");	 //$NON-NLS-1$
		List<ConservationArea> cas = query.list();
		for (ConservationArea ca : cas){
			Path toIgnore = FileSystems.getDefault().getPath(ca.getFileDataStoreLocation(), Intelligence.INTELLIGENCE_DIR, Informant.DIR_NAME);
			watcher.addIgnorePath(toIgnore);
		}
	}

	@Override
	public void replicationDisabled(IFileStoreWatcher watcher, Session session) {
	}

}
