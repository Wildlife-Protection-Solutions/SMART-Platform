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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class containing configuration data for patrol import.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ImportConfig {
	
	private boolean keepIDs = false;
	private boolean ignoreWarnings = false;
	private List<String> warnings = new ArrayList<String>();
	private List<String> warnFiles = new ArrayList<String>();
	
	public boolean isKeepIDs() {
		return keepIDs;
	}
	public void setKeepIDs(boolean keepIDs) {
		this.keepIDs = keepIDs;
	}
	
	public boolean isIgnoreWarnings() {
		return ignoreWarnings;
	}
	public void setIgnoreWarnings(boolean ignoreWarnings) {
		this.ignoreWarnings = ignoreWarnings;
	}
	
	public List<String> getWarnings() {
		return warnings;
	}
	
	public List<String> getWarningFiles() {
		return warnFiles;
	}
	
	public void addWarnings(List<String> warnings, File f){
		if(warnings.size() > 0){
			this.warnFiles.add(f.getAbsolutePath());
		}
		this.warnings.addAll(warnings);
	}

	public void addWarning(String warning, File f) {
		this.warnFiles.add(f.getAbsolutePath());
		this.warnings.add(warning);
	}
}
