package org.wcs.smart.upgrade.v112;

import java.util.Arrays;

public class ByteWrapper {

	private byte[] wrap;
	
	public ByteWrapper(byte[] wrap){
		this.wrap = wrap;
	}
	@Override
	public boolean equals(Object other){
		if (other instanceof ByteWrapper){
			return Arrays.equals(wrap, ((ByteWrapper)other).wrap);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return Arrays.hashCode(wrap);
	}
}
