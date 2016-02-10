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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.persister.entity.Joinable;
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
	public void processExport(File tempDirectory) {
		Session s = HibernateManager.openSession();
		try{
			for (Class<?> c : classesToRemove){
				String tableName = ((Joinable)s.getSessionFactory().getClassMetadata(c)).getTableName();
				
				String fileName1 = tableName + "." + c.getSimpleName() + ".def"; //$NON-NLS-1$ //$NON-NLS-2$
				String fileName2 = tableName + "." + c.getSimpleName() + ".dat"; //$NON-NLS-1$ //$NON-NLS-2$
				
				for (String filename : new String[]{fileName1, fileName2}){
					File f = new File(new File(tempDirectory, ICaDataExportEngine.DATABASE_DIR), filename);
					try{
						if (f.exists()) f.delete();
					}catch (Exception ex){
						f.delete();
					}
				}
					
			}
		}finally{
			s.close();
		}
		
		//we also want to remove any items from the dataqueue folder in the data store
		Path filestore = tempDirectory.toPath().resolve(ICaDataExportEngine.FILESTORE_DIR);
		
		Path dataqueue = filestore.resolve(ConnectDataQueuePlugin.DATA_QUEUE_DIR);
		if (Files.exists(dataqueue) && Files.isDirectory(dataqueue)){
			try{
				FileUtils.deleteDirectory(dataqueue.toFile());
			}catch(Exception ex){
				ConnectDataQueuePlugin.log("Failed to remove data queue folder from Conservation Area export.", ex);			 //$NON-NLS-1$
			}
		}
		
	}

}
