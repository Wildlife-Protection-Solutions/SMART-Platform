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
package org.wcs.smart.cybertracker.importer.json;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.json.simple.JSONObject;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.MobileDeviceUtils;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.json.CtJsonObservationParser;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartFileUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for importing new CyberTracker data.
 * 
 * @author Emily
 *
 */
public class ImportDialog extends SmartStyledTitleDialog{

	private Button opDevice, opFile, opArchive;
	private ListViewer lstFiles;
	private ListViewer lstArchive;
	
	private JsonImportEditor editor;
	
	public ImportDialog(Shell parentShell, JsonImportEditor editor) {
		super(parentShell);
		this.editor = editor;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.IMPORT_BUTTON_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setFocus();		
		super.setReturnCode(IDialogConstants.CANCEL_ID);
		
		validate();
	}
	
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		p.x = (int)(p.x * 1.2);
		p.y = p.y + 2;
		return p;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		parent = (Composite) super.createDialogArea(parent);
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(c, SWT.NONE);
		l.setText(Messages.ImportDialog_ImportFromLbl);
		
		Composite ll = new Composite(c, SWT.NONE);
		ll.setLayout(new GridLayout(3, false));
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		opDevice = new Button(ll, SWT.RADIO);
		opDevice.setText(Messages.ImportDialog_DeviceOption);
		
		opFile = new Button(ll, SWT.RADIO);
		opFile.setText(Messages.ImportDialog_FilesOption);
		
		opArchive = new Button(ll, SWT.RADIO);
		opArchive.setText(Messages.ImportDialog_ArchiveOption);
		
		new Label(c, SWT.NONE);
		
		Composite stack = new Composite(c, SWT.NONE);
		stack.setLayout(new StackLayout());
		((StackLayout)stack.getLayout()).marginHeight = 0;
		((StackLayout)stack.getLayout()).marginWidth = 0;
		stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite emptyStack = new Composite(stack, SWT.NONE);
		emptyStack.setLayout(new GridLayout());
		((GridLayout)emptyStack.getLayout()).marginWidth = 0;
		((GridLayout)emptyStack.getLayout()).marginHeight = 0;
		
		l = new Label(emptyStack, SWT.WRAP);
		l.setText(MessageFormat.format(Messages.ImportDialog_DeviceMsg, MobileDeviceUtils.DATA_FOLDER));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 200;
		
		Composite fileStack = new Composite(stack, SWT.NONE);
		fileStack.setLayout(new GridLayout(3, false));
		((GridLayout)fileStack.getLayout()).marginWidth = 0;
		((GridLayout)fileStack.getLayout()).marginHeight = 0;
		
