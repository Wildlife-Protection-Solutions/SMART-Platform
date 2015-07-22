package org.wcs.smart.connect.test.session;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Iterator;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Test;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class LoginTest {

	@Test
	public void testNoSession() throws Exception{
		
		//create basic authentication header
		URIBuilder builder = new URIBuilder(SmartConnect.USER_API_URL + "/" + SmartConnect.USERNAME);
		
		//Valid Username and password
		HttpGet get = new HttpGet(builder.build());
		String info = Base64.encode( (SmartConnect.USERNAME + ":" + SmartConnect.PASSWORD).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		httpClient.execute(get, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("200 OK Expected", HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
		//invalid username and password
		get = new HttpGet(builder.build());
		info = Base64.encode( (SmartConnect.USERNAME + "2:" + SmartConnect.PASSWORD).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		httpClient.execute(get, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Invalid User, Expected Unauthorized", HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
	}
	
	

	@Test
	public void testSessionValidUser() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.LOGIN_API_URL);
		builder.setParameter("username", SmartConnect.USERNAME);
		builder.setParameter("password", SmartConnect.PASSWORD);
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String jsessionid = httpClient.execute(put, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("200 OK Expected", HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
				String cookie = null;
				for (Iterator<?> iterator = response.headerIterator(); iterator.hasNext();) {
					Header type = (Header) iterator.next();
					if (type.getName().equalsIgnoreCase("Set-Cookie")){
						cookie = type.getValue();
					}
				}
				
				Assert.assertTrue("Cookie Value has session",
						cookie != null && cookie.toUpperCase().contains("JSESSIONID"));
				
				return cookie.split(";")[0].split("=")[1];
				
			}
		});
		
		Assert.assertNotNull("Session should not be null.", jsessionid);
		
		//Get the user
		//make sure we can do something with this session
		builder = new URIBuilder(SmartConnect.USER_API_URL + "/" + SmartConnect.USERNAME);
		HttpGet get = new HttpGet(builder.build());
		BasicCookieStore cookieStore = new BasicCookieStore();
	    BasicClientCookie cookie = new BasicClientCookie("JSESSIONID", jsessionid);
	    cookie.setDomain("localhost");
	    cookie.setPath("/server/");
	    cookie.setSecure(true);
	    cookieStore.addCookie(cookie);
		
		 HttpContext localContext = new BasicHttpContext();
		    localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("200 OK Expected", HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
				return null;
			}
		}, localContext);
		
		//Logout
		URIBuilder logoutbuilder = new URIBuilder(SmartConnect.LOGOUT_API_URL);
		HttpGet logout= new HttpGet(logoutbuilder.build());
		httpClient.execute(logout, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("200 OK Expected", HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
				return null;
			}
		});
		
		//attempt to get user again; this should request in unauthorized
		httpClient.execute(get, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Logged out; should be unathorized", 
						HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusLine().getStatusCode());
				return null;
			}
		}, localContext);
	}
		
	@Test
	public void testSessionInvalidUser() throws Exception{
		URIBuilder builder = new URIBuilder(SmartConnect.LOGIN_API_URL);
		builder.setParameter("username", SmartConnect.USERNAME);
		builder.setParameter("password", SmartConnect.PASSWORD + "abc");
		
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		httpClient.execute(put, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("Invalid User, Unauthorized Excepted", HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusLine().getStatusCode());
				return null;
			}
		});
	}
}
