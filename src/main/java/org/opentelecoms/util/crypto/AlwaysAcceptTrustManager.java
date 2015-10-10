/*
 *  An implementation of a javax.net.ssl TrustManager
 *  that will simply trust every certificate without inspecting
 *  or verifying the contents.
 *  
 *  WARNING: using this class is not actually a good idea for
 *  anything other than testing.  Please don't use it if you
 *  don't understand this.
 *  
 *  Origins: there are plenty of examples of this class on the web
 *  without any attribution.  This is a fresh implementation.
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
