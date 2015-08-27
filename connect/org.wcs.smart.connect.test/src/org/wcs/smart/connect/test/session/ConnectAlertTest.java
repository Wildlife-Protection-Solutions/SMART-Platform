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
	public enum AlertStatusEnum {
		ACTIVE("ACTIVE"),
		DISABLED("DISABLED");
	 
		protected String value;
			
		private AlertStatusEnum(String value) {
			this.value = value;
		}
		public String getValue() {
			return value;
		}
	}

	private static UUID alertUuid;

	
	private static String invalidUserGeneratedId = "iuasdfiuasdf";
	
	private static String userGeneratedId = "alert123"; 
	private Date date = new Date();
	private String description = "basic description of an alert. Some was seen in the woods wearing a big blue costume and singing songs...";
	private UUID typeUuid; //set in the getAlertTypes() call
	private UUID invalidTypeUuid = UUID.fromString("f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a59");
	private Integer level = new Integer(3);
	private UUID caUuid;
	private UUID invalidCaUuid = UUID.fromString("f0eedf88-9c0c-4ef8-bb6d-6bb9bd340a39");
	private AlertStatusEnum status = AlertStatusEnum.ACTIVE;
	private Double x = new Double(-2);
	private Double y = new Double(-1);
	
 
	private static String userGeneratedId2 = "alert456"; 
	private Date date2 = new Date();
	
	private String description2 = "new description";
	private Integer level2 = new Integer(4);
	private AlertStatusEnum status2 = AlertStatusEnum.DISABLED;
	private Double x2 = new Double(29);
	private Double y2 = new Double(0);
	

	
	@Test
	public void testAlert() throws Exception{
		getCaUuid(); //gets the first CA uuid so we have a valid one for testing
		testGetAlertTypes(); //gets the first alert type uuid so we have a valid one for testing
		testCreateAlert();
		testGetAllAlerts();
		testGetAlert();
		testUpdateAlert();
		testUpdateAlertInvalidAlert();
		testUpdateAlertInvalidType();
		testGetAlertInvalidCa();
		testGetAlertsFromCa();
		testDeleteAlert();
	}
	public void getCaUuid() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.CA_API_URL );
		
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
				System.out.println("0)" + userObject);
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
				caUuid = UUID.fromString(n.findValue("uuid").textValue());
				
				return null;
			}
		});
	}
	public void testGetAlertTypes() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + "alertTypes");
		
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
				System.out.println("0)" + userObject);
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
				typeUuid = UUID.fromString(n.findValue("uuid").textValue());
				
				return null;
			}
		});
	}
		
	public void testCreateAlert() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + userGeneratedId);
		
		// Login 
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPost post = new HttpPost(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		post.addHeader("Authorization", "basic " + info);
		post.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);

		String json = "{\"userGeneratedId\": \"" + userGeneratedId  + 
				"\", \"date\": \"" + date.getTime() +
				"\", \"description\": \"" + description +
				"\", \"typeUuid\": \"" + typeUuid +
				"\", \"level\": \"" + level +
				"\", \"caUuid\": \"" + caUuid  + 
				"\", \"status\": \"" + status +
				"\", \"x\": \"" + x + 
				"\", \"y\": \"" + y +
				"\"}";
		
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        post.setEntity(entity);
		
		httpClient.execute(post, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("1)" + userObject);
				Assert.assertEquals("Created expected", HttpURLConnection.HTTP_CREATED, 
						response.getStatusLine().getStatusCode());
				
				
				//get the uuid to use for next tests
				//String userObject = EntityUtils.toString(response.getEntity());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode n = mapper.readTree(userObject);
				alertUuid = UUID.fromString(n.findValue("uuid").textValue());
				
				Assert.assertEquals("Validating layer name", userGeneratedId, n.findValue("userGeneratedId").textValue());
				Assert.assertEquals("Validating description", description, n.findValue("description").textValue());
				Assert.assertEquals("Validating type", typeUuid.toString(), n.findValue("typeUuid").textValue());
				Assert.assertEquals("Validating level", level.intValue(), n.findValue("level").intValue());
				Assert.assertEquals("Validating ca uuid", caUuid.toString(), n.findValue("caUuid").textValue());
				Assert.assertEquals("Validating status", status.toString(), n.findValue("status").textValue());
				Assert.assertTrue("Validating x", x == n.findValue("x").asDouble());
				Assert.assertTrue("Validating y", y == n.findValue("y").asDouble());
				
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
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("2) (already exists expected)" + userObject);
				Assert.assertEquals("Second attempt to create alert should fail", HttpURLConnection.HTTP_BAD_REQUEST, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	
//	@Test
	public void testGetAllAlerts() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL);
		
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
				
				 
				Assert.assertTrue("Valid level property in a geojson feature?", n.get("features").get(0).get("properties").get("level").asInt() > 0  && n.get("features").get(0).get("properties").get("level").asInt() < 10);
				Assert.assertTrue("Valid x property in first geojson feature?", n.get("features").get(0).get("properties").get("x").asInt() > -181  && n.get("features").get(0).get("properties").get("x").asInt() < 181);
				Assert.assertTrue("Valid y property in first geojson feature?", n.get("features").get(0).get("properties").get("y").asInt() > -91  && n.get("features").get(0).get("properties").get("y").asInt() < 91);
				
				return null;
			}
		});
	}
	
	
	
