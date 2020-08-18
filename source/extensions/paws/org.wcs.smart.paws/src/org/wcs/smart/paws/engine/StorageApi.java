/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsService;
import org.wcs.smart.paws.ui.LoginDialog;

import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.ListBlobsOptions;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.TokenCredentials;
import com.microsoft.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.storage.blob.models.ContainerListBlobFlatSegmentResponse;

import io.reactivex.Single;

/**
 * API for interacting with the azure storage 
 * @author Emily
 *
 */
public enum StorageApi {

	INSTANCE;
	
	private final Object dlock = new Object();

	private String token;
	private String containerName;
	
	private PawsService service;
	
	public ContainerURL getContainerURL(String containerName) throws Exception {
		if (service == null) throw new Exception(Messages.StorageApi_AuthCodeRequired);
		String url = service.getStorageUrl() + "/" + containerName; //$NON-NLS-1$
		
		TokenCredentials tc = new TokenCredentials(token);
		ContainerURL  containerURL = new ContainerURL(new URL(url), StorageURL.createPipeline(tc, new PipelineOptions()));
		return containerURL;
	}
	
	public void resetToken() {
		this.token = null;
	}
	
	/**
	 * 
	 * @return the token if generated or null if not yet generated
	 */
	public String getToken(){
		return this.token;
	}
	
	private void getWorkspace() {
		try(Session session = HibernateManager.openSession()){
			PawsService ws = QueryFactory.buildQuery(session, PawsService.class,  
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$
			if (ws == null || !ws.isConfigured()) {
				token = null;
				service = null;
			}
			
			if (service != null && (
					!service.getClientId().equalsIgnoreCase(ws.getClientId()) ||
					!service.getStorageUrl().equalsIgnoreCase(ws.getStorageUrl()) ||
					!service.getOAuthUrl().equalsIgnoreCase(ws.getOAuthUrl()))){
				//something has changed; require a new token
				token = null;
			}
			this.service = ws;
		}		
	}
	
	/**
	 * Gets an authorization code and updates the container name for the paws run
	 * @param shell
	 * @param run
	 * @return
	 * @throws Exception
	 */
	public boolean getAuthorizationCode(Shell shell, PawsRun run) throws Exception {
		getWorkspace();
		if (token != null) {
			run.setContainerName(containerName);
			return true;
		}
		
		LoginDialog dialog = new LoginDialog(shell);
		dialog.open();
		
		String authorizationCode = dialog.getAuthorizationCode();

		if (authorizationCode == null) {
			//fail
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.StorageApi_AuthFailedTitle, Messages.StorageApi_AuthFailedMsg);
			return false;
		}
		
		acquireToken(authorizationCode);
		
		if (containerName == null || containerName.isBlank()) {
			throw new Exception("Could not determine container name for the user.  Email field is not provided in authentication token id_token field."); //$NON-NLS-1$
		}
		run.setContainerName(containerName);
		
		
		return true;
	}
	
