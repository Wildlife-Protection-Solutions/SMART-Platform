package org.wcs.smart.connect.test.session;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CaUploaderTest {

	public static final String EMPTY_CA_EXPORT = "EmptyCa.export.zip";
	public static final String EMPTY_CA_UUID = "a24d825f-f7f8-4790-9d46-44861132b787";
	public static final String VERSION_UUID =  "00000000-0000-0000-0000-000000000001";

	@Test
	public void testUploadFileSingleCall() throws Exception{
		File f = new File(ClassLoader.getSystemResource(EMPTY_CA_EXPORT).toURI());
		
		//create ca
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPost post = SmartConnect.createPost(SmartConnect.CA_API_URL + "/" + EMPTY_CA_UUID + "?version=" + VERSION_UUID, 
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
				Assert.assertEquals("Uploading status expected", "PROCESSING", n.findValue("status").asText());
				Assert.assertEquals("Current size should be filesize", Files.size(f.toPath()), n.findValue("current_size").asInt());
				Assert.assertEquals("Total size should be filesize", Files.size(f.toPath()), n.findValue("expected_size").asInt());
						
				return null;
			}
		});
		
		//create ca
		HttpDelete delete= SmartConnect.createDelete(SmartConnect.CA_API_URL + "/" + EMPTY_CA_UUID + "?dataonly=false&username="+SmartConnect.USERNAME+"&password=" + SmartConnect.PASSWORD, null);
		httpClient.execute(delete, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_NO_CONTENT, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	@Test
	public void testUploadFileMultiParts() throws Exception{
		File f = new File(ClassLoader.getSystemResource(EMPTY_CA_EXPORT).toURI());
		//create ca
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPost post = SmartConnect.createPost(SmartConnect.CA_API_URL + "/" + EMPTY_CA_UUID + "?version=" + VERSION_UUID, 
			new String[][]{{"X-Upload-Content-Type", SmartConnect.MT_APPLICATION_OCTET},
						{"X-Upload-Content-Length", String.valueOf(Files.size(f.toPath()))}});
				
		final String[] uploadUrl = new String[]{""};
		httpClient.execute(post, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
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
		
		//put the first 2000 bytes
		HttpPut put = SmartConnect.createPut(url, new String[][]
				{{HttpHeaders.CONTENT_TYPE,SmartConnect.MT_APPLICATION_OCTET}});
		
		byte[] all = Files.readAllBytes(f.toPath());
		final byte[] fileBytes = Arrays.copyOf(all, 2000);
		put.setEntity(new ByteArrayEntity(fileBytes));
		httpClient.execute(put, new ResponseHandler<String>() {
				@Override
				public String handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					Assert.assertEquals("Accepted expected", HttpURLConnection.HTTP_ACCEPTED, 
							response.getStatusLine().getStatusCode());
					return null;
				}
		});
		
		final int[] start = new int[]{-1};
		//get should return processing with 2000 byte current size
		HttpGet get = SmartConnect.createGet(url, null);  
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_OK, 
						response.getStatusLine().getStatusCode());

				String userObject = EntityUtils.toString(response.getEntity());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				Assert.assertEquals("Uploading status expected", "UPLOADING", n.findValue("status").asText());
				Assert.assertEquals("Current size should be filesize", fileBytes.length, n.findValue("current_size").asInt());
				Assert.assertEquals("Total size should be filesize", Files.size(f.toPath()), n.findValue("expected_size").asInt());
				start[0] = n.findValue("current_size").asInt();
						
				return null;
			}
		});
		
		//put the remaining bytes
		byte[] fileBytes2 = Arrays.copyOfRange(all, start[0], all.length);
		put.setEntity(new ByteArrayEntity(fileBytes2));
		httpClient.execute(put, new ResponseHandler<String>() {
				@Override
				public String handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					Assert.assertEquals("Accepted expected", HttpURLConnection.HTTP_ACCEPTED, 
							response.getStatusLine().getStatusCode());
					return null;
				}
		});
		
		//at this point we should be done
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_OK, 
						response.getStatusLine().getStatusCode());

				String userObject = EntityUtils.toString(response.getEntity());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				Assert.assertEquals("Uploading status expected", "PROCESSING", n.findValue("status").asText());
				Assert.assertEquals("Current size should be filesize", Files.size(f.toPath()), n.findValue("current_size").asInt());
				Assert.assertEquals("Total size should be filesize", Files.size(f.toPath()), n.findValue("expected_size").asInt());
						
				return null;
			}
		});
		
		//delete CA
		HttpDelete delete= SmartConnect.createDelete(SmartConnect.CA_API_URL + "/" + EMPTY_CA_UUID + "?dataonly=false&username="+SmartConnect.USERNAME+"&password=" + SmartConnect.PASSWORD, null);
		httpClient.execute(delete, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_NO_CONTENT, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	
	
}
