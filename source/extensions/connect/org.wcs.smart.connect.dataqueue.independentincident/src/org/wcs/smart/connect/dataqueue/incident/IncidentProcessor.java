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
package org.wcs.smart.connect.dataqueue.incident;

import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.incident.internal.Messages;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem.Type;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.xml.IncidentImporter;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Independent incident data queue processor for importing
 * incidents.
 * 
 * @author Emily
 *
 */
public class IncidentProcessor implements IItemProcessor {


	public IncidentProcessor() {
	}

	@Override
	public boolean canProcess(Type type) {
		return (type == Type.INCIDENT_XML);
	}

	@Override
	public ProcessingStatus process(DataQueueItem item, IProgressMonitor monitor)
			throws Exception {
		LocalDataQueueItem litem = (LocalDataQueueItem)item;
		Path file = litem.getFullFilePath();
		
		Waypoint wp = IncidentImporter.importIncident(file.toFile(), monitor);
		if (wp == null){
			return new ProcessingStatus(LocalDataQueueItem.Status.COMPLETE_WARN, Messages.IncidentProcessor_nothingloaded);	
		}else{
			try{
				IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_ADDED, wp);
			}catch (Exception ex){
				ConnectDataQueuePlugin.log(Messages.IncidentProcessor_, ex);
			}
			return new ProcessingStatus(LocalDataQueueItem.Status.COMPLETE, MessageFormat.format(Messages.IncidentProcessor_incidentloaded, wp.getId()));			
		}
	}

}