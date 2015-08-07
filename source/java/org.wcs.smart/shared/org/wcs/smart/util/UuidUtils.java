/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.util;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Shared uuid utilites for switching between strings and uuids and byte
 * arrays.
 * 
 * @author Emily
 *
 */
public class UuidUtils {

	/**
	 * 0 UUID
	 */
	public static String ZERO_UUID_STR = "00000000000000000000000000000000"; //$NON-NLS-1$
	
	public static UUID byteToUUID(byte[] bytes){
		if (bytes == null) return null;
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		return new UUID(bb.getLong(), bb.getLong());
	}

	public static String uuidToString(UUID uuid) {
		if (uuid == null) return null;
		return uuid.toString().replaceAll("-", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Converts a hex encoded uuid string without dashed to
	 * a uuid
	 * 
	 * @param uuid
	 * @return
	 */
	public static UUID stringToUuid(String uuid) {
		Pattern p = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})"); //$NON-NLS-1$
		String withdash = p.matcher(uuid).replaceAll("$1-$2-$3-$4-$5"); //$NON-NLS-1$
		return UUID.fromString(withdash);
		
	}
	
	public static String getDirectoryPath(UUID uuid) {
		return uuidToString(uuid);
	}
}
