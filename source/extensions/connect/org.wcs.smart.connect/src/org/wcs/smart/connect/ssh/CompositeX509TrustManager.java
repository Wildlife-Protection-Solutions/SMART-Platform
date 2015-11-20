package org.wcs.smart.connect.ssh;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.X509TrustManager;

import org.wcs.smart.connect.internal.Messages;

/* 
 * see: http://stackoverflow.com/questions/1793979/registering-multiple-keystores-in-jvm
 * http://codyaray.com/2013/04/java-ssl-with-multiple-keystores
 */
public class CompositeX509TrustManager implements X509TrustManager {

	private X509TrustManager[] trustManagers;
	
	public CompositeX509TrustManager(X509TrustManager[] managers){
		this.trustManagers = managers;
	}
	
	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {

		for (X509TrustManager manager : trustManagers){
			try{
				manager.checkClientTrusted(chain, authType);
				return;
			}catch(CertificateException ex){
				//failed; check the next one
			}
		}
		throw new CertificateException(Messages.CompositeX509TrustManager_SshError);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		for (X509TrustManager manager : trustManagers){
			try{
				manager.checkServerTrusted(chain, authType);
				return;
			}catch(CertificateException ex){
				//failed; check the next one
			}
		}
		throw new CertificateException(Messages.CompositeX509TrustManager_SshError);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		List<X509Certificate> certs = new ArrayList<X509Certificate>();
		for (X509TrustManager manager :  trustManagers){
			for (X509Certificate cert: manager.getAcceptedIssuers()){
				certs.add(cert);
			}
		}
		return certs.toArray(new X509Certificate[certs.size()]);
	}

}
