/*
 *  An implementation of a javax.net.ssl TrustManager that combines the 
 *  algorithms of a user-supplied TrustManager and a default TrustManager
 *  from the TrustManagerFactory
 *  
 *  Each certificate is checked against the default TrustManager, and if
 *  it is rejected, it is then checked against the user-supplied TrustManager
 *  
 *  A typical application of this class is to support all the CAs on a
 *  platform (such as Android's built-in CAs) but also trust some
 *  additional CA (for example, the CACert.org root)
 *  
 *  Copyright 2012 Daniel Pocock <daniel@pocock.com.au>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
