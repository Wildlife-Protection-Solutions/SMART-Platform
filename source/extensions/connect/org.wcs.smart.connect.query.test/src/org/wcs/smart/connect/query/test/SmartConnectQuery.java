package org.wcs.smart.connect.query.test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.util.UuidUtils;

import au.com.bytecode.opencsv.CSVReader;


public class SmartConnectQuery {
	
	public final static String API_URL = "/api"; //$NON-NLS-1$
	
	private String username;
	private String password;
	private ConnectServer server;
	private ResteasyClient client;
	private Certificate currentCertificate;
	
	/**
	 * 
	 * @param url
	 * @param username
	 * @param password
	 */
	public SmartConnectQuery(ConnectServer server, String username, String password){
		this.server = server;
		this.username = username;
		this.password = password;
	}
	
	/**
	 * Gets a list of Conservation Areas on a connect
	 * server.
	 * 
	 * @return List of ConservationAreaInfo 
	 */
	public List<String[]> executeQuery(UUID queryUuid, DateFilter dFilter) throws Exception{
		createClient();
		ResteasyWebTarget target = client.target(server.getServerUrl() + API_URL);
		
		ConnectClient simple = target.proxy(ConnectClient.class);
		Response r = null;
		try{
			r = simple.getQueryResults(UuidUtils.uuidToString(queryUuid), "csv", null, null, dFilter.getDateFieldOption().getKey(), ",");
			CSVReader reader = new CSVReader(new InputStreamReader(r.readEntity(InputStream.class)));
			return reader.readAll();
		}catch (NotFoundException ex){
			return null;
		}finally{
			if (r != null) r.close();
		}
	}
	
	
	private void createClient(){
		if (client != null) return;
		// load the keystore that includes self-signed cert as a "trusted" entry
		try{
			
			X509TrustManager trustManager = getTrustManager(); 
			SSLContext ctx = SSLContext.getInstance("TLS"); //$NON-NLS-1$
			ctx.init(null, new TrustManager[]{trustManager}, null);
		
			SchemeRegistry registry = new SchemeRegistry();
			SSLSocketFactory factory = new SSLSocketFactory(ctx);
			registry.register(new Scheme("https", 443, factory)); //$NON-NLS-1$
			
			ClientConnectionManager cm = new PoolingClientConnectionManager(registry);
			HttpClient httpClient = new DefaultHttpClient(cm);
			ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
		
			client = new ResteasyClientBuilder()
				.httpEngine(engine)
				.build();

			client.register(new AddAuthHeadersRequestFilter(username, password));
			
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}
	}
	
	private X509TrustManager getTrustManager() throws Exception{
		this.currentCertificate = null;
		//get default jvm trust manager
		if (server.getCertificateFileName() == null){
			//use the default jvm trust manager
			TrustManagerFactory factory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			factory.init((KeyStore)null);
			for (TrustManager t : factory.getTrustManagers()){
				if (t instanceof X509TrustManager){
					return (X509TrustManager) t;
				}
			}
			throw new RuntimeException("SSH ERROR");
		}

		//build a trust manager using the locally provided certificate
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null);
		Path certpath = server.getLocalCertificateFile();
		try(InputStream is = new BufferedInputStream(Files.newInputStream(certpath))){
			currentCertificate = CertificateFactory.getInstance("X.509").generateCertificate(is); //$NON-NLS-1$
			String key = "smart-"; //$NON-NLS-1$
			if (server.getConservationArea() != null){
				key += server.getConservationArea().getUuid().toString();
			}
			keyStore.setCertificateEntry(key, currentCertificate);
		}
		TrustManagerFactory factory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		factory.init(keyStore);
		for (TrustManager t : factory.getTrustManagers()){
			if (t instanceof X509TrustManager){
				return (X509TrustManager) t;
			}
		}
		throw new RuntimeException("SSH Error");
	}
	
	/**
	 * Filter for adding auth headers to every request
	 * @author Emily
	 *
	 */
	public static class AddAuthHeadersRequestFilter implements ClientRequestFilter {

	    private final String username;
	    private final String password;

	    public AddAuthHeadersRequestFilter(String username, String password) {
	        this.username = username;
	        this.password = password;
	    }

	    @Override
	    public void filter(ClientRequestContext requestContext) throws IOException {
	        String token = username + ":" + password; //$NON-NLS-1$
	        String base64Token = Base64.encodeBase64String(token.getBytes(StandardCharsets.UTF_8));
	        requestContext.getHeaders().add("Authorization", "Basic " + base64Token); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	}
	
	public ConnectServer getServer(){
		return this.server;
	}
	
	/**
	 * Closes connect to SMART Server
	 */
	public void close(){
		if (client != null){
			client.close();
			client = null;
		}
	}
}
