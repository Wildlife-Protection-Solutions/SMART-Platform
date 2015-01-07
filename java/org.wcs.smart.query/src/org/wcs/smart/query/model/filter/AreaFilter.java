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
package org.wcs.smart.query.model.filter;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.AreaDropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;


/**
 * Area filter for waypoint queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AreaFilter implements IFilter {

	/**
	 * Type of geometry the area filter
	 * should be applied to. 
	 */
	public static enum AreaFilterGeometryType{
		WAYPOINT("wp"), TRACK("t");  //$NON-NLS-1$  //$NON-NLS-2$
		
		String key;
		AreaFilterGeometryType(String key){
			this.key = key;
		}
		public String getKey(){
			return this.key;
		}
	}
	
	/**
	 * Creates a new area filter.
	 * 
	 * @param key the filter key in the form area:<geomtype{wp | t}>:<type>:key
	 * where type is from Area.AreaType
	 * @return
	 */
	public static AreaFilter createFilter(String key){
		String[] bits = key.split(":");  //$NON-NLS-1$
		AreaFilterGeometryType geomType = null;
		if (bits[1].equalsIgnoreCase(AreaFilterGeometryType.WAYPOINT.key)){
			geomType = AreaFilterGeometryType.WAYPOINT;
		}else if (bits[1].equalsIgnoreCase(AreaFilterGeometryType.TRACK.key)){
			geomType = AreaFilterGeometryType.TRACK;
		}
		Area.AreaType type = Area.AreaType.valueOf(bits[2]);
		return new AreaFilter(type, bits[3], geomType);
	}
	
	
	private Area.AreaType type;
	private String key;
	private AreaFilterGeometryType geomType;
	
	/**
	 * Creates a new area filter
	 * @param type the area type
	 * @param key the key id
	 */
	public AreaFilter(Area.AreaType type, String key, AreaFilterGeometryType geomType){
		this.type = type;
		this.key = key;
		this.geomType = geomType;
	}
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		return "area:" +geomType.key + ":" + type + ":" + key;  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
	}

	/**
	 * @return the area filter key
	 */
	public String getKey(){
		return this.key;
	}
	
	/**
	 * @return the area filter type
	 */
	public Area.AreaType getType(){
		return this.type;
	}
	
	/**
	 * 
	 * @return the geometry type associated with the filter
	 */
	public AreaFilterGeometryType getGeometryType(){
		return this.geomType;
	}

	/**
	 * changes the geometry type associated with the filter
	 */
	public void changeGeometryType(AreaFilterGeometryType geomType){
		this.geomType = geomType;
	}
	
	/*
	 * Loads the area for the given type/key.
	 */
	private Area loadArea(Session session){
		@SuppressWarnings("unchecked")
		List<Area> areas = session.createCriteria(Area.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("type", this.type)).add(Restrictions.eq("keyId", this.key)).list(); //$NON-NLS-2$ //$NON-NLS-1$ //$NON-NLS-3$
		if (areas.size() == 0){
			throw new IllegalStateException(
					MessageFormat.format(
							Messages.AreaFilter_InvalidAreaKey, new Object[]{this.type.getGuiName(), this.key}));
		}else{
			return areas.get(0);
		}
		
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getDropItems(org.hibernate.Session)
	 * @return {@link AreaDropItem}
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception {		
		try{
			return new DropItem[]{ new AreaDropItem(loadArea(session), geomType)};
		}catch(Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
	}
	
	public void accept(IFilterVisitor visitor){
		visitor.visit(this);
	}

}
