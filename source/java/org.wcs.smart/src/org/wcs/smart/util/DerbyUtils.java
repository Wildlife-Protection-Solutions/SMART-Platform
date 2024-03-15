package org.wcs.smart.util;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.ZoneId;
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
	
	/**
	 * Converts a timestamp that is in the given zoneID to a timestamp
	 * at the UTC equivalent time.
	 * 
	 * 2023-08-01 12:00:00 -> 2023-01-01 19:00:00
	 * 2023-12-01 12:00:00 -> 2023-12-01 20:00:00
	 * 
	 * Used as a part of the 7 to 8 upgrade script where the created/modified
	 * datetimes are converted to utc time instead of localtime as these
	 * can be updated on Connect where the timezone is not in localtime.
	 * 
	 * @param local Timestamp in UTC time
	 * @return
	 */
	public static Timestamp localToUtc(Timestamp local, String zoneId) {
		if (local == null) return null;
		
		ZoneId zone = ZoneId.of(zoneId);
		int offset = local.toLocalDateTime().atZone(zone).getOffset().getTotalSeconds();
		long newts = local.getTime() - (offset*1000);
		return new Timestamp(newts);
	}
	
}
