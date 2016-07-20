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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.commons.io.IOUtils;
import org.wcs.smart.SmartPlugIn;

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

    //TODO: all the code below is for testing purposes and should be removed later!!!
    
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws Exception {
//		decompressFile(new File("d:\\SMART\\miniz\\x1.JSON"), new File("d:\\SMART\\miniz\\x1d.JSON"));
//		compressFile(new File("d:\\SMART\\d64.txt"), new File("d:\\SMART\\d64cmp.cmp"));

		System.out.println(decompressFile(new File("d:\\SMART\\miniz\\x1.JSON")));
	}

    /**
     * Decompresses a zlib compressed file.
     */
    public static void decompressFile(File compressed, File raw) throws IOException {
        FileInputStream fileIn = new FileInputStream(compressed);
        //fileIn.skip(13);
		InputStream in = new InflaterInputStream(fileIn);
        OutputStream out = new FileOutputStream(raw);
        shovelInToOut(in, out);
        in.close();
        out.close();
    }

    /**
     * Compresses a file with zlib compression.
     */
    public static void compressFile(File raw, File compressed) throws IOException {
        InputStream in = new FileInputStream(raw);
        OutputStream out = new DeflaterOutputStream(new FileOutputStream(compressed));
        shovelInToOut(in, out);
        in.close();
        out.close();
    }
    
    /**
     * Shovels all data from an input stream to an output stream.
     */
    private static void shovelInToOut(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1000];
        int len;
        while((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }

}
