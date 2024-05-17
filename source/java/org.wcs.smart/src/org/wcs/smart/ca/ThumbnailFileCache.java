/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.ca;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.SmartPlugIn;

/**
 * Single file cache for caching thumbnails.
 * 
 * At this time create a new cache deletes the
 * old cache.
 * 
 * @author Emily
 *
 */
public class ThumbnailFileCache {

	//name of the cache file
	private Path fileName;
	//map from key to location 
	private Map<String, Long> key2location;
		
	public ThumbnailFileCache(Path fileName) {
		this.fileName = fileName;
		init();
	}
	
	private void init() {
		key2location = new HashMap<>();
		
		//re-creates the cache each time
		//optionally could keep it and read data
		if (Files.exists(fileName)) {
			try {
				Files.delete(fileName);
			} catch (IOException e) {
				SmartPlugIn.log(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Removes all data in the cache by deleting the file
	 */
	public void clear() {
		try {
			if (Files.exists(fileName)) Files.delete(fileName);
		} catch (IOException e) {
			SmartPlugIn.log(e.getMessage(), e);
		}
		key2location.clear();
	}
	
	/**
	 * Currently removes from the cache by removing the key from
	 * location. Does not remove data from the file.
	 * 
	 * @param id
	 */
	public void remove(String id) {
		if (!key2location.containsKey(id)) return ;
		//TODO: delete bytes in file
		//long offset = key2location.get(id);
		key2location.remove(id);		
	}
	
	/**
	 * Adds data to cache
	 * @param id
	 * @param data
	 */
	public void putData(String id, byte[] data) {
		if (id == null) return;
		long offset = put(data);
		if (offset == -1) return;
		
		key2location.put(id,  offset);
	}
	
	/**
	 * Gets data from the cache or null if data not found
	 * @param id
	 * @return
	 */
	public byte[] getData(String id) {
		if (id == null) return null;
		if (!key2location.containsKey(id)) return null;
		long offset = key2location.get(id);
		if (offset < -1) return null;
		
		return load(offset);
		
	}
	
	private long put(byte[] data) {
		try(SeekableByteChannel file = Files.newByteChannel(fileName, StandardOpenOption.CREATE, StandardOpenOption.WRITE)){
			long offset = file.size();
			
			file.position(offset);
			
			ByteBuffer size = ByteBuffer.allocate(4);
			size.putInt(data.length);
			size.position(0);
			
			file.write(size);
			
			ByteBuffer bdata = ByteBuffer.wrap(data);
			file.write(bdata);
			
			return offset;
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			return -1;
		}
		
	}
	
	private byte[] load(long offset) {
		try(SeekableByteChannel file = Files.newByteChannel(fileName, StandardOpenOption.READ, StandardOpenOption.CREATE)){
			file.position(offset);
			
			//read length of thumbnail
			ByteBuffer length = ByteBuffer.allocate(4);
			
			if (file.read(length) != 4) return null;
			length.position(0);
			int size = length.getInt();

			ByteBuffer data = ByteBuffer.allocate(size);
			if (file.read(data) != size) return null;
			
			return data.array();
			
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			return null;
		}
	}
	
}
