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
package org.wcs.smart.connect.dataqueue.er;

import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.er.internal.Messages;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem.Type;
import org.wcs.smart.connect.dataqueue.model.DataQueueProcessingOption;
import org.wcs.smart.connect.dataqueue.model.DataQueueProcessingOption.DataQueueProcessingOptionPk;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.xml.MissionImporter;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Mission data queue processor for importing
 * missions from SMART Connect data queue.
 * 
 * @author Emily
 *
 */
public class MissionProcessor implements IItemProcessor {


	public MissionProcessor() {
	}

	@Override
	public boolean canProcess(Type type) {
		return (type == Type.MISSION_XML);
	}

	@Override
	public ProcessingStatus process(DataQueueItem item, IProgressMonitor monitor)
			throws Exception {
		LocalDataQueueItem lItem = (LocalDataQueueItem)item;
		
		Path file = lItem.getFullFilePath();

		DataQueueProcessingOption keepIdoption = null;
		Session s = HibernateManager.openSession();
		try{
			DataQueueProcessingOptionPk pk = new DataQueueProcessingOptionPk();
			pk.setConservationArea(item.getConservationArea());
			pk.setOptionKey(ErDataQueueProcessorOption.ER_GENERATE_IDS.name());
			keepIdoption = (DataQueueProcessingOption)s.get(DataQueueProcessingOption.class, pk);
		}finally{
			s.close();
		}
		
		//fire events
		Mission mission = MissionImporter.importMission(file.toFile(), !ErDataQueueProcessorOption.ER_GENERATE_IDS.getValueAsBoolean(keepIdoption), monitor);
		if (mission != null){
			try{
				SurveyEventHandler.getInstance().fireEvent(SurveyEventHandler.EventType.MISSION_ADDED, mission);
			}catch (Exception ex){
				ConnectDataQueuePlugin.log("Error firing mission added event after importing mission from connect data queue.", ex); //$NON-NLS-1$
			}
			return new ProcessingStatus(LocalDataQueueItem.Status.COMPLETE, MessageFormat.format(Messages.MissionProcessor_MissionImported, mission.getId()));
		}else{
			return new ProcessingStatus(LocalDataQueueItem.Status.COMPLETE_WARN, Messages.MissionProcessor_NotImported);
		}			
		
	}

}