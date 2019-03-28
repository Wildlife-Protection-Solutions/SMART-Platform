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
package org.wcs.smart.i2.query.observation.filter;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;

import org.wcs.smart.i2.query.Operator;

/**
 * System attribute filters.  Represented by the string "sa:<type>:<attribute>" 
 *  
 * 
 * @author Emily
 *
 */
public class SystemAttributeFilter implements IQueryFilter, IColumnIdentifierProvider  {

	/**
	 * Key for filter 
	 */
	public static final String SA_KEY = "sa"; //$NON-NLS-1$
	
	/**
	 * Seperator for filter items
	 */
	public static final String INTERNAL_SEPERATOR = ":"; //$NON-NLS-1$
	
	/**
	 * Supported system attributes
	 * @author Emily
	 *
	 */
	public enum SystemAttribute{
		DATE_CREATED,
		DATE_MODIFIED
	}
	
	/**
	 * Supported types for system attributes 
	 * 
	 * @author Emily
	 *
	 */
	public enum Type{
		RECORD,
		ENTITY
	}
	
	/**
	 * Key: sa:type:attribute 
	 * 
	 * @param key
	 * @param operator
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static SystemAttributeFilter create(String key, Operator operator, Date date1, Date date2){
		
		String[] parts = key.split(INTERNAL_SEPERATOR);
		
		if (!parts[0].toLowerCase(Locale.ROOT).equals(SA_KEY)) throw new IllegalStateException("Not a valid system attribute filter."); //$NON-NLS-1$
		
		Type type = Type.valueOf(parts[1].toUpperCase(Locale.ROOT));
		if (type == null) throw new IllegalStateException(MessageFormat.format("Invalid type for system attribute:{0}", parts[1])); //$NON-NLS-1$
		
		SystemAttribute sa = SystemAttribute.valueOf(parts[2].toUpperCase(Locale.ROOT));
		if (sa == null) throw new IllegalStateException(MessageFormat.format("Invalid attribute for system attribute:{0}", parts[2])); //$NON-NLS-1$
		
		SystemAttributeFilter filter = new SystemAttributeFilter(type, sa, operator, date1, date2);
		return filter;
	}

	private Type type;
	private SystemAttribute attribute;
	
	private Date[] dateValues = null;
	private Operator op = null;
	
	private SystemAttributeFilter(Type type, SystemAttribute attribute, Operator op, Date d1, Date d2) {
		this.type = type;
		this.attribute = attribute;
		this.op = op;
		this.dateValues = new Date[] {d1,d2};
	}
	
	public Operator getOperator() {
		return this.op;
	}
	
	public Date[] getDateValues() {
		return this.dateValues;
	}
	
	public Type getType() {
		return this.type;
	}
	
	public SystemAttribute getAttribute() {
		return this.attribute;
	}

	@Override
	public String getUniqueColumnIdentifier() {
		StringBuilder sb = new StringBuilder();
		sb.append(SA_KEY);
		sb.append("_");  //$NON-NLS-1$
		sb.append(type.name());
		sb.append("_"); //$NON-NLS-1$
		sb.append(attribute.name());
		sb.append("_"); //$NON-NLS-1$
		sb.append(op.getKey());
		sb.append("_"); //$NON-NLS-1$
		sb.append(dateValues[0].getTime());
		sb.append("_"); //$NON-NLS-1$
		sb.append(dateValues[1].getTime());
		
		return sb.toString();
	}
}
