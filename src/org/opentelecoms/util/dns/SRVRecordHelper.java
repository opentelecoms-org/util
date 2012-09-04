/*
 *  Encapsulates all the logic required to obtain and parse DNS SRV
 *  records and provide the corresponding InetAddresses as an
 *  ordered Collection (the class itself extends Vector)
 *  
 *  If SRV records don't exist for a particular domain/service,
 *  then any A records for the domain will be obtained instead
 *  (maybe this should be extended to CNAME records)
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

package org.opentelecoms.util.dns;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

public class SRVRecordHelper extends Vector<InetSocketAddress> {

	public static final int DEFAULT_SIP_PORT = 5060;
	public static final int DEFAULT_SIPS_PORT = 5061;
	public static final String SIPS_SCHEME = "sips";
	
	Logger logger = Logger.getLogger(getClass().getName());
	
	private static final long serialVersionUID = -2656070797887094655L;
	final private String TAG = "SRVRecordHelper";
	private String useProtocol;

	public String getProtocol() { return useProtocol; }

	private class RecordHelperThread extends Thread {
		
		CyclicBarrier barrier;
		String domain;
		int type;
		Vector<Record> records;
		public RecordHelperThread(CyclicBarrier barrier, String domain, int type) {
			this.barrier = barrier;
			this.domain = domain;
			this.type = type;
		}
				
		public Collection<Record> getRecords() {
			return records;
		}

		public void run() {
			try {
				
				try {
					Resolver resolver = new ExtendedResolver();
					resolver.setTimeout(2);
					Lookup lookup = new Lookup(domain, type);
					lookup.setResolver(resolver);
					records = new Vector<Record>();
					Record[] _records = null;
				
					try {
						_records = lookup.run();
					} catch (Exception ex) {
						// ignore the exception, as we try the lookup
						// in different ways
						logger.log(Level.SEVERE, "exception", ex);
					}
				
					if (_records != null)
						for(Record record : _records)
							records.add(record);
				} catch (Exception ex) {
					records = null;
				}
				barrier.await();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			} catch (BrokenBarrierException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public SRVRecordHelper(String service, String protocol, String domain, int port) {
		boolean withProtocol = protocol.length() > 0;
		boolean withPort = port > 0;
		boolean withNummeric = isNumericAddress(domain);

		TreeSet<SRVRecord> srvRecords = new TreeSet<SRVRecord>(new SRVRecordComparator());
		Vector<ARecord> aRecords = new Vector<ARecord>();

		if ( withProtocol ) {
			useProtocol = protocol;
		} else {
			if ( withNummeric || withPort) {
				if ( service.equals(SIPS_SCHEME) ) {
					useProtocol = "tcp";
				} else {
					useProtocol = "udp";
				}
			} else {
				// TODO try NAPTR
				// When no NAPTR found try SRV
				// Only SRV for now
				if ( service.equals(SIPS_SCHEME) )
					useProtocol = "tcp";
				else
					useProtocol = "udp";
			}
		}

		if ( withNummeric ) {
			int usePort = DEFAULT_SIP_PORT;

			if ( withPort )
				usePort = port;
			else if ( service.equals( SIPS_SCHEME ))
				usePort = DEFAULT_SIPS_PORT;

			add(new InetSocketAddress(domain,
						  usePort));
			// No DNS lookup needed
			return;
		}

		try {
			int numQueries;
			if ( withPort )
				numQueries = 1; // A
			else
				numQueries = 2; // SRV + A

			CyclicBarrier b = new CyclicBarrier(numQueries + 1);
			RecordHelperThread srv_t = null;
			RecordHelperThread a_t = new RecordHelperThread(b, domain, Type.A);

			if ( !withPort ) {
				String mDomain = "_" + service + "._" + useProtocol + "." + domain;
				srv_t = new RecordHelperThread(b, mDomain, Type.SRV);
				srv_t.start();
			}

			a_t.start();
			
			// Wait for all lookups to finish
			try {
				b.await();
			} catch (InterruptedException e) {
				logger.log(Level.SEVERE, "InterruptedException", e);
			} catch (BrokenBarrierException e) {
				logger.log(Level.SEVERE, "BrokenBarrierException", e);
			}

			if ( srv_t != null ) {
				for (Record record : srv_t.getRecords()) {
					if(record instanceof SRVRecord) {
						srvRecords.add((SRVRecord)record);
					}
					if(record instanceof ARecord) {
						// In case of additional records
						aRecords.add((ARecord)record);
					}
				}
			}
			for (Record record : a_t.getRecords()) {
				if(record instanceof ARecord) {
					aRecords.add((ARecord)record);
				}
			}
		} catch (Exception ex) {
			logger.warning("Exception during DNS lookup: " + ex.getClass().getName() + ", " + ex.getMessage());
		}

		boolean srvFound = false;
		if ( srvRecords.size() > 0 ) {
			// Process SRV records
			for(SRVRecord srvRecord : srvRecords) {
				Name target = srvRecord.getTarget();
				int usePort = srvRecord.getPort();
				boolean addrFound = false;

				// Check A records first
				for(ARecord aRecord : aRecords) {
					if ( aRecord.getName() == target ) {
						add(new InetSocketAddress(aRecord.getAddress(), usePort));
						addrFound = true;
						break;
					}
				}

				if ( addrFound )
					continue;
				// Lookup addresses
				try {
					InetAddress addr = InetAddress.getByName(target.toString());
					add(new InetSocketAddress(addr, usePort));
				} catch (UnknownHostException e) {
				}
			}
		}

		if( size() == 0 ) {
			// SRV not used or not found
			// Fallback to using A records
			int usePort = DEFAULT_SIP_PORT;

			if ( withPort )
				usePort = port;
			else if ( service.equals( SIPS_SCHEME ))
				usePort = DEFAULT_SIPS_PORT;
			for(ARecord record : aRecords) {
				add(new InetSocketAddress(record.getAddress(), usePort));
			}
		}
	}

	protected boolean isNumericAddress(String addr) {
		if ( addr == null )
			return false;
		if ( addr.contains(":") )
			return isNumericIp6Address(addr);
		else
			return isNumericIp4Address(addr);
	}

	protected boolean isNumericIp4Address(String addr) {
		if ( addr == null ||
		     addr.length() < 7 || addr.length() > 15)
			return false;

		StringTokenizer token = new StringTokenizer(addr,".");
		if ( token.countTokens() != 4)
			return false;

		while ( token.hasMoreTokens()) {
			String numStr = token.nextToken();
			try {
				int num = Integer.parseInt(numStr);
				if ( num < 0 || num > 255)
					return false;
			} catch (NumberFormatException ex) {
				return false;
			}
		}
		return true;
	}

	private final int maxIp6Len = 8 * (4 + 1) - 1;

	protected boolean isNumericIp6Address(String addr) {
		if ( addr == null ||
		     addr.length() < 2 || addr.length() > maxIp6Len )
			return false;

		StringTokenizer token = new StringTokenizer(addr,":");
		if ( token.countTokens() < 3 ||
		     token.countTokens() > 8 )
			return false;

		while ( token.hasMoreTokens()) {
			String numStr = token.nextToken();
			if ( numStr.length() == 0 )
				// TODO check number of empty fields
				continue;

			try {
				int num = Integer.parseInt(numStr, 16);
					if ( num < 0 || num > 65535 )
						return false;
			} catch (NumberFormatException ex) {
				return false;
			}
		}
		return true;
	}
}
