/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.incident;

import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

/**
 * Interface for incident xml exporters for plugins that
 * contribute additional incidents
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public interface IIncidentXmlExporter {

	/**
	 * Determines the output file name for incident export
	 * based on given input file and if attachments are included.
	 * 
	 * @param dir
	 * @param name
	 * @param includeAttributes
	 * @return
	 */
	public static Path getOutputFile(Path dir, String name, boolean includeAttachs) throws Exception {
		name = SmartUtils.getFileName(name);
		String ext = includeAttachs ? ".zip" : ".xml"; //$NON-NLS-1$ //$NON-NLS-2$
		return dir.resolve(name + ext);
	}
	
	/**
	 * Exports the given waypoint to the file.
	 * 
	 * @param incident
	 * @param file
	 * @param includeAttachments
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	public Path exportIncident(Waypoint incident, Path file, boolean includeAttachments, IProgressMonitor monitor) throws Exception;

}
