/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.uploader;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.PostgresUUIDType;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.replication.metadata.MetadataPackager;
import org.wcs.smart.connect.replication.metadata.PackageMetadata;

/**
 * Metadata packager for postgresql.
 * 
 * @author Emily
 *
 */
public class PostgresqlMetadataCreator {
	
	/**
	 * Generates a metadata package file for the given conservation
	 * area and revision.
	 * 
	 * @param session database session
	 * @param caUuid conservation area id
	 * @param file file to write to 
	 * @param revision the revision number to write to the metadata packagee
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void generateMetadata(Session session, 
			UUID caUuid, Path file, long revision) throws Exception{
		
		ConservationAreaInfo ca = (ConservationAreaInfo) session.load(ConservationAreaInfo.class, caUuid);
		if (ca == null){
			throw new Exception("Could not determine conservation area info.");
		}
		
		PackageMetadata metadata = new PackageMetadata();
		metadata.setConservationArea(caUuid);
		metadata.setVersion(ca.getVersion());
		metadata.setClientRevision(revision);
		metadata.setServerRevision(revision);
		
		//plugin versions
		SQLQuery q = session.createSQLQuery("SELECT version, plugin_id FROM connect.ca_plugin_version WHERE ca_uuid = :ca ");
		q.setParameter("ca", ca.getUuid(), PostgresUUIDType.INSTANCE);
		List<Object[]> plugins = q.list();
		for (Object[] version : plugins){
			metadata.setPluginVersion((String)version[1], (String)version[0]);
		}
		
		MetadataPackager.INSTANCE.writeMetadata(file, metadata);
	}
	
}
