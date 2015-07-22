package org.wcs.smart.connect.test.session;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

public class I18nTest {

	@Test
	public void testi18nHeaders() throws Exception{
		//make a request in english
		URIBuilder builder = new URIBuilder(SmartConnect.LOGIN_PAGE_URL);
		// Login Via Put Call
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		put.setHeader("Accept-Language", "en,en-US");
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("200 OK Expected", HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
				
				String language = null;
				for (Header h : response.getAllHeaders()){
					if (h.getName().equalsIgnoreCase("Content-Language")){
						Assert.assertNull("Should not have duplicate content-language headers", language);
						language = h.getValue();
					}
				}
				Assert.assertEquals(language,  "en");
				return null;
			}
		});
		
		//make a request in es
		put = new HttpPut(builder.build());
		put.setHeader("Accept-Language", "es,en;q=0.7");
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("200 OK Expected", HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
				
				String language = null;
				for (Header h : response.getAllHeaders()){
					if (h.getName().equalsIgnoreCase("Content-Language")){
						Assert.assertNull("Should not have duplicate content-language headers", language);
						language = h.getValue();
					}
				}
				Assert.assertEquals(language,  "es");
				return null;
			}
		});
		
		//make a request in something we don't support
		put = new HttpPut(builder.build());
		put.setHeader("Accept-Language", "ab,en;q=0.7,es;q=0.3");
		httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				Assert.assertEquals("200 OK Expected",
						HttpURLConnection.HTTP_OK, response.getStatusLine()
								.getStatusCode());

				String language = null;
				for (Header h : response.getAllHeaders()) {
					if (h.getName().equalsIgnoreCase("Content-Language")) {
						Assert.assertNull(
								"Should not have duplicate content-language headers",
								language);
						language = h.getValue();
					}
				}
				Assert.assertEquals(language, "en");
				return null;
			}
		});
	}
}
