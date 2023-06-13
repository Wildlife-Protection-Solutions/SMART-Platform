/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.dialogs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.xml.XmlToProfile;
import org.wcs.smart.i2.xml.model.Profile;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for creating a new profile.  Allows users to create
 * new profiles from a template.
 * 
 * @author Emily
 *
 */
public class NewProfileDialog extends SmartStyledTitleDialog {
	
	private Button opCreateNew;
	private Button opUseTemplate;
	private ComboViewer cmbTemplates;
	
	private URL template = null;
	
	public NewProfileDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected void okPressed() {
		if (opCreateNew.getSelection()) {
			template = null;
		}else {
			template = ((ProfileFile) cmbTemplates.getStructuredSelection().getFirstElement()).url;
		}
		super.okPressed();
	}
	
	public URL getTemplate() {
		return template;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	

		opCreateNew = new Button(temp, SWT.RADIO);
		opCreateNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		opCreateNew.setSelection(true);
		opCreateNew.setText(Messages.NewProfileDialog_CreateNew);
		
		
		opUseTemplate = new Button(temp, SWT.RADIO);
		opUseTemplate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		opUseTemplate.setSelection(false);
		opUseTemplate.setText(Messages.NewProfileDialog_UseTemplate);
				
		Label l = new Label(temp, SWT.NONE);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)l.getLayoutData()).widthHint = 20;
		
		cmbTemplates = new ComboViewer(temp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbTemplates.setContentProvider(ArrayContentProvider.getInstance());
		cmbTemplates.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbTemplates.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof ProfileFile) {
					return ((ProfileFile)element).name;
				}
				return super.getText(element);
			}
		});
		cmbTemplates.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbTemplates.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		cmbTemplates.getControl().setEnabled(false);
		
		opUseTemplate.addListener(SWT.Selection, e->{
			cmbTemplates.getControl().setEnabled(opUseTemplate.getSelection());
			validate();
		});
				
		cmbTemplates.addSelectionChangedListener(e->validate());
		opCreateNew.addListener(SWT.Selection, e->validate());
		
		setTitle(Messages.NewProfileDialog_Title);
		getShell().setText(Messages.NewProfileDialog_Title);
		setMessage(Messages.NewProfileDialog_Message);
		
		loadTemplates();
		
		return parent;
	}
	
	private void validate() {
		if (opCreateNew.getSelection()) {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
			return;
		}else {
			if (!cmbTemplates.getStructuredSelection().isEmpty()) {
				if (cmbTemplates.getStructuredSelection().getFirstElement() instanceof ProfileFile) {
					getButton(IDialogConstants.OK_ID).setEnabled(true);
					return;
				}
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		return;
	}
	
	private void loadTemplates(){
		Job job = new Job(Messages.NewProfileDialog_loadingtemplatesjob) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Enumeration<URL> e = Intelligence2PlugIn.getDefault().getBundle().findEntries("templates", "*.zip", false); //$NON-NLS-1$ //$NON-NLS-2$
				List<ProfileFile> profiles = new ArrayList<>();
				
				XmlToProfile pp = new XmlToProfile(SmartDB.getCurrentConservationArea());
				
				while(e.hasMoreElements()) {
					URL item = e.nextElement();
					try {
						//unzip these models in memory; this may cause issues if these get large
						byte[] data = null;
						try(InputStream is = XmlToProfile.class.getResourceAsStream(item.getPath())){
							data = is.readAllBytes();
						}
						
						String xml = null;
						
						try(ZipFile archiveFile = new ZipFile(new SeekableInMemoryByteChannel(data)) ){
							Enumeration<ZipArchiveEntry> entries = archiveFile.getEntries();
							while (entries.hasMoreElements()) {
								ZipArchiveEntry zipEntry = entries.nextElement();
								if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".xml")) { //$NON-NLS-1$
									xml = new String(archiveFile.getInputStream(zipEntry).readAllBytes());
									break;
								}
							}
							
						}
						try(InputStream is = new ByteArrayInputStream(xml.getBytes())){
							Profile im = pp.readXmlFile(is);
							//todo get best name for current language
							ProfileFile pf = new ProfileFile(im.getNames().get(0).getValue(), item);
							profiles.add(pf);
						}
					}catch (Exception ex) {
						Intelligence2PlugIn.log(ex.getMessage(), ex);
					}
					
				}	
				
				Display.getDefault().asyncExec(()->{
					cmbTemplates.setInput(profiles);
					if (!profiles.isEmpty()) {
						cmbTemplates.setSelection(new StructuredSelection(profiles.get(0)));
					}
				});
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	class ProfileFile{
		URL url;
		String name;
		
		public ProfileFile(String name, URL url) {
			this.name = name;
			this.url = url;
		}
	}
}

