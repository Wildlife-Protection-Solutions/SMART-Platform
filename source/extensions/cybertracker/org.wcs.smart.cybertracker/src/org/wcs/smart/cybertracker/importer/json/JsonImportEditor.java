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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.importer.json.JsonFileProcessor.FileState;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Editor for displaying and managing 
 * JSON files imported locally.
 * 
 * @author Emily
 *
 */
public class JsonImportEditor extends EditorPart {

	public static final String ID = "org.wcs.smart.cybertracker.importer.json.ImportEditor"; //$NON-NLS-1$

	private FormToolkit toolkit;
	
	private List<FileWrapper> allFiles;
	private TableViewer filesTable;
	
	private Composite detailsSection;
	private Text txtDetails;
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		Composite spacer = toolkit.createComposite(parent);
		spacer.setLayout(new GridLayout());
		
		Form form = toolkit.createForm(spacer);
		form.setText(Messages.JsonImportEditor_FormText);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		form.getBody().setLayout(layout);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite main = toolkit.createComposite(form.getBody());
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite btnComp = toolkit.createComposite(main);
		btnComp.setLayout(new GridLayout());
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)btnComp.getLayout()).marginWidth = 0;
		((GridLayout)btnComp.getLayout()).marginHeight = 0;
		
		Button btnAddFiles = toolkit.createButton(btnComp, Messages.JsonImportEditor_ImportButton1, SWT.PUSH);
		btnAddFiles.addListener(SWT.Selection, e->{
			ImportDialog dialog = new ImportDialog(getSite().getShell(), this);
			dialog.open();
		});
		
		SashForm sash = new SashForm(main, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		filesTable = new TableViewer(sash, SWT.FULL_SELECTION | SWT.BORDER);
		filesTable.setContentProvider(ArrayContentProvider.getInstance());
		filesTable.getTable().setHeaderVisible(true);
		filesTable.getTable().setLinesVisible(true);
		
		TableViewerColumn stateCol = new TableViewerColumn(filesTable,  SWT.NONE);
		stateCol.getColumn().setText(Messages.JsonImportEditor_StateColumn);
		stateCol.getColumn().setWidth(50);
		stateCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FileWrapper) {
					FileWrapper fw = (FileWrapper)element;
					if (fw.status == null) return Messages.JsonImportEditor_ProcessingStatus;
					return fw.status.getStatus().name();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn fileCol = new TableViewerColumn(filesTable,  SWT.NONE);
		fileCol.getColumn().setText(Messages.JsonImportEditor_FileColumn);
		fileCol.getColumn().setWidth(200);
		fileCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FileWrapper) return ((FileWrapper)element).file.getFileName().toString();
				return super.getText(element);
			}
		});
		
		
		
		TableViewerColumn messageCol = new TableViewerColumn(filesTable,  SWT.NONE);
		messageCol.getColumn().setText(Messages.JsonImportEditor_MessageColumn);
		messageCol.getColumn().setWidth(200);
		messageCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FileWrapper) {
					FileWrapper fw = (FileWrapper)element;
					if (fw.status == null) return ""; //$NON-NLS-1$
					if (fw.status.getMessage() == null) return ""; //$NON-NLS-1$
					return fw.status.getMessage();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn errorCol = new TableViewerColumn(filesTable,  SWT.NONE);
		errorCol.getColumn().setText(Messages.JsonImportEditor_ErrorColumn);
		errorCol.getColumn().setWidth(200);
		errorCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FileWrapper) {
					FileWrapper fw = (FileWrapper)element;
					if (fw.status == null) return ""; //$NON-NLS-1$
					if (fw.status.getException() == null) return ""; //$NON-NLS-1$
					return fw.status.getException().getMessage();
				}
				return super.getText(element);
			}
		});
		
		Menu tableMenu = new Menu(filesTable.getControl());
		
		MenuItem reProcessItem = new MenuItem(tableMenu, SWT.PUSH);
		reProcessItem.setText(Messages.JsonImportEditor_ReprocessOp);
		reProcessItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		reProcessItem.addListener(SWT.Selection, e->{
			List<Path> toProcess = new ArrayList<>();
			for (Iterator<?> iterator = filesTable.getStructuredSelection().iterator(); iterator.hasNext();) {
				FileWrapper fileWrapper = (FileWrapper) iterator.next();
				toProcess.add(fileWrapper.file);
			}
			processFiles(toProcess);
		});
		new MenuItem(tableMenu, SWT.SEPARATOR);
		MenuItem removeItem = new MenuItem(tableMenu, SWT.PUSH);
		removeItem.setText(DialogConstants.DELETE_BUTTON_TEXT);
		removeItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		removeItem.addListener(SWT.Selection, e->{
			List<FileWrapper> toProcess = new ArrayList<>();
			for (Iterator<?> iterator = filesTable.getStructuredSelection().iterator(); iterator.hasNext();) {
				FileWrapper fileWrapper = (FileWrapper) iterator.next();
				toProcess.add(fileWrapper);
			}
			allFiles.removeAll(toProcess);
			filesTable.refresh();
		});
		MenuItem clearItem = new MenuItem(tableMenu, SWT.PUSH);
		clearItem.setText(Messages.JsonImportEditor_ClearOp);
		clearItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		clearItem.addListener(SWT.Selection, e->{
			allFiles.clear();
			filesTable.refresh();
		});
		
		tableMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				reProcessItem.setEnabled(!filesTable.getSelection().isEmpty());
				removeItem.setEnabled(!filesTable.getSelection().isEmpty());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		filesTable.getControl().setMenu(tableMenu);
		filesTable.addSelectionChangedListener(e->updateDetails());
		
		detailsSection = toolkit.createComposite(sash);
		detailsSection.setLayout(new GridLayout());
		((GridLayout)detailsSection.getLayout()).marginWidth = 0;
		((GridLayout)detailsSection.getLayout()).marginHeight = 0;
		
		txtDetails = toolkit.createText(detailsSection, "", SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		txtDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		sash.setWeights(new int[] {3,1});
		allFiles = new ArrayList<>();
		filesTable.setInput(allFiles);
	}
	
	private void updateDetails() {
		Object x= filesTable.getStructuredSelection().getFirstElement();
		if ( x == null ) {
			txtDetails.setText(""); //$NON-NLS-1$
		}else {
			StringBuilder sb = new StringBuilder();
			FileWrapper fw = (FileWrapper)x;
			sb.append(fw.file.getFileName().toString());
			sb.append("\n\n"); //$NON-NLS-1$
			if (fw.status != null) {
				sb.append(Messages.JsonImportEditor_StatusMessage);
				sb.append(fw.status.status.name());
				sb.append("\n\n"); //$NON-NLS-1$
				sb.append(Messages.JsonImportEditor_MessageLabel);
				sb.append("\n"); //$NON-NLS-1$
				if (fw.status.message != null) {
					sb.append(fw.status.message);
				}
				sb.append("\n\n"); //$NON-NLS-1$
			
				sb.append(Messages.JsonImportEditor_ErrorLabel);
				sb.append("\n"); //$NON-NLS-1$
				if (fw.status.ex != null) {
					sb.append(fw.status.ex.getMessage());				
					sb.append("\n"); //$NON-NLS-1$
					try(ByteArrayOutputStream stream = new ByteArrayOutputStream()){
						PrintStream ps = new PrintStream(stream, true, "utf-8"); //$NON-NLS-1$
						fw.status.ex.printStackTrace(ps);
						sb.append(new String(stream.toByteArray(), StandardCharsets.UTF_8));
						sb.append("\n"); //$NON-NLS-1$
					}catch (Exception ex) {
						
					}
				}
			}
			txtDetails.setText(sb.toString());
		}
	}
	public void processFiles(List<Path> toProcess) {
		for (FileWrapper w : allFiles) {
			if (toProcess.contains(w.file)) {
				w.status = null;
			}
		}
		
		for (Path p : toProcess) {
			FileWrapper pp = new FileWrapper(p);
			if (!allFiles.contains(pp)) allFiles.add(pp);
		}
		filesTable.refresh();
		
		HashMap<Path, FileState> results = (new JsonFileProcessor()).process(toProcess);
		for (FileWrapper w : allFiles) {
			if (results.containsKey(w.file)) {
				w.status = results.get(w.file);
			}
		}
		filesTable.refresh();
		filesTable.getTable().getColumn(0).pack();
		filesTable.getTable().getColumn(1).pack();
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
		}
	}
		
	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void setFocus() {
		filesTable.getControl().setFocus();
	}

	@Override
	public void doSaveAs() {
		//not allowed
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	
	private static class FileWrapper{
		Path file;
		JsonFileProcessor.FileState status;
		
		public FileWrapper(Path file) {
			this.file = file;
			this.status = null;
		}
		
		@Override
		public boolean equals(Object other) {
			if (other == this) return true;
			if (other == null) return false;
			if (getClass() != other.getClass()) return false;
			
			return file.equals(((FileWrapper)other).file);
		}
		
		@Override
		public int hashCode() {
			return file.hashCode();
		}
	}
	
	/**
	 * static editor input - we only have one input
	 */
	public final static IEditorInput INPUT = new IEditorInput() {

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		@Override
		public String getName() {
			return Messages.JsonImportEditor_Name;
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return Messages.JsonImportEditor_Tooltip;
		}
		
		@Override
		public int hashCode() {
			return 0;
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj == INPUT;
		}
	};
}