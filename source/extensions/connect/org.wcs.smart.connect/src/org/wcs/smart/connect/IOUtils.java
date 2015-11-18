package org.wcs.smart.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;

public class IOUtils {

	 /**
	* The default buffer size to use for copying.
	*/
//	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
	private static final int DEFAULT_BUFFER_SIZE = 48;
	/*
	 * end of file marker
	 */
	private static final int EOF = -1;
	
	
	public static long copy(InputStream input, OutputStream output, long size, CopyProgressMonitor monitor) throws InterruptedException, IOException{
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE]; 
		long count = 0;
        int n = 0;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
            
            if (monitor.isCanceled()) throw new InterruptedException("Download cancelled by user.");
            monitor.addToCopiedCount(n);
        }
        return count;
	}

	/**
	 * Progress monitor used for controlling the progress of copying a file.
	 * 
	 * @author Emily
	 *
	 */
	public static class CopyProgressMonitor {
		
		private IProgressMonitor wrapper;
		private long totalSize;
		private long totalCount;
		
		public CopyProgressMonitor(IProgressMonitor wrapper, long totalSize){
			this.wrapper = wrapper;
			this.totalSize = totalSize;
			this.totalCount = 0;
		}
		
		public boolean isCanceled(){
			return wrapper.isCanceled();
		}
		
		public void addToCopiedCount(long byteCnt){
			int lastPercent = (int)((totalCount / (double)totalSize) * 100);
			this.totalCount += byteCnt;
			int thisPercent = (int)((totalCount / (double)totalSize) * 100);
			if (thisPercent != lastPercent){
				wrapper.worked(thisPercent - lastPercent);
			}
			
			
		}
	}
}
