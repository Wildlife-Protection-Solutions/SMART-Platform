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
package org.wcs.smart.i2.ui.record.importer;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.xml.RecordXmlImporter;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog box for importing records from xml files.
 * 
 * @author Emily
 *
 */
public class ImportRecordXmlDialog extends TitleAreaDialog {

	private List<Path> files;
	private ListViewer lstViewer ;
	
	@Inject
	private IEclipseContext context;
	
	public ImportRecordXmlDialog(Shell parentShell) {
		super(parentShell);
		files = new ArrayList<>();
	}
	
	
	@Override
	public void createButtonsForButtonBar(Composite parent){
		Button importbtn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.IMPORT_BUTTON_TEXT, true);
		importbtn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void okPressed() {
		final boolean[] close = new boolean[]{false};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.ImportRecordXmlDialog_TaskName, files.size());
					Session session = HibernateManager.openSession(new AttachmentInterceptor());
					try{
						RecordXmlImporter importer = new RecordXmlImporter(session);
						for (Path p : files){
							importer.importRecord(p, new SubProgressMonitor(monitor, 1));
							if (monitor.isCanceled()){
								return;
							}
						}
						close[0] = importer.finish(context.get(IEventBroker.class));
					}finally{
						session.close();
					}
					monitor.done();
				}
			});
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(Messages.ImportRecordXmlDialog_ErrorMsg + ex.getMessage(), ex);
			return;
		}
		if (close[0]) super.okPressed();
	}
	
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite main = (Composite) super.createDialogArea(parent);
		
		Composite body = new Composite(main, SWT.NONE);
		body.setLayout(new GridLayout(3, false));
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(body, SWT.NONE);
		l.setText(Messages.ImportRecordXmlDialog_FileLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		lstViewer = new ListViewer(body, SWT.BORDER);
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		lstViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Path){
					return element.toString();
				}
				return super.getText(element);
			}
		});
		lstViewer.setInput(files);
		
		Composite btnComposite = new Composite(body, SWT.NONE);
		btnComposite.setLayout(new GridLayout());
		((GridLayout)btnComposite.getLayout()).marginHeight = 0;
		((GridLayout)btnComposite.getLayout()).marginWidth = 0;
		btnComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnAdd = new Button(btnComposite, SWT.NONE);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnAdd.addListener(SWT.Selection,  e-> addFile());
		
		Button btnDelete = new Button(btnComposite, SWT.NONE);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnDelete.addListener(SWT.Selection,  e-> removeFiles());
		
		setTitle(Messages.ImportRecordXmlDialog_Title);
		setMessage(Messages.ImportRecordXmlDialog_Message);
		getShell().setText(Messages.ImportRecordXmlDialog_Title);
		
		return main;
	}


	private void addFile() {
		FileDialog fd = new FileDialog(getShell(), SWT.MULTI);
		fd.setFilterExtensions(new String[]{"*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[]{Messages.ImportRecordXmlDialog_allFiles, Messages.ImportRecordXmlDialog_zipFiles});
		if (fd.open() != null){
			Path root = Paths.get(fd.getFilterPath());
			for (String f : fd.getFileNames()){
				Path file = root.resolve(f);
				if (!files.contains(file)) files.add(file);
			}
			lstViewer.refresh();
		}
		getButton(IDialogConstants.OK_ID).setEnabled(!files.isEmpty());
	}
	
	private void removeFiles() {
		List<Path> toRemove = new ArrayList<Path>();
		
		for (Iterator<?> iterator = ((IStructuredSelection)lstViewer.getSelection()).iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			if (item instanceof Path) toRemove.add((Path)item);
		}
		files.removeAll(toRemove);
		lstViewer.refresh();
		getButton(IDialogConstants.OK_ID).setEnabled(!files.isEmpty());
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

}
