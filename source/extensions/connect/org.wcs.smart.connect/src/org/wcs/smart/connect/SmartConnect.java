/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.api.ConnectClient;
import org.wcs.smart.connect.api.io.IOUtils;
import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.connect.api.model.ConservationAreaProxy;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.internal.server.replication.PackageToLargeException;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class to interface with a SMART Connect server.
 * This open a client to the server and when finished with the object
 * users should call close
 * 
 * @author Emily
 *
 */
public class SmartConnect {

	public final static String API_URL = "/api"; //$NON-NLS-1$
	
	private String username;
	private String password;
	private ConnectServer server;
	private ResteasyClient client;
	private Certificate currentCertificate;

	/**
	 * Lock to ensure only one upload or download change log 
	 * task is being performed at any given time.
	 */
	public static final Semaphore UPLOAD_LOCK = new Semaphore(1); 
	
	
	private static SmartConnect lastConnect = null;
	
	/**
	 * Finds the connect instance reusing existing connection
	 * if exists.
	 * 
	 * @param server the connect server
	 * @param username the connect username
	 * @param password the connect password plaintext
	 * 
	 * @return
	 */
	public synchronized static final SmartConnect findInstance(ConnectServer server, String username, String password){
		if (lastConnect != null){
			//if server; username; password and certification file are all the same
			//we can reuse the last instance; otherwise we need to close and 
			//create a new instance
			if (lastConnect.server.equals(server) 
					&& lastConnect.username.equals(username) 
					&& (lastConnect.password != null && lastConnect.password.equals(password))){
				//compare certifications; cannot simply compare the filename
				//as the same filename is used for all certificates
				if (lastConnect.server.getCertificateFileName() == null && server.getCertificateFileName() == null){
					lastConnect.server = server;
					return lastConnect;
				}else if (lastConnect.currentCertificate != null && server.getCertificateFileName() != null){
					try(InputStream is = new BufferedInputStream(Files.newInputStream(server.getLocalCertificateFile()))){
						Certificate temp = CertificateFactory.getInstance("X.509").generateCertificate(is); //$NON-NLS-1$
						if (temp.equals(lastConnect.currentCertificate)){
							lastConnect.server = server;
							return lastConnect;
						}
					}catch (Exception ex){
						ConnectPlugIn.log(ex.getMessage(), ex);
					}
				}	
			}
		}
		if (lastConnect != null){
			lastConnect.close();
		}
		lastConnect = new SmartConnect(server, username, password);
		return lastConnect;
	}
	
	/**
	 * Close all open Smart Connect instances
	 */
	public synchronized static final void closeAll(){
		if (lastConnect != null){ 
			lastConnect.close();
			lastConnect = null;
		}
	}
	
	/**
	 * 
	 * @param url
	 * @param username
	 * @param password
	 */
	private SmartConnect(ConnectServer server, String username, String password){
		this.server = server;
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	/**
	 * Creates and returns the resteasy client
	 * @return
	 */
	public ResteasyClient getClient(){
		createClient();
		return client;
	}
	
	private void createClient(){
		if (client != null) return;
		// load the keystore that includes self-signed cert as a "trusted" entry
		try{
			
			X509TrustManager trustManager = getTrustManager(); 
			SSLContext ctx = SSLContext.getInstance("TLS"); //$NON-NLS-1$
			ctx.init(null, new TrustManager[]{trustManager}, null);
		
			SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(ctx);
			org.apache.http.config.Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("https", factory) //$NON-NLS-1$
					.build();
//			registry.register("https", 443, factory); //$NON-NLS-1$
			
			HttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
			HttpClient httpClient = HttpClientBuilder.create()
					.setConnectionManager(cm)
					.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
					.build();
			
			ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
			
			client = ((ResteasyClientBuilder)ClientBuilder.newBuilder())
				.httpEngine(engine)
				.build();

			client.register(new AddAuthHeadersRequestFilter(username, password));
			
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}
	}
	
	private X509TrustManager getTrustManager() throws Exception{
		this.currentCertificate = null;
		//get default jvm trust manager
		if (server.getCertificateFileName() == null){
			//use the default jvm trust manager
			TrustManagerFactory factory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			factory.init((KeyStore)null);
			for (TrustManager t : factory.getTrustManagers()){
				if (t instanceof X509TrustManager){
					return (X509TrustManager) t;
				}
			}
			throw new RuntimeException(Messages.SmartConnect_SshError1);
		}

		//build a trust manager using the locally provided certificate
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null);
		Path certpath = server.getLocalCertificateFile();
		try(InputStream is = new BufferedInputStream(Files.newInputStream(certpath))){
			currentCertificate = CertificateFactory.getInstance("X.509").generateCertificate(is); //$NON-NLS-1$
			String key = "smart-"; //$NON-NLS-1$
			if (server.getConservationArea() != null){
				key += server.getConservationArea().getUuid().toString();
			}
			keyStore.setCertificateEntry(key, currentCertificate);
		}
		TrustManagerFactory factory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		factory.init(keyStore);
		for (TrustManager t : factory.getTrustManagers()){
			if (t instanceof X509TrustManager){
				return (X509TrustManager) t;
			}
		}
		throw new RuntimeException(Messages.SmartConnect_SshError2);
	}
	
	
	public ConnectServer getServer(){
		return this.server;
	}
	
