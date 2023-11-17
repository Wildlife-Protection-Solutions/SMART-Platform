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
package org.wcs.smart.connect.replication;

import java.nio.file.Path;
import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.replication.metadata.MetadataPackager;
import org.wcs.smart.connect.replication.metadata.PackageMetadata;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Metadata packager which accompanies all upload and downloads from the server.
 * 
 * @author Emily
 *
 */
public enum DerbyMetadataPackager {

	INSTANCE;
	
	public void generateMetadata(Session session, ConnectServer server, 
			Path file, long revision) throws Exception{
		
		ConservationArea ca = session.get(ConservationArea.class, server.getConservationArea().getUuid());
		
		ConnectServerStatus status = (ConnectServerStatus) session.get(ConnectServerStatus.class, ca.getUuid());
		if (status == null){
			 throw new Exception(Messages.DerbyMetadataPackager_ServerStatusError);
		}
		
		PackageMetadata metadata = new PackageMetadata();
		metadata.setConservationArea(ca.getUuid());
		metadata.setVersion(status.getVersion());
		metadata.setClientRevision(revision);
		metadata.setServerRevision(status.getServerRevision());
		
		//plugin versions
		metadata.setPluginVersions(getLocalPluginVersions(session));
		
		MetadataPackager.INSTANCE.writeMetadata(file, metadata);
	}
	
	public HashMap<String, String> getLocalPluginVersions(Session session){
		return HibernateManager.getPlugInVersions(session);
	}
}
