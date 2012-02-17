package org.opentelecoms.util.crypto;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class AppendingTrustManager implements X509TrustManager {
	
	Logger logger = Logger.getLogger(getClass().getName());
	
	X509TrustManager _tm;
	X509TrustManager _local;
	
	public AppendingTrustManager(X509TrustManager tm, KeyStore ks) throws NoSuchAlgorithmException, KeyStoreException {
		this._tm = tm;
		
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		TrustManager[] tm2 = tmf.getTrustManagers();
	    _local = (X509TrustManager)tm2[0];
	}

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		try {
			_tm.checkClientTrusted(arg0, arg1);
		} catch (CertificateException ce) {
			_local.checkClientTrusted(arg0, arg1);
			logger.info("Trusting a client certificate based on local trust store");
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		try {
			_tm.checkServerTrusted(arg0, arg1);
		} catch (CertificateException ce) {
			try {
				_local.checkServerTrusted(arg0, arg1);
				logger.info("Trusting a server certificate based on local trust store");
			} catch (CertificateException ce2) {
				logger.warning("Not trusted locally either: " + ce2.getMessage());
				throw ce2;
			}
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		X509Certificate[] _a1 = _tm.getAcceptedIssuers();
		X509Certificate[] _a2 = _local.getAcceptedIssuers();
		int count = _a1.length + _a2.length;
		X509Certificate[] _all = new X509Certificate[count];
		for(int i = 0; i < _a1.length; i++)
			_all[i] = _a1[i];
		for(int i = 0; i < _a2.length; i++)
			_all[_a1.length + i] = _a2[i];
		return _all;
	}
	
}
