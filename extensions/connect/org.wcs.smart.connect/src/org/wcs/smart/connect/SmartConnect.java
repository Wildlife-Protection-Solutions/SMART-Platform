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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.UUID;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.wcs.smart.connect.api.ConnectClient;
import org.wcs.smart.connect.api.model.ConservationAreaInfo;
import org.wcs.smart.connect.api.model.UploadStatus;

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
	
	private String url;
	private String username;
	
	private ResteasyClient client; 
	
	/**
	 * 
	 * @param url
	 * @param username
	 * @param password
	 */
	public SmartConnect(String url, String username, String password){
		this.url = url;
		this.username = username;
		
		client = new ResteasyClientBuilder().build();
		client.register(new AddAuthHeadersRequestFilter(username, password));
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
			ResteasyWebTarget target = client.target(url + API_URL);
		
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
		ResteasyWebTarget target = client.target(url + API_URL);
		
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
	 * Gets the information on a connect server related to the
	 * given conservation area.
	 * 
	 * @param caUuid
	 * @return ConservationAreaInfo or null if not found
	 */
	public ConservationAreaInfo getCaInfo(UUID caUuid) throws Exception{
		ResteasyWebTarget target = client.target(url + API_URL);
		
		ConnectClient simple = target.proxy(ConnectClient.class);
		try{
			return simple.getConservationArea(caUuid.toString());
		}catch (NotFoundException ex){
			return null;
		}
	}
	
	/**
	 * Gets the status of an smart connect upload url.
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public UploadStatus getUploadStatus(String url) throws Exception{
		ResteasyWebTarget target = client.target(url);
		
		Response r = target.request().get();
		try{
			if (r.getStatus() == HttpURLConnection.HTTP_OK){
				//parse target
				
				return r.readEntity(UploadStatus.class);
				
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
	public void uploadFile(String url, File f, long startByte) throws Exception{
        
		//ensure we do not retry, retrying is a problem
		HttpClient lclient = ((ApacheHttpClient4Engine) client.httpEngine()).getHttpClient();
        ((AbstractHttpClient )lclient).setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler
                (0, false));
        
        ConnectClient service =  client.target(url).proxy(ConnectClient.class);
		
		try(FileInputStream fis = new FileInputStream(f)){
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
	
	public final String processException (Throwable ex){
		if (ex instanceof NotFoundException){
			String msg = MessageFormat.format("Could not connect to ({0}).", new Object[]{url});
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
