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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.api.ConnectClient;
import org.wcs.smart.connect.api.model.ConservationAreaInfo;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class to interface with a SMART Connect server.
 * This open a client to the server and when finished with the object
 * users should call close
 * 
 * @author Emily
 *
 */
public class SmartConnect implements AutoCloseable {

	public final static String API_URL = "/api";
	
	public static final int MAX_RETRY_DOWNLOAD = 10;
	
	private String username;
	private ConnectServer server;
	
	private ResteasyClient client; 
	
	/**
	 * 
	 * @param url
	 * @param username
	 * @param password
	 */
	public SmartConnect(ConnectServer server, String username, String password){
		this.server = server;
		this.username = username;
		
		client = new ResteasyClientBuilder().build();
		client.register(new AddAuthHeadersRequestFilter(username, password));
	}
	
	public ConnectServer getServer(){
		return this.server;
	}
	
	@Override
	public void close(){
		client.close();
	}
	
	/**
	 * Validates the user/password/url associated.  Errors are logged
	 * 
	 * @return error message if not valid, otherwise null
	 */
	public String validateUser(){
		try{
			ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		
			ConnectClient simple = target.proxy(ConnectClient.class);
			simple.getUser(username);
			return null;
		}catch(Throwable t){
			return processException(t);
		}	
	}
	
	/**
	 * Gets the url for uploading conservation area data to 
	 * a connect server.  This will create a new ca on the server
	 * if necessary.
	 * 
	 * @param caUuid
	 * @return the upload url for the new conservation area
	 */
	public String getCaUploadUrl(UUID caUuid, UUID version, File f) throws Exception{
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		
		ConnectClient simple = target.proxy(ConnectClient.class);
		Response r = simple.getDataUploadUrl(f.length(), caUuid.toString(), version.toString());
		try{
			if (r.getStatus() == HttpStatus.SC_OK){
				return r.getHeaderString(HttpHeaders.LOCATION);
			}else{
				throw new WebApplicationException(r);
			}
		}finally{
			r.close();
		}
	}
	
	/**
	 * Gets the url for uploading conservation area data to 
	 * a connect server.  This will create a new ca on the server
	 * if necessary.
	 * 
	 * @param caUuid
	 * @return the upload url for the new conservation area
	 */
	public String getSyncUploadUrl(UUID caUuid, Path file) throws Exception{
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		
		ConnectClient simple = target.proxy(ConnectClient.class);
		Response r = simple.updateConservationArea(Files.size(file), caUuid.toString());
		try{
			if (r.getStatus() == HttpStatus.SC_OK){
				return r.getHeaderString(HttpHeaders.LOCATION);
			}else{
				throw new WebApplicationException(r);
			}
		}finally{
			r.close();
		}
	}
	
	/**
	 * Gets the information on a connect server related to the
	 * given conservation area.
	 * 
	 * @param caUuid
	 * @return ConservationAreaInfo or null if not found
	 */
	public ConservationAreaInfo getCaInfo(UUID caUuid) throws Exception{
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		
		ConnectClient simple = target.proxy(ConnectClient.class);
		try{
			return simple.getConservationArea(caUuid.toString());
		}catch (NotFoundException ex){
			return null;
		}
	}
	
	/**
	 * Gets the information on a connect server related to the
	 * all conservation areas
	 * 
	 * @param caUuid
	 * @return ConservationAreaInfo or null if not found
	 */
	public List<ConservationAreaInfo> getConservationAreas() throws Exception{
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		
		ConnectClient simple = target.proxy(ConnectClient.class);
		try{
			return simple.getConservationAreas();
		}catch (NotFoundException ex){
			return null;
		}
	}
	
	public String startConservationAreaDownload(UUID caUuid) throws Exception{
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		ConnectClient simple = target.proxy(ConnectClient.class);
		Response r = null;
		try{
			r = simple.downloadConservationArea(caUuid.toString(), ConnectClient.DATA_PARAM_ALL);
			if (r.getStatus() == Status.ACCEPTED.getStatusCode()){
				return r.getHeaderString(HttpHeaders.LOCATION);
			}
			throw new Exception("Error connecting to Connect Server. " + r.getStatus() + ": " + r.getStatusInfo().getReasonPhrase());
		}catch (NotFoundException ex){
			return null;
		}finally{
			if (r != null){
				r.close();
			}
		}
	}
	public String startChangeLogDownload(UUID caUuid, UUID version, Long revision) throws Exception{
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		ConnectClient simple = target.proxy(ConnectClient.class);
		Response r = null;
		try{
			r = simple.downloadChangeLog(caUuid.toString(), ConnectClient.DATA_PARAM_PACKAGE, version.toString(), String.valueOf(revision));
			if (r.getStatus() == Status.ACCEPTED.getStatusCode()){
				return r.getHeaderString(HttpHeaders.LOCATION);
			}
			throw new Exception("Error connecting to Connect Server. " + r.getStatus() + ": " + r.getStatusInfo().getReasonPhrase());
		}catch (NotFoundException ex){
			return null;
		}finally{
			if (r != null){
				r.close();
			}
		}
	}
	public Path downloadFileFromUrl(String url) throws Exception{
		int tryCount = 0;
		
		//download file name
		Path filestore = FileSystems.getDefault()
			.getPath(SmartContext.INSTANCE.getFilestoreLocation())
			.resolve(ConnectSyncHistoryRecord.CONNECT_FILESTORE_DIR)
			.resolve(System.nanoTime() + ".temp.zip");
		
		Long size = null;
		
		//first request; this one gives us the requested size
		while(size == null && tryCount < MAX_RETRY_DOWNLOAD){
			Response r = null;
			try{
				ResteasyWebTarget target = client.target(url);
				r = target.request().get();
			
				if (r.getStatus() == HttpURLConnection.HTTP_OK){
				
					size = Long.valueOf(r.getHeaderString(HttpHeaders.CONTENT_LENGTH));
							
					//parse target
					try(InputStream is = r.readEntity(InputStream.class)){
						Files.copy(is, filestore);
					}	
					if (Files.size(filestore) > size){
						throw new Exception("Downloaded file size greater than expected file size.");
					}
					if (Files.size(filestore) == size){
						return filestore;
					}
				}
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}finally{
				if (r != null) r.close();
			}
			tryCount ++;
		}
		
		//try a maximum of 10 times
		while(tryCount < MAX_RETRY_DOWNLOAD){
			downloadRequest(filestore, url, size);
			
			if (Files.size(filestore) > size){
				throw new Exception("Downloaded file size greater than expected file size.");
			}
			if (Files.size(filestore) == size){
				return filestore;
			}
			tryCount++;
		}
		throw new Exception("Failed to download conservation export from server.");
	}
	
