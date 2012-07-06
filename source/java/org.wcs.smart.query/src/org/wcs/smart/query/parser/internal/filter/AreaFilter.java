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
package org.wcs.smart.query.parser.internal.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.query.ui.formulaDnd.AreaDropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.util.SmartUtils;


/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author egouge
 * @since 1.0.0
 */
public class AreaFilter implements IFilter {

	public static AreaFilter createFilter(String key){
		String[] bits = key.split(":");
		Area.AreaType type = Area.AreaType.valueOf(bits[1]);
		return new AreaFilter(type, bits[2]);
	}
	
	
	private Area.AreaType type;
	private String key;
	
	
	public AreaFilter(Area.AreaType type, String key){
		this.type = type;
		this.key = key;
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		return "area:" + type + ":" + key;
	}

	public String getKey(){
		return this.key;
	}
	public Area.AreaType getType(){
		return this.type;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("smart.pointinpolygon(" );
		sb.append(tableMapping.get(Waypoint.class) + ".x, ");
		sb.append(tableMapping.get(Waypoint.class) + ".y, ");
		sb.append(  type.name() + "_" + key + ".geom");
//		sb.append(tableMapping.get(Area.class) + ".geom");
		sb.append(")");
//		sb.append(" AND ");
//		sb.append(tableMapping.get(Area.class) + ".keyid = '");
//		sb.append(key);
//		sb.append("' AND ");
//		sb.append(tableMapping.get(Area.class) + ".area_type = '");
//		sb.append(type.name());
//		sb.append("' AND ");
//		sb.append(tableMapping.get(Area.class) + ".ca_uuid = x'");
//		sb.append(SmartUtils.encodeHex(SmartDB.getCurrentConservationArea().getUuid()));
//		sb.append("')");
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
	}

	private Area loadArea(Session session){
		@SuppressWarnings("unchecked")
		List<Area> areas = session.createCriteria(Area.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("type", this.type)).add(Restrictions.eq("keyId", this.key)).list();
		if (areas.size() == 0){
			throw new IllegalStateException("Could not find area filter for type " + this.type.getGuiName() + " and key " +this.key);
		}else{
			return areas.get(0);
		}
		
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getDropItems(org.hibernate.Session)
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception {		
		return new DropItem[]{ new AreaDropItem(loadArea(session))};
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}

}
