package org.wcs.smart.connect.uploader;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.type.PostgresUUIDType;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.UploadItem;
import org.wcs.smart.connect.model.UploadItem.Status;
import org.wcs.smart.connect.model.UploadItem.Type;


public class CaLoader implements IUploadItemProcessor {
	
	private final Logger logger = Logger.getLogger(CaLoader.class.getName());
	
	@Override
	public Type getSupportedType() {
		return UploadItem.Type.CA;
	}

	@Override
	public void processItem(UploadItem item, Session session) {
		session.beginTransaction();
		
		try{
			session.update(item);
		
			//load data
			PostgresqlCaLoader ldr = new PostgresqlCaLoader(session);
			ldr.importData(DataStoreManager.INSTANCE.getFile(item.getLocalFilename()));
			session.flush();
			
			//update ca item label
			ConservationAreaInfo info = (ConservationAreaInfo) session.get(ConservationAreaInfo.class, item.getConservationAreaInfo().getUuid());
			String sql = "SELECT id, name from smart.conservation_area WHERE uuid = :uuid"; //$NON-NLS-1$
			List<?> data = session.createSQLQuery(sql).setParameter("uuid", info.getUuid(), PostgresUUIDType.INSTANCE).list(); //$NON-NLS-1$
			if (data.size() > 0){
				info.setLabel( ((Object[])data.get(0))[1].toString() + " [" + ((Object[])data.get(0))[0].toString() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			info.setStatus(ConservationAreaInfo.Status.DATA);
			//update item status
			item.setStatus(Status.COMPLETE);
			
			session.getTransaction().commit();
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			ex.printStackTrace();
			session.getTransaction().rollback();
			
			//start a new transaction; update status to error
			session.beginTransaction();
			try{
				session.update(item);
				item.setStatus(Status.ERROR);
				item.setMessage("Error extracting data, " + ex.getMessage());
				session.getTransaction().commit();
			}catch (Exception ex2){
				logger.log(Level.SEVERE, ex2.getMessage(), ex2);
				session.getTransaction().rollback();
			}
		}
	}

}
