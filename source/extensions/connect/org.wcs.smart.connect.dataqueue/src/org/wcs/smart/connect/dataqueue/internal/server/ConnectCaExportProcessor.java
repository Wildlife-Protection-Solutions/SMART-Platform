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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.hibernate.Session;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.server.ICaExportPreprocessor;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * A Conservation Area export processor that removes the data queue
 * table and any local data queue items.  These items are not to be sent 
 * to Connect.
 * 
 * @author Emily
 *
 */
public class ConnectCaExportProcessor implements ICaExportPreprocessor{
	
	private static Class<?>[] classesToRemove = new Class<?>[]{
		LocalDataQueueItem.class
	};
	
	@Override
	public void processExport(ICaDataExportEngine engine) {
		try(Session s = HibernateManager.openSession()){
			for (Class<?> c : classesToRemove){
				
				String tableName = HibernateManager.getTableName(c);
				
				String fileName1 = tableName + "." + c.getSimpleName() + ".def"; //$NON-NLS-1$ //$NON-NLS-2$
				String fileName2 = tableName + "." + c.getSimpleName() + ".dat"; //$NON-NLS-1$ //$NON-NLS-2$
				
				for (String filename : new String[]{fileName1, fileName2}){
					Path f = engine.getWorkingLocation().resolve(ICaDataExportEngine.DATABASE_DIR).resolve(filename);
					engine.excludePath(f);
				}	
			}
		}

		//we also want to remove any items from the dataqueue folder in the data store
		Path dataqueue = Paths.get(engine.getConservationArea().getFileDataStoreLocation()).resolve(ConnectDataQueuePlugin.DATA_QUEUE_DIR);
		engine.excludePath(dataqueue);
	}
}
