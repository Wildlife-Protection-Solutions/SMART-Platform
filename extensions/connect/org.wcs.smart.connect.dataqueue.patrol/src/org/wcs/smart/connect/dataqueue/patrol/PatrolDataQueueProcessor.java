package org.wcs.smart.connect.dataqueue.patrol;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem.Type;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.xml.in.ImportConfig;
import org.wcs.smart.patrol.xml.in.PatrolImporter;

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
		
		Path file = FileSystems.getDefault()
				.getPath(SmartContext.INSTANCE.getFilestoreLocation())
				.resolve(lItem.getFile());

		ImportConfig config = new ImportConfig();
		config.setIgnoreWarnings(false);
		config.setKeepIDs(true);
		
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
