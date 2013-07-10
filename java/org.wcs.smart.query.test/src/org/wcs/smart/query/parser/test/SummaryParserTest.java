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
package org.wcs.smart.query.parser.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.GridQueryDefinition;
import org.wcs.smart.query.parser.internal.summary.SumQueryDefinition;

/**
 * Sets of tests for the summary parser.
 * @author Emily
 *
 */
@SuppressWarnings({"nls", "restriction"})
public class SummaryParserTest {

	@Test
	public void testFilters() throws Exception{
		String valuePart = "patrol:sum:numdays"; 
		String rowGroupByPart = "";
		String colGroupByPart = "";
		String valueFilter = "";
		String rateFilter = "";
		String query = valuePart + "|" + rowGroupByPart +"|" + colGroupByPart + "|" + valueFilter + "|" + rateFilter;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		
		valuePart = "patrol:sum:numdays"; 
		rowGroupByPart = "patrol:id:\"0001\":\"0002\"";
		colGroupByPart = "";//"patrol:station:\"\",date:month";
		valueFilter = "category:threats.biologicalresourceuse.species.pigs";
		rateFilter = "attribute:n:size > 1.2";
		query = valuePart + "|" + rowGroupByPart +"|" + colGroupByPart + "|" + valueFilter + "|" + rateFilter;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertEquals(test.getValueFilter().asString(), valueFilter);
		Assert.assertEquals(test.getRateFilter().asString(), rateFilter);
	}
	
