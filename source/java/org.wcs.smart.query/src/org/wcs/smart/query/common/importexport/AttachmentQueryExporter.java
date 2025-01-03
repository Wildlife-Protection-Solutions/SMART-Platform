/*
 * Copyright (C) 2025 Wildlife Conservation Society
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
package org.wcs.smart.query.common.importexport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.AttachmentNamer;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.ObservationQueryResult;
import org.wcs.smart.query.common.engine.WaypointQueryResult;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.common.model.WaypointQuery;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.util.SmartUtils;

/**
 * Supports the exporting of query attachments associated with Waypoint 
 * or Observation queries only.
 * 
 * NOTE: this has not been added to the query exports extension point and 
 * is not directly available in the the export wizard, but rather integrated
 * into the CSV export option.
 */
public class AttachmentQueryExporter implements IQueryExporter{

	public static final AttachmentQueryExporter INSTANCE = new AttachmentQueryExporter();
	
	//postfix dir name
	public static final String OUTPUT_DIR = "attachments"; //$NON-NLS-1$
	

	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		return (query instanceof WaypointQuery || query instanceof ObservationQuery);		
	}

	@Override
	public String getId() {
		return "org.wcs.smart.query.attachment.exporter"; //$NON-NLS-1$
	}

	@Override
	public boolean supportsProjection() {
		return false;
	}

	@Override
	public String getName() {
		return Messages.AttachmentQueryExporter_ExporterName;
	}

	@Override
	public String getDefaultExtension() {
		return null;
	}

	@Override
	public void export(Query query, IQueryResult results, Path file, Map<String, Object> parameters,
			IProgressMonitor monitor) throws Exception {
		
		//clean out output directory
		if (Files.exists(file)) {
			if (!Files.isDirectory(file)) {
				throw new Exception(Messages.AttachmentQueryExporter_NotADirectoryError);
			}
			SmartUtils.deleteDirectory(file);
		}
		
		SmartUtils.createDirectory(file);
		
		try(Session session = HibernateManager.openSession()){
			
			int total = 0;
			//these are either WaypointAttachment or ObservationAttachment objects
			IQueryResultSetIterator<? extends IAttachmentResultItem> itr = null;
			if (results instanceof ObservationQueryResult<?> r) {
				itr = r.getImageIterator(session);
				total = r.getImageCount();
			}else if (results instanceof WaypointQueryResult<?> r) {
				itr = r.getImageIterator(session);
				total = r.getImageCount();
			}
			SubMonitor sub = SubMonitor.convert(monitor);
			sub.subTask(Messages.AttachmentQueryExporter_taskname);
			sub.setWorkRemaining(total);
			while(itr.hasNext()) {
				IAttachmentResultItem item = itr.next();
			  	ISmartAttachment attachment = item.getAttachment();
				Path toFile = AttachmentNamer.INSTANCE.createUniqueFilenameForExport(attachment, file);
				EncryptUtils.decryptAttachment(attachment, toFile);
				sub.split(1);
			}
		}
	}

}
