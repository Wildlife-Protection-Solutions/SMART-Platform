package org.wcs.smart.connect.test.session;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class SmartConnect {

	public static final String BASE_URL = "https://localhost:8443/server/";
	public static final String USER_API_URL = SmartConnect.BASE_URL + "api/connectuser";
	public static final String ALERT_API_URL = SmartConnect.BASE_URL + "api/connectalert";
	public static final String LOGIN_API_URL = SmartConnect.BASE_URL + "login";
	public static final String LOGOUT_API_URL = SmartConnect.BASE_URL + "logout";
	public static final String LOGIN_PAGE_URL = SmartConnect.BASE_URL + "connect/login";
	public static final String TEST_LOGIN_PAGE_URL = SmartConnect.BASE_URL + "api/test";
	
	public static final String CA_API_URL = SmartConnect.BASE_URL + "api/conservationarea";
	public static final String UPLOAD_API_URL = SmartConnect.BASE_URL + "api/uploader";
	
	public static final String USERNAME = "smart";
	public static final String PASSWORD = "smart";
	
	public static final String MT_APPLICATION_JSON = "application/json";
	public static final String MT_APPLICATION_OCTET = "application/octet-stream";
	
	public static CloseableHttpClient createHttpClient() throws Exception{
		SSLContext sslcontext = SSLContexts.custom()
                .loadTrustMaterial(null, new TrustStrategy() {

                    @Override
                    public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                        return true;
                    }
                })
                .build();
        // Allow TLSv1 protocol only
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslcontext,
                new String[] { "TLSv1" },
                null,
        	    new javax.net.ssl.HostnameVerifier(){
                	 
        	        public boolean verify(String hostname,
        	                javax.net.ssl.SSLSession sslSession) {
        	            if (hostname.equals("localhost")) {
        	                return true;
        	            }
        	            return false;
        	        }
        	    });
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();
        return httpClient;
	}
	
	
	public static HttpPost createPost(String url, String[][] headers) throws Exception{
		return createPost(url, headers, USERNAME, PASSWORD);
	}
	
	public static HttpPost createPost(String url, String headers[][], String user, String password) throws Exception{
		URIBuilder builder = new URIBuilder(url);
		HttpPost post = new HttpPost(builder.build());
		String info = Base64.encode( (user + ":" + password).getBytes() );
		post.addHeader("Authorization", "basic " + info);
		
		if (headers != null){
			for (String[] header : headers){
				post.addHeader(header[0], header[1]);
			}
		}		
		return post;
	}
	
	
	public static HttpGet createGet(String url, String[][] headers) throws Exception{
		return createGet(url, headers, USERNAME, PASSWORD);
	}
	
	public static HttpGet createGet(String url, String headers[][], String user, String password) throws Exception{
		URIBuilder builder = new URIBuilder(url);
		HttpGet get = new HttpGet(builder.build());
		String info = Base64.encode( (user + ":" + password).getBytes() );
		get.addHeader("Authorization", "basic " + info);
		
		if (headers != null){
			for (String[] header : headers){
				get.addHeader(header[0], header[1]);
			}
		}		
		return get;
	}
	
	
	public static HttpPut createPut(String url, String[][] headers) throws Exception{
		return createPut(url, headers, USERNAME, PASSWORD);
	}
	
	public static HttpPut createPut(String url, String headers[][], String user, String password) throws Exception{
		URIBuilder builder = new URIBuilder(url);
		HttpPut put = new HttpPut(builder.build());
		String info = Base64.encode( (user + ":" + password).getBytes() );
		put.addHeader("Authorization", "basic " + info);
		
		if (headers != null){
			for (String[] header : headers){
				put.addHeader(header[0], header[1]);
			}
		}		
		return put;
	}
	
	
	public static HttpDelete createDelete(String url, String[][] headers) throws Exception{
		return createDelete(url, headers, USERNAME, PASSWORD);
	}
	
	public static HttpDelete createDelete(String url, String headers[][], String user, String password) throws Exception{
		URIBuilder builder = new URIBuilder(url);
		HttpDelete delete = new HttpDelete(builder.build());
		String info = Base64.encode( (user + ":" + password).getBytes() );
		delete.addHeader("Authorization", "basic " + info);
		
		if (headers != null){
			for (String[] header : headers){
				delete.addHeader(header[0], header[1]);
			}
		}		
		return delete;
	}
}
