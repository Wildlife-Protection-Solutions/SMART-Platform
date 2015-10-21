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
package org.wcs.smart.security;

import java.io.Serializable;
import java.nio.ByteBuffer;

import javax.xml.bind.DatatypeConverter;

/**
 * Class represents encrypted data that can be serialised.
 *  
 * @author elitvin
 * @since 3.2.0
 */
public class EncryptedData implements Serializable {
	
	private static final long serialVersionUID = 1771476894573989245L;

	private byte[] salt;
	private byte[] iv;
	private byte[] data;
	
	public EncryptedData() {
	}

	public EncryptedData(byte[] salt, byte[] iv, byte[] data) {
		this.salt = salt;
		this.iv = iv;
		this.data = data;
	}
	
	public byte[] getSalt() {
		return salt;
	}
	public void setSalt(byte[] salt) {
		this.salt = salt;
	}
	
	public byte[] getIv() {
		return iv;
	}
	public void setIv(byte[] iv) {
		this.iv = iv;
	}
	
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}

	public boolean isEmpty() {
		return data == null;
	}
	
	/**
	 * Converts encrypted data to hex string
	 */
	public String toString(){
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES * 3 + salt.length + iv.length + data.length);
		bb.putInt(salt.length);
		bb.put(salt);
		bb.putInt(iv.length);
		bb.put(iv);
		bb.putInt(data.length);
		bb.put(data);
		return DatatypeConverter.printHexBinary(bb.array());
	}
	
	/**
	 * Converts hex string to encrypted data
	 * @param data
	 * @return
	 */
	public static final EncryptedData fromString(String data){
		//convert hex string to byte array
		byte[] dataa = DatatypeConverter.parseHexBinary(data);
		ByteBuffer l = ByteBuffer.allocate(dataa.length).put(dataa);
		l.rewind();

		byte[] salt = new byte[l.getInt()];
		l.get(salt);
		
		byte[] iv = new byte[l.getInt()];
		l.get(iv);
		
		byte[] thisdata = new byte[l.getInt()];
		l.get(thisdata);
		
		return new EncryptedData(salt, iv, thisdata);
	}
}
