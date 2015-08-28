package org.wcs.smart.util;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.derby.impl.services.uuid.BasicUUIDFactory;

public class DerbyUtils {

	private static BasicUUIDFactory factory = new BasicUUIDFactory();
	
	public static byte[] createUuid(){
		String struuid = factory.createUUID().toString();
		UUID uuid = UUID.fromString(struuid);
		
		return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits())
				.putLong(uuid.getLeastSignificantBits()).array();
		
	}
}
