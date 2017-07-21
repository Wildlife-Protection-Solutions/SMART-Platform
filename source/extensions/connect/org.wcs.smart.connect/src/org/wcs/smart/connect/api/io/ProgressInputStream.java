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

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Wrapper around an input stream that checks the canceled state
 * of a progress monitor and throws and exception if the monitor
 * has been canceled.  Also updates the monitor as
 * bytes are read.
 * 
 * @author Emily
 *
 */
public class ProgressInputStream extends InputStream{

	
	public InputStream wrapper;
	public SubMonitor monitor; 
	
	private long expectedSize;
	private long currentSize = 0;
	private int lastValue = 0;
	/**
	 * 
	 * @param in wrapper input stream
	 * @param monitor progress monitor to monitor
	 */
	public ProgressInputStream(InputStream in, long expectedSize, SubMonitor monitor){
		this.wrapper = in;
		this.monitor = monitor;
		this.expectedSize = expectedSize;
		monitor.setWorkRemaining( 100 );
	}
	
	private void updateMonitor() {
		monitor.checkCanceled();
		int thisPercent = (int)((currentSize / (double)expectedSize) * 100);
		if (thisPercent != lastValue) monitor.worked(thisPercent - lastValue);
		lastValue = thisPercent;       
	}
	@Override
    public int read() throws IOException, OperationCanceledException {
		int value = wrapper.read();
		currentSize ++;
		updateMonitor();
    	return value;
    }

	@Override
    public int read(byte b[]) throws IOException, OperationCanceledException {
		monitor.checkCanceled();
    	int value = wrapper.read(b);
    	if (value >= 0){
    		currentSize += value;
    		updateMonitor();
    	}
    	return value;
    }
	
	@Override
    public int read(byte b[], int off, int len) throws IOException, OperationCanceledException {
		monitor.checkCanceled();
    	int value = wrapper.read(b, off, len);
    	if ( value >= 0){
    		currentSize += value;
    		updateMonitor();
    	}
    	return value;
	}

    @Override
    public long skip(long n) throws IOException, OperationCanceledException {
    	monitor.checkCanceled();
    	long value = wrapper.skip(n);
    	if ( value >= 0){
    		currentSize += value;
    		updateMonitor();
    	}
    	return value;
    }


    @Override
    public int available() throws IOException {
        return wrapper.available();
    }

    @Override
    public void close() throws IOException {
    	wrapper.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
    	wrapper.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
    	wrapper.reset();
    }

    @Override
    public boolean markSupported() {
        return wrapper.markSupported();
    }
}
