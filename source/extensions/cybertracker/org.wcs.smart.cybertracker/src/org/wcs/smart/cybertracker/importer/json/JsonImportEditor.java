package org.wcs.smart.cybertracker.importer.json;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.eclipse.swt.widgets.FileDialog;
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
import org.wcs.smart.ui.properties.DialogConstants;

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
		
		Form form = toolkit.createForm(parent);
		form.setText("Import CyberTracker Json Data");
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		
		Composite main = toolkit.createComposite(form.getBody());
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite btnComp = toolkit.createComposite(main);
		btnComp.setLayout(new GridLayout());
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)btnComp.getLayout()).marginWidth = 0;
		((GridLayout)btnComp.getLayout()).marginHeight = 0;
		
		Button btnAddFiles = toolkit.createButton(btnComp, "Import From File...", SWT.PUSH);
		btnAddFiles.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(getSite().getShell(), SWT.OPEN | SWT.MULTI);
			fd.setText("Import Cybertracker JSON Data");
			fd.setFilterExtensions(new String[] {"*.json", "*.*"});
			fd.setFilterNames(new String[] {"JSON Files (*.json)", "All Files (*.*)"});
			if (fd.open() == null) return;
			String path = fd.getFilterPath();
			String[] files = fd.getFileNames();
			
			List<Path> toProcess = new ArrayList<>();
			Path root = Paths.get(path);
			for (String f : files) {
				toProcess.add(root.resolve(f));
			}
			processFiles(toProcess);
		});
		
		SashForm sash = new SashForm(main, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		filesTable = new TableViewer(sash, SWT.FULL_SELECTION | SWT.BORDER);
//		filesTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filesTable.setContentProvider(ArrayContentProvider.getInstance());
		filesTable.getTable().setHeaderVisible(true);
		filesTable.getTable().setLinesVisible(true);
		
		TableViewerColumn stateCol = new TableViewerColumn(filesTable,  SWT.NONE);
		stateCol.getColumn().setText("State");
		stateCol.getColumn().setWidth(50);
		stateCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FileWrapper) {
					FileWrapper fw = (FileWrapper)element;
					if (fw.status == null) return "PROCESSING";
					return fw.status.getStatus().name();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn fileCol = new TableViewerColumn(filesTable,  SWT.NONE);
		fileCol.getColumn().setText("File");
		fileCol.getColumn().setWidth(200);
		fileCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FileWrapper) return ((FileWrapper)element).file.getFileName().toString();
				return super.getText(element);
			}
		});
		
		
		
		TableViewerColumn messageCol = new TableViewerColumn(filesTable,  SWT.NONE);
		messageCol.getColumn().setText("Message");
		messageCol.getColumn().setWidth(200);
		messageCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FileWrapper) {
					FileWrapper fw = (FileWrapper)element;
					if (fw.status == null) return "";
					if (fw.status.getMessage() == null) return "";
					return fw.status.getMessage();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn errorCol = new TableViewerColumn(filesTable,  SWT.NONE);
		errorCol.getColumn().setText("Error");
		errorCol.getColumn().setWidth(200);
		errorCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FileWrapper) {
					FileWrapper fw = (FileWrapper)element;
					if (fw.status == null) return "";
					if (fw.status.getException() == null) return "";
					return fw.status.getException().getMessage();
				}
				return super.getText(element);
			}
		});
		
		Menu tableMenu = new Menu(filesTable.getControl());
		
		MenuItem reProcessItem = new MenuItem(tableMenu, SWT.PUSH);
		reProcessItem.setText("Reprocess File");
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
		clearItem.setText("Clear Table");
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
		
		txtDetails = toolkit.createText(detailsSection, "", SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		txtDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		sash.setWeights(new int[] {3,1});
		allFiles = new ArrayList<>();
		filesTable.setInput(allFiles);
	}
	
	private void updateDetails() {
		Object x= filesTable.getStructuredSelection().getFirstElement();
		if ( x == null ) {
			txtDetails.setText("");
		}else {
			StringBuilder sb = new StringBuilder();
			FileWrapper fw = (FileWrapper)x;
			sb.append(fw.file.getFileName().toString());
			sb.append("\n\n");
			if (fw.status != null) {
				sb.append("STATUS: ");
				sb.append(fw.status.status.name());
				sb.append("\n\n");
				sb.append("MESSAGE:\n");
				if (fw.status.message != null) {
					sb.append(fw.status.message);
				}
				sb.append("\n\n");
			
				sb.append("ERROR:\n");
				if (fw.status.ex != null) {
					sb.append(fw.status.ex.getMessage());				
					sb.append("\n");
					try(ByteArrayOutputStream stream = new ByteArrayOutputStream()){
						PrintStream ps = new PrintStream(stream, true, "utf-8");
						fw.status.ex.printStackTrace(ps);
						sb.append(new String(stream.toByteArray(), StandardCharsets.UTF_8));
						sb.append("\n");
					}catch (Exception ex) {
						
					}
				}
			}
			txtDetails.setText(sb.toString());
		}
	}
	public void processFiles(List<Path> toProcess) {
		for (FileWrapper w : allFiles) {
			if (toProcess.contains(w)) {
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
			return "Import CyberTracker JSON Data";
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return "Import JSON Data from files";
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