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

import org.junit.Assert;

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

}
