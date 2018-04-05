package org.wcs.smart.cybertracker.importer.json;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.hibernate.HibernateManager;

public class JsonFileProcessor {
	
	public static final String CT_TYPE = "JSON_CT"; //$NON-NLS-1$
	public static final String CT_ZIP_TYPE = "JSON_ZLIB_CT"; //$NON-NLS-1$
	
	private static volatile List<IJsonProcessor> jsonProcessors;
	
	public enum FileStatus{
		OK, 
		CANCELLED,
		NO_DATA,
		ERROR
	}

	public class FileState{
		Path file;
		FileStatus status;
		String message;
		Throwable ex;
		
		public FileState(Path file, FileStatus status, String message, Exception ex) {
			this.file = file;
			this.status = status;
			this.message = message;
			this.ex = ex;
		}
		public FileState(Path file, FileStatus status, String message) {
			this(file, status, message, null);
		}
		
		public FileState(Path file, FileStatus status) {
			this(file, status, "");
		}
		public FileState(Path file) {
			this(file, null, "");
		}
		
		public FileStatus getStatus() {
			return this.status;
		}
		public String getMessage() {
			return this.message;
		}
		public Throwable getException() {
			return this.ex;
		}
	}
	
	public JsonFileProcessor() {
	}

	
	public  HashMap<Path, FileState> process(List<Path> files) {
		final HashMap<Path, FileState> results = new HashMap<>();
		
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				
					SubMonitor progress = SubMonitor.convert(monitor, files.size());
					progress.setTaskName("Processing JSON Files");
					
					
					try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
						for (Path p : files) {
							try {
								FileState returnValue = process(p, session, progress.split(1));
								if (returnValue.ex != null) {
									CyberTrackerPlugIn.log(returnValue.ex.getMessage(), returnValue.ex);
								}
								results.put(p, returnValue);
							}catch(UserCancelledException cancelled) {
								throw cancelled;
							}
						}
					}catch (UserCancelledException ex) {
						for (Path p : files) {
							if (!results.containsKey(p)) {
								results.put(p, new FileState(p, FileStatus.CANCELLED));
							}
						}
					}
					
				}
				
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//summarize results with option to reprocess individual files
		return results;
	}
	
	private FileState process(Path file, Session session, IProgressMonitor monitor) throws UserCancelledException {
		String json = null;
		try(Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)){
			json = IOUtils.toString(in);
		}catch (Exception ex) {
			return new FileState(file, FileStatus.ERROR, MessageFormat.format("Unable to read JSON file.  Not a valid JSON. Note compressed JSON is not currently supported. ({0})", ex.getMessage()), ex);
		}
		
		FileState returnValue = new FileState(file);
		
		final List<JSONObject> features = new ArrayList<>();;
		try {
			features.addAll(JsonCtParser.parseFeaturesFromJsonString(json));
		}catch (Throwable ex) {
			returnValue.ex = ex;
			returnValue.status = FileStatus.ERROR;
			returnValue.message = MessageFormat.format("Error parsing features from JSON string {0}", ex.getMessage());
			return returnValue;
		}
		
		
		session.beginTransaction();
		try {
			List<JSONObject> notProc = new ArrayList<JSONObject>();
			notProc.addAll(features);
			
			
			List<IJsonProcessor> processors = getProcessors();
			
			StringBuilder statusMsg = new StringBuilder();
			for (IJsonProcessor p : processors){
				List<JSONObject> processed = p.processJson(features, session);
				notProc.removeAll(processed);
				String msg = p.getStatusMessage();
				if (msg != null){
					statusMsg.append(msg);
				}
			}
	
			returnValue.message = statusMsg.toString();
				
			if (notProc.size() == features.size()){
				//nothing has been processed perhaps we re-queue this item
				//for later as maybe something else needs to be processed first
				session.getTransaction().rollback();
				returnValue.message = "No data can be processed from file. Either file has already been processed and/or the observation counter is not sequential";
				returnValue.status = FileStatus.NO_DATA;
				return returnValue;
			}
				
			final boolean[] cont = new boolean[]{true};			
			if (!notProc.isEmpty()){
				//not all items have been processed
				final List<String> warnings = new ArrayList<String>();
				for (JSONObject o : notProc){
					warnings.add(o.toJSONString());
				}	
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						String message = MessageFormat.format("{0} of {1} features where processed. The following {2} features could not be processed.  If you continue this data will NOT be imported into SMART. Do you want to continue?", features.size() - notProc.size(), features.size(), notProc.size() );
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(),"Warning", message,
								warnings, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
						if (wd.open() == 0){
							cont[0] = true;
						}else{
							cont[0] = false;
						}
					}
					
				});
				}
			
			if (!cont[0]){
				returnValue.message = "Not all data was processed.  User cancelled file import.";
				returnValue.status = FileStatus.CANCELLED;
				return returnValue;
			}
			session.getTransaction().commit();

			for (IJsonProcessor p : processors){
				try{
					p.afterSave();
				}catch (Throwable t){
					returnValue.message = "Data was saved but an error occurred running post processors.  You may need to manually refresh your views. " + returnValue.message;
					returnValue.ex = t;
					returnValue.status = FileStatus.ERROR;
					return returnValue;
				}
			}
			//done ok file loaded
			returnValue.status = FileStatus.OK;
			return returnValue;
		}catch (UserCancelledException ex){
			session.getTransaction().rollback();
			throw ex;
	
		}catch (Throwable ex){
			returnValue.message = "Error Processing File: " + ex.getMessage();
			returnValue.ex = ex;
			returnValue.status = FileStatus.ERROR;
			return returnValue;
		}
	
	}

	private static List<IJsonProcessor> getProcessors() throws Exception{
		if (jsonProcessors == null){
			synchronized (JsonFileProcessor.class) {
				if (jsonProcessors == null){
					ArrayList<IJsonProcessor> temp = new ArrayList<IJsonProcessor>();
					IConfigurationElement[] config = Platform.getExtensionRegistry()
							.getConfigurationElementsFor(IJsonProcessor.EXTENSION_ID);
					
					for (IConfigurationElement e : config) {
						if (e.getName().equalsIgnoreCase("JsonProcessor")){ //$NON-NLS-1$
							IJsonProcessor proc = (IJsonProcessor) e.createExecutableExtension("class"); //$NON-NLS-1$
							temp.add(proc);
						}
					}
					jsonProcessors = temp;
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