	/**
	 * Closes connect to SMART Server
	 */
	public void close(){
		if (client != null){
			client.close();
			client = null;
		}
	}
	
	/**
	 * 
	 * 
	 * @return list of alert types available on the server
	 */
	public List<AlertType> getAlertTypes() throws Exception{
		createClient();
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		ConnectClient simple = target.proxy(ConnectClient.class);
		return simple.getAlertTypes();
	}
	
	
	/**
	 * Validates the user/password/url associated.  
	 * All errors are logged.
	 * 
	 * @return error message if not valid, otherwise null
	 */
	public String validateUser(){
		createClient();
		try{
			ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		
			ConnectClient simple = target.proxy(ConnectClient.class);
			String user = simple.getUser(username, Boolean.TRUE.toString());
			if (user == null) return null;	//this should return null if able to connect ok; otherwise it might return something
			String error = MessageFormat.format(Messages.SmartConnect_connectError, server.getServerUrl());
			if (!server.getServerUrl().endsWith("/server")){ //$NON-NLS-1$
				error += "\n\n" + Messages.SmartConnect_ServerErrorMsg; //$NON-NLS-1$
			}
			return error;
		}catch(Throwable t){
			return processException(t);
		}	
	}
	
	/**
	 * Gets the URL for uploading conservation area export to 
	 * a connect server.  
	 * Performing this task will create a new Conservation Area
	 * on the server if necessary.
	 * 
	 * @param caUuid
	 * @return the upload url for the new conservation area
	 */
	public String getCaUploadUrl(UUID caUuid, UUID version, File f) throws WebApplicationException{
		createClient();
		
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		ConnectClient simple = target.proxy(ConnectClient.class);
		try(Response r = simple.getDataUploadUrl(f.length(), caUuid.toString(), version.toString())){
			if (r.getStatus() == HttpStatus.SC_OK){
				return r.getHeaderString(HttpHeaders.LOCATION);
			}else{
				throw new WebApplicationException(r);
			}
		}
	}
	
	/**
	 * Gets the URL for uploading a Conservation Area change log to 
	 * the connect server.
	 * 
	 * @param caUuid
	 * @return the upload url for the new conservation area
	 */
	public String getSyncUploadUrl(UUID caUuid, Path file) throws Exception{
		createClient();
		
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		ConnectClient simple = target.proxy(ConnectClient.class);
		try(Response r = simple.updateConservationArea(Files.size(file), caUuid.toString())){
			if (r.getStatus() == HttpStatus.SC_OK){
				return r.getHeaderString(HttpHeaders.LOCATION);
			}else{
				throw new WebApplicationException(r);
			}
		}
	}
	
	/**
	 * Gets the information on a connect server associated with 
	 * the given Conservation Area UUID.
	 * 
	 * @param caUuid the conservation area UUID
	 * @return ConservationAreaInfo or null if not found
	 */
	public ConservationAreaProxy getCaInfo(UUID caUuid) throws Exception{
		createClient();
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		
		ConnectClient simple = target.proxy(ConnectClient.class);
		try{
			return simple.getConservationArea(caUuid.toString());
		}catch (NotFoundException ex){
			return null;
		}
	}
	
