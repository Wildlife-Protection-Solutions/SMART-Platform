package org.wcs.smart.i2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileCharSequence implements CharSequence {
	
	private RandomAccessFile randomAccess; 
	private long start; 
	private long end; 
	 
	public FileCharSequence(File file) throws IOException {
		randomAccess = new RandomAccessFile(file, "r");
		start = 0;
		end = randomAccess.length();
	} 
	 
	private FileCharSequence(FileCharSequence prototype, long start, long end) {
		this.randomAccess = prototype.randomAccess;
		this.start = start;
		this.end = end;
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
		return new FileCharSequence(this, this.start + start, this.start + end);
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

}
