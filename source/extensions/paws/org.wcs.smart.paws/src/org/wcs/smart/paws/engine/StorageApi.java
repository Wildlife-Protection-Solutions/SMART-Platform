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
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsWorkspace;
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
	private PawsWorkspace workspace;
	
	public ContainerURL getContainerURL() throws Exception {
		if (workspace == null) throw new Exception("A call to getAuthorizationCode is required before you can continue");
		String url = workspace.getStorageAccountUrl() + "/" + workspace.getContainer();
		TokenCredentials tc = new TokenCredentials(token);
		ContainerURL  containerURL = new ContainerURL(new URL(url), StorageURL.createPipeline(tc, new PipelineOptions()));
		return containerURL;
	}
	
	private void getWorkspace() {
		try(Session session = HibernateManager.openSession()){
			PawsWorkspace ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult();
			if (ws == null || !ws.isConfigured()) {
				token = null;
				workspace = null;
			}
			
			if (workspace != null && (
					!workspace.getClientId().equalsIgnoreCase(ws.getClientId()) ||
					!workspace.getContainer().equalsIgnoreCase(ws.getContainer()) ||
					!workspace.getStorageAccountUrl().equalsIgnoreCase(ws.getStorageAccountUrl()) ||
					!workspace.getUrl().equalsIgnoreCase(ws.getUrl()))){
				//something has changed; require a new token
				token = null;
			}
			this.workspace = ws;
		}		
	}
	
	public boolean getAuthorizationCode(Shell shell) throws Exception {
		getWorkspace();
		if (token != null) return true;
		
		LoginDialog dialog = new LoginDialog(shell);
		dialog.open();
		
		String authorizationCode = dialog.getAuthorizationCode();
		if (authorizationCode == null) {
			//fail
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Authorization Failed", "Authorization failed, run not created.");
			return false;
		}
		acquireToken(authorizationCode);
		return true;
	}
	
	private void acquireToken(String authorizationCode) throws Exception{
	
		String redirecturi = "https://login.microsoftonline.com/common/oauth2/nativeclient";
		
		StringBuilder sb = new StringBuilder();
		sb.append(workspace.getUrl());
		sb.append("/");
		sb.append("token");
		
		
		StringBuilder params = new StringBuilder();
		params.append("client_id=" + workspace.getClientId()); 
		params.append("&code=" + authorizationCode );
		params.append("&redirect_uri=" + redirecturi );
		params.append("&resource=https://storage.azure.com/");
		params.append("&grant_type=authorization_code");
		String pp = params.toString();	
		
		String stoken = null;
		HttpURLConnection conn = (HttpURLConnection) (new URL(sb.toString())).openConnection();
		try {
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", Integer.valueOf( pp.getBytes().length).toString());
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
					stoken = (String) json.get("access_token");
					if (stoken == null || stoken.isBlank()) {
						throw new Exception("access_token not found");
					}
				
				}catch (Exception ex) {
					throw new Exception("Unable to parse access token from json : " +content.toString(), ex);
				}
			}else {
				PawsPlugIn.log("Authorization token cannot be found. Response: " + status + " Request: " + sb.toString() + " " + params.toString(), null);
				throw new Exception("Authorization Token cannot be found.  Response code: " + status);
			}
		}finally {
			conn.disconnect();
		}
		token = stoken;
	}
	
	/**
	 * Remove all data associated with the given paws run
	 * 
	 * @param run
	 * @throws Exception
	 */
	public void deleteBlobs(PawsRun run) throws Exception{
		
		try {
			ContainerURL  containerURL = getContainerURL();
			if (containerURL == null) throw new Exception("Azure storage container not configured correctly.");
			
			List<String> urlToDelete = new ArrayList<>();
			ListBlobsOptions options = new ListBlobsOptions();
			containerURL.listBlobsFlatSegment(null, options, null)
					.flatMap(r -> listAllBlobs(containerURL, r, urlToDelete, run))
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

			List<Throwable> fails = new ArrayList<>();
			for (String d : urlToDelete) {
				containerURL.createBlockBlobURL(d).delete().subscribe(rsp -> {},  error -> fails.add(error));
			}
			if (!fails.isEmpty()){
				for (Throwable t : fails) PawsPlugIn.log(t.getMessage(), t);
				throw new Exception("Delete fail.");
			}
		}catch (Exception ex){
			PawsPlugIn.displayLog(MessageFormat.format("Unable to remove data from Azure container ({0}). You should remove these files manually.", run.getId()), ex);
		}
        
	}
	
	private Single<ContainerListBlobFlatSegmentResponse> listAllBlobs(ContainerURL url,
			ContainerListBlobFlatSegmentResponse response, List<String> toDelete, PawsRun run) {

		// Process the blobs returned in this result segment (if the segment is empty,
		// blobs() will be null.
		if (response.body().segment() != null) {
			for (BlobItem b : response.body().segment().blobItems()) {
				if (b.name().startsWith(run.getRunId() + "/")) {
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
							r, toDelete, run));
		}
	}
}