	/**
	 * Gets a list of Conservation Areas on a connect
	 * server.
	 * 
	 * @return List of ConservationAreaInfo 
	 */
	public List<ConservationAreaProxy> getConservationAreas() throws Exception{
		createClient();
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		
		ConnectClient simple = target.proxy(ConnectClient.class);
		try{
			return simple.getConservationAreas();
		}catch (NotFoundException ex){
			return null;
		}
	}
	
	/**
	 * Initiates a Conservation Area download.  Returns a URL
	 * where the status of the download can be polled.
	 * The download process: 1) initiate the download which
	 * returns a status url, 2) when the status is complete it
	 * will provide a url where the download can be found.
	 * 
	 * @param caUuid the ConservationArea to download
	 * @return the status URL
	 * @throws Exception
	 */
	public String startConservationAreaDownload(UUID caUuid) throws Exception{
		createClient();
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		ConnectClient simple = target.proxy(ConnectClient.class);
		try(Response r = simple.downloadConservationArea(caUuid.toString(), ConnectClient.DATA_PARAM_ALL)){
			if (r.getStatus() == Status.ACCEPTED.getStatusCode()){
				return r.getHeaderString(HttpHeaders.LOCATION);
			}else{
				throw new WebApplicationException(r);
			}
		}
	}
	
	/**
	 * Initiates a change log download for a given conservation area.
	 * This process is similar to the conservation area download: 1) initiate
	 * the process returning a status url, 2) the status url provides
	 * a download url when the package is ready.
	 * 
	 * @param caUuid the conservation area uuid
	 * @param version the conservation area version
	 * @param revision the start revision
	 * @return
	 * @throws Exception
	 */
	public String startChangeLogDownload(UUID caUuid, UUID version, Long revision) throws Exception{
		createClient();
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		ConnectClient simple = target.proxy(ConnectClient.class);
		
		try(Response r = simple.downloadChangeLog(caUuid.toString(), ConnectClient.DATA_PARAM_PACKAGE, version.toString(), String.valueOf(revision))){
			if (r.getStatus() == Status.ACCEPTED.getStatusCode()){
				return r.getHeaderString(HttpHeaders.LOCATION);
			}else{
				throw new WebApplicationException(Messages.SmartConnect_NoUrlError, r);
			}
		}
	}
	
