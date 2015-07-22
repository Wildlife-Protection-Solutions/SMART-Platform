package org.wcs.smart.connect.uploader;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.connect.model.UploadItem;
import org.wcs.smart.connect.model.UploadItem.Status;

public enum ItemProcessManager {

	INSTANCE;
	
	private static List<IUploadItemProcessor> processors = new ArrayList<IUploadItemProcessor>();
	static{
		processors.add(new CaLoader());
	};
	
	
	public void processItem(UploadItem item, Session session) throws Exception{
		
		//update status
		session.beginTransaction();
		session.update(item);
		item.setStatus(Status.PROCESSING);
		session.getTransaction().commit();
		
		IUploadItemProcessor processor = findProcessor(item);
		if (processor == null){
			item.setStatus(Status.ERROR);
			item.setMessage(MessageFormat.format("No processor found for the file type {0}", item.getType().toString()));
		}
		processor.processItem(item, session);
	}
	
	private IUploadItemProcessor findProcessor(UploadItem item){
		for (IUploadItemProcessor p : processors){
			if (p.getSupportedType().equals(item.getType())){
				return p;
			}
		}
		return null;
	}
}
