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
package org.wcs.smart.ct2smart.ui;

import java.util.List;

import org.wcs.smart.ct2smart.matcher.model.CtCategory;
import org.wcs.smart.ct2smart.matcher.model.CtCategoryMap;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class CategoryMatcher {

	public void match(List<CtCategory> newList, List<CtCategory> oldList) {
		boolean found = false;
		
		for (CtCategory nc : newList) {
			for (CtCategory oc : oldList) {
				for (CtCategoryMap ncm : nc.getCtCategoryMap()) {
					found = false;
					for (CtCategoryMap ocm : oc.getCtCategoryMap()) {
						if (ncm.getAi().equals(ocm.getAi()) && ncm.getVi().equals(ocm.getVi())) {
							found = true;
							break;
						}
					}
					if (!found) {
						break;
					}
				}
				if (found) {
					nc.setCategoryKey(oc.getCategoryKey());
					break;
				}
			}
		}
	}
}
