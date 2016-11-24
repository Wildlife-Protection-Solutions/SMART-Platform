/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.ui.SmartShellDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Shell dialog for displaying details of an intel record
 * 
 * @author Emily
 *
 */
public class RecordDetailsShell extends SmartShellDialog{
	
	private IntelRecord record;
	
	private Color bgColor = null;
	private Font boldFont = null;
	private ScrolledComposite sc;
	private Composite scContent;
	
	private boolean blnMouseDown;
	private int xPos, yPos;
	
	private Label lblName;
	private Label lblDateCreated;
	private Label lblDateModified;
	private Label lblNarrative;
	private Label lblEntities;
	
	private Composite owner;
	
	public RecordDetailsShell(Shell ownerShell, IntelRecord record){
		super(ownerShell, SWT.RESIZE);
		this.record = record;
	}
	
	public IntelRecord getRecord(){
		return this.record;
	}
	
	public void setRecord(IntelRecord record){
		if (!this.record.equals(record)){
			this.record = record;
			for (Label l : new Label[]{lblName, lblDateCreated, lblDateModified, lblNarrative, lblEntities}){
				l.setText("");
			}
			lblName.setText(DialogConstants.LOADING_TEXT);
			loadRecord.schedule(500);
		}
	}
	private void createHeaderLabel(String name, Composite parent){
		Label l = new Label(parent, SWT.NONE);
		l.setText(name);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
	}
	
	private Label createContentLabel(Composite parent){
		Label l = new Label(parent, SWT.WRAP);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 100;
		return l;
	}
	
	public void createContents(Composite parent){
		if (owner != null) return;
		
		owner = new Composite(parent, SWT.NONE);
		owner.setLayout(new GridLayout());
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lblName = new Label(owner, SWT.NONE);
		FontData fd = lblName.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.height = fd.height + 1;
		boldFont = new Font(lblName.getDisplay(), fd);
		lblName.setFont(boldFont);
		lblName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lblName.addListener(SWT.MouseDown, this);
		lblName.addListener(SWT.MouseUp, this);
		lblName.addListener(SWT.MouseMove, this);
		lblName.addListener(SWT.MouseEnter, this);
		lblName.addListener(SWT.MouseExit, this);		
		
		sc = new ScrolledComposite(owner, SWT.V_SCROLL);
		scContent = new Composite(sc, SWT.NONE);
		sc.setContent(scContent);
		scContent.setLayout(new GridLayout(2, false));
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		createHeaderLabel("Date Created:", scContent);
		lblDateCreated= createContentLabel(scContent);
		
		createHeaderLabel("Date Modified:", scContent);
		lblDateModified = createContentLabel(scContent);
		
		createHeaderLabel("Narrative:", scContent);
		lblNarrative = createContentLabel(scContent);
		
		createHeaderLabel("Entities:", scContent);
		lblEntities = createContentLabel(scContent);
		sc.setSize(scContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		owner.addListener(SWT.Resize, r->{
			sc.setMinSize(scContent.computeSize(sc.getClientArea().width, SWT.DEFAULT));
		});
		
		//configure background color
		bgColor = new Color(parent.getDisplay(), 255, 255, 225);
		
		List<Control> items = new ArrayList<Control>();
		items.add(owner);
		while(!items.isEmpty()){
			Control c = items.remove(0);
			c.setBackground(bgColor);
			if (c instanceof Composite){
				for (Control kid : ((Composite) c).getChildren()){
					items.add(kid);
				}
			}
		}
		
		loadRecord.schedule();
	}


	
	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		
		if (event.type == SWT.Dispose){
			if (bgColor != null){
				bgColor.dispose();
				bgColor = null;
			}
			if (boldFont != null){
				boldFont.dispose();
				boldFont = null;
			}
			return;
		}else if (event.type == SWT.Deactivate){
			close();
			return;
		}else if (event.type == SWT.MouseDown){
			if (event.widget != lblName) return;
			blnMouseDown = true;
			xPos = event.x;
			yPos = event.y;
		} else if (event.type == SWT.MouseUp) {
			if (event.widget != lblName) return;
			blnMouseDown = false;
		} else if (event.type == SWT.MouseMove) {
			if (event.widget != lblName) return;
			if (blnMouseDown) {
				shell.setLocation(shell.getLocation().x + (event.x - xPos),
						shell.getLocation().y + (event.y - yPos));
			}
		} else if (event.type == SWT.MouseEnter) {
			if (event.widget != lblName) return;
			shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_SIZEALL));
		} else if (event.type == SWT.MouseExit) {
			if (event.widget != lblName) return;
			shell.setCursor(null);
		}
	}
	
	private Job loadRecord = new Job("load record job"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final StringBuilder sbEntities = new StringBuilder();
			String name ="";
			String narr = "";
			Date dCreated;
			Date dModified;
			Session s = HibernateManager.openSession();
			try{
				IntelRecord temp = (IntelRecord)s.get(IntelRecord.class, record.getUuid());
				name = temp.getTitle();
				narr = temp.getDescription();
				dCreated = temp.getDateCreated();
				dModified = temp.getDateModified();
				for (IntelEntityRecord e : temp.getEntities()){
					sbEntities.append(e.getEntity().getIdAttributeAsText());
					sbEntities.append("\n");
				}
			}finally{
				s.close();
			}
			final String name1 = name;
			final String narr1 = narr;
			final Date dCreated1 = dCreated;
			final Date dModified1 = dModified;
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {	
					lblName.setText(name1);
					lblNarrative.setText(narr1);
					lblEntities.setText(sbEntities.toString());
					lblDateCreated.setText(DateFormat.getDateInstance().format(dCreated1));
					lblDateModified.setText(DateFormat.getDateInstance().format(dModified1));
					owner.layout(true);
					sc.setMinSize(scContent.computeSize(sc.getClientArea().width, SWT.DEFAULT));
				}
			});
			return Status.OK_STATUS;
		}
		
	};
}
