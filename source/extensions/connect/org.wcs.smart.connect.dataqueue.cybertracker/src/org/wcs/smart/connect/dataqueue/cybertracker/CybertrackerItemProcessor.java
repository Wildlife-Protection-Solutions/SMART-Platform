package org.wcs.smart.connect.dataqueue.cybertracker;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.ListJoin;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem.Type;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem.Status;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.util.ZLibUtil;
import org.wcs.smart.hibernate.HibernateManager;

public class CybertrackerItemProcessor implements IItemProcessor {

	public CybertrackerItemProcessor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean canProcess(Type type) {
		return type == Type.JSON_CT || type == Type.JSON_ZLIB_CT;
	}

	@Override
	public ProcessingStatus process(DataQueueItem item, IProgressMonitor monitor)
			throws Exception {
		LocalDataQueueItem litem = (LocalDataQueueItem)item;
		
		String json = null;
		if (item.getType() == Type.JSON_ZLIB_CT){
			json = ZLibUtil.decompressFile(litem.getFullFilePath().toFile());
		}else if (item.getType() == Type.JSON_CT){
			try(Reader in = Files.newBufferedReader(litem.getFullFilePath(), StandardCharsets.UTF_8)){
				json = IOUtils.toString(in);
			}
		}

		List<JSONObject> features = (new JsonCtParser()).parseFeaturesFromJsonString(json);
		
		
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			
			List<JSONObject> notProc = new ArrayList<JSONObject>();
			notProc.addAll(features);
			
			List<IJsonProcessor> processors = getProcessors();
			for (IJsonProcessor p : processors){
				
				List<JSONObject> processed = p.processJson(features, session);
				notProc.removeAll(processed);
			}

			StringBuilder sb = new StringBuilder();
			sb.append("The following " + notProc.size() + " features could not be processed.  If you continue these data will NOT be imported into SMART.");
			for (JSONObject o : notProc){
				sb.append(o.toJSONString());
				sb.append("\n\n");
			}
			sb.append("Do you want to continue?");
			final boolean[] cont = new boolean[]{true};
			if (!notProc.isEmpty()){
				//TODO:
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
						cont[0] = MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
								"ERROR", sb.toString());
					}
					
				});
			}
			if (!cont[0]){
				session.getTransaction().rollback();
				ProcessingStatus status = new ProcessingStatus(Status.ERROR, "User cancelled because not all data could be processed.");
				return status;
			}
			
			session.getTransaction().commit();
			
			ProcessingStatus status = new ProcessingStatus(Status.COMPLETE, "Data loaded into SMART");
			
			
			for (IJsonProcessor p : processors){
				try{
					p.afterSave();
				}catch (Throwable t){
					CyberTrackerPlugIn.displayError("Error", t.getMessage(), t);
				}
			}
			return status;
		}catch (Exception ex){
			ex.printStackTrace();
			session.getTransaction().rollback();
			
			ProcessingStatus status = new ProcessingStatus(Status.ERROR, "Error processing data: " + ex.getMessage());
			return status;
		}finally{
			session.close();
		}
	}

	private static volatile List<IJsonProcessor> jsonProcessors;
	
	private static List<IJsonProcessor> getProcessors() throws Exception{
		if (jsonProcessors == null){
			synchronized (CybertrackerItemProcessor.class) {
				if (jsonProcessors == null){
					jsonProcessors = new ArrayList<IJsonProcessor>();
					IConfigurationElement[] config = Platform.getExtensionRegistry()
							.getConfigurationElementsFor(IJsonProcessor.EXTENSION_ID);
					
					for (IConfigurationElement e : config) {
						if (e.getName().equalsIgnoreCase("JsonProcessor")){
							IJsonProcessor proc = (IJsonProcessor) e.createExecutableExtension("class");
							jsonProcessors.add(proc);
						}
					}
				}
			}
			
		}
		List<IJsonProcessor> copy = new ArrayList<IJsonProcessor>(jsonProcessors.size());
		for (IJsonProcessor p : jsonProcessors){
			copy.add(p.getClass().newInstance());
		}
		return copy;
	}
}
