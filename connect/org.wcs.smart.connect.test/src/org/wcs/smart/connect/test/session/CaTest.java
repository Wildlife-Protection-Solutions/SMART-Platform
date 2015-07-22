package org.wcs.smart.connect.test.session;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CaTest {

	@Test
	public void testConservationAreaAPI() throws Exception{
		testListConservationArea();
		testCreateConservationArea();
		testDeleteConservationArea();
	}
	
	public void testListConservationArea() throws Exception{
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpGet get = SmartConnect.createGet(SmartConnect.CA_API_URL,  null);
		
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_OK, 
						response.getStatusLine().getStatusCode());
				String contentType = null;
				for (Header h : response.getAllHeaders()){
					if (h.getName().equalsIgnoreCase("Content-Type")){
						Assert.assertNull("Duplicate content type headers provided.", contentType);
						contentType = h.getValue();
					}
				}
				Assert.assertEquals("Json expected", SmartConnect.MT_APPLICATION_JSON, contentType);
				
				String userObject = EntityUtils.toString(response.getEntity());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				Assert.assertTrue("No conservation areas loaded", n.size() >= 0);
				return null;
			}
		});
	}
	
	public void testCreateConservationArea() throws Exception{
		String cauuid = "12345678-1234-1234-1234-1234567890ab";
		String cauuid2 = "12345678-1234-1234-1234-1234567890ac";
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPost post = SmartConnect.createPost(SmartConnect.CA_API_URL + "/" + cauuid, 
				new String[][]{{"X-Upload-Content-Type", SmartConnect.MT_APPLICATION_OCTET},
								{"X-Upload-Content-Length", "1"}});

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
				Assert.assertEquals("no content", 0, Integer.parseInt(contentLength));
				Assert.assertTrue("location", location.startsWith(SmartConnect.UPLOAD_API_URL));
				return null;
			}
		});
		
		//second post of same ca should fail
		post = SmartConnect.createPost(SmartConnect.CA_API_URL + "/" + cauuid, 
				new String[][]{{"X-Upload-Content-Type", SmartConnect.MT_APPLICATION_OCTET},
								{"X-Upload-Content-Length", "1"}});
		httpClient.execute(post, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Cannot upload duplicate ca's", HttpURLConnection.HTTP_BAD_REQUEST, response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
		//second post of same ca with no content should fail
		post = SmartConnect.createPost(SmartConnect.CA_API_URL + "/" + cauuid2, 
				new String[][]{{"X-Upload-Content-Type", SmartConnect.MT_APPLICATION_OCTET},
				 			  {"X-Upload-Content-Length", "0"}});
		httpClient.execute(post, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Cannot upload ca with no data", HttpURLConnection.HTTP_BAD_REQUEST, response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	public void testDeleteConservationArea() throws Exception{
		String cauuid = "12345678-1234-1234-1234-1234567890ab";
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpDelete call = SmartConnect.createDelete(SmartConnect.CA_API_URL + "/" + cauuid, null);

		httpClient.execute(call, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_NO_CONTENT, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
		//execute a second time should return resource not found
		httpClient.execute(call, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("second delete should return not found", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
}
