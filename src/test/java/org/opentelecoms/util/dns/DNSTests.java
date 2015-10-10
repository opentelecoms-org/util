package org.opentelecoms.util.dns;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;

import org.junit.Test;

public class DNSTests {

	@Test
	public void testSRVForSIP() {
		SRVRecordHelper srh = new SRVRecordHelper("sips", "tcp", "sip5060.net", 5060);
		assertTrue(srh.size() > 0);
		
		InetSocketAddress isa = new InetSocketAddress("sip-server.sip5060.net", 5061);
		assertTrue(srh.contains(isa));
	}
	
	// Test for a STUN/TURN server
	@Test
	public void testSRVForSTUN() {
		SRVRecordHelper srh = new SRVRecordHelper("stun", "udp", "sip5060.net", 0);
		assertTrue(srh.size() > 0);
		
		InetSocketAddress isa = new InetSocketAddress("stun-test.sip5060.net", 3478);
		assertTrue(srh.contains(isa));
	}
	
}