	private boolean promptToDownload(final Long actualSize, final Long checkSize){
		final boolean[] cont = new boolean[]{false};
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				cont[0] = MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
						Messages.SmartConnect_DialogTitle, 
						MessageFormat.format(Messages.SmartConnect_SizeConfirmMessage, actualSize / 1000000.0, checkSize/1000000.0));		
			}
			
			
		});
		return cont[0];
	}
	
	/*
	 * parse a filename from the content header string
	 */
	private String parseFilenameFromContent(String content){
		if (content == null) return null;
		int index = content.indexOf("filename="); //$NON-NLS-1$
		if (index > 0){
			int index2 = content.indexOf(";", index); //$NON-NLS-1$
			if (index2 < 0) index2 = content.length();
			String filename = content.substring(index+"filename=".length(), index2); //$NON-NLS-1$
			if(filename.startsWith("\"")){ //$NON-NLS-1$
				filename = filename.substring(1);
			}
			if (filename.endsWith("\"")){ //$NON-NLS-1$
				filename = filename.substring(0, filename.length() - 1);
			}
			if (filename.length() > 0){
				return filename;
			}
		}
		return null;
	}
	/**
	 * Downloads a file from a given URL.  This will try multiple
	 * times to download.
	 * Provides the option for prompting before continuing with download if
	 * download package size is large.
	 *  
	 *  //TODO: DOES NOT DOWNLOAD CHUNKS - ALL OR NOTHING; CANNOT RESUME
	 * 
	 * Downloads to a temporary directory that is cleaned out on startup.  If users
	 * want to keep there files, they must move them to a different location 
	 * once the file has been downloaded.
	 *  
	 * @param url the URL to download from
	 * @param promptDownloadSizeMb prompt the user to continue if the download
	 * file size is larger than this size.  Can be null if should never prompt.
	 * @param monitor subprogress monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor 
	 * @return the downloaded file
	 * @throws Exception
	 */
	public Path downloadFileFromUrl(String url, Integer promptDownloadSizeMb, IProgressMonitor monitor) throws PackageToLargeException, OperationCanceledException, Exception{
		createClient();
		int tryCount = 0;
		
		//download file name
		Path filestorea = FileSystems.getDefault()
			.getPath(SmartContext.INSTANCE.getFilestoreLocation())
			.resolve(ConnectDatastore.CONNECT_FILESTORE_DIR)
			.resolve(ConnectDatastore.DOWNLOAD_FILESTORE_DIR);
		
		//create necessary dirs
		Files.createDirectories(filestorea);
		Path filestore = null;
		Long size = null;
		
		long waitTime = ConnectServerOption.ConnectionOption.RETY_WAIT_TIME.getIntegerValue(server);
		SubMonitor progress = SubMonitor.convert(monitor);
		
		//first request; this one gives us the requested size
		while(size == null && tryCount < ConnectServerOption.ConnectionOption.MAX_RETRY_DOWNLOAD.getIntegerValue(server)){
			
			try{
				createClient();
				ResteasyWebTarget target = client.target(url);
				try(Response r = target.request().get()){

					if (r.getStatus() == HttpURLConnection.HTTP_OK){
						size = Long.valueOf(r.getHeaderString(HttpHeaders.CONTENT_LENGTH));
						if (filestore == null){
							String filename = parseFilenameFromContent(r.getHeaderString(HttpHeaders.CONTENT_DISPOSITION));
							if (filename != null){
								filestore = filestorea.resolve(System.nanoTime() + "." + filename); //$NON-NLS-1$
							}else{
								filestorea = filestorea.resolve(System.nanoTime() + ".temp"); //$NON-NLS-1$
							}
						}
						
						if (promptDownloadSizeMb != null &&
								size > promptDownloadSizeMb * 1000000 ){
							//prompt to download before continuing
							if (!promptToDownload(size, promptDownloadSizeMb * 1000000l)){
								throw new PackageToLargeException(Messages.SmartConnect_UserCanceledError);
							}
						}
						//parse target
						
						try(InputStream is = r.readEntity(InputStream.class);
							OutputStream out = Files.newOutputStream(filestore)){
							IOUtils.copy(is, out, size, progress);
						}
						
						if (Files.size(filestore) > size){
							throw new Exception(Messages.SmartConnect_FileToLargeError);
						}
						if (Files.size(filestore) == size){
							return filestore;
						}
					}
				}
			}catch (PackageToLargeException ex){
				//we do not want to try again
				throw ex;
			}catch (OperationCanceledException ex){
				throw ex;	//user cancelled do not try again
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
			tryCount ++;
			Thread.sleep(waitTime);
		}
		
		//try a maximum of 10 times
		while(tryCount < ConnectServerOption.ConnectionOption.MAX_RETRY_DOWNLOAD.getIntegerValue(server)){
			downloadRequest(filestore, url, size, progress);
			
			if (Files.size(filestore) > size){
				throw new Exception(MessageFormat.format(Messages.SmartConnect_FileToBigError, Files.size(filestore), size));
			}
			if (Files.size(filestore) == size){
				return filestore;
			}
			tryCount++;
			Thread.sleep(waitTime);
			waitTime = waitTime*2;
		}
		throw new Exception(Messages.SmartConnect_DownloadFailed);
	}

	
	/**
	 * Attempts to download the file provided at the URL to the path.  This will
	 * continue the download if it fails. 
	 * @param p the path to download to
	 * @param url the url to download the file from
	 * @param maxLength total download filesize
	 * @throws Exception
	 */
	private void downloadRequest(Path p, String url, long packageSize, SubMonitor monitor) throws OperationCanceledException{
		ResteasyWebTarget target = client.target(url);
		
		try {
			Long start = Files.size(p);
			Long end = packageSize-1;
			
			Builder requestBuilder = target.request();
			if (start != 0){
				requestBuilder.header("Range", "bytes=" + start + "-" + end); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			
			try(Response r = requestBuilder.get()){
				if (r.getStatus() == HttpURLConnection.HTTP_OK || r.getStatus() == HttpURLConnection.HTTP_PARTIAL){
					
					//parse target
					try(InputStream is = r.readEntity(InputStream.class);
							OutputStream out = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND)){
						IOUtils.copy(is, out, packageSize, monitor);
					}
				}
			}
		}catch (OperationCanceledException ex){
			throw ex;
		}catch (Exception ex){
			ConnectPlugIn.log(ex.getMessage(), ex);
		}
	}
	
	/**
	 * Gets the status of an smart connect work item represented
	 * by the url.
	 * 
	 * @param url the status url
	 * @return
	 * @throws Exception
	 */
	public WorkItemStatus getWorkItemStatus(String url) throws Exception{
		createClient();
		
		ResteasyWebTarget target = client.target(url);
		try(Response r = target.request().get()){
			if (r.getStatus() == HttpURLConnection.HTTP_OK){
				//parse target
				return r.readEntity(WorkItemStatus.class);
			}else{
				throw new WebApplicationException(r);
			}
		}
	}
	
	/**
	 * Uploads a file to the given URL starting at the given byte.  Files are uploaded
	 * in chunks of 25,000,000bytes.  Exceptions are throw in an upload fails - this 
	 * method does not retry uploads.
	 * 
	 * @param url the url to upload file to
	 * @param f the file to upload
	 * @param startbyte the start position in the file 
	 * @Param monitor  the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor
	 */
	public void uploadFile(String url, Path f, long startByte, SubMonitor monitor) throws Exception{
        createClient();
        
		//retry handler set when creating client;
        ConnectClient service =  client.target(url).proxy(ConnectClient.class);
        
        //upload data in chunks so we don't run into problems with
        //uploading large files; it also better
        //reflects the workload
        //if this fails at anypoint it exits this loop so
        //failures/retries should be done in a different loop
        int chunkSize = 25_000_000;
        long total = Files.size(f);
        long count = startByte;
        SubMonitor m = SubMonitor.convert(monitor);
        
        int worksize = (int)Math.ceil((double)total / chunkSize);
        m.setWorkRemaining( worksize );
        try(FileInputStream fs = new FileInputStream(f.toFile())){
	        while( count < total) {
	        	try(ByteArrayOutputStream bout = new ByteArrayOutputStream()){	        	
		        	try(WritableByteChannel fout = Channels.newChannel(bout)){
		        		fs.getChannel().transferTo(count, chunkSize, fout);
		        	}	
		        	try(InputStream fis = new ByteArrayInputStream(bout.toByteArray() )){
		    			service.updateFile(fis);
		    		}
	        	}
	        	m.worked(1);
	        	count += chunkSize;
	        	//System.out.println(count + "/" + total);
	        }
        }
	}

	private String processException (Throwable root){
		String msg = null;
		Throwable ex = root;
		if (ex instanceof ProcessingException){
			ex = root.getCause();
		}
		if (ex instanceof NotFoundException){
			msg = MessageFormat.format(Messages.SmartConnect_ConnectionError, new Object[]{server.getServerUrl()});
		}else if (ex instanceof NotAuthorizedException){
			msg = Messages.SmartConnect_InvalidUserNameError;
		}else if (ex.getCause() instanceof javax.net.ssl.SSLHandshakeException){
			msg = Messages.SmartConnect_GeneralError + ex.getCause().getMessage();
		}else if (ex.getCause() instanceof SSLException){
			msg = Messages.SmartConnect_GeneralError + ex.getCause().getMessage();
		}else{
			msg = Messages.SmartConnect_GeneralError + ex.getMessage();
		}
		ConnectPlugIn.log(msg, root);
		return msg;
	}
	
	/**
	 * Filter for adding auth headers to every request
	 * @author Emily
	 *
	 */
	public static class AddAuthHeadersRequestFilter implements ClientRequestFilter {

	    private final String username;
	    private final String password;

	    public AddAuthHeadersRequestFilter(String username, String password) {
	        this.username = username;
	        this.password = password;
	    }

	    @Override
	    public void filter(ClientRequestContext requestContext) throws IOException {
	        String token = username + ":" + password; //$NON-NLS-1$
	        String base64Token = Base64.encodeBase64String(token.getBytes(StandardCharsets.UTF_8));
	        requestContext.getHeaders().add("Authorization", "Basic " + base64Token); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	}
	
	/**
	 * Parses the "error" node from a json string.
	 * 
	 * @param json
	 * @return
	 */
	public static String parseErrorMessage(String json){
		try{
			return (new ObjectMapper()).readTree(json).get("error").textValue(); //$NON-NLS-1$
		}catch (Exception ex){
			return null;
		}
	}
}