		lstFiles = new ListViewer(fileStack, SWT.BORDER | SWT.V_SCROLL);
		lstFiles.setContentProvider(ArrayContentProvider.getInstance());
		lstFiles.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstFiles.getControl().getLayoutData()).heightHint = 150;
		lstFiles.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((Path)element).getFileName().toString(); // + " [" + ((Path)element).getParent().toString() + "]";
			}
		});
		lstFiles.setInput(new ArrayList<>());
		
		Composite btns = new Composite(fileStack, SWT.NONE);
		btns.setLayout(new GridLayout());
		((GridLayout)btns.getLayout()).marginHeight = 0;
		((GridLayout)btns.getLayout()).marginWidth = 0;
		btns.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnAdd = new Button(btns, SWT.PUSH);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setToolTipText(Messages.ImportDialog_addfiletooltip);
		btnAdd.addListener(SWT.Selection,e->addFile());
		btnAdd.setBackground(btns.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnDelete = new Button(btns, SWT.PUSH);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.setToolTipText(Messages.ImportDialog_deletefiletooltip);
		btnDelete.addListener(SWT.Selection,e->deleteFile());
		btnDelete.setEnabled(false);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setBackground(btns.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnInfo = new Button(btns, SWT.PUSH);
		btnInfo.setImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_INFO));
		btnInfo.setToolTipText(Messages.ImportDialog_fileinfotooltip);
		btnInfo.addListener(SWT.Selection,e->viewDetails((Path)lstFiles.getStructuredSelection().getFirstElement()));
		btnInfo.setEnabled(false);
		btnInfo.setText(Messages.ImportDialog_DetailsBtn);
		btnInfo.setBackground(btns.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Menu m = new Menu(lstFiles.getControl());
		MenuItem addFile = new MenuItem(m, SWT.PUSH);
		addFile.setText(Messages.ImportDialog_addFileMenu);
		addFile.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addFile.addListener(SWT.Selection, e->addFile());
		MenuItem deleteFile = new MenuItem(m, SWT.PUSH);
		deleteFile.setText(Messages.ImportDialog_deleteFileMenu);
		deleteFile.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteFile.addListener(SWT.Selection, e->deleteFile());
		deleteFile.setEnabled(false);
		MenuItem fileDetails = new MenuItem(m, SWT.PUSH);
		fileDetails.setText(Messages.ImportDialog_FileInfoMenu);
		fileDetails.setImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_INFO));
		fileDetails.addListener(SWT.Selection, e->{
			viewDetails((Path)lstFiles.getStructuredSelection().getFirstElement());
		});
		lstFiles.getControl().setMenu(m);
		
		lstFiles.addSelectionChangedListener(e->{
			btnDelete.setEnabled(!lstFiles.getSelection().isEmpty());
			deleteFile.setEnabled(!lstFiles.getSelection().isEmpty());
			fileDetails.setEnabled(!lstFiles.getSelection().isEmpty());
			btnInfo.setEnabled(!lstFiles.getSelection().isEmpty());
		});

		
		Composite archiveStack = new Composite(stack, SWT.NONE);
		archiveStack.setLayout(new GridLayout(2, false));
		((GridLayout)archiveStack.getLayout()).marginWidth = 0;
		((GridLayout)archiveStack.getLayout()).marginHeight = 0;
		
		lstArchive = new ListViewer(archiveStack, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		lstArchive.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstArchive.getControl().getLayoutData()).heightHint = 150;
		lstArchive.setContentProvider(ArrayContentProvider.getInstance());
		lstArchive.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				StringBuilder sb = new StringBuilder();
				sb.append(((Path)element).getFileName().toString());
				try {
					
					sb.append(" ["); //$NON-NLS-1$
					sb.append(LocalDateTime.ofInstant(Files.getLastModifiedTime((Path)element).toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
					sb.append(" ]"); //$NON-NLS-1$
				}catch (Exception ex) {
					ex.printStackTrace();
				}
				return sb.toString();
			}
		});
		Path storageFolder = ICyberTrackerConstants.getStorageFolder(SmartDB.getCurrentConservationArea());
		
		List<Path> allFiles = new ArrayList<>();
		try {
			if (Files.exists(storageFolder)) {
				try(Stream<Path> stream = Files.walk(storageFolder)){
					stream.filter(e->Files.isRegularFile(e)).forEach(p->allFiles.add(p));
				}
			}
		} catch (IOException e1) {
			CyberTrackerPlugIn.log(e1.getMessage(), e1);
		}
		lstArchive.setInput(allFiles);
		
		
		Menu mArchive = new Menu(lstArchive.getControl());
		MenuItem mFileDetails = new MenuItem(mArchive, SWT.PUSH);
		mFileDetails.setText(Messages.ImportDialog_FileInfoMenu);
		mFileDetails.setImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_INFO));
		mFileDetails.addListener(SWT.Selection, e->{
			viewDetails((Path)lstArchive.getStructuredSelection().getFirstElement());
		});
		lstArchive.getControl().setMenu(mArchive);
		mFileDetails.setEnabled(false);
		
		ToolBar tbArchive = new ToolBar(archiveStack, SWT.VERTICAL | SWT.FLAT );
		tbArchive.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		
		ToolItem btnInfoArchive = new ToolItem(tbArchive, SWT.PUSH);
		btnInfoArchive.setImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_INFO));
		btnInfoArchive.setToolTipText(Messages.ImportDialog_fileinfotooltip);
		btnInfoArchive.addListener(SWT.Selection,e->viewDetails((Path)lstArchive.getStructuredSelection().getFirstElement()));
		btnInfoArchive.setEnabled(false);
		
		lstArchive.addSelectionChangedListener(e->{
			mFileDetails.setEnabled(!lstArchive.getSelection().isEmpty());
			btnInfoArchive.setEnabled(!lstArchive.getSelection().isEmpty());
		});

		
		Link lnkOp = new Link(archiveStack, SWT.NONE);
		lnkOp.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		lnkOp.setText("<a>" + Messages.CyberTrackerFileImportDialog_OpenArchiveFolderOp + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnkOp.addListener(SWT.Selection,e->{
			try {
				Path f = ICyberTrackerConstants.getStorageFolder(SmartDB.getCurrentConservationArea());
				SmartFileUtils.openFileBrowser(f);
			} catch (IOException e1) {
				CyberTrackerPlugIn.log(e1.getMessage(),  e1);
			}
		});
		
		
		opDevice.setSelection(true);
		((StackLayout)stack.getLayout()).topControl = emptyStack;
		stack.layout(true);
		
		opDevice.addListener(SWT.Selection, e->{
			((StackLayout)stack.getLayout()).topControl = emptyStack;
			stack.layout(true);
			validate();
		});
		
		opFile.addListener(SWT.Selection, e->{
			((StackLayout)stack.getLayout()).topControl = fileStack;
			stack.layout(true);
			validate();
		});
		
		opArchive.addListener(SWT.Selection, e->{
			((StackLayout)stack.getLayout()).topControl = archiveStack;
			stack.layout(true);
			validate();
		});
		
		getShell().setText(Messages.ImportDialog_ShellText1);
		setTitle(Messages.ImportDialog_DialogTitle1);
		setMessage(Messages.ImportDialog_DialogMessage1);
		return parent;
	}

	@SuppressWarnings("unchecked")
	private void addFile() {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
		fd.setFilterExtensions(new String[] {"*.json", "*.zlib", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		fd.setFilterNames(new String[] {Messages.ImportDialog_JSONFile, Messages.ImportDialog_CompressedJSON, Messages.ImportDialog_AllFiles});
		
		if (fd.open() == null) return;
		List<Path> items = ((List<Path>)lstFiles.getInput());
		for (String s : fd.getFileNames()) {
			Path p = Paths.get(fd.getFilterPath()).resolve(s);
			if (p != null && !items.contains(p)) items.add(p);
		}			
		lstFiles.refresh();
		validate();
	}
	
	@SuppressWarnings("unchecked")
	private void deleteFile() {		
		List<Path> items = ((List<Path>)lstFiles.getInput());
		List<Path> r = new ArrayList<>();
		for (Iterator<Path> iterator = lstFiles.getStructuredSelection().iterator(); iterator.hasNext();) {
			Path path = (Path) iterator.next();
			r.add((Path)path);
		}
		items.removeAll(r);
		lstFiles.refresh();
		validate();	
	}
	
	@Override
	public void okPressed() {
		List<Path> files = new ArrayList<>();
		if ( opArchive.getSelection()) {
			
			IStructuredSelection sel = lstArchive.getStructuredSelection();
			for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
				Object o = (Object) iterator.next();
				if (o instanceof Path) files.add((Path)o);
			}
		}else if (opFile.getSelection()) {
			List<?> items = (List<?>) lstFiles.getInput();
			for (Object x : items) {
				if (x instanceof Path) files.add((Path)x);
			}
			
		}else {
			//import from device
			//create a temp dir for flies
			Path importPath = null;
			try {
				Path p = SmartContext.INSTANCE.getTempFilestoreLocation();
				importPath = Files.createTempDirectory(p, "ctimport"); //$NON-NLS-1$
			}catch (Exception ex) {
				CyberTrackerPlugIn.displayError(Messages.ImportDialog_Error, Messages.ImportDialog_CannotCreateFile + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				return;
			}

			//import from device
			try {
				MobileDeviceUtils.importFromDevice(importPath);
			}catch (Exception ex) {
				CyberTrackerPlugIn.displayError(Messages.ImportDialog_Error, Messages.ImportDialog_DeviceImportError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			}

			//move files to archive folder
			List<Path> importedFiles = new ArrayList<>();
			try (Stream<Path> stream = Files.list(importPath)){
				stream.forEach(fp->importedFiles.add(fp));
			}catch (Exception ex) {
				CyberTrackerPlugIn.displayError(Messages.ImportDialog_Error, MessageFormat.format(Messages.ImportDialog_ReadError, importPath.toString(), ex.getMessage()), ex);
				return;
			}
			if (importedFiles.isEmpty()) {
				MessageDialog.openWarning(getShell(), Messages.ImportDialog_ImportTitle, Messages.ImportDialog_NoData);
				if (Files.exists(importPath)) {
					try {
						SmartUtils.deleteDirectory(importPath);
					}catch (Exception e) {
						CyberTrackerPlugIn.log(e.getMessage(), e);
					}
				}
				return;
			}
			
			Path archiveFolder = ICyberTrackerConstants.getStorageFolder(SmartDB.getCurrentConservationArea());
			if (!Files.exists(archiveFolder)) {
				try {
					Files.createDirectories(archiveFolder);
				} catch (IOException e) {
					CyberTrackerPlugIn.displayError(Messages.ImportDialog_Error, e.getMessage(), e);
					e.printStackTrace();
				}
			}
			List<Path> filesInArchive = new ArrayList<>();
			boolean error = false;
			for (Path fp : importedFiles) {
				Path target = archiveFolder.resolve(fp.getFileName().toString());
				try {
					Files.move(fp, target, StandardCopyOption.REPLACE_EXISTING);
					filesInArchive.add(target);
				}catch (Exception ex) {
					CyberTrackerPlugIn.displayError(Messages.ImportDialog_Error, MessageFormat.format(Messages.ImportDialog_CopyError, fp.toString()), ex);
					error = true;
				}
			}
			files.addAll(filesInArchive);
			

			if (!error) {
				//delete temp directory (otherwise there is likely data in it)
				try {
					SmartUtils.deleteDirectory(importPath);
				}catch (Exception e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
				}
			}
		}
		
		//open editor and import files
		try {
			editor.processFiles(files);
			super.okPressed();
		} catch (Throwable t) {
			SmartPlugIn.displayLog(t.getLocalizedMessage(), t);
		}
	}
	
	private void validate() {
		String error = null;
		if (opDevice.getSelection()) {
			
		}else if (opFile.getSelection()) {
			if (((List<?>)lstFiles.getInput()).isEmpty()) {
				error = Messages.ImportDialog_FileRequired;
			}
		}else if (opArchive.getSelection()) {
			
		}
		
		setErrorMessage(error);
		getButton(IDialogConstants.OK_ID).setEnabled(error == null);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	private void viewDetails(Path p) {
		//TODO: only works on JSON files; not compressed json
		String json = ""; //$NON-NLS-1$
		final List<JSONObject> features = new ArrayList<>();
		
		int incidentcnt = 0;
		String startDateTime = null;
		String endDateTime = null;
		
		try(Reader in = Files.newBufferedReader(p, StandardCharsets.UTF_8)){
			json = IOUtils.toString(in);
			features.addAll(CtJsonUtil.parseFeaturesFromJsonString(json, Locale.getDefault()));
			
			for (int i = 0; i < features.size(); i ++) {
				JSONObject f = (JSONObject)features.get(i);
				if (f.get(CtJsonObservationParser.PROPERTIES_KEY) != null && f.get(CtJsonObservationParser.PROPERTIES_KEY) instanceof JSONObject) {
					JSONObject prop = (JSONObject) f.get(CtJsonObservationParser.PROPERTIES_KEY);
					if (startDateTime == null) {
						startDateTime = prop.get(CtJsonObservationParser.DATETIME_KEY).toString();
					}
					if (i == features.size() - 1) {
						endDateTime = prop.get(CtJsonObservationParser.DATETIME_KEY).toString();
					}
					if (prop.containsKey(CtJsonObservationParser.SIGHTINGS_KEY)) {
						incidentcnt++;
					}
				}
			}
			
		}catch (Exception ex) {
			MessageDialog.openInformation(getShell(), Messages.ImportDialog_ContentsTitle, MessageFormat.format(Messages.ImportDialog_ParseErrorMsg + "\n\n{0}", ex.getMessage(), p.getFileName().toString() )); //$NON-NLS-1$
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.ImportDialog_FileSummaryLabel);
		sb.append("\n\n"); //$NON-NLS-1$
		sb.append(Messages.ImportDialog_FileLabel);
		sb.append(p.getFileName().toString());
		sb.append("\n"); //$NON-NLS-1$
		sb.append(Messages.ImportDialog_FirstDateLabel);
		sb.append(startDateTime);
		sb.append("\n"); //$NON-NLS-1$
		sb.append(Messages.ImportDialog_LastDateLabel);
		sb.append(endDateTime);
		sb.append("\n"); //$NON-NLS-1$
		sb.append(Messages.ImportDialog_FeatureCountLabel);
		sb.append(features.size());
		sb.append("\n"); //$NON-NLS-1$
		sb.append(Messages.ImportDialog_ObsCountLabel);
		sb.append(incidentcnt);
		
		MessageDialog.openInformation(getShell(), Messages.ImportDialog_FileContentsTitle, sb.toString());		
	}
}
