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
package org.wcs.smart.i2.ui.views.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Tree filter item for a type of conservation area area.  All the children
 * are the individual polygons.
 * 
 * @author Emily
 *
 */
public class AreaTypeTreeFilterItem extends DeferredTreeFilterItem {

	private Object LOCK = new Object();
	
	private Area.AreaType type;
	
	public AreaTypeTreeFilterItem(Area.AreaType type) {
		super(SmartLabelProvider.getAreaTypeName(type));
		this.type = type;
	}

	
	@Override
	public List<FilterTreeItem> getChildren() {
		if (kids == null ){
			synchronized (LOCK) {
				if (kids == null){
					Session s = HibernateManager.openSession();
					try{
						ArrayList<FilterTreeItem> temp = new ArrayList<>();
						List<Area> items = HibernateManager.loadAreas(type, s);
						if (items != null){
							for (Area a : items){
								temp.add(new AreaTreeFilterItem(a));
							}
						}
						kids = temp;
					}finally{
						s.close();
					}
				}
			}
			
		}
		if (kids == null) return null;
		return Collections.unmodifiableList(kids);
	}
}
