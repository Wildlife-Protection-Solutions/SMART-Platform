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
package org.wcs.smart.connect.api.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.wcs.smart.connect.internal.Messages;

public class IOUtils {

	 /**
	* The default buffer size to use for copying.
	*/
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
	/*
	 * end of file marker
	 */
	private static final int EOF = -1;
	
	/**
	 * Copies data from an input stream to and output stream monitoring the canceled state
	 * of the progress monitor throwing an exception if interrupted.  Also updates
	 * progress and bytes are copied.
	 * 
	 * Modeled after the Apache IOUtils copy function.
	 *  
	 * @param input input stream
	 * @param output output stream
	 * @param monitor the copy progress monitor
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static long copy(InputStream input, OutputStream output, CopyProgressMonitor monitor) throws InterruptedException, IOException{
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE]; 
		long count = 0;
        int n = 0;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
            
            if (monitor.isCanceled()) throw new InterruptedException(Messages.IOUtils_DownloadCancelled);
            monitor.addToCopiedCount(n);
        }
        return count;
	}


}
