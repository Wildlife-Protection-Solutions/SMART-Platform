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
package org.wcs.smart.patrol.internal.ui.views;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * Class to track date group by objects for grouping patrol by month or year.
 * @author Emily
 *
 */
public class DateGroupBy{
	
	public enum Type{MONTH, YEAR};
	
	private final static SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMM, yyyy"); //$NON-NLS-1$
	private final static SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy"); //$NON-NLS-1$
	
	private Calendar c;
	private Type type;
	
	public DateGroupBy(Date date, Type type){
		c = Calendar.getInstance();
		c.setTime(date);
		this.type = type;
	}
	
	public Type getType(){
		return this.type;
	}
	
	public int getYear(){
		return c.get(Calendar.YEAR);
	}
	
	public int getMonth(){
		return c.get(Calendar.MONTH);
	}
	
	public String getLabel(){
		if (type == Type.MONTH){
			return MONTH_FORMAT.format(c.getTime());
		}else if (type == Type.YEAR){
			return YEAR_FORMAT.format(c.getTime());
		}
		return c.getTime().toString();
	}
	
	public int hashCode(){
		switch(getType()){
		case MONTH:
			return Objects.hash(getMonth(), getYear());
		case YEAR:
			return Objects.hash(getYear());
		}
		return super.hashCode();
	}
	
	public boolean equals(Object other){
		if (this == other) return true;
		if (!getClass().equals(other.getClass())) return false;
		DateGroupBy g = (DateGroupBy) other;
		if (!getType().equals(g.getType())) return false;
		switch(getType()){
		case MONTH:
			return Objects.equals(g.getMonth(), getMonth()) && Objects.equals(g.getYear(), getYear());
		case YEAR:
			return Objects.equals(g.getYear(), getYear());
		}
		return false;
	}
}
	