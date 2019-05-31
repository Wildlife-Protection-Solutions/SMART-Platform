package org.wcs.smart.paws.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsWorkspace;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.TokenCredentials;
import com.microsoft.azure.storage.blob.TransferManager;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * This job packages up all data specified by a paws run, uploads the data
 * to azure storage folder, and starts the paws analysis. 
 * 
 * @author Emily
 *
 */
public class PawsRunJob extends Job{
	
	private PawsRun run;
	private IEventBroker eventBroker;
	private String authorizationCode;
	
	//object to wait on for azure callbacks 
	private final Object lock = new Object();
	
	private String token;
	
	public PawsRunJob(PawsRun run, String authorizationCode, IEventBroker eventBroker) {
		super("packaging and uploading data for PAWS analysis: " + run.getId());
		this.run = run;
		this.eventBroker = eventBroker;
		this.authorizationCode = authorizationCode;
	}
	  
	
//	private final static String AUTHORITY = "https://login.microsoftonline.com/common";
//	    private final static String CLIENT_ID = "YOUR_PUBLIC_CLIENT_ID";
//	private final static String RESOURCE = "https://graph.windows.net";
	private void acquireToken() {
		
		//open a web browser and make the user login
		//this gets a token
		
		//https://login.microsoftonline.com/548d829f-38d0-42ac-9a2c-220944e5c275/oauth2/v2.0/authorize?client_id=98c26dd0-2a8e-4984-a1fe-8e3c1a75b114&response_type=code&redirect_url=http%3A%2F%2Flocalhost%3A12345&response_mode=query&state=1234&scope=other&sso_reload=true
		//String clientid = "fd200799-513a-4000-9f01-8cf6485771de";
		
//		String token = "https://login.microsoftonline.com/548d829f-38d0-42ac-9a2c-220944e5c275/oauth2/v2.0/token";
		

		String auth = "https://login.microsoftonline.com/common";///oauth2/v2.0/token";//?client_id=98c26dd0-2a8e-4984-a1fe-8e3c1a75b114&grant_type=authorization_code&code=authorization_code&redirect_uri=
		
		ExecutorService service = null;
		try {
//			service = Executors.newFixedThreadPool(1);
//			
//			String clientid = "fd200799-513a-4000-9f01-8cf6485771de";
//			context = PublicClientApplication.builder(clientid).authority(auth).executorService(service).build();
//////			context = new PublicClientApplication(auth, false, service);
////			
//			String redirectUri = "https://login.microsoftonline.com/common/oauth2/nativeclient";
//			
//			AuthorizationCodeParameters params = AuthorizationCodeParameters.builder(authorizationCode, new URI(redirectUri))
//				.scopes(Collections.singleton("api://fd200799-513a-4000-9f01-8cf6485771de/Test"))
//				.build();
//			java.util.concurrent.Future<AuthenticationResult> future = context.acquireToken(params);
////					context.acquireToken(new AuthorizationCodeParameters)
////					context.acquireTokenByAuthorizationCode(authorizationCode, "api://fd200799-513a-4000-9f01-8cf6485771de/Test", clientid, new URI(redirectUri), null);
//			this.token = future.get();
			
			
//			String url = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
//			String tenantid = "548d829f-38d0-42ac-9a2c-220944e5c275";
			String tenantid = "common";
//			String url = "https://login.microsoftonline.com/" + tenantid + "/oauth2/v2.0/token";
			String url = "https://login.microsoftonline.com/" + tenantid + "/oauth2/v2.0/token";
			String params="client_id=fd200799-513a-4000-9f01-8cf6485771de" + 
					"&code=" + authorizationCode +
					"&redirect_uri=https://login.microsoftonline.com/common/oauth2/nativeclient"+
					"&scope=api://fd200799-513a-4000-9f01-8cf6485771de/Test" +
					"&grant_type=authorization_code";
//			System.out.println(url);
			
			HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", Integer.valueOf( params.getBytes().length).toString());
			conn.setDoOutput(true);
			
			conn.getOutputStream().write(params.getBytes());
			conn.getOutputStream().close();
			
			int status = conn.getResponseCode();
//			if (status != HttpResponse)
			System.out.println("STATUS: " + status);
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
			conn.disconnect();
			
			System.out.println(content.toString());
			
			JSONObject json = (JSONObject) (new JSONParser()).parse(content.toString());
			
//			this.token = AuthenticationResult.builder()
//				.scopes((String)json.get("scope"))
//				.extExpiresOn( (Long)json.get("ext_expires_in"))
//				.expiresOn((Long)json.get("expires_in"))
//				.accessToken((String)json.get("access_token")).build();
			this.token = (String) json.get("access_token");
				
			
		}catch (Exception ex) {
			ex.printStackTrace();
			//TODO
		} finally {
//			service.shutdown();
	    }

	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		PawsDataEngine engine = new PawsDataEngine(run);
		//package data
		Path packageDir = null;
		try{
			packageDir = engine.createDataPackage();
		}catch (Exception ex){
			handleError("Unable to package SMART data for PAWS analysis.", ex);
			return Status.OK_STATUS;
		}
//		if (true) return Status.OK_STATUS;
		
		//upload package to azure
		//TODO: update the required jar files when new build is released
		//https://github.com/Azure/autorest-clientruntime-for-java/issues/569
		ContainerURL containerURL;
		try{
			acquireToken();
			if (this.token == null) throw new Exception("Invalid token");
			String url = null;
			try(Session session = HibernateManager.openSession()){
				PawsWorkspace ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
						new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult();
				
				if (ws == null || ws.getApiKey() == null || ws.getUrl() == null){
					handleError("PAWS Workspace not configured.  You must first configure the PAWS Workspace before you can run paws analysis.", new Exception("No Paws Workspace Configured."));
					return Status.OK_STATUS;
				}
				url = ws.getUrl(); // + "?" + ws.getApiKey();
			}
			TokenCredentials tc = new TokenCredentials(this.token);
//			HttpPipelineBuilder builder = new HttpPipelineBuilder();
//			builder.withCredentialsPolicy(new com.microsoft.rest.v2.credentials.TokenCredentials("Bearer", token));
//			builder.withHttpLoggingPolicy(HttpLogDetailLevel.BODY_AND_HEADERS, true);	
//			containerURL = new ContainerURL(new URL(url), builder.build());
		    containerURL = new ContainerURL(new URL(url), StorageURL.createPipeline(tc, new PipelineOptions()));
	    
	        //upload files
	        for(Path p : engine.getDataFiles()) {
				if (Files.exists(p)){
					//upload file to folder
					BlockBlobURL blobURL = containerURL.createBlockBlobURL(run.getRunId() + "/" + p.getFileName().toString());
					blobURL.getProperties();
					uploadFile(blobURL, p, MessageFormat.format("Error uploading basemap data for layer {0}.", p.getFileName().toString()));
				}
			
			}
			
		}catch (Throwable ex){
			String msg = "Unable to upload SMART data to Azure instance of PAWS analysis. "
					+ "Some data may remain on Azure blob storage folder (" 
					+ run.getRunId() 
					+ ").  You should remove this data manually ";
			handleError(msg, ex);
			return Status.OK_STATUS;
		}
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				PawsRun pw = session.get(PawsRun.class, run.getUuid());
				if (pw != null){
					pw.setStatus(PawsRun.Status.RUNNING);
				}else{
					//TODO: assume this has been deleted; warn user that there might be data on azure storage folder?
					//Maybe we should do this on delete
				}
				session.getTransaction().commit();
			}catch (Exception ex2){
				try{
					session.getTransaction().rollback();
				}catch (Exception e3){
					PawsPlugIn.log(e3.getMessage(), e3);	
				}
				PawsPlugIn.displayLog("Failed to update Paws Run status.  Run will not continue." + "\n\n" + ex2.getMessage(), ex2);
				return Status.OK_STATUS;
			}
		}
		fireModified();
		
		
		//run paws analysis
		try{
			Path configJson = packageDir.resolve(PawsDataEngine.CONFIG_FILE_NAME);
			String json = Files.readString(configJson);
			System.out.println(json);
			//TODO:
//			PawsApi.INSTANCE.run(run.getConservationArea(), json);
		}catch (Exception ex){
			handleError("Error calling PAWS API", ex);
			return Status.OK_STATUS;
		}
		
		//add to queue to check status 
		PawsStatusJob.getInstance().addItem(run);
		

		return Status.OK_STATUS;
	}

	private void uploadFile(BlockBlobURL blobURL, Path file, String errorMsg) throws Throwable{
		final Throwable[] uperror = new Throwable[]{null}; 
		AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(file);
			TransferManager.uploadFileToBlockBlob(fileChannel, blobURL, 8 * 1024 * 1024, null, null)
					.subscribe(response -> {
						try{
							int stat = response.response().statusCode();
							if (stat != HttpResponseStatus.OK.code() && stat != HttpResponseStatus.CREATED.code()) {
								throw new Exception(errorMsg + " " + MessageFormat.format("(Server return code: {0})", stat));
							}
						}catch (Throwable t){
							uperror[0] = t;
						}finally{
							synchronized (lock) {
								lock.notifyAll();
							}
						}
					},
					error->{
						uperror[0] = error;
						synchronized (lock) {
							lock.notifyAll();
						}
					});
		
		//wait for transfer to finish
		synchronized (lock) {
			lock.wait();
		}
		if (uperror[0] != null) throw uperror[0];
	}
	
	private void handleError(String msg, Throwable ex){
		PawsPlugIn.displayLog(msg + "\n\n" + ex.getMessage(), ex);
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				session.saveOrUpdate(run);
				run.setStatus(PawsRun.Status.ERROR);
				run.setStatusMessage(msg + ex.getMessage());
				session.getTransaction().commit();
			}catch (Exception ex2){
				PawsPlugIn.log(ex2.getMessage(), ex2);
			}
		}
		fireModified();
	}
	
	private void fireModified(){
		eventBroker.post(PawsEvent.PAWS_RUN_MODIFY, Collections.singleton(run));
	}
	
	
	
}
