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
package org.wcs.smart.i2.ui.views;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.InternalQueryManager;
import org.wcs.smart.i2.model.AbstractIntelQuery;

/**
 * Query proxy for queries view.
 * 
 * @author Emily
 *
 */
public class QueryProxy implements IAdaptable{
	
	private String name;
	private UUID uuid;
	private String type;
	private String profileFilter;
	
	public QueryProxy(String name, UUID uuid, String typeKey, String profileFilter){
		this.name = name;
		this.uuid = uuid;
		this.type = typeKey;
		this.profileFilter = profileFilter;
	}
	
	public void update(String name, String typeKey, String profileFilter){
		this.name = name;
		this.type = typeKey;
		this.profileFilter = profileFilter;
	}
	
	public String getProfileFilter() {
		return this.profileFilter;
	}
	
	public String getTypeKey() {
		return this.type;
	}
	
	public String getName(){
		return this.name;
	}
	
	public UUID getUuid(){
		return this.uuid;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		for (Class<? extends AbstractIntelQuery> c : InternalQueryManager.INSTANCE.getQueryTypeClasses()) {
			if (c == adapter) {
				try {
					AbstractIntelQuery q = c.getDeclaredConstructor().newInstance();
					q.setUuid(getUuid());
					return (T)q;
				}catch (Exception ex) {
					Intelligence2PlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public boolean equals(Object other){
		if (other == null) return false;
		if (other == this) return true;
		if (getClass() != other.getClass()) return false;
		return Objects.equals(uuid, ((QueryProxy)other).uuid);
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(uuid);
	}

}
