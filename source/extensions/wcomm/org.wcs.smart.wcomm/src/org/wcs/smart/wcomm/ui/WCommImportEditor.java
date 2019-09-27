/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.wcomm.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.wcs.smart.wcomm.Messages;
import org.wcs.smart.wcomm.WCommPlugIn;
import org.wcs.smart.wcomm.WcommMapping;

/**
 * WCoMM Data Import editor
 * 
 * @author Emily
 *
 */
public class WCommImportEditor extends MultiPageEditorPart{

	public static final String ID = "org.wcs.smart.wcomsms.data.import"; //$NON-NLS-1$
	
	private ImportDataPage importPage;
	private MappingPage mappingPage;
	
	private WcommMapping mapping = null;
	
	@Override
	public void dispose() {
		
	}

	
	public WcommMapping getMapping() {		
		if (mapping == null) {
			try {
				mapping = WcommMapping.load();
			}catch (Exception ex) {
				WCommPlugIn.displayLog(ex.getMessage(), ex);
				mapping = WcommMapping.empty();
			}
		}
		return mapping;
	}
	
	@Override
	protected void createPages() {
		try {
			importPage = new ImportDataPage();
			int i = addPage(importPage, getEditorInput());
			setPageText(i, Messages.WCommImportEditor_ImportTabName);
				
			mappingPage = new MappingPage(this);
			int mapIndex = addPage(mappingPage, getEditorInput());
			setPageText(mapIndex, Messages.WCommImportEditor_MappingsTabName);
		}catch (PartInitException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}


	@Override
	public void doSaveAs() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

}
