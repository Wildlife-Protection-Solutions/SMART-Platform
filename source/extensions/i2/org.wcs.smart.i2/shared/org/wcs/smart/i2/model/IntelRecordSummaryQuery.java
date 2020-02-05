/*
 * Copyright (C) 2019 Wildlife Conservation Society
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

import java.io.Reader;
import java.io.StringReader;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;
import org.wcs.smart.i2.query.observation.parser.ParseException;
import org.wcs.smart.i2.query.observation.parser.Parser;

/**
 * Record summary query
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
@Entity
@Table(name="smart.i_record_summary_query")
public class IntelRecordSummaryQuery extends AbstractIntelQuery {
	
	private static final long serialVersionUID = 1L;
	
	public static final String KEY = "i2_record_summ_query"; //$NON-NLS-1$

	@Override
	@Transient
	public String getTypeKey() {
		return KEY;
	}
	
	@Transient
	public static SumQueryDefinition parseQuery(String queryString) throws Exception{
		if (queryString.isEmpty()) throw new ParseException("At least one value must be supplied."); //$NON-NLS-1$
		try(Reader is = new StringReader(queryString)){
			Parser parser = new Parser(is);
			return parser.ParseSummary();
		}catch (Throwable ex) {
			throw new Exception(ex);
		}
	}
}