	private void acquireToken(String authorizationCode) throws Exception{
	
		String redirecturi = "https://login.microsoftonline.com/common/oauth2/nativeclient"; //$NON-NLS-1$
		
		StringBuilder sb = new StringBuilder();
		sb.append(service.getOAuthUrl());
		sb.append("/"); //$NON-NLS-1$
		sb.append("token"); //$NON-NLS-1$
		
		
		StringBuilder params = new StringBuilder();
		params.append("client_id=" + service.getClientId());  //$NON-NLS-1$
		params.append("&code=" + authorizationCode ); //$NON-NLS-1$
		params.append("&redirect_uri=" + redirecturi ); //$NON-NLS-1$
		params.append("&resource=https://storage.azure.com/"); //$NON-NLS-1$
		params.append("&grant_type=authorization_code"); //$NON-NLS-1$
		String pp = params.toString();	
		
		token = null;
		HttpURLConnection conn = (HttpURLConnection) (new URL(sb.toString())).openConnection();
		try {
			conn.setRequestMethod("POST"); //$NON-NLS-1$
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); //$NON-NLS-1$ //$NON-NLS-2$
			conn.setRequestProperty("Content-Length", Integer.valueOf( pp.getBytes().length).toString()); //$NON-NLS-1$
			conn.setDoOutput(true);
			conn.getOutputStream().write(pp.getBytes());
			conn.getOutputStream().close();
				
			int status = conn.getResponseCode();
			if (status == HttpURLConnection.HTTP_OK) {
				StringBuffer content = new StringBuffer();
				String inputLine;
				try(BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
					while ((inputLine = in.readLine()) != null) {
						content.append(inputLine);
					}
				}
				try {
					JSONObject json = (JSONObject) (new JSONParser()).parse(content.toString());
					String temptoken = (String) json.get("access_token"); //$NON-NLS-1$
					if (temptoken == null || temptoken.isBlank()) {
						throw new Exception("access_token not found"); //$NON-NLS-1$
					}
				
					String idtoken = (String) json.get("id_token"); //$NON-NLS-1$
					if (idtoken == null || idtoken.isBlank()) {
						throw new Exception("id_token not found in token request"); //$NON-NLS-1$
					}
					String body = idtoken.split("\\.")[1]; //$NON-NLS-1$
					String bodyjson = new String(Base64.getDecoder().decode(body));

					JSONObject id = (JSONObject) (new JSONParser()).parse(bodyjson);
					String email = (String) id.get("email"); //$NON-NLS-1$
					
					token = temptoken;
					containerName = email.replaceAll("\\.", "") //$NON-NLS-1$ //$NON-NLS-2$ 
							.replaceAll("@","") //$NON-NLS-1$ //$NON-NLS-2$
							.replaceAll("_","") //$NON-NLS-1$ //$NON-NLS-2$
							.toLowerCase();
					
				}catch (Exception ex) {
					throw new Exception(MessageFormat.format(Messages.StorageApi_CannotParseAuth, ex.getMessage()) , ex);
				}
			}else {
				PawsPlugIn.log("Authorization token cannot be found. Response: " + status + " Request: " + sb.toString() + " " + params.toString(), null);  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
				throw new Exception(MessageFormat.format(Messages.StorageApi_TokenNotFoundException,  status));
			}
		}finally {
			conn.disconnect();
		}
	}
	
	
	/**
	 * Gets a list of url to all blobs in the given container that are located
	 * in the given url;
	 * 
	 * @param containerURL
	 * @param url
	 * @return
	 * @throws InterruptedException 
	 */
	public List<String> getBlobs(ContainerURL containerURL, String url) throws InterruptedException{
		
		if (!url.endsWith("/")) url = url +"/"; //$NON-NLS-1$ //$NON-NLS-2$
		final String furl = url;
		
		List<String> items = new ArrayList<>();
		
		ListBlobsOptions options = new ListBlobsOptions();
		containerURL.listBlobsFlatSegment(null, options, null)
			.flatMap(r -> listAllBlobs(containerURL, r, items, furl))
			.subscribe(response -> {
				synchronized (dlock) {
					dlock.notify();
				}
			},
			error->{
				synchronized (dlock) {
					dlock.notify();
				}		
			});
		
		synchronized (dlock) {
			dlock.wait();
		}
		return items;
	}
	

	/**
	 * Remove all data associated with the given paws run
	 * 
	 * @param run
	 * @throws Exception
	 */
	public void deleteBlobs(PawsRun run) throws Exception{
		try {
			ContainerURL  containerURL = getContainerURL(run.getContainerName());
			if (containerURL == null) throw new Exception(Messages.StorageApi_StorageNotConfigured);
			
			List<String> urlToDelete = getBlobs(containerURL, run.getRunId());
			List<Throwable> fails = new ArrayList<>();
			for (String d : urlToDelete) {
				containerURL.createBlockBlobURL(d).delete().subscribe(rsp -> {},  error -> fails.add(error));				
			}
			if (!fails.isEmpty()){
				for (Throwable t : fails) PawsPlugIn.log(t.getMessage(), t);
				throw new Exception(Messages.StorageApi_DeleteFailed);
			}
		}catch (Exception ex){
			PawsPlugIn.displayLog(MessageFormat.format(Messages.StorageApi_UnableToCleanupAzure, run.getId()), ex);
		}
        
	}
	
	private Single<ContainerListBlobFlatSegmentResponse> listAllBlobs(ContainerURL url,
			ContainerListBlobFlatSegmentResponse response, List<String> toDelete, String filterUrl) {

		// Process the blobs returned in this result segment (if the segment is empty,
		// blobs() will be null.
		if (response.body().segment() != null) {
			for (BlobItem b : response.body().segment().blobItems()) {
				if (b.name().startsWith(filterUrl)) {
					toDelete.add(b.name());
				}
			}
		}

		// If there is not another segment, return this response as the final response.
		if (response.body().nextMarker() == null) {
			return Single.just(response);
		} else {
			/*
			 * IMPORTANT: ListBlobsFlatSegment returns the start of the next segment; you
			 * MUST use this to get the next segment (after processing the current result
			 * segment
			 */

			String nextMarker = response.body().nextMarker();

			/*
			 * The presence of the marker indicates that there are more blobs to list, so we
			 * make another call to listBlobsFlatSegment and pass the result through this
			 * helper function.
			 */

			return url.listBlobsFlatSegment(nextMarker, new ListBlobsOptions().withMaxResults(10), null)
					.flatMap(r -> listAllBlobs(url,
							r, toDelete, filterUrl));
		}
	}
}
