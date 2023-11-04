package org.wcs.smart.connect.cybertracker.json.importer;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.api.DataQueueEventService;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem.Status;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.hibernate.AttachmentInterceptor;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.json.IJsonPostProcessor;
import org.wcs.smart.cybertracker.json.IJsonProcessor;
import org.wcs.smart.cybertracker.json.JsonImportWarning;

public class SmartMobileJsonFileProcessor {
	
	private final Logger logger = Logger.getLogger(SmartMobileJsonFileProcessor.class.getName());

	public static final String CT_TYPE = "JSON_CT"; //$NON-NLS-1$
	public static final String CT_ZIP_TYPE = "JSON_ZLIB_CT"; //$NON-NLS-1$

	private ServerDataQueueItem item;
	private SessionFactory factory;
	
	public static SmartMobileJsonFileProcessor create(ServerDataQueueItem item, SessionFactory factory) {
		return new SmartMobileJsonFileProcessor(item, factory);
	}

	public static boolean canProcess(String type) {
		return (type.toUpperCase(Locale.ROOT).equals(CT_TYPE) || 
				type.toUpperCase(Locale.ROOT).equals(CT_ZIP_TYPE));
	}
	
	private SmartMobileJsonFileProcessor(ServerDataQueueItem item, SessionFactory factory) {
		this.item = item;
		this.factory = factory;
	}

	public void process()  {

		try(Session session = HibernateManager.newSession(factory, Locale.getDefault(), new AttachmentInterceptor())){
				
			this.item = session.get(item.getClass(), item.getUuid());
			
			//extract JSON
			String json = extractJson(session);
			if (json == null) return;
			
			//process file
			run(json, session);	
		}
		
		postProcess();
		
		return;
				
	}
	
	private String extractJson(Session session) {
		try {
			Path path = DataStoreManager.INSTANCE.getFile(item.getFile());
	
			if (item.getType().toUpperCase(Locale.ROOT).equals(CT_ZIP_TYPE)){			
		    	try (InputStream in = new InflaterInputStream(Files.newInputStream(path))) {
					return IOUtils.toString(in, "UTF-8"); //$NON-NLS-1$
				} 
			}else if (item.getType().toUpperCase(Locale.ROOT).equals(CT_TYPE)){
				try(Reader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
					return IOUtils.toString(in);
				}
			}
			throw new Exception("No JSON features extracted from file.");
			
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(),ex);
			updateItemStatus(session, Status.ERROR, "Could not extract json data from file. See server log for full details.", null);
		}
		return null;

	}

	private void postProcess() {
		try (Session session = factory.openSession()){
			for (IJsonPostProcessor p : SmartMobileJsonProcessorManager.INSTANCE.getPostProcessors()) {
				try {
					p.postProcess(session);
				}catch (Exception ex) {
					logger.log(Level.SEVERE, ex.getMessage(), ex);
				}
			}
		}
	}

	
	private void run(String json, Session session) {
		
		List<JSONObject> features = null;
		try {
			features = CtJsonUtil.parseFeaturesFromJsonString(json);
		}catch (Exception ex ) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			updateItemStatus(session,Status.ERROR,MessageFormat.format("Could not parse json data from file: {0}", ex.getMessage()), null);
			return;
		}

		session.beginTransaction();
		try {
			item = session.get(item.getClass(), item.getUuid());
			ConservationArea ca = session.get(ConservationArea.class, item.getConservationArea());
			
			List<JSONObject> notProc = new ArrayList<JSONObject>();
			notProc.addAll(features);
			
			StringBuilder statusMsg = new StringBuilder();
			
			List<JsonImportWarning> warnings = new ArrayList<>();
			
			IJsonProcessor[] processors = SmartMobileJsonProcessorManager.INSTANCE.getProcessors(ca, session);
			for (IJsonProcessor p : processors){
				List<JSONObject> processed = p.processJson(features, session);
				notProc.removeAll(processed);
				String msg = p.getStatusMessage();
				if (msg != null) statusMsg.append(msg);
				
				warnings.addAll(p.getWarnings());
			}
				
			if (notProc.size() == features.size()){
				//nothing has been processed perhaps we re-queue this item
				//for later as maybe something else needs to be processed first
				session.getTransaction().rollback();

				updateItemStatus(session,Status.QUEUED, MessageFormat.format("No data from file could be processed ({0}). Item requeued.", LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) ),warnings);
				return ;
			}

			if (!notProc.isEmpty()){
				//not all items have been processed
				StringBuilder sb = new StringBuilder();
				sb.append(MessageFormat.format("{0} features were processed. {1} features could not be processed. These features will be dropped: ", features.size(), notProc.size()));
				for (JSONObject o : notProc){
					sb.append(o.toJSONString() + "; ");
				}
				sb.deleteCharAt(sb.length()-1);
				sb.deleteCharAt(sb.length()-1);
				
				warnings.add(new JsonImportWarning(sb.toString()));
			}
			
			if (warnings.isEmpty()) {
				item.setStatus(Status.COMPLETE);
			}else {
				item.setStatus(Status.COMPLETE_WARN);
			}
			item.setStatusMessage(statusMsg.toString());
			item.setWarningMessages(null);
			for (JsonImportWarning warn : warnings) {
				item.addWarningMessage(warn.getMessage());
			}
			
			session.getTransaction().commit();

			DataQueueEventService.addUpdateToQueue(item);
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);

			try {
				session.getTransaction().rollback();
			}catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
				return;
			}

			String message = MessageFormat.format("Error processing data in file: {0}", ex.getMessage() );
			updateItemStatus(session,  Status.ERROR, message, null);
		}

		
	}
	
	private void updateItemStatus(Session session, Status newStatus, String message, List<JsonImportWarning> warnings) {
		session.beginTransaction();
		try {
			item = session.get(item.getClass(), item.getUuid());
			//update item details
			item.setStatus(newStatus);
			
			item.setStatusMessage(message);
			
			//add any warnings as these may help identify why the data could not be processed
			item.setWarningMessages(null);
			if (warnings != null) {
				for (JsonImportWarning warn : warnings) {
					item.addWarningMessage(warn.getMessage());
				}
			}
			
			session.getTransaction().commit();
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		}
		DataQueueEventService.addUpdateToQueue(item);
	}
}
