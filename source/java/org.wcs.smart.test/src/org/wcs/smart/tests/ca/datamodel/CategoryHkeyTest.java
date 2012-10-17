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
package org.wcs.smart.tests.ca.datamodel;

import junit.framework.Assert;

import org.junit.Test;
import org.wcs.smart.ca.datamodel.Category;

/**
 * Tests category hkey related functions.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CategoryHkeyTest {

	@Test
	public void testtrimHkeyToLevel(){
		Assert.assertNull(Category.trimHkeyToLevel(1,null));
		Assert.assertNull(Category.trimHkeyToLevel(1,"abc"));
		Assert.assertNull(Category.trimHkeyToLevel(1,""));
		Assert.assertNull(Category.trimHkeyToLevel(1,"   "));
		Assert.assertNull(Category.trimHkeyToLevel(-1,"abc."));
		
		Assert.assertEquals("a.", Category.trimHkeyToLevel(0,"a."));
		Assert.assertEquals(null, Category.trimHkeyToLevel(1,"a."));
		Assert.assertEquals("a.b.", Category.trimHkeyToLevel(1,"a.b."));
		Assert.assertEquals("a.", Category.trimHkeyToLevel(0,"a.b."));
		
		Assert.assertEquals("a.", Category.trimHkeyToLevel(0,"a.b.c."));
		Assert.assertEquals("a.b.", Category.trimHkeyToLevel(1,"a.b.c."));
		Assert.assertEquals("a.b.c.", Category.trimHkeyToLevel(2,"a.b.c."));
	}

	//	@Test
//	public void testParentHkey(){
//		Assert.assertNull(Category.computeParentHkey(null));
//		Assert.assertNull(Category.computeParentHkey("abc"));
//		Assert.assertNull(Category.computeParentHkey(""));
//		Assert.assertNull(Category.computeParentHkey("   "));
//		Assert.assertEquals("", Category.computeParentHkey("."));
//		Assert.assertEquals("", Category.computeParentHkey("abc."));
//		Assert.assertEquals("abc.", Category.computeParentHkey("abc.def."));
//		Assert.assertEquals("abc.def.", Category.computeParentHkey("abc.def.ghi."));
//		Assert.assertEquals("abc.def.ghi.three.four.", Category.computeParentHkey("abc.def.ghi.three.four.five."));
//	}
//	
//	@Test
//	public void testTrimHkeyToChild(){
//		Assert.assertNull(Category.trimHkeyToChild(null,null));
//		Assert.assertNull(Category.trimHkeyToChild("abc","abc."));
//		Assert.assertNull(Category.trimHkeyToChild("abc.","abc"));
//		Assert.assertNull(Category.trimHkeyToChild("", "abc."));
//		Assert.assertNull(Category.trimHkeyToChild("abc.", ""));
//		Assert.assertNull(Category.trimHkeyToChild("   ", "abc."));
//		Assert.assertNull(Category.trimHkeyToChild("abc.", "   "));
//		
//		Assert.assertEquals("abc.def.", Category.trimHkeyToChild("abc.", "abc.def."));
//		Assert.assertEquals("abc.def.", Category.trimHkeyToChild("abc.", "abc.def.ghi."));
//		Assert.assertEquals("abc.def.", Category.trimHkeyToChild("abc.", "abc.def.ghi.lmo."));
//		
//		Assert.assertNull(Category.trimHkeyToChild("abc.def.", "abc.def."));
//		Assert.assertEquals("abc.def.ghi.", Category.trimHkeyToChild("abc.def.", "abc.def.ghi."));
//		Assert.assertEquals("abc.def.ghi.", Category.trimHkeyToChild("abc.def.", "abc.def.ghi.lmo."));
//		
//	}
}
