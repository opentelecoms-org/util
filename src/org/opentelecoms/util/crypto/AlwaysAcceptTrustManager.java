package org.opentelecoms.util.crypto;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.X509TrustManager;

public class AlwaysAcceptTrustManager implements X509TrustManager {
	
	Logger logger = Logger.getLogger(getClass().getName());

	public AlwaysAcceptTrustManager() {
		
	}
	
	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		logger.warning("Trusting a client certificate without verification");
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		logger.warning("Trusting a server certificate without verification");
		
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		// FIXME - should we return something here?
		logger.warning("Not returning any accepted issuer list");
		return null;
	}
	
	
}
