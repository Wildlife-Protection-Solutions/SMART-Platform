/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
import java.util.function.Function;
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
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.json.IJsonPostProcessor;
import org.wcs.smart.cybertracker.json.IJsonProcessor;
import org.wcs.smart.cybertracker.json.JsonImportWarning;

/**
 * SMART Mobile json file processor - delegates processing to correct file. 
 * 
 * @author Emily
 *
 */
public class SmartMobileJsonFileProcessor {
	
	private final Logger logger = Logger.getLogger(SmartMobileJsonFileProcessor.class.getName());

	public static final String CT_TYPE = "JSON_CT"; //$NON-NLS-1$
	public static final String CT_ZIP_TYPE = "JSON_ZLIB_CT"; //$NON-NLS-1$

	private ServerDataQueueItem item;
	private SessionFactory factory;
	private Locale locale;
	
	public static SmartMobileJsonFileProcessor create(ServerDataQueueItem item, SessionFactory factory, Locale locale) {
		return new SmartMobileJsonFileProcessor(item, factory, locale);
	}

	public static boolean canProcess(String type) {
		return (type.toUpperCase(Locale.ROOT).equals(CT_TYPE) || 
				type.toUpperCase(Locale.ROOT).equals(CT_ZIP_TYPE));
	}
	
	private SmartMobileJsonFileProcessor(ServerDataQueueItem item, SessionFactory factory, Locale locale) {
		this.item = item;
		this.factory = factory;
		this.locale = locale;
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
			throw new Exception(Messages.getString("SmartMobileJsonFileProcessor.NoFeaturesException", locale)); //$NON-NLS-1$
			
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(),ex);
			updateItemStatus(session, Status.ERROR, Messages.getString("SmartMobileJsonFileProcessor.InvalidJsonError", locale), null); //$NON-NLS-1$
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
			updateItemStatus(session,Status.ERROR,MessageFormat.format(Messages.getString("SmartMobileJsonFileProcessor.JsonParseError", locale), ex.getMessage()), null); //$NON-NLS-1$
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
				List<JSONObject> processed = p.processJson(features, session, this.locale);
				notProc.removeAll(processed);
				String msg = p.getStatusMessage(this.locale);
				if (msg != null) statusMsg.append(msg);
				
				warnings.addAll(p.getWarnings());
			}
				
			if (notProc.size() == features.size()){
				//nothing has been processed perhaps we re-queue this item
				//for later as maybe something else needs to be processed first
				session.getTransaction().rollback();

				updateItemStatus(session,Status.QUEUED, MessageFormat.format(Messages.getString("SmartMobileJsonFileProcessor.NoFeatureProcessedError", locale), LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) ),warnings); //$NON-NLS-1$
				return ;
			}

			if (!notProc.isEmpty()){
				//not all items have been processed
				final int fsize = features.size();
				Function<Locale,String> warn = (l)->{ 
					StringBuilder sb = new StringBuilder();
					sb.append(MessageFormat.format(Messages.getString("SmartMobileJsonFileProcessor.FeatureProccessedCount", l), fsize, notProc.size())); //$NON-NLS-1$
					for (JSONObject o : notProc){
						sb.append(o.toJSONString() + "; "); //$NON-NLS-1$
					}
					sb.deleteCharAt(sb.length()-1);
					sb.deleteCharAt(sb.length()-1);
					return sb.toString();
				};
				
				warnings.add(new JsonImportWarning(warn));
			}
			
			if (warnings.isEmpty()) {
				item.setStatus(Status.COMPLETE);
			}else {
				item.setStatus(Status.COMPLETE_WARN);
			}
			item.setStatusMessage(statusMsg.toString());
			item.setWarningMessages(null);
			for (JsonImportWarning warn : warnings) {
				item.addWarningMessage(warn.getMessage(this.locale));
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

			String message = MessageFormat.format(Messages.getString("SmartMobileJsonFileProcessor.ProcessingError", locale), ex.getMessage() ); //$NON-NLS-1$
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
					item.addWarningMessage(warn.getMessage(this.locale));
				}
			}
			
			session.getTransaction().commit();
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		}
		DataQueueEventService.addUpdateToQueue(item);
	}
}
