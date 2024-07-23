/*
 * Copyright (C) 2024 Wildlife Conservation Society
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.InflaterInputStream;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.api.DataQueueEventService;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.cybertracker.SmartMobileDeviceManager;
import org.wcs.smart.cybertracker.model.SmartMobileDevice;

/**
 * Background job for parsing SMART Mobile device ids from
 * SMART Mobile data files.
 */
public enum SmartMobileDeviceIdParserManager {
	
	INSTANCE;

	//background job
	private Runnable processingJob = new Runnable() {
		@Override
		public void run() {
			doWork();
		}		
	};
	
	private Future<?> jobFuture = null;

	private List<Object[]> toProcess = Collections.synchronizedList(new ArrayList<>());
	
	/**
	 * Adds and item to list and start processing if necessary
	 * 
	 * @param item
	 * @param sessionFactory
	 */
	public synchronized void startProcessing(ServerDataQueueItem item, SessionFactory sessionFactory) {
		
		toProcess.add(new Object[] {item, sessionFactory});
		
		if (jobFuture == null || jobFuture.isDone() || jobFuture.isCancelled()) {
			//schedule job
			ExecutorService executor = SmartContext.INSTANCE.getClass(ExecutorService.class);
			this.jobFuture = executor.submit(processingJob);
		}		
	}

	private void doWork() {
		while(!toProcess.isEmpty()) {
			Object[] data = toProcess.remove(0);
			ServerDataQueueItem item = (ServerDataQueueItem) data[0];
			SessionFactory factory = (SessionFactory) data[1];
			
			Path file = DataStoreManager.INSTANCE.getFile(item.getFile());
			boolean deleteOnExit = false;
			String pattern = "\"deviceId\": \"([0-9a-zA-Z]*)\""; //$NON-NLS-1$
			Matcher matcher = Pattern.compile(pattern).matcher(""); //$NON-NLS-1$
			
			Set<String> deviceIds = new HashSet<>();
			
			if (item.getType().equals(SmartMobileJsonFileProcessor.CT_ZIP_TYPE)) {
				try {
					Path decoded = DataStoreManager.INSTANCE.getTemporaryDirectory()
							.resolve(item.getUuid().toString() + System.nanoTime() + ".temp"); //$NON-NLS-1$
					try (InputStream in = new InflaterInputStream(Files.newInputStream(file));
							OutputStream out = Files.newOutputStream(decoded)) {
						IOUtils.copy(in, out);					
					} 
					file = decoded;
					deleteOnExit = true;
				}catch (IOException ex) {
					//log and move on
					Logger.getLogger(SmartMobileDeviceIdParserManager.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
					continue;
				}
			}
			try(Stream<String> lines = Files.lines(file)){
				lines.forEach(line->{
					if (matcher.reset(line).find()) {
						deviceIds.add(matcher.group(1));						
					}
				});
			}catch (Exception ex) {
				Logger.getLogger(SmartMobileDeviceIdParserManager.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
				continue;
			}
			
			if (deleteOnExit) {
				try {
					Files.delete(file);
				}catch (IOException ex) {
					Logger.getLogger(SmartMobileDeviceIdParserManager.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
				}
			}
			
			try(Session session = factory.openSession()){
			
				ConservationArea ca = session.get(ConservationArea.class, item.getConservationArea());
				StringJoiner devices = new StringJoiner(", "); //$NON-NLS-1$
				for (String dId : deviceIds) {
					SmartMobileDevice device = SmartMobileDeviceManager.INSTANCE.findDevice(session, dId, ca);
					if (device != null) {
						devices.add(device.getName());
					}else {
						devices.add(dId);
					}
				}
			
				session.beginTransaction();
				try {
					ServerDataQueueItem i = session.get(ServerDataQueueItem.class, item.getUuid());
					i.setUploadedBy(MessageFormat.format("{0}: {1}", i.getUploadedBy(), devices.toString())); //$NON-NLS-1$
					session.getTransaction().commit();
				}catch (Exception ex) {
					Logger.getLogger(SmartMobileDeviceIdParserManager.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
					continue;
				}
			}
			
			DataQueueEventService.addUpdateToQueue(item);
		}
	}
}
