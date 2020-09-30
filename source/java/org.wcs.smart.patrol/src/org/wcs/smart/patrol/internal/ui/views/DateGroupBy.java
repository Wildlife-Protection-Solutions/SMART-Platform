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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Class to track date group by objects for grouping patrol by month or year.
 * @author Emily
 *
 */
public class DateGroupBy{
	
	public enum Type{MONTH, YEAR};
	
	private final static DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM, yyyy"); //$NON-NLS-1$
	private final static DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy"); //$NON-NLS-1$
	
	private LocalDate c;
	private Type type;
	
	public DateGroupBy(LocalDate date, Type type){
		this.c = date;
		this.type = type;
	}
	
	public Type getType(){
		return this.type;
	}
	
	public int getYear(){
		return c.getYear();
	}
	
	public int getMonth(){
		return c.getMonthValue();
	}
	
	public String getLabel(){
		if (type == Type.MONTH){
			return c.format(MONTH_FORMAT);
		}else if (type == Type.YEAR){
			return c.format(YEAR_FORMAT);
		}
		return c.toString();
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
	