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
package org.wcs.smart.intelligence.xml.export;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.intelligence.model.Intelligence;

/**
 * Class responsible for exporting
 * a intelligence to an xml file.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceExporter {

	public static File exportIntelligence(Intelligence intelligence, File file, boolean includeAttachments, IProgressMonitor monitor) throws Exception {
		return null;
	}

	/**
	 * Determines the output file name for export
	 * based on given input file
	 * and if attachments are included.
	 * 
	 * @param inputFile
	 * @param includeAttachs
	 * @return
	 */
	public static File getOutputFile(String inputFile, boolean includeAttachs) {
		if (!includeAttachs){
			return new File(inputFile);
		}else{
			//turn into zip file
			File in = new File(inputFile);
			int index = in.getName().lastIndexOf('.');
			String name = in.getName();
			if (index > 0){
				name = name.substring(0, index);
			}
			String zip = in.getParent() + File.separator + name + ".zip"; //$NON-NLS-1$
			return new File(zip);
		}
	}
	
}
