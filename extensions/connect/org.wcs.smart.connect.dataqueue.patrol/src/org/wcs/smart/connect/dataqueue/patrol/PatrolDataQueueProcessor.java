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
package org.wcs.smart.connect.dataqueue.patrol;

import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem.Type;
import org.wcs.smart.connect.dataqueue.model.DataQueueProcessingOption;
import org.wcs.smart.connect.dataqueue.model.DataQueueProcessingOption.DataQueueProcessingOptionPk;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.xml.in.ImportConfig;
import org.wcs.smart.patrol.xml.in.PatrolImporter;

/**
 * Data queue processor for processing patrol files.
 * @author Emily
 *
 */
public class PatrolDataQueueProcessor implements IItemProcessor {


	public PatrolDataQueueProcessor() {
	}

	@Override
	public boolean canProcess(Type type) {
		return (type == Type.PATROL_XML);
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
			pk.setOptionKey(PatrolDataQueueProcessorOption.GENERATE_IDS.name());
			keepIdoption = (DataQueueProcessingOption)s.get(DataQueueProcessingOption.class, pk);
		}finally{
			s.close();
		}
		
		ImportConfig config = new ImportConfig();
		config.setIgnoreWarnings(false);
		config.setKeepIDs(!PatrolDataQueueProcessorOption.GENERATE_IDS.getValueAsBoolean(keepIdoption));
		
		//fire events
		Patrol p = PatrolImporter.importPatrol(file.toFile(), config, monitor);
		if (p != null){
			try{
				PatrolEventManager.getInstance().patrolAdded(p);
			}catch (Exception ex){
				
			}
			return new ProcessingStatus(LocalDataQueueItem.Status.COMPLETE, MessageFormat.format("1 Patrol loaded: {0}", p.getId()));
		}else{
			return new ProcessingStatus(LocalDataQueueItem.Status.COMPLETE_WARN, "0 Patrols loaded (user cancelled).");
		}
		
	}

}
