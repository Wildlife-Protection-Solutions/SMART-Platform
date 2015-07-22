package org.wcs.smart.connect.uploader;

import org.hibernate.Session;
import org.wcs.smart.connect.model.UploadItem;

public interface IUploadItemProcessor {

	public UploadItem.Type getSupportedType();
	
	public void processItem(UploadItem item, Session session);
}
