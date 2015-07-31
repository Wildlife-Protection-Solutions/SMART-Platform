package org.wcs.smart.util;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.regex.Pattern;

public class UuidUtils {

	public static String ZERO_UUID_STR = "00000000000000000000000000000000";
	
	public static UUID byteToUUID(byte[] bytes){
		if (bytes == null) return null;
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		return new UUID(bb.getLong(), bb.getLong());
	}

	public static String uuidToString(UUID uuid) {
		if (uuid == null) return null;
		return uuid.toString().replaceAll("-", "");
	}

	/**
	 * Converts a hex encoded uuid string without dashed to
	 * a uuid
	 * 
	 * @param uuid
	 * @return
	 */
	public static UUID stringToUuid(String uuid) {
		Pattern p = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
		String withdash = p.matcher(uuid).replaceAll("$1-$2-$3-$4-$5");
		return UUID.fromString(withdash);
		
	}
	
	public static String getDirectoryPath(UUID uuid) {
		return uuidToString(uuid);
	}
}
