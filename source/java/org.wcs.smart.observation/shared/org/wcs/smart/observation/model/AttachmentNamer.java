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
package org.wcs.smart.observation.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Tools for naming exported attachments. This targets waypoint and observation
 * attachments by adding date/time and id information to the name, but can be used
 * for any attachment.
 */
public enum AttachmentNamer {
	
	INSTANCE;
	
	//attachment name date/time format
	private static final DateTimeFormatter dtformatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss"); //$NON-NLS-1$
	
	/**
	 * For exporting attachments this computes a unique name in the directory for the attachment
	 * that is of the form:
	 *  ObservationAttachment: yyyyMMddHhmmss.<wpid>.<obsuuid>.<wpuuid>.<filename>.<ext>
	 *  WaypointAttachment: yyyyMMddHhmmss.<wpid>.<wpuuid>.<filename>.<ext>
	 *  Others: <filename>.<ext>
	 *  
	 * To avoid duplicates a number may be added before the extension <filename>.<unq>.<ext> 
	 * 
	 * @param attachment
	 * @param outputDir
	 * @return
	 */
	public Path createUniqueFilenameForExport(ISmartAttachment attachment, Path outputDir) {
		
		String filename = null;
		if (attachment instanceof ObservationAttachment oa) {
			  filename = oa.getObservation().getWaypoint().getDateTime().format(dtformatter)
					  + "." + cleanId(oa.getObservation().getWaypoint().getId()) //$NON-NLS-1$
					  + "." + UuidUtils.uuidToString(oa.getObservation().getUuid())  //$NON-NLS-1$
					  + "." + UuidUtils.uuidToString(oa.getObservation().getWaypoint().getUuid()); //$NON-NLS-1$
			
		}else if (attachment instanceof WaypointAttachment wa) {
			filename = wa.getWaypoint().getDateTime().format(dtformatter)
						+ "." + cleanId(wa.getWaypoint().getId()) //$NON-NLS-1$
						+ "." + UuidUtils.uuidToString(wa.getWaypoint().getUuid()); //$NON-NLS-1$
		}
		filename = filename + "." + SharedUtils.getFilenameWithoutExtension(attachment.getFilename()); //$NON-NLS-1$
		String ext = SharedUtils.getFilenameExtension(attachment.getFilename());
		
		//ensure unique filename
		Path toFile = outputDir.resolve(filename + "." + ext); //$NON-NLS-1$
		int i = 1;
		while(i < 5000 && Files.exists(toFile)) {
			toFile = outputDir.resolve(filename + "." + i + "." + ext); //$NON-NLS-1$ //$NON-NLS-2$
			i++;
		}
		return toFile;		
	}
	

	private String cleanId(String id) {
		return id.replaceAll("[^\\p{L}\\p{Nd}_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