	private void downloadRequest(Path p, String url, long maxLength) throws Exception{
		ResteasyWebTarget target = client.target(url);
		Response r = null;
		try {
			Long start = Files.size(p);
			Long end = maxLength;
			
			Builder requestBuilder = target.request();
			if (start != 0 && end != maxLength){
				requestBuilder.header("Range", "bytes=" + start + "-" + end);
			}
			
			r = target.request().get();
			if (r.getStatus() == HttpURLConnection.HTTP_OK){
				try(InputStream is = r.readEntity(InputStream.class);
						OutputStream out = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND)){
					IOUtils.copyLarge(is, out);
				}
			}
		}catch (Exception ex){
			ConnectPlugIn.log(ex.getMessage(), ex);
		}finally{
			r.close();
		}
	}
	
	/**
	 * Gets the status of an smart connect upload url.
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public WorkItemStatus getWorkItemStatus(String url) throws Exception{
		ResteasyWebTarget target = client.target(url);
		
		Response r = target.request().get();
		try{
			if (r.getStatus() == HttpURLConnection.HTTP_OK){
				//parse target
				
				return r.readEntity(WorkItemStatus.class);
				
			}else if (r.getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED){
				throw new NotAuthorizedException(r);
			}else if (r.getStatus() == HttpURLConnection.HTTP_NOT_FOUND){
				throw new NotFoundException(r);
			}else{
				throw new ClientErrorException(r);
			}
		}finally{
			r.close();
		}
	}
	/**
	 * 
	 * @param caUuid
	 * @return the upload url for the new conservation area
	 */
	//TODO: look into socket exceptions
	public void uploadFile(String url, Path f, long startByte) throws Exception{
        
		//ensure we do not retry, retrying is a problem
		HttpClient lclient = ((ApacheHttpClient4Engine) client.httpEngine()).getHttpClient();
        ((AbstractHttpClient )lclient).setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler
                (0, false));
        
        ConnectClient service =  client.target(url).proxy(ConnectClient.class);
		
		try(InputStream fis = Files.newInputStream(f)){
			fis.skip(startByte);
			service.updateFile(fis);
		}
		

		//		FileInputStream fis = new FileInputStream(f);
//		fis.skip(startByte);
//		post.setEntity(new InputStreamEntity(fis, f.length() - startByte));
//		
//		lclient.execute(post, new ResponseHandler<String>() {
//
//			@Override
//			public String handleResponse(HttpResponse response)
//					throws ClientProtocolException, IOException {
//				System.out.println(response.getStatusLine());
//				return "";
//			}
//		});
//		return null;
////		
////		
////		
////		
//
//		ResteasyWebTarget target = client.target(url);
//		
//
//		FileInputStream fis = new FileInputStream(f);
////			//skip the first x bytes
//			fis.skip(startByte);
//			
//			javax.ws.rs.client.Entity<FileInputStream> e = 
//				javax.ws.rs.client.Entity.entity(fis, MediaType.APPLICATION_OCTET_STREAM_TYPE);
//
//			Response r = target.request().put(e);
//			System.out.println("done");
//			r.close();
//		//}
//		return null;
	}
	
	public static String parseErrorMessage(String json){
		try{
			return (new ObjectMapper()).readTree(json).get("error").textValue();
		}catch (Exception ex){
			return null;
		}
	}
	
	public final String processException (Throwable ex){
		if (ex instanceof NotFoundException){
			String msg = MessageFormat.format("Could not connect to ({0}).", new Object[]{server.getServerUrl()});
			ConnectPlugIn.log(msg, ex);
			return msg;
		}else if (ex instanceof NotAuthorizedException){
			String msg = "Invalid SMART Connect username or password.";
			ConnectPlugIn.log(msg, ex);
			return msg;
		}else{
			String msg = "Could not connect to server: " + ex.getMessage();
			ConnectPlugIn.log(msg, ex);
			return msg;
		}
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
	        String token = username + ":" + password;
	        String base64Token = Base64.encodeBase64String(token.getBytes(StandardCharsets.UTF_8));
	        requestContext.getHeaders().add("Authorization", "Basic " + base64Token);
	    }
	}
	
	
}