//	@Test	
	public void testGetAlert() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + alertUuid);
		
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
				Assert.assertEquals("Validating size", 11, n.size());
				Assert.assertEquals("Validating usergen id", userGeneratedId, n.findValue("userGeneratedId").textValue());
				Assert.assertTrue("Validating x", n.findValue("x").intValue() > -181 && n.findValue("x").intValue() < 181);
				Assert.assertTrue("Validating y", n.findValue("y").intValue() > -91 && n.findValue("y").intValue() < 91);
				return null;
			}
		});
	}
	
//	@Test
	public void testUpdateAlert() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + userGeneratedId);
		
		// Login 
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		date2 = new Date();
		System.out.println(date2.getTime());
		String json = "{\"userGeneratedId\": \"" + userGeneratedId2  + "\", \"caUuid\": \"" + caUuid  +  "\", \"date\": \"" + date2.getTime() + "\", \"description\": \"" + description2 + "\", \"level\": \"" + level2 + "\", \"status\": \"" + status2 + "\", \"typeUuid\": \"" + typeUuid + "\", \"x\": \"" + x2 + "\", \"y\": \"" + y2 + "\"}";
		
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
				Assert.assertEquals("Validating size", 11, n.size());
				Assert.assertEquals("Validating usergen id", userGeneratedId2, n.findValue("userGeneratedId").textValue());
				Assert.assertEquals("Validating caUuid", caUuid.toString(), n.findValue("caUuid").textValue());
				Assert.assertEquals("Validating date2", date2.getTime(), n.findValue("date").asLong());
				Assert.assertEquals("Validating description2", description2, n.findValue("description").textValue());
				Assert.assertTrue("Validating level2", level2 == n.findValue("level").intValue());
				Assert.assertEquals("Validating status2", status2.toString(), n.findValue("status").textValue());
				Assert.assertEquals("Validating typeUuid", typeUuid.toString(), n.findValue("typeUuid").textValue());
				
				return null;
			}
		});
		
	}
	
//	@Test
	public void testUpdateAlertInvalidAlert() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + invalidUserGeneratedId );
		// Login 
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		String json = "{\"userGeneratedId\": \"" + invalidUserGeneratedId  + "\", \"caUuid\": \"" + caUuid  + "\", \"date\": \"" + date.getTime() + "\", \"description\": \"" + description + "\", \"level\": \"" + level + "\", \"status\": \"" + status + "\", \"typeUuid\": \"" + typeUuid + "\", \"x\": \"" + x + "\", \"y\": \"" + y + "\"}";
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("6) (expected not found) " + userObject);
				Assert.assertEquals("Invalid Alert requested", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	
//	@Test
	public void testUpdateAlertInvalidType() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + invalidUserGeneratedId );
		// Login 
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		String json = "{\"userGeneratedId\": \"" + invalidUserGeneratedId  + "\", \"caUuid\": \"" + caUuid  + "\", \"date\": \"" + date.getTime() + "\", \"description\": \"" + description + "\", \"level\": \"" + level + "\", \"status\": \"" + status + "\", \"typeUuid\": \"" + invalidTypeUuid + "\", \"x\": \"" + x + "\", \"y\": \"" + y + "\"}";
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("7) (expected not found)" + userObject);
				Assert.assertEquals("Invalid Alert Type", HttpURLConnection.HTTP_BAD_REQUEST, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});	
	}
	
//	@Test
	public void testGetAlertInvalidCa() throws Exception {
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/" + invalidCaUuid);
		
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
				System.out.println("8)(should be blank)" + userObject);
				Assert.assertEquals("not found expected", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	
//	@Test
	public void testGetAlertsFromCa() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL + "/ca/" + caUuid);
		
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
				System.out.println("9)" + userObject);
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
				for(int x=0; x < n.size(); x++){
					Assert.assertEquals("CA matches", caUuid.toString(), n.get(x).get("caUuid").toString().replace("\"", ""));
				}
				
				return null;
			}
		});
		
	}
	
	
//	@Test
	public void testDeleteAlert() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.ALERT_API_URL  + "/" + alertUuid);
		
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
		
		//lets get the alert; this should fail as alertshould be deleted
		HttpGet get = new HttpGet(builder.build());
		get.addHeader("Authorization", "basic " + info);
		httpClient.execute(get, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				String userObject = EntityUtils.toString(response.getEntity());
				System.out.println("11) (should be blank)" + userObject);
				Assert.assertEquals("Alert deleted, should return not found", HttpURLConnection.HTTP_NOT_FOUND, 
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
