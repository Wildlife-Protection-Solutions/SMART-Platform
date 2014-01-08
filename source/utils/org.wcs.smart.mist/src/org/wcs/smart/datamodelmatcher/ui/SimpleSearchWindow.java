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

package org.wcs.smart.datamodelmatcher.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;

public class SimpleSearchWindow extends Dialog {
	public final Shell shell;
	AttributeSelection attrSelection;
	Tree tree;
	String langCode;
	List list;
	Text filter;
	
	public SimpleSearchWindow(Shell parentShell, Tree tree, String langCode, Text filter, AttributeSelection attrSelection) {
		super(parentShell);
		this.shell = new Shell(parentShell, SWT.SHELL_TRIM | SWT.OK | SWT.APPLICATION_MODAL);
		this.tree = tree;
		this.langCode = langCode;
		this.filter = filter;
		this.attrSelection = attrSelection; 
		this.shell.setText("Search Results");
	}

	
	
	public int open() {
		GridLayout layout = new GridLayout(1, false);

		shell.setLayout(layout);
		    
		GridData gridData = new GridData(SWT.FILL,SWT.FILL, true, true);
		shell.setLayoutData(gridData);
		
		shell.setMinimumSize(300, 400);
		  
		    
        // (widget creation, set result, etc).
      
        //main composite and layout
		final Composite main = new Composite(shell, SWT.None);
		GridLayout mlayout = new GridLayout(1, true);
	    main.setLayout(mlayout);
	    GridData mainGridData = new GridData(SWT.FILL,SWT.FILL, true, true);
	    main.setLayoutData(mainGridData);
	    
	    Label label = new Label(main, SWT.NONE);
	    String eol = System.getProperty("line.separator"); 
	    label.setText("Select from the search results listed below and the item will be selected in the value tree on the main" + eol + "Matching Window. Once you have the correct selection, you can close this window.");

	    list = new List (main, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL| SWT.H_SCROLL);
	    GridData gd = new GridData();
	    gd.widthHint = 500;
	    gd.heightHint = 300;
	    list.setLayoutData(gd);
		searchTree(tree.getItem(0),filter.getText(), "");

	
		Rectangle clientArea = shell.getClientArea ();
		list.setBounds (clientArea.x, clientArea.y, 100, 100);
		
		
		list.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
	  		    String selected;
				String[] selection = list.getSelection();
    	  		if (selection.length != 0) {
    	  		    selected = selection[0];
    	  		    attrSelection.updateTree(selected, tree.getItem(0), "");
    	  		}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		Button close = new Button(main, SWT.NONE);
		close.setText("Close");
		close.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
		close.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event arg0) {
					shell.dispose();
				}
		});
		
		 shell.pack();
         shell.open();
         Display display = shell.getDisplay();
	      
         while (!shell.isDisposed()) {
                 if (!display.readAndDispatch()) display.sleep();
         }

		
		return 1;	
	}
	
	private boolean searchTree(TreeItem ti, String pattern, String prefix) {

		for(int z=0; z < ti.getItemCount(); z++){
			TreeNodeType c = (TreeNodeType)ti.getItem(z).getData();
			if(c != null){
				for(int x=0; x < c.getNames().size(); x++){
					  if(c.getNames().get(x).getLanguageCode().equals(langCode)){
						  String name = c.getNames().get(x).getValue();
						  if(name.toLowerCase().contains(pattern.toLowerCase())){
							  String fullKey;
							  if(prefix.equals("")){
								  fullKey = c.getKey();
							  }else{
								  fullKey = prefix + c.getKey();
							  }
							  list.add(fullKey);
						  }
						  break;
					  }
				  }				
			}
			
			searchTree(ti.getItem(z), pattern,  prefix + c.getKey() + ".");
		}
		return false;
	}
}
