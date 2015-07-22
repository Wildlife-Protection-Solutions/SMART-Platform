package org.wcs.smart.connect.test.session;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoadCaTestDb {
	
	public static final String FULL_CA_EXPORT = "FullCa.export.zip";
	public static final String FULL_CA_UUID = "8f7fbe1b-201a-4ef4-bda8-14f5581e65ce";
	
	@Test
	public void testFullUploadFileSingleCall() throws Exception{
		File f = new File(ClassLoader.getSystemResource(FULL_CA_EXPORT).toURI());
		
		//create ca
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPost post = SmartConnect.createPost(SmartConnect.CA_API_URL + "/" + FULL_CA_UUID, 
				new String[][]{{"X-Upload-Content-Type", SmartConnect.MT_APPLICATION_OCTET},
								{"X-Upload-Content-Length", String.valueOf(Files.size(f.toPath()))}});
		
		final String[] uploadUrl = new String[]{""};
		httpClient.execute(post, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_OK, 
						response.getStatusLine().getStatusCode());
				String contentLength = null;
				String location = null;
				for (Header h : response.getAllHeaders()){
					if (h.getName().equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)){
						Assert.assertNull("Duplicate content type headers provided.", contentLength);
						contentLength = h.getValue();
					}else if (h.getName().equalsIgnoreCase(HttpHeaders.LOCATION)){
						Assert.assertNull("Duplicate location headers provided.", location);
						location = h.getValue();
					}
				}
				uploadUrl[0] = location;
				return null;
			}
		});
		
		String url = uploadUrl[0];
		//lets to a get on the upload URL
		HttpGet get = SmartConnect.createGet(url, null);  
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_OK, 
						response.getStatusLine().getStatusCode());

				//content -type check 
				String contentType = null;
				for (Header h : response.getAllHeaders()){
					if (h.getName().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)){
						Assert.assertNull("Duplicate content type headers provided.", contentType);
						contentType = h.getValue();
					}
				}
				Assert.assertEquals("Json expected", SmartConnect.MT_APPLICATION_JSON, contentType);
				
				String userObject = EntityUtils.toString(response.getEntity());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				Assert.assertEquals("Uploading status expected", "UPLOADING", n.findValue("status").asText());
				Assert.assertEquals("Current size should be 0", 0, n.findValue("current_size").asInt());
				Assert.assertEquals("Total size should be 0", Files.size(f.toPath()), n.findValue("expected_size").asInt());
				
				return null;
			}
		});
		
		//put the entire file
		HttpPut put = SmartConnect.createPut(url, new String[][]
				{{HttpHeaders.CONTENT_TYPE,SmartConnect.MT_APPLICATION_OCTET}});
		put.setEntity(new FileEntity(f));
		httpClient.execute(put, new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse response)
							throws ClientProtocolException, IOException {
						Assert.assertEquals("Accepted expected", HttpURLConnection.HTTP_ACCEPTED, 
								response.getStatusLine().getStatusCode());
						return null;
					}
				});
		
		//lets to a get on the upload URL
		get = SmartConnect.createGet(url, null);  
		httpClient.execute(get, new ResponseHandler<String>() {

					@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_OK, 
						response.getStatusLine().getStatusCode());

				//content -type check 
				String contentType = null;
				for (Header h : response.getAllHeaders()){
					if (h.getName().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)){
						Assert.assertNull("Duplicate content type headers provided.", contentType);
						contentType = h.getValue();
					}
				}
				Assert.assertEquals("Json expected", SmartConnect.MT_APPLICATION_JSON, contentType);
						
				String userObject = EntityUtils.toString(response.getEntity());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				Assert.assertEquals("Uploading or processing status expected", 
						"PROCESSING", n.findValue("status").asText());
				Assert.assertEquals("Current size should be filesize", Files.size(f.toPath()), n.findValue("current_size").asInt());
				Assert.assertEquals("Total size should be filesize", Files.size(f.toPath()), n.findValue("expected_size").asInt());
						
				return null;
			}
		});
		

	}
}