	@Test
	public void testPatrolTypeGroupBy() throws Exception{
		String valuePart = "patrol:sum:numdays"; 
		String rowGroupByPart = "patrol:patroltype:\"AIR\":\"GROUND\"";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart +"|" + colGroupByPart + "|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		
		valuePart = "patrol:sum:numdays"; 
		rowGroupByPart = "patrol:id:\"0001\":\"0002\"";
		colGroupByPart = "";//"patrol:station:\"\",date:month";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart +"|" + colGroupByPart + "|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	@Test
	public void testAll() throws Exception{
		
		String valuePart = "patrol:sum:numdays,patrol:sum:distance,patrol:avg:distance"; 
		String rowGroupByPart = "patrol:station:abcdef:efghi,date:month";
		String colGroupByPart = "";//"patrol:station:\"\",date:month";
		String queryPart = "patrol:station equals \"station1\"";
		String query = valuePart + "|" + rowGroupByPart +"|" + colGroupByPart + "|" + queryPart + "|";
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertEquals(test.getValueFilter().asString(), queryPart);
		Assert.assertNull(test.getRateFilter());
		
		
		valuePart = "patrol:sum:numdays,patrol:sum:distance"; 
		rowGroupByPart = "patrol:id:\"ABC-EM_000001\":\"ABC-EM_000002\":\"ABC-EM_000003\":\"ABC-EM_000004\":\"ABC-EM_00000\"";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart +"|" + colGroupByPart + "|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	
	@Test
	public void testCombinedPatrolValues() throws Exception{
		
		String valuePart = "patrol:sum:numdays/patrol:sum:numdays";
		String rowGroupByPart = "";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	
	@Test
	public void testPatrolValues() throws Exception{
		
		String valuePart = "patrol:sum:numdays";
		String rowGroupByPart = "";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "patrol:sum:numdays,patrol:min:numnights"; 
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "patrol:max:distance,patrol:avg:numhours,category:sum:obs:pigs.fish.blue"; 
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "category:max:pigs.fish.blue"; 
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		boolean error = false;
		try{
			test = parseQuery(query);
		}catch (Exception ex){
			error = true;
		}
		Assert.assertTrue(error);
		
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		error = false;
		try{
			test = parseQuery(query);
		}catch (Exception ex){
			error = true;
		}
		Assert.assertTrue(error);
		
		
		valuePart = "attribute:n:sum:numberofcans,attribute:n:avg:numberofcans,attribute:n:min:numberofcans,attribute:n:max:numberofcans"; 
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	@Test
	public void testGroupBy() throws Exception{
		
		String valuePart = ""; 
		String rowGroupByPart = "patrol:station:";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "";
		colGroupByPart = "patrol:station:,patrol:mandate:";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = ""; 
		colGroupByPart = "patrol:station:,patrol:mandate:,patrol:other:";
		rowGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		boolean error = false;
		try{
			test = parseQuery(query);
		}catch (Exception ex){
			error = true;
		}
		Assert.assertTrue(error);
		
		valuePart = ""; 
		rowGroupByPart = "patrol:id:,patrol:team:,patrol:patroltype:,patrol:transport:,patrol:leader:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = ""; 
		rowGroupByPart = "patrol:id:\"adfsf3234nasdf2345\":\"2342146af\",patrol:team:23423fadv";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = ""; 
		rowGroupByPart = "";
		colGroupByPart = "category:1:,date:day";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = ""; 
		rowGroupByPart = "date:month,date:year"; //,date:week
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = ""; 
		rowGroupByPart = "date:month";
		colGroupByPart = "category:0:,date:day";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	
	private SumQueryDefinition parseQuery(String query) throws Exception{
		InputStream is = new ByteArrayInputStream(query.getBytes());
		Parser parser = new Parser(is);		
		SumQueryDefinition myQuery = parser.SumQuery();
		is.close();
		return myQuery;
	}
	
	private GridQueryDefinition parseGridQuery(String query) throws Exception{
		InputStream is = new ByteArrayInputStream(query.getBytes());
		Parser parser = new Parser(is);		
		GridQueryDefinition myQuery = parser.GridQuery();
		is.close();
		return myQuery;
	}

	
	@Test
	public void testAttributeValues() throws Exception{
		
		String valuePart = "attribute:n:sum:age,attribute:n:min:size";
		String rowGroupByPart = "";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "attribute:n:max:age,attribute:n:avg:size";
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "category:threats.pigs.normal:attribute:n:max:age";
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	
	@Test
	public void testCategoryValues() throws Exception{
		
		String valuePart = "category:sum:wp:threats.";
		String rowGroupByPart = "";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "category:sum:obs:threats.";
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "category:sum:obs:";
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	@Test
	public void testConservationAreaGroupBy() throws Exception{
		
		String valuePart = "";
		String rowGroupByPart = "patrol:ca:";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	@Test
	public void testCategoryGroupBy() throws Exception{
		
		String valuePart = "";
		String rowGroupByPart = "category:1:";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "category:0:threat.pigs.funny.1:threat.pigs.funny.2";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "category:2:threat.pigs.funny.1,category:3:threat.old.fun:threat.old.fun.a.:threat.old.fun.b:threat.old.fun.c";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	
	@Test
	public void testAttributeListGroupBy() throws Exception{
		
		String valuePart = "";
		String rowGroupByPart = "attribute:l:type:";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "attribute:l:type:a:b";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "attribute:l:type:a,attribute:l:type:a:b:c,attribute:l:type:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "category:threat.pigs.:attribute:l:type:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "category:1:attribute:l:type:a:b";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "category:threat.pigs.:attribute:l:type:a,category:threat.pigs.:attribute:l:type:a:b:c,attribute:l:type:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	
	
	@Test
	public void testAttributeTreeGroupBy() throws Exception{
		
		String valuePart = "";
		String rowGroupByPart = "attribute:t:type:2:";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "attribute:t:type:1:a:b";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "attribute:t:type:4:a,attribute:t:type:1:a:b:c,attribute:l:type:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "category:threat.pigs.:attribute:t:type:1:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "category:threat.pgis.:attribute:t:type:1:a:b";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "category:threat.pigs.:attribute:t:type:1:a,category:threat.pigs.:attribute:t:type:1:a:b:c,attribute:l:type:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	@Test
	public void testAreaGroupBy() throws Exception{
		//"CA" | "BA" | "ADMIN" | "MNGT" | "PATRL"
		String valuePart = "";
		String rowGroupByPart = "area:CA:ca1:ca2";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "area:error:ca1:ca2:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		boolean error = false;
		try{
			test = parseQuery(query);
		}catch (Exception ex){
			error = true;
		}
		Assert.assertTrue(error);
		
		valuePart = "";
		rowGroupByPart = "area:BA:ba2";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "area:ADMIN:admin1";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "area:PATRL:p1:p2:p3:p4:p5:p6";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "";
		rowGroupByPart = "area:MNGT:m1:m2";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
	}
	
	
	@Test
	public void gridTestValues() throws Exception{
		
		String valuePart = "attribute:n:sum:age";
		String queryFilter = "";
		String rateFilter = "";
		String query = valuePart + "|" + "1" + "|" + queryFilter + "|" + rateFilter;
		GridQueryDefinition test = parseGridQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertNull(test.getValueFilter());
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "attribute:n:sum:age";
		queryFilter = "patrol:station equals \"station1\"";
		rateFilter = "";
		query = valuePart + "|" + "1" + "|" + queryFilter + "|" + rateFilter;
		test = parseGridQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getValueFilter().asString(), queryFilter);
		Assert.assertNull(test.getRateFilter());
		
		valuePart = "attribute:n:sum:age";
		queryFilter = "patrol:station equals \"station1\"";
		rateFilter =  "patrol:station equals \"station4\"";
		query = valuePart + "|" + "1" + "|" + queryFilter + "|" + rateFilter;
		test = parseGridQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getValueFilter().asString(), queryFilter);
		Assert.assertEquals(test.getRateFilter().asString(), rateFilter);
		
		valuePart = "category:sum:wp:";
		queryFilter = "patrol:station equals \"station1\"";
		rateFilter =  "patrol:station equals \"station4\"";
		query = valuePart + "|" + "1" + "|" + queryFilter + "|" + rateFilter;
		test = parseGridQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getValueFilter().asString(), queryFilter);
		Assert.assertEquals(test.getRateFilter().asString(), rateFilter);
		
		valuePart = "category:sum:obs:";
		queryFilter = "patrol:station equals \"station1\"";
		rateFilter =  "patrol:station equals \"station4\"";
		query = valuePart + "|" + "1" + "|" + queryFilter + "|" + rateFilter;
		test = parseGridQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getValueFilter().asString(), queryFilter);
		Assert.assertEquals(test.getRateFilter().asString(), rateFilter);
		
		valuePart = "";
		queryFilter = "";
		rateFilter = "";
		query = valuePart + "|" + "1" + "|" + queryFilter + "|" + rateFilter;
		boolean error = false;
		try{
			test = parseGridQuery(query);
		}catch (Exception ex){
			error = true;
		}
		Assert.assertTrue(error);
			
	}
}
