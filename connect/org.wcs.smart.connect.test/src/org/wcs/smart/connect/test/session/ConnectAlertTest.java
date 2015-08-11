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

public class ConnectAlertTest {
	

	private static UUID alertUuid;

	
	private static String invalidUserGeneratedId = "iuasdfiuasdf";
	
	private static String userGeneratedId = "alert123"; 
	private Date date = new Date(2015,1,1);
	private String description = "basic description of an alert. Some was seen in the woods wearing a big blue costume and singing songs...";
	private UUID type_uuid = UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a50");
	private UUID invalid_type_uuid = UUID.fromString("f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a59");
	private Integer level = new Integer(3);
	private UUID caUuid = UUID.fromString("a0eedf99-9c0c-4ef8-bb6d-6bb9bd340a36");
	private UUID invalidCaUuid = UUID.fromString("f0eedf88-9c0c-4ef8-bb6d-6bb9bd340a39");
	private String status = "ACTIVE";
	private Double x = new Double(-2);
	private Double y = new Double(-1);
	private UUID creator_uuid = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a36");

	private static String userGeneratedId2 = "alert456"; 
	private Date date2 = new Date(2015,12,12);
	private String description2 = "new description";
	private Integer level2 = new Integer(4);
	private String status2 = "UNACTIVE";
	private Double x2 = new Double(23);
	private Double y2 = new Double(0);
	

	
	
