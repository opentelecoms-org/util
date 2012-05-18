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

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

public class SRVRecordHelper extends Vector<InetSocketAddress> {
	
	Logger logger = Logger.getLogger(getClass().getName());
	
	private static final long serialVersionUID = -2656070797887094655L;
	final private String TAG = "SRVRecordHelper";
	
	public SRVRecordHelper(String service, String protocol, String domain, int defaultPort) {
		String mDomain = "_" + service + "._" + protocol + "." + domain;
		
		TreeSet<SRVRecord> srvRecords = new TreeSet<SRVRecord>(new SRVRecordComparator());
		Vector<ARecord> aRecords = new Vector<ARecord>();
		
		try {
			Resolver resolver = new ExtendedResolver();
			resolver.setTimeout(2);
			Lookup lookup = new Lookup(mDomain, Type.SRV);
			lookup.setResolver(resolver);
			Vector<Record> _records = new Vector<Record>();
			Record[] records = null;
			
			try {
				records = lookup.run();
			} catch (Exception ex) {
				// ignore the exception, as we try the lookup
				// again to search for A records
			}
			
			if (records != null)
				for(Record record : records)
					_records.add(record);
			
			if(_records.size() == 0) {
				lookup = new Lookup(domain, Type.A);
				lookup.setResolver(resolver);
				records = lookup.run();
				if (records != null)
					for(Record record : records)
						_records.add(record);
			}

			for (Record record : _records) {
				if(record instanceof SRVRecord) {
					srvRecords.add((SRVRecord)record);
				}
				if(record instanceof ARecord) {
					aRecords.add((ARecord)record);
				}
			}
		} catch (Exception ex) {
			logger.warning("Exception during DNS lookup: " + ex.getClass().getName() + ", " + ex.getMessage());
		}

		for(SRVRecord srvRecord : srvRecords) {
			add(new InetSocketAddress(srvRecord.getTarget().toString(), srvRecord.getPort()));
		}
		
		if(defaultPort > 0 && size() == 0) {
			for(ARecord record : aRecords) {
				add(new InetSocketAddress(record.getName().toString(), defaultPort));
			}
		}
		
	}

}
