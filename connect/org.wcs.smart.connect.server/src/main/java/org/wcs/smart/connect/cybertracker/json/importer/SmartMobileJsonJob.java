package org.wcs.smart.connect.cybertracker.json.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.wcs.smart.connect.api.DataQueueEventService;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;

import jakarta.persistence.Tuple;

public class SmartMobileJsonJob implements Runnable{

	public static final int MAX_TRIES = 5;
	
	private SessionFactory factory;
	
	private HashMap<UUID, Integer> processedCount;
	private Set<UUID> toSkip;
	
	public SmartMobileJsonJob(SessionFactory factory) {
		this.factory = factory;
		this.processedCount = new HashMap<>();
		toSkip = new HashSet<>();
	}
	
	@Override
	public void run() {
		List<ServerDataQueueItem> last = null;
		
		while(true) {
			
			List<ServerDataQueueItem> toProcess = null;
			
			try(Session session = factory.openSession()){
				
				//1. find all queued data queue items with appropriate type
				//in order of upload - processed older files first
				List<String> types = new ArrayList<>();
				types.add(SmartMobileJsonFileProcessor.CT_TYPE);
				types.add(SmartMobileJsonFileProcessor.CT_ZIP_TYPE);
				
				Query<ServerDataQueueItem> q = null;
				if (toSkip.isEmpty()) {
					q = session.createQuery("FROM ServerDataQueueItem WHERE type in (:types) and status = :status order by uploadedDate asc", ServerDataQueueItem.class)
							.setParameter("types", types)
							.setParameter("status", ServerDataQueueItem.Status.QUEUED);
				}else {
					q = session.createQuery("FROM ServerDataQueueItem WHERE type in (:types) and status = :status and uuid not in (:toskip) order by uploadedDate asc", ServerDataQueueItem.class)
							.setParameter("types", types)
							.setParameter("status", ServerDataQueueItem.Status.QUEUED)
							.setParameter("toskip", toSkip);
							
				}
				toProcess = q.list();
			}
			//nothing more to process
			if (toProcess.isEmpty()) return;
			
			//if last array same as current array then stop
			if (last != null) {
				if (last.size() == toProcess.size()) {
					boolean aresame = true;
					for (int i = 0; i < last.size(); i ++) {
						if (!last.get(i).equals(toProcess.get(i))) {
							aresame = false;
							break;
						}
					}
					if (aresame) return;
				}
			}
			last = new ArrayList<>(toProcess);
			
			for (ServerDataQueueItem item : toProcess) {
				
				
				try(Session session = factory.openSession()){
					
					if (!processedCount.containsKey(item.getUuid())) {
						processedCount.put(item.getUuid(), 0);
					}
					int newcnt = processedCount.get(item.getUuid()) + 1;
					processedCount.put(item.getUuid(), newcnt);
					if (newcnt > MAX_TRIES) {
						//if we keep reprocessing the same item over and over again we probably want to just stop until a new file is uploaded
						//attempt more than X processing attempts skip this file for remainder of while loop
						toSkip.add(item.getUuid());
					}
				
					//2. check it out
					session.beginTransaction();
					Object x = session.createNativeQuery("UPDATE connect.data_queue SET status = :ps WHERE uuid = :uuid and status = :qu RETURNING *  ", Tuple.class)
							.setParameter("ps", ServerDataQueueItem.Status.PROCESSING.name())
							.setParameter("uuid", item.getUuid())
							.setParameter("qu", ServerDataQueueItem.Status.QUEUED.name())
							.getSingleResult();
					session.getTransaction().commit();
					
					if (x == null) {
						//item already updated
						continue;
					}				
					//evict
					session.evict(item);
					
					//send event
					item = session.get(item.getClass(), item.getUuid());
					DataQueueEventService.addUpdateToQueue(item);
				}

				//3. process it
				processItem(item);
			}

			//4. repeat
		}
		
		
		//TODO: find any items that are old than say 3 months and not processed and set to error ???
		

	}
	
	private void processItem(ServerDataQueueItem item) {
		
		if (item == null) return;
		
		if (!SmartMobileJsonFileProcessor.canProcess(item.getType())) return;
		
		SmartMobileJsonFileProcessor.create(item, factory).process();
	}

	
}
