package org.wcs.smart.query.parser.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.SumQueryDefinition;

public class SummaryParserTest {

	@Test
	public void testAll() throws Exception{
		
		String valuePart = "patrol:sum:numdays,patrol:sum:distance,patrol:avg:distance"; 
		String rowGroupByPart = "patrol:station:,date:month";
		String colGroupByPart = "patrol:station:,date:month";
		String queryPart = "patrol:station equals \"station1\"";
		String query = valuePart + "|" + rowGroupByPart +"|" + colGroupByPart + "|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertEquals(test.getQueryFilter().asString(), queryPart);
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
		Assert.assertNull(test.getQueryFilter());
		
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
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "patrol:sum:numdays,patrol:min:numnights"; 
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "patrol:max:distance,patrol:avg:numhours,category:sum:pigs.fish.blue"; 
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
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
		Assert.assertNull(test.getQueryFilter());
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
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "";
		colGroupByPart = "patrol:station:,patrol:mandate:";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
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
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = ""; 
		rowGroupByPart = "patrol:id:adfsf3234nasdf2345:2342146af,patrol:team:23423fadv";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = ""; 
		rowGroupByPart = "";
		colGroupByPart = "category:1:,date:day";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = ""; 
		rowGroupByPart = "date:month,date:year"; //,date:week
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = ""; 
		rowGroupByPart = "date:month";
		colGroupByPart = "category:0:,date:day";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
	}
	
	
	private SumQueryDefinition parseQuery(String query) throws Exception{
		InputStream is = new ByteArrayInputStream(query.getBytes());
		Parser parser = new Parser(is);		
		SumQueryDefinition myQuery = parser.SumQuery();
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
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "attribute:n:max:age,attribute:n:avg:size";
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "category:threats.pigs.normal:attribute:n:max:age";
		rowGroupByPart = "";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
	}
	
	
	@Test
	public void testCategoryValues() throws Exception{
		
		String valuePart = "category:sum:threats.";
		String rowGroupByPart = "";
		String colGroupByPart = "";
		String queryPart = "";
		String query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		SumQueryDefinition test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
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
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "category:0:threat.pigs.funny.1:threat.pigs.funny.2";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "category:2:threat.pigs.funny.1,category:3:threat.old.fun:threat.old.fun.a.:threat.old.fun.b:threat.old.fun.c";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
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
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "attribute:l:type:a:b";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "attribute:l:type:a,attribute:l:type:a:b:c,attribute:l:type:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "category:threat.pigs.:attribute:l:type:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "category:1:attribute:l:type:a:b";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "category:threat.pigs.:attribute:l:type:a,category:threat.pigs.:attribute:l:type:a:b:c,attribute:l:type:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
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
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "attribute:t:type:1:a:b";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "attribute:t:type:4:a,attribute:t:type:1:a:b:c,attribute:l:type:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "category:threat.pigs.:attribute:t:type:1:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "category:threat.pgis.:attribute:t:type:1:a:b";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
		
		valuePart = "";
		rowGroupByPart = "category:threat.pigs.:attribute:t:type:1:a,category:threat.pigs.:attribute:t:type:1:a:b:c,attribute:l:type:";
		colGroupByPart = "";
		queryPart = "";
		query = valuePart + "|" + rowGroupByPart + "|" + colGroupByPart +"|" + queryPart;
		test = parseQuery(query);
		Assert.assertEquals(test.getValuePart().asString(), valuePart);
		Assert.assertEquals(test.getRowGroupByPart().asString(), rowGroupByPart);
		Assert.assertEquals(test.getColumnGroupByPart().asString(), colGroupByPart);
		Assert.assertNull(test.getQueryFilter());
	}
}
