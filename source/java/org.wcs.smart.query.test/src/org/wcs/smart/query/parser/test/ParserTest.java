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
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.patrol.query.parser.internal.parser.ParseException;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.query.model.filter.QueryFilter;

/**
 * Sets of test for testing query parser.
 * 
 * @author Emily
 * @since 1.0.0
 */
@SuppressWarnings({"nls"})
public class ParserTest {

	@Test
	public void testCategory() throws Exception{
		
		String query = "category:pigs";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "category:threats.biologicalresourceuse.species.pigs";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
	}
	
	@Test
	public void testNumericAttribute() throws Exception{
		String query = "attribute:n:size > 1.2";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "attribute:n:age >= 3.4";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "attribute:n:age <= 4.0";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "attribute:n:age < 5.0";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "attribute:n:age = 234.0";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "attribute:n:age != -12.3";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "attribute:n:age <> 0.0";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), "attribute:n:age != 0.0");
	}
	
	@Test
	public void testAreaFilters() throws Exception{
		String query = "area:wp:CA:key";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "area:t:MNGT:key";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "area:t:ADMIN:key";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "area:wp:PATRL:key";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "area:t:BA:key";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		try{
			query = "area:p:BA:key";
			parseAllQuery(query);
			Assert.assertTrue(false);
		}catch (Throwable ex){
			Assert.assertTrue(true);
		}
		
		try{
			query = "area:BA:key";
			parseAllQuery(query);
			Assert.assertTrue(false);
		}catch (Throwable ex){
			Assert.assertTrue(true);
		}
		
		try{
			query = "area:t:ABC:key";
			parseAllQuery(query);
			Assert.assertTrue(false);
		}catch (Throwable ex){
			Assert.assertTrue(true);
		}
	}
	
	@Test
	public void testBooleanAttribute() throws Exception{
		String query = "attribute:b:camp";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
	}
	
	@Test
	public void testListAttribute() throws Exception{
		String query = "attribute:l:weapontype = gun";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
	}
	@Test
	public void testTreeAttribute() throws Exception{
		String query = "attribute:t:weapontype = gun.hand.jamesbond";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "attribute:t:species = ailuridae.ailurusfulgensred714.";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
	}
	
	@Test
	public void testStringAttribute() throws Exception{
		String query = "attribute:s:color equals \"red\"";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "attribute:s:color contains \"white\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "attribute:s:color notcontains \"purple\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		boolean ex = false;
		try{
			query = "attribute:s:color notcontains purple";
			test = parseQuery(query);
		}catch (ParseException e){
			ex = true;
		}
		Assert.assertTrue(ex);
	}
	
	@Test
	public void testPatrolValueAttribute() throws Exception{
		String query = "patrol:id equals \"abc\"";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "patrol:station equals \"abc\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "patrol:team equals \"abc\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "patrol:mandate equals \"abc\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "patrol:patroltype equals \"abc\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "patrol:transport equals \"abc\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "patrol:leader equals \"abc\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "patrol:member equals \"abc\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "patrol:member = \"abc\"";
		boolean ok = false;
		try{
			test = parseQuery(query);
		}catch (ParseException ex){
			ok = true;
		}
		Assert.assertTrue(ok);
		
		query = "patrol:member > \"abc\"";
		ok = false;
		try{
			test = parseQuery(query);
		}catch (ParseException ex){
			ok = true;
		}
		Assert.assertTrue(ok);
		
	}
	@Test
	public void testPatrolBooleanAttribute() throws Exception{
		String query = "patrol:armed";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "patrol:armed equals \"abc\"";
		boolean ok = false;
		try{
			test = parseQuery(query);
			System.out.println(test.asString() + ":" + query);
		}catch (ParseException ex){
			ok = true;
		}
		Assert.assertTrue(ok);
	}
	
	@Test
	public void testBrackets() throws Exception{
		String query = "( patrol:armed )";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), "(patrol:armed)");

		query = "( category:threats.fish.color.blue or attribute:n:age>=30) AND (patrol:id equals \"00001\" OR patrol:id equals \"90002\")";
		test = parseQuery(query);
		System.out.println(test.asString());
		Assert.assertEquals(test.asString(), "(category:threats.fish.color.blue or attribute:n:age >= 30.0) and (patrol:id equals \"00001\" or patrol:id equals \"90002\")");
	}
	
	@Test
	public void testNot() throws Exception{
		String query = "NOT category:threats.pigs";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "NOT (category:threats.pigs or category:threats.items)";
		String queryOK = "NOT (category:threats.pigs or category:threats.items)";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), queryOK);
		
		query = "NOT (category:threats.pigs)";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "NOT (attribute:b:camp)";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "NOT attribute:b:camp";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "NOT attribute:n:age = 0.0";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "NOT attribute:n:age ";
		boolean error = false;
		try{
			test = parseQuery(query);
		}catch (ParseException ex){
			error = true;
		}
		Assert.assertTrue(error);
		
		query = "NOT (category:threats.pigs) or NOT category:threats.mules";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "NOT ((category:threats.pigs) or NOT category:threats.mules)";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "NOT ((category:threats.pigs) NOT category:threats.mules)";
		try{
			test = parseQuery(query);
		}catch (ParseException ex){
			error = true;
		}
		Assert.assertTrue(error);
	}
	
	
	@Test
	public void testNumericCategoryAttribute() throws Exception{
		String query = "category:fishing:attribute:n:size > 1.2";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "category:fishing:attribute:n:age >= 3.4";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "category:fishing:attribute:n:age <= 4.0";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "category:fishing:attribute:n:age < 5.0";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "category:fishing:attribute:n:age = 234.0";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "category:fishing:attribute:n:age != -12.3";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "category:fishing:attribute:n:age <> 0.0";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), "category:fishing:attribute:n:age != 0.0");
	}
	
	
	@Test
	public void testBooleanCategoryAttribute() throws Exception{
		String query = "category:fish:attribute:b:camp";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
	}
	
	@Test
	public void testListCategoryAttribute() throws Exception{
		String query = "category:hunging:attribute:l:weapontype = gun";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
	}
	@Test
	public void testTreeCategoryAttribute() throws Exception{
		String query = "category:hungting:attribute:t:weapontype = gun.hand.jamesbond";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
	}
	
	@Test
	public void testStringCategoryAttribute() throws Exception{
		String query = "category:a:attribute:s:color equals \"red\"";
		IFilter test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "category:a:attribute:s:color contains \"white\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "category:b:attribute:s:color notcontains \"purple\"";
		test = parseQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		boolean ex = false;
		try{
			query = "category:asdfwqef:attribute:s:color notcontains purple";
			test = parseQuery(query);
		}catch (ParseException e){
			ex = true;
		}
		Assert.assertTrue(ex);
	}
	
	@Test
	public void testFilterType() throws Exception{
//		String query="";
//		QueryFilter test = parseAllQuery(query);
//		Assert.assertEquals(test.asString(), query);
		
		String query = "waypoint|patrol:team equals \"b83514b938ae40e9a9118d17c5706234\"";
		QueryFilter test = parseAllQuery(query);
		Assert.assertEquals(test.asString(), query);
		
//		query="";
//		test = parseAllQuery(query);
//		Assert.assertEquals(test.asString(), query);
		
		query = "observation|patrol:team equals \"b83514b938ae40e9a9118d17c5706234\"";
		test = parseAllQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "observation|category:a:attribute:s:color contains \"white\"";
		test = parseAllQuery(query);
		Assert.assertEquals(test.asString(), query);
		
		query = "waypoint|category:a:attribute:s:color contains \"white\" or attribute:t:weapontype = gun.hand.jamesbond";
		test = parseAllQuery(query);
		Assert.assertEquals(test.asString(), query);
	}
	
	
	private IFilter parseQuery(String query) throws Exception{
		InputStream is = new ByteArrayInputStream(query.getBytes());
		Parser parser = new Parser(is);		
		QueryFilter myQuery = parser.QueryFilter();
		is.close();
		return myQuery.getFilter();
	}
	
	private QueryFilter parseAllQuery(String query) throws Exception{
		InputStream is = new ByteArrayInputStream(query.getBytes());
		Parser parser = new Parser(is);		
		QueryFilter myQuery = parser.QueryFilter();
		is.close();
		return myQuery;
	}
	
}
