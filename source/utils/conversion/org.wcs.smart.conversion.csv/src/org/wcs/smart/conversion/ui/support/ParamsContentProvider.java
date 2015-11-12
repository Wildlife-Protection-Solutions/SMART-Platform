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
package org.wcs.smart.conversion.ui.support;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.conversion.model.Param;
import org.wcs.smart.conversion.tool.MissionBuilder;

/**
 * Content provider for SMART mapping params.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ParamsContentProvider implements IStructuredContentProvider {
	
	private static final List<String> knownKeys = Arrays.asList(
			MissionBuilder.SURVEY_DESIGN_KEY,
			MissionBuilder.SURVEY_KEY
	);

	private Map<String, Param> map = new TreeMap<String, Param>();

	public ParamsContentProvider(List<Param> params) {
		for (Param param : params) {
			map.put(param.getKey(), param);
		}
		
		for (String knownKey : knownKeys) {
			if (!map.containsKey(knownKey)) {
				Param p = new Param();
				p.setKey(knownKey);
				map.put(p.getKey(), p);
			}
		}
	}
	
	@Override
	public Object[] getElements(Object inputElement) {
		return map.values().toArray();
	}

	@Override
	public void dispose() {
		//nothing
	}

	@Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		//nothing
	}

}
