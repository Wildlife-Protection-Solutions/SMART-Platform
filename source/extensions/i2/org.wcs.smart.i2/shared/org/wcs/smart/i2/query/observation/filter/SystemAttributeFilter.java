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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.wcs.smart.i2.query.Operator;

/**
 * System attribute filters.  Represented by the string "sa:<attribute>" 
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
		ENTITY_DATE_CREATED,
		ENTITY_DATE_MODIFIED,
		RECORD_DATE_CREATED,
		RECORD_DATE_MODIFIED,
		RECORD_DATE,
		RECORD_SOURCE,
		RECORD_STATUS;
		
		public String getKey() {
			return SA_KEY + INTERNAL_SEPERATOR + name().toLowerCase(Locale.ROOT);
		}
		
		public boolean isDate() {
			return this == ENTITY_DATE_CREATED ||
					this == ENTITY_DATE_MODIFIED ||
					this == RECORD_DATE_MODIFIED ||
					this == RECORD_DATE_CREATED ||
					this == RECORD_DATE;
		}
		
		public boolean isUtcDate() {
			return this == ENTITY_DATE_CREATED ||
					this == ENTITY_DATE_MODIFIED ||
					this == RECORD_DATE_MODIFIED ||
					this == RECORD_DATE_CREATED;
		}
	}
	
	
	public static SystemAttributeFilter create(String key, Operator operator, String value){
		String[] parts = key.split(INTERNAL_SEPERATOR);
		if (!parts[0].toLowerCase(Locale.ROOT).equals(SA_KEY)) throw new IllegalStateException("Not a valid system attribute filter."); //$NON-NLS-1$
		SystemAttribute sa = SystemAttribute.valueOf(parts[1].toUpperCase(Locale.ROOT));
		if (sa == null) throw new IllegalStateException(MessageFormat.format("Invalid attribute for system attribute:{0}", parts[1])); //$NON-NLS-1$
		
		SystemAttributeFilter filter = new SystemAttributeFilter(sa, operator, value);
		return filter;
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
	public static SystemAttributeFilter create(String key, Operator operator, LocalDate date1, LocalDate date2){
		
		String[] parts = key.split(INTERNAL_SEPERATOR);
		
		if (!parts[0].toLowerCase(Locale.ROOT).equals(SA_KEY)) throw new IllegalStateException("Not a valid system attribute filter."); //$NON-NLS-1$
		
		SystemAttribute sa = SystemAttribute.valueOf(parts[1].toUpperCase(Locale.ROOT));
		if (sa == null) throw new IllegalStateException(MessageFormat.format("Invalid attribute for system attribute:{0}", parts[1])); //$NON-NLS-1$
		
		SystemAttributeFilter filter = new SystemAttributeFilter(sa, operator, date1, date2);
		return filter;
	}
	
	public static SystemAttributeFilter create(String key, Operator operator, LocalDateTime date1, LocalDateTime date2){
		
		String[] parts = key.split(INTERNAL_SEPERATOR);
		
		if (!parts[0].toLowerCase(Locale.ROOT).equals(SA_KEY)) throw new IllegalStateException("Not a valid system attribute filter."); //$NON-NLS-1$
		
		SystemAttribute sa = SystemAttribute.valueOf(parts[1].toUpperCase(Locale.ROOT));
		if (sa == null) throw new IllegalStateException(MessageFormat.format("Invalid attribute for system attribute:{0}", parts[1])); //$NON-NLS-1$
		
		SystemAttributeFilter filter = new SystemAttributeFilter(sa, operator, date1, date2);
		return filter;
	}

	private SystemAttribute attribute;
	
	private LocalDate[] dateValues = null;
	private LocalDateTime[] dateTimeValues = null;
	private String key = null;
	private Operator op = null;
	
	public SystemAttributeFilter(SystemAttribute attribute, Operator op, LocalDate d1, LocalDate d2) {
		this.attribute = attribute;
		this.op = op;
		this.key = null;
		this.dateValues = new LocalDate[] {d1,d2};
	}
	
	public SystemAttributeFilter(SystemAttribute attribute, Operator op, LocalDateTime d1, LocalDateTime d2) {
		this.attribute = attribute;
		this.op = op;
		this.key = null;
		this.dateTimeValues = new LocalDateTime[] {d1,d2};
	}
	
	public SystemAttributeFilter(SystemAttribute attribute, Operator op, String key) {
		this(attribute,op, (LocalDate)null,(LocalDate)null);
		this.key = key;
	}
	
	public Operator getOperator() {
		return this.op;
	}
	
	public String getStringKey() {
		return this.key;
	}
	
	public LocalDateTime[] getDateTimeValues() {
		return this.dateTimeValues;
	}
	
	public LocalDate[] getDateValues() {
		return this.dateValues;
	}

	public SystemAttribute getAttribute() {
		return this.attribute;
	}

	@Override
	public String getUniqueColumnIdentifier() {
		StringBuilder sb = new StringBuilder();
		sb.append(SA_KEY);
		sb.append("_"); //$NON-NLS-1$
		sb.append(attribute.name());
		sb.append("_"); //$NON-NLS-1$
		sb.append(op.getKey());
		if (key == null) {
			sb.append("_"); //$NON-NLS-1$
			sb.append(dateValues[0].toString());
			sb.append("_"); //$NON-NLS-1$
			sb.append(dateValues[1].toString());
		}else {
			sb.append(key);
		}
		
		return sb.toString();
	}
	
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(SA_KEY);
		sb.append(":"); //$NON-NLS-1$
		sb.append(attribute.getKey());
		sb.append(" "); //$NON-NLS-1$
		sb.append(op.getKey());
		sb.append(" "); //$NON-NLS-1$
		if (key == null) {
			sb.append(DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR).format(dateValues[0]));
			sb.append(" "); //$NON-NLS-1$
			sb.append(Operator.AND.getKey());
			sb.append(DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR).format(dateValues[1]));
		}else {
			sb.append(Operator.EQUALS.getKey());
			sb.append(" "); //$NON-NLS-1$
			sb.append(key);
		}
		return sb.toString();
		
	}
}
