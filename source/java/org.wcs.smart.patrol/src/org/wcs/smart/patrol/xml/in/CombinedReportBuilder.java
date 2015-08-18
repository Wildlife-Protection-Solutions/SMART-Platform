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
package org.wcs.smart.patrol.xml.in;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.util.SharedUtils;

/**
 * Builds combiner report for batch import.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class CombinedReportBuilder {
	
	private String duplicateAttributesError;
	private String categoryNotFound;
	private String notAllDataImported;
	
	public CombinedReportBuilder() {
		duplicateAttributesError = createPattern(Messages.XmlToPatrolConverter_DuplicateAttributesError);
		categoryNotFound = createPattern(Messages.XmlToPatrolConverter_Warning_CategoryNotFound);
		notAllDataImported = createPattern(Messages.XmlToPatrolConverter_Warning_NotAllDataImported);
	}

	private String createPattern(String msg) {
		// Transforms: "This is {0}. So {1} is {2}." -> "This is (.*)\. So (.*) is (.*)\." 
		msg = msg.replaceAll("\\.", "\\\\."); //$NON-NLS-1$ //$NON-NLS-2$
		return msg.replaceAll("\\{\\d\\}", "(.*)");  //$NON-NLS-1$//$NON-NLS-2$
	}
	
	public String buildReport(List<String> warnings, List<String> files) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (String warn : warnings) {
			String adopted = adopt(warn);
			Integer count = map.get(adopted);
			if (count == null) {
				count = 0;
			}
			map.put(adopted, count+1);
		}
		
		List<String> wList = new ArrayList<String>(map.keySet());
		Collections.sort(wList);
		
		StringBuilder sb = new StringBuilder();
		for (String warn : wList) {
			Integer count = map.get(warn);
			if (count > 1) {
				sb.append(MessageFormat.format(Messages.CombinedReportBuilder_Times, count));
			}
			sb.append(warn);
			sb.append(SharedUtils.LINE_SEPARATOR);
		}
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append(Messages.CombinedReportBuilder_FilesLabel);
		sb.append(SharedUtils.LINE_SEPARATOR);
		for (String file : files){
			sb.append(file);
			sb.append(SharedUtils.LINE_SEPARATOR);
		}
		
		return sb.toString();
	}

	private String adopt(String warn) {
		if (warn.matches(duplicateAttributesError)) {
			//extract attribute value
			String attr = warn.replaceAll(duplicateAttributesError, "$3"); //$NON-NLS-1$
			return MessageFormat.format(Messages.XmlToPatrolConverter_DuplicateAttributesError, Messages.CombinedReportBuilder_Id, Messages.CombinedReportBuilder_DateTime, attr);

		} else if (warn.matches(categoryNotFound)) {
			String cat = warn.replaceAll(categoryNotFound, "$1"); //$NON-NLS-1$
			return MessageFormat.format(Messages.XmlToPatrolConverter_Warning_CategoryNotFound, cat, Messages.CombinedReportBuilder_Id, Messages.CombinedReportBuilder_DateTime);
			
		} else if (warn.matches(notAllDataImported)) {
			return MessageFormat.format(Messages.XmlToPatrolConverter_Warning_NotAllDataImported, Messages.CombinedReportBuilder_Id, Messages.CombinedReportBuilder_DateTime);
			
		}
		return warn;
	}
	
	
}