	@Test
	public void testAlert() throws Exception{

		testCreateAlert();
		//testGetAllAlerts();
		//testGetAlert();
		//testGetAlertsFromCa();
		//testUpdateAlert();
		//testUpdateAlertInvalidAlert();
		//testUpdateAlertInvalidType();
		//testUpdateAlertInvalidCa();
		//testDeleteAlert();
	}
	
	
	public void testGetAllAlerts() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpGet get = new HttpGet(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		
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
				Assert.assertTrue("Validating at least on user", n.size() > 0);
				return null;
			}
		});
	}
	
	public void testCreateAlert() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + userGeneratedId);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPost post = new HttpPost(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		post.addHeader("Authorization", "basic " + info);
		post.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
//		String json = "{\"username\": \"" + username + "\", \"password\":\"" + password+"\", \"email\":\"" + email + "\"}";
		String json = "{\"userGeneratedId\": \"" + userGeneratedId  + 
				"\", \"date\": \"" + date +
				"\", \"description\": \"" + description +
				"\", \"type_uuid\": \"" + type_uuid +
				"\", \"level\": \"" + level +
				"\", \"ca_uuid\": \"" + caUuid  + 
				"\", \"status\": \"" + status +
				"\", \"x\": \"" + x + 
				"\", \"y\": \"" + y +
				"\", \"creator_uuid\": \"" + creator_uuid + 
				"\"}";
		
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        post.setEntity(entity);
		
		httpClient.execute(post, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Created expected", HttpURLConnection.HTTP_CREATED, 
						response.getStatusLine().getStatusCode());
				
				
				//get the uuid to use for next tests
				String userObject = EntityUtils.toString(response.getEntity());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				alertUuid = UUID.fromString(n.findValue("userGeneratedId").textValue());
				
				return null;
			}
		});
		//try put again - this time it should fail
		post = new HttpPost(builder.build());
		post.addHeader("Authorization", "basic " + info);
		post.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		post.setEntity(entity);
		
		httpClient.execute(post, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Second attempt to create user should fail", HttpURLConnection.HTTP_BAD_REQUEST, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
		
	public void testGetAlert() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + alertUuid);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpGet get = new HttpGet(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		
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
				Assert.assertEquals("Validating size", 11, n.size());
				Assert.assertEquals("Validating usergen id", userGeneratedId, n.findValue("userGeneratedId").textValue());
				/*
				 * TODO 
				 * Validate the rest of the variables
				 * 
				 * description = "basic description of an alert. Some was seen in teh woods wearing a big blue costume and singing songs...";
				private UUID type_uuid = UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a50");
				private UUID invalid_type_uuid = UUID.fromString("f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a59");
				private Integer level = new Integer(3);
				private UUID ca_uuid = UUID.fromString("a0eedf99-9c0c-4ef8-bb6d-6bb9bd340a36");
				private UUID invalid_ca_uuid = UUID.fromString("f0eedf88-9c0c-4ef8-bb6d-6bb9bd340a39");
				private String status = "ACTIVE";
				private Double x = new Double(-2);
				private Double y = new Double(-1);
				private UUID creator_uuid
				*/
				
				return null;
			}
		});
	}
	
	public void testUpdateAlert() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + userGeneratedId);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		//String json = "{\"username\": \"" + username2 + "\", \"email\":\"" + email2 + "\"}";
		String json = "{\"userGeneratedId\": \"" + userGeneratedId  + "\", \"ca_uuid\": \"" + caUuid  + "\", \"creator_uuid\": \"" + creator_uuid+ "\", \"date\": \"" + date2 + "\", \"description\": \"" + description2 + "\", \"level\": \"" + level2 + "\", \"status\": \"" + status2 + "\", \"type_uuid\": \"" + type_uuid + "\", \"x\": \"" + x2 + "\", \"y\": \"" + y2 + "\"}";
		
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {

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
				Assert.assertEquals("Validating size", 11, n.size());
				Assert.assertEquals("Validating usergen id", userGeneratedId, n.findValue("userGeneratedId").textValue());
				return null;
			}
		});
		
		//lets get the old alert
		HttpGet get = new HttpGet(builder.build());
		get.addHeader("Authorization", "basic " + info);
		
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Alert updated, old alertshould return not found", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
		//lets get the new user
		builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + userGeneratedId);
		get = new HttpGet(builder.build());
		get.addHeader("Authorization", "basic " + info);
		
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
				Assert.assertEquals("Validating size", 11, n.size());
				Assert.assertEquals("Validating usergen id", userGeneratedId, n.findValue("userGeneratedId").textValue());
				
				//TODO 
				//test the other values updated properly
				return null;
			}
		});
	}
	
	
	public void testUpdateAlertInvalidAlert() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + invalidUserGeneratedId );
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		String json = "{\"userGeneratedId\": \"" + invalidUserGeneratedId  + "\", \"ca_uuid\": \"" + caUuid  + "\", \"creator_uuid\": \"" + creator_uuid+ "\", \"date\": \"" + date + "\", \"description\": \"" + description + "\", \"level\": \"" + level + "\", \"status\": \"" + status + "\", \"type_uuid\": \"" + type_uuid + "\", \"x\": \"" + x + "\", \"y\": \"" + y + "\"}";
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Invalid Alert requested", HttpURLConnection.HTTP_BAD_REQUEST, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	
	
	public void testUpdateAlertInvalidType() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + invalidUserGeneratedId );
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		String json = "{\"userGeneratedId\": \"" + invalidUserGeneratedId  + "\", \"ca_uuid\": \"" + caUuid  + "\", \"creator_uuid\": \"" + creator_uuid+ "\", \"date\": \"" + date + "\", \"description\": \"" + description + "\", \"level\": \"" + level + "\", \"status\": \"" + status + "\", \"type_uuid\": \"" + invalid_type_uuid + "\", \"x\": \"" + x + "\", \"y\": \"" + y + "\"}";
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Invalid Alert Type", HttpURLConnection.HTTP_BAD_REQUEST, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});	
	}
	
	
	private void testUpdateAlertInvalidCa() throws Exception {
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + invalidCaUuid);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpGet get = new HttpGet(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		
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
				Assert.assertEquals("not a valid Ca, should return not found", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());

				return null;
			}
		});
	}
	
	private void testGetAlertsFromCa() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/ca/" + caUuid);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpGet get = new HttpGet(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		
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
				Assert.assertEquals("Validating size", 11, n.size());

				return null;
			}
		});
		
	}
	
	public void testDeleteAlert() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL  + "/" + alertUuid);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpDelete post = new HttpDelete(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		post.addHeader("Authorization", "basic " + info);
		httpClient.execute(post, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Delete OK", HttpURLConnection.HTTP_OK, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
		//lets get the user; this should fail as user should be deleted
		HttpGet get = new HttpGet(builder.build());
		get.addHeader("Authorization", "basic " + info);
		httpClient.execute(get, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Alert deleted, should return not found", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
				
		//try to delete again
		// Login Via Put Call
		HttpDelete delete = new HttpDelete(builder.build());
		delete.addHeader("Authorization", "basic " + info);
		httpClient.execute(delete, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Delete resource doesn't exist", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
}
