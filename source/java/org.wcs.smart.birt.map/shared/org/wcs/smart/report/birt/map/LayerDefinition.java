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
package org.wcs.smart.report.birt.map;

import java.util.Objects;

import org.eclipse.birt.report.model.api.OdaDataSetHandle;

/**
 * Map layer definition
 * @author Emily
 *
 */
public class LayerDefinition {
	
	private OdaDataSetHandle handle;
	private MapLayerInfo info;
	
	public LayerDefinition(){
		this(null, null);
	}
	
	public LayerDefinition(OdaDataSetHandle handle, MapLayerInfo info){
		this.handle = handle;
		this.info = info;
	}
	
	public OdaDataSetHandle getHandle(){
		return this.handle;
	}
	
	public MapLayerInfo getInfo(){
		return this.info;
	}
	
	
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		
		LayerDefinition o = (LayerDefinition) other;
		return Objects.equals(handle, o.handle) && Objects.equals(info,o.info);
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(handle, info);
	}
}
