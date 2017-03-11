/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Char sequence implementation of a file.  Used for searching files
 * @author Emily
 *
 */
public class FileCharSequence implements CharSequence, AutoCloseable{
	
	private RandomAccessFile randomAccess; 
	private long start; 
	private long end; 
	 
	public FileCharSequence(File file) throws IOException {
		randomAccess = new RandomAccessFile(file, "r"); //$NON-NLS-1$
		start = 0;
		end = randomAccess.length();
	}
	
	@Override
	public char charAt(int index) {
		try {
			randomAccess.seek(start + index);
			return (char) randomAccess.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int length() {
		return (int) (end - start); 
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		throw new UnsupportedOperationException();
		
	}
	
	public String getSubString(int start, int end) throws IOException{
		byte[] bytes = new byte[end-start];
		randomAccess.seek(start);
		randomAccess.readFully(bytes);
		return new String(bytes);
	}
	
	@Override
	public String toString() {
		try {
			byte[] bytes = new byte[length()];
			randomAccess.seek(start);
			randomAccess.readFully(bytes);
			return new String(bytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws Exception {
		randomAccess.close();
	}

}
