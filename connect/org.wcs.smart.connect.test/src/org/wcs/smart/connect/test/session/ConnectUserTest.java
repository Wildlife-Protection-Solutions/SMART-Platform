package org.wcs.smart.connect.test.session;

import java.io.IOException;
import java.net.HttpURLConnection;

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

public class ConnectUserTest {

	private static String username = "connecttest";
	private static String password = "connectpassword";
	private static String email = "connet@smart.com";
	
	private static String username2 = "connecttest_v2";
	private static String email2 = "connet_v2@smart.com";
	private static String password2 = "connectpassword2";
	
	private static String invalidusername = "abc";
	private static String invalidpassword = "abc";
	
	@Test
	public void testUser() throws Exception{
		
		testCreateUser();
		testUserLogin();
		testGetUser();
		testUpdateUser();
		testUpdateUserInvalidUsername();
		testUpdatePassword();
		testUpdateInvalidPassword();
		testDeleteUser();

	}

	
	@Test
	public void testGetAllUsers() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.USER_API_URL);
		
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
	
	public void testCreateUser() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.USER_API_URL + "/" + username);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPost post = new HttpPost(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		post.addHeader("Authorization", "basic " + info);
		post.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		//String json = "{\"username\": \"" + username + "\", \"password\":\"" + password+"\", \"email\":\"" + email + "\"}";
		
		String json = "{\"username\": \"" + "testjeffy" + "\", \"email\":\"" + "234asdasd" +"\", \"email\":\"" + email + "\"}";
		
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        post.setEntity(entity);
		
		httpClient.execute(post, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Created expected", HttpURLConnection.HTTP_CREATED, 
						response.getStatusLine().getStatusCode());
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
	
	public void testUserLogin() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.TEST_LOGIN_PAGE_URL);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpGet get = new HttpGet(builder.build());
		String info = Base64.encode( (username + ":" + password).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_OK, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	
	public void testGetUser() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.USER_API_URL + "/" + username);
		
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
				Assert.assertEquals("Validating only username and email returned", 2, n.size());
				Assert.assertEquals("Validating username", username, n.findValue("username").textValue());
				Assert.assertEquals("Validating email", email, n.findValue("email").textValue());
				return null;
			}
		});
	}
	
	public void testUpdateUser() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.USER_API_URL + "/" + username);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		String json = "{\"username\": \"" + username2 + "\", \"email\":\"" + email2 + "\"}";
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
				Assert.assertEquals("Validating only username and email returned", 2, n.size());
				Assert.assertEquals("Validating username", username2, n.findValue("username").textValue());
				Assert.assertEquals("Validating email", email2, n.findValue("email").textValue());
				return null;
			}
		});
		
		//lets get the old user
		HttpGet get = new HttpGet(builder.build());
		get.addHeader("Authorization", "basic " + info);
		
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Username updated, old username should return not found", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
		//lets get the new user
		builder = new URIBuilder(SmartConnect.USER_API_URL + "/" + username2);
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
				Assert.assertEquals("Validating only username and email returned", 2, n.size());
				Assert.assertEquals("Validating username", username2, n.findValue("username").textValue());
				Assert.assertEquals("Validating email", email2, n.findValue("email").textValue());
				return null;
			}
		});
	}
	
	public void testUpdatePassword() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.USER_API_URL + "/" + username2);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		String json = "{\"oldpassword\": \"" + password+ "\", \"password\":\"" + password2 + "\"}";
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
				Assert.assertEquals("Validating only username and email returned", 2, n.size());
				Assert.assertEquals("Validating username", username2, n.findValue("username").textValue());
				Assert.assertEquals("Validating email", email2, n.findValue("email").textValue());
				return null;
			}
		});
		
		//lets test old password no longer allows you to login the old user
		URIBuilder builder2 = new URIBuilder(SmartConnect.TEST_LOGIN_PAGE_URL);
		HttpGet get = new HttpGet(builder2.build());
		info = Base64.encode( (username2 + ":" + password).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Old password should not longer work", HttpURLConnection.HTTP_UNAUTHORIZED, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
		//lets test new password allows you to login the old user
		get = new HttpGet(builder2.build());
		info = Base64.encode( (username2 + ":" + password2).getBytes() );
		get.addHeader("Authorization", "basic " + info);
				
		httpClient.execute(get, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("New password should authenticate", HttpURLConnection.HTTP_OK, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	public void testUpdateInvalidPassword() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.USER_API_URL + "/" + username2);
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		String json = "{\"oldpassword\": \"" + password+ "\", \"password\":\"" + invalidpassword + "\"}";
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Invalid new password", HttpURLConnection.HTTP_BAD_REQUEST, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
		//lets provide the wrong original password but a valid new password
		put = new HttpPut(builder.build());
		info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		json = "{\"oldpassword\": \"" + invalidpassword + "\", \"password\":\"" + SmartConnect.PASSWORD + "\"}";
        entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Invalid original password", HttpURLConnection.HTTP_BAD_REQUEST, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
	
	public void testUpdateUserInvalidUsername() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.USER_API_URL + "/" + username2);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		
		put.addHeader("Authorization", "basic " + info);
		put.addHeader("Content-Type", SmartConnect.MT_APPLICATION_JSON);
		String json = "{\"username\": \"" + invalidusername + "\"}";
        HttpEntity entity = new ByteArrayEntity(json.getBytes());
        put.setEntity(entity);
        
		httpClient.execute(put, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("OK expected", HttpURLConnection.HTTP_BAD_REQUEST, 
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
				Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, n.get("status").asInt());
				String error = n.get("error").asText();
				Assert.assertNotNull(error);
				Assert.assertTrue(error != null && !error.isEmpty());
				return null;
			}
		});
	}
	
	
	public void testDeleteUser() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.USER_API_URL  + "/" + username2);
		
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
				Assert.assertEquals("Username deleted, username should return not found", HttpURLConnection.HTTP_NOT_FOUND, 
						response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
		//also ensure user can no longer login
		get = new HttpGet(builder.build());
		get.addHeader("Authorization", "basic " + Base64.encode( (username2 + ":" + password).getBytes() ));
		httpClient.execute(get, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("User deleted, should not longer login", HttpURLConnection.HTTP_UNAUTHORIZED, 
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
