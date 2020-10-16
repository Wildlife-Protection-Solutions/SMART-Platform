/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.connect.query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 
 * Proxy class to represent query folders.
 * 
 * @author Chris
 *
 */
public class FolderProxy<T> {
	
	private final String name;
	private final UUID caUuid;
	private List<FolderProxy<T>> subFolders = new ArrayList<>();
	private List<T> queries = new ArrayList<>();

	public FolderProxy(final String name, UUID caUuid) {
		this.name = name;
		this.caUuid = caUuid;
	}
	
	public UUID getCaUuid() {
		return this.caUuid;
	}
	
	public void addSubFolder(FolderProxy<T> folder) {
		subFolders.add(folder);
	}
	
	public void addItem(T query) {
		queries.add(query);
	}

	public String getName() {
		return name;
	}

	public List<FolderProxy<T>> getSubFolders() {
		return subFolders;
	}

	public List<T> getItems() {
		return queries;
	}
	
}
