package org.wcs.smart.connect.test.session;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.UUID;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.apache.xml.internal.security.utils.Base64;

public class ConnectMapLayerTest {

	private String layerName = "junit_test_layer";
	private int layerType = 1;
	private boolean active = true;
	
	private String token = "12345";
	private String mapboxId = "zxcvb123";
	private String wmsLayerList = "layer1 layer2 layer3";
	
	private UUID layerUuid = null;
	private UUID invalidLayerUuid =  UUID.fromString("f0eedf88-9c0c-4ef8-bb6d-6bb9bd340a39");
	
	@Test
	public void testLayers() throws Exception{
		testCreateLayer();
		testGetAllLayers();
		testGetLayer();
		testUpdateLayer();
		testUpdateLayerInvalidLayer();
		testUpdateLayerInvalidType();
		testDeleteLayer();
	}
		
	public void testCreateLayer() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.LAYER_API_URL + "/" + layerName);
		
		// Login
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPost post = new HttpPost(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		post.addHeader("Authorization", "basic " + info);
		post.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);

		String json = "{\"layerName\": \"" + layerName + 
				"\", \"layerType\": \"" + layerType +
				"\", \"active\": \"" + active +
				"\", \"token\": \"" + token +
				"\", \"mapboxId\": \"" + mapboxId +
				"\", \"wmsLayerList\": \"" + wmsLayerList  + 
				"\"}";
		
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        post.setEntity(entity);
		
		httpClient.execute(post, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("1)" + userObject);
				Assert.assertEquals("Create expected", HttpURLConnection.HTTP_CREATED, 
						response.getStatusLine().getStatusCode());
				
				
				//get the uuid to use for next tests
				//String userObject = EntityUtils.toString(response.getEntity());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				layerUuid = UUID.fromString(n.findValue("uuid").textValue());
				
				Assert.assertEquals("LayerName Test", layerName, n.findValue("layerName").textValue());
				return null;
			}
		});
	}
	
	
//	@Test
	public void testGetAllLayers() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.LAYER_API_URL);
		
		// Login
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpGet get = new HttpGet(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("3)" + userObject);
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
				
				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				Assert.assertTrue("Validating at least on user", n.size() > 0);
				
				Assert.assertTrue("Valid type in json for first layer?", n.get(0).get("layerType").asInt() > 0  && n.get(0).get("layerType").asInt() < 10);
				Assert.assertTrue("Valid layer name?", n.get(0).get("layerName").asText().length() > 0 && n.get(0).get("layerName").asText().length() < 33);
				return null;
			}
		});
	}
	
	
	
//	@Test	
	public void testGetLayer() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.LAYER_API_URL + "/" + layerUuid);
		
		// Login 
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpGet get = new HttpGet(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("4)" + userObject);
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
				
				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				Assert.assertEquals("Validating size", 7, n.size());
				Assert.assertEquals("Validating layer name", layerName, n.findValue("layerName").textValue());
				Assert.assertEquals("Validating type", layerType, n.findValue("layerType").intValue());
				Assert.assertEquals("Validating active", active, n.findValue("active").booleanValue());
				Assert.assertEquals("Validating token", token, n.findValue("token").textValue());
				Assert.assertEquals("Validating mapbox id", mapboxId, n.findValue("mapboxId").textValue());
				Assert.assertEquals("Validating layer list", wmsLayerList, n.findValue("wmsLayerList").textValue());
			
				return null;
			}
		});
	}
	
//	@Test
	public void testUpdateLayer() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.LAYER_API_URL + "/" + layerUuid);
		
		// Login 
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);

		String json = "{\"layerName\": \"" + layerName + 
				"\", \"layerType\": \"" + layerType +
				"\", \"active\": \"" + "false" +
				"\", \"token\": \"" + "updated token" +
				"\", \"mapboxId\": \"" + "updated mapboxId" +
				"\", \"wmsLayerList\": \"" + wmsLayerList  + 
				"\"}";
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("5)" + userObject);
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
				

				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				Assert.assertEquals("Validating size", 7, n.size());
				Assert.assertEquals("Validating layer name", layerName, n.findValue("layerName").textValue());
				Assert.assertEquals("Validating type", layerType, n.findValue("layerType").intValue());
				Assert.assertEquals("Validating active", false, n.findValue("active").booleanValue());
				Assert.assertEquals("Validating token", "updated token", n.findValue("token").textValue());
				Assert.assertEquals("Validating mapbox id", "updated mapboxId", n.findValue("mapboxId").textValue());
				Assert.assertEquals("Validating layer list", wmsLayerList, n.findValue("wmsLayerList").textValue());
			
				return null;
			}
		});
		
	}
	
//	@Test
	public void testUpdateLayerInvalidLayer() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.LAYER_API_URL + "/" + invalidLayerUuid );
		// Login 
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		String json = "{\"layerName\": \"" + layerName + 
				"\", \"layerType\": \"" + layerType +
				"\", \"active\": \"" + "false" +
				"\", \"token\": \"" + "updated token" +
				"\", \"mapboxId\": \"" + "updated mapboxId" +
				"\", \"wmsLayerList\": \"" + wmsLayerList  + 
				"\"}";

        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("6) (expected not found) " + userObject);
				Assert.assertEquals("Invalid Layer requested", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	
//	@Test
	public void testUpdateLayerInvalidType() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.LAYER_API_URL + "/" + layerUuid );
		// Login 
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		String json = "{\"layerName\": \"" + layerName + 
				"\", \"layerType\": \"" + -1 +
				"\", \"active\": \"" + "false" +
				"\", \"token\": \"" + "updated token" +
				"\", \"mapboxId\": \"" + "updated mapboxId" +
				"\", \"wmsLayerList\": \"" + wmsLayerList  + 
				"\"}";
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("7) (expected not found)" + userObject);
				Assert.assertEquals("Invalid Layer Type", HttpURLConnection.HTTP_BAD_REQUEST, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});	
		
		String json2 = "{\"layerName\": \"" + "a really long layer name with too many characters that should fail" + 
				"\", \"layerType\": \"" + layerType +
				"\", \"active\": \"" + "false" +
				"\", \"token\": \"" + "updated token" +
				"\", \"mapboxId\": \"" + "updated mapboxId" +
				"\", \"wmsLayerList\": \"" + wmsLayerList  + 
				"\"}";
        HttpEntity entity2 = new ByteArrayEntity(json2.getBytes());
        put.setEntity(entity2);
        
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("7.1) (expected not found)" + userObject);
				Assert.assertEquals("Invalid Layer Type", HttpURLConnection.HTTP_BAD_REQUEST, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});	
	}
	
//	@Test
	public void testDeleteLayer() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.LAYER_API_URL  + "/" + layerUuid);
		
		// Login 
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpDelete post = new HttpDelete(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		post.addHeader("Authorization", "basic " + info);
		httpClient.execute(post, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("10)" + userObject);
				Assert.assertEquals("Delete OK", HttpURLConnection.HTTP_OK, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
		//lets get the layer back; this should fail as layer should be deleted
		HttpGet get = new HttpGet(builder.build());
		get.addHeader("Authorization", "basic " + info);
		httpClient.execute(get, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("11) (should be blank)" + userObject);
				Assert.assertEquals("Layer deleted, should return not found", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
				
		//try to delete again
		// Login 
		HttpDelete delete = new HttpDelete(builder.build());
		delete.addHeader("Authorization", "basic " + info);
		httpClient.execute(delete, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("12) (expected 404)" + userObject);
				Assert.assertEquals("Delete resource doesn't exist", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
}
