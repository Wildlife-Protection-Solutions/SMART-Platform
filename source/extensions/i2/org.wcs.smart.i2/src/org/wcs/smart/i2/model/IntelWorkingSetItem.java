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
package org.wcs.smart.i2.model;

import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Working set item for displaying in the working set view
 * @author Emily
 *
 */
public class IntelWorkingSetItem {

	private IntelWorkingSetCategory category;
	private String label;
	private UUID uuid;
	private boolean isVisible;
	private ImageDescriptor descriptor;
	
	public IntelWorkingSetItem(IntelWorkingSetCategory category, String label, boolean isVisible, UUID uuid){
		this.category = category;
		this.label = label;
		this.uuid = uuid;
	}
	
	public IntelWorkingSetItem(IntelWorkingSetCategory category, String label, boolean isVisible, UUID uuid, ImageDescriptor descriptor){
		this.category = category;
		this.label = label;
		this.uuid = uuid;
		this.descriptor = descriptor;
		this.isVisible = isVisible;
	}
	
	public boolean isVisible(){
		return this.isVisible;
	}
	
	public IntelWorkingSetCategory getCategory(){
		return this.category;
	}
	
	public String getLabel(){
		return this.label;
	}
	
	public UUID getUuid(){
		return this.uuid;
	}
	
	public ImageDescriptor getImageDescriptor(){
		return descriptor;
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof IntelWorkingSetItem){
			return ((IntelWorkingSetItem) other).getUuid().equals(getUuid());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return this.uuid.hashCode();
	}
	
}
