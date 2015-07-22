package org.wcs.smart.connect.test.session;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
import org.apache.http.util.EntityUtils;

import com.sun.org.apache.xml.internal.security.utils.Base64;


public class SessionTest {

	public static final String BASE_URL = "https://localhost:8443/server/";
	static {
	    //for localhost testing only
	    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
	    new javax.net.ssl.HostnameVerifier(){
 
	        public boolean verify(String hostname,
	                javax.net.ssl.SSLSession sslSession) {
	            if (hostname.equals("localhost")) {
	                return true;
	            }
	            return false;
	        }
	    });
	    
	    /*
	     *  fix for
	     *    Exception in thread "main" javax.net.ssl.SSLHandshakeException:
	     *       sun.security.validator.ValidatorException:
	     *           PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException:
	     *               unable to find valid certification path to requested target
	     */
	    TrustManager[] trustAllCerts = new TrustManager[] {
	       new X509TrustManager() {
	          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	            return null;
	          }

	          public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

	          public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

	       }
	    };

	    try{
	    SSLContext sc = SSLContext.getInstance("SSL");
	    sc.init(null, trustAllCerts, new java.security.SecureRandom());
	    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	    // Create all-trusting host name verifier
	    HostnameVerifier allHostsValid = new HostnameVerifier() {
	        public boolean verify(String hostname, SSLSession session) {
	          return true;
	        }
	    };
	    // Install the all-trusting host verifier
	    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	    }catch (Exception ex){
	    	ex.printStackTrace();
	    }
	}

	
	
	public static void testLogin() throws Exception{
		// Trust own CA and all self-signed certs

        

		String vurl = BASE_URL + "login";
		String user = "smart";
		String pass = "smart";
		
		URIBuilder builder = new URIBuilder(vurl);
		builder.setParameter("username", user);
		builder.setParameter("password", pass);
		
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		HttpPut put = new HttpPut(builder.build());
		String x = httpClient.execute(put, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				// TODO Auto-generated method stub
				for (Iterator<?> iterator = response.headerIterator(); iterator.hasNext();) {
					Header type = (Header) iterator.next();
					System.out.println(type.getName());
					System.out.println(type.getValue());
					System.out.println("----");
				}
				System.out.println(EntityUtils.toString(response.getEntity()));
				System.out.println(response.getStatusLine());
				return null;
			}
		});
		System.out.println(x);
	}
	
	public static void getUserTest() throws Exception{
		String sessionId = "E5528AC55F88DA376D3176CF2A2A5867";
		
		String vurl = BASE_URL + "api/connectuser/smart";
		URIBuilder builder = new URIBuilder(vurl);
		HttpGet get = new HttpGet(builder.build());
		System.out.println(get.getURI().toASCIIString());
		//get.setHeader("Authorization", "SMART Credential=8277EEAD7A3D60050595B2602841A74F");
		
		BasicCookieStore cookieStore = new BasicCookieStore();
	    BasicClientCookie cookie = new BasicClientCookie("JSESSIONID", sessionId);
	    cookie.setDomain("localhost");
	    cookie.setPath("/server/");
	    cookie.setSecure(true);
	    cookieStore.addCookie(cookie);
//		get.setHeader("Cookie", "JSESSIONID="+sessionId);
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		
		 HttpContext localContext = new BasicHttpContext();
		    localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				// TODO Auto-generated method stub
				if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK){
					System.out.println(EntityUtils.toString(response.getEntity()));
				}else{
					System.out.println("FAIL");
					System.out.println(response.getStatusLine());
				}
				return null;
			}
		}, localContext);
		
	}

	public static void getUserTestUserPassword() throws Exception{
		String username = "smart";
		String password = "smart";
		
	
		String info = Base64.encode( (username + ":" + password).getBytes() );
		
		String vurl = BASE_URL + "api/connectuser/smart";
		URIBuilder builder = new URIBuilder(vurl);
		HttpGet get = new HttpGet(builder.build());
		
		get.addHeader("Authorization", "basic " + info);
		
		CloseableHttpClient httpClient = SmartConnect.createHttpClient();
		
		httpClient.execute(get, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				// TODO Auto-generated method stub
				if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK){
					System.out.println(EntityUtils.toString(response.getEntity()));
					System.out.println(response.getStatusLine());
					
					for (Header h : response.getAllHeaders()){
						System.out.println(h.getName() + ":" + h.getValue());
					}
				}else{
					System.out.println("FAIL");
					System.out.println(response.getStatusLine());
				}
				return null;
			}
		});
		
	}
	public static void main(String[] args) throws Exception{
		
//		testLogin();
		getUserTest();
//		getUserTestUserPassword();
	}
}

