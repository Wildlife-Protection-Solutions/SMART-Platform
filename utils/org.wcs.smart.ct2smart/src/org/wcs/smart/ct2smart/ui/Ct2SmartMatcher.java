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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.util.PrefUtil;
import org.eclipse.ui.internal.util.PrefUtil.ICallback;
import org.wcs.smart.ct2smart.matcher.FileUtil;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class Ct2SmartMatcher {

	public static void main(String[] args) throws Exception {
		//this is required for filtered trees to work
		PrefUtil.setUICallback(new FakeCallback());
		PlatformUI.getPreferenceStore().getBoolean(IWorkbenchPreferenceConstants.SHOW_FILTERED_TEXTS);		

		MatchSession session = new MatchSession();
		session.setCt2Smart(FileUtil.loadCt2Smart(new File("match_super_x.xml")));
		session.setDataModel(loadDataModel(new File("d:\\dev\\data\\mist\\datamodel.xml")));
		
		
		Display display = new Display();
		Shell shell = new Shell(display);
		
		Point size = shell.computeSize(-1, -1);
        shell.setBounds(50, 50, size.x, size.y);    
		
		
		GridLayout layout = new GridLayout(1, true);
		shell.setLayout(layout);
	    
	    GridData gridData = new GridData(SWT.LEFT,SWT.TOP, false, false);
	    shell.setLayoutData(gridData);
		
		shell.setText("CyberTracker to SMART - Data Model Matcher");
		new DmMatcherDialog(shell, session);
		//new SourceDialog(shell);

		shell.pack();
		shell.open();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose();
	}

	private static DataModel loadDataModel(File file) throws JAXBException, FileNotFoundException {
		FileInputStream is = new FileInputStream(file);

		//read file directly instead of using the XmlSmartDataModelManager
		//because that manager uses classes which require hibernate and
		//we don't have to have to include hibernate in our build
		//this.smartDataModel = XmlSmartDataModelManager.readDataModel(is);
		JAXBContext context = JAXBContext.newInstance("org.wcs.smart.internal.ca.datamodel.xml.generate");
		Unmarshaller un = context.createUnmarshaller();	
		Object o = un.unmarshal(is);
		return (DataModel) o;
	}

}
