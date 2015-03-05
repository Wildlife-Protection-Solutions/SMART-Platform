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
package org.wcs.smart.conversion.tool;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class TeamMembersParser {

	public Set<String> parse(Collection<String> rawData) {
		Set<String> result = new HashSet<String>();
		for (String s : rawData) {
			result.addAll(parseMembers(s));
		}
		return result;
	}

	public Set<String> parseMembers(String rawString) {
		String[] split = rawString.split(",|;|:|\r\n| and | AND "); //$NON-NLS-1$
		Set<String> result = new HashSet<String>();
		for (String s : split) {
			String[] dobleSpSplit = s.trim().split("  "); //$NON-NLS-1$
			for (String str : dobleSpSplit) {
				String x = str.trim();
				if (!x.isEmpty()) {
					result.addAll(spaceCut(x));
				}
			}
		}
		return result;
	}

	/**
	 * Assume that it might be "fistName1 lastName1 firstName2 lastName2 ..." 
	 */
	private Collection<String> spaceCut(String str) {
		Set<String> result = new HashSet<String>();
		int i;
		int fromIndex = 0;
		while (true) {
			i = str.indexOf(" ", fromIndex); //$NON-NLS-1$
			if (i == -1) {
				result.add(str.substring(fromIndex));
				return result;
			}
			//look for next space
			i = str.indexOf(" ", i+1); //$NON-NLS-1$
			if (i == -1) {
				result.add(str.substring(fromIndex));
				return result;
			}
			result.add(str.substring(fromIndex, i));
			fromIndex = i+1;
		}
	}

}
