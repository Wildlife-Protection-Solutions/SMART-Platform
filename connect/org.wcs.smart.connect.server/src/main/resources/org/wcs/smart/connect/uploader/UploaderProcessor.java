package org.wcs.smart.connect.uploader;

import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.connect.model.UploadItem;

public class UploaderProcessor implements Runnable {

	private final Logger logger = Logger.getLogger(UploaderProcessor.class.getName());
	
	private UploadItem item;
	private Session session;
	private SessionFactory factory;
	
	public UploaderProcessor(UploadItem item, SessionFactory factory){
		this.item = item;
		this.factory = factory;
	}
	@Override
	public void run() {
		session = factory.openSession();
		try{
			ItemProcessManager.INSTANCE.processItem(item, session);
		}catch (Exception ex){
			logger.severe(ex.getMessage());
		}finally{
			session.close();
		}
		
	}

}
