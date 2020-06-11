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
package org.wcs.smart.cybertracker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

import org.apache.commons.io.IOUtils;

/**
 * Util to decompress compressed json that is sent by CyberTracker.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ZLibUtil {

    /**
     * Decompresses a zlib compressed file to a json string.
     */
    public static String decompressFile(File compressed) throws Exception {
    	try (InputStream in = new InflaterInputStream(new FileInputStream(compressed))) {
			return IOUtils.toString(in, "UTF-8"); //$NON-NLS-1$
		} 
    }

}
