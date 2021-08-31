/**
 * 
 */
package org.wcs.smart.cybertracker.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.util.SmartFileUtils;

/**
 * Dialog for importing CyberTracker data from file.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class CyberTrackerFileImportDialog extends SmartStyledTitleDialog {

	private Button btnFromStorage;
	private Button btnFromFile;

	private TableViewer storageViewer;
	
	private Text txtFile;
	private Button btnBrowse;	

	private Label lblFile;
	
	private Path[] files;

	private enum StorageColumn {
		FILEDATE (Messages.CyberTrackerFileImportDialog_Column_Date),
		FILENAME(Messages.CyberTrackerFileImportDialog_Column_Filename);
		
		private String name;
		
		StorageColumn(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
	};
	
	public CyberTrackerFileImportDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		
		Composite container = new Composite(main, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Label lblOp = new Label(container, SWT.NONE);
		lblOp.setText(Messages.CyberTrackerFileImportDialog_ImportFrom);
		lblOp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite opComp = new Composite(container, SWT.NONE);
		opComp.setLayout(new GridLayout(2, false));
		opComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)opComp.getLayout()).marginWidth = 0; 
		((GridLayout)opComp.getLayout()).marginHeight = 0;
		
		btnFromStorage = new Button(opComp, SWT.RADIO);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER,true, false);
		gd.horizontalIndent = 10;
		btnFromStorage.setLayoutData(gd);
		btnFromStorage.setSelection(true);
		btnFromStorage.setText(Messages.CyberTrackerFileImportDialog_ArchiveFile);
		btnFromStorage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnFromStorage.getSelection()) {
					btnFromFile.setSelection(false);
					importOptionChanged();
				}
			}
		});

		Link lnkOp = new Link(opComp, SWT.NONE);
		lnkOp.setText("<a>" + Messages.CyberTrackerFileImportDialog_OpenArchiveFolderOp + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnkOp.addListener(SWT.Selection,e->{
			
			try {
				Path f = ICyberTrackerConstants.getStorageFolder(SmartDB.getCurrentConservationArea());
				SmartFileUtils.openFileBrowser(f);
			} catch (IOException e1) {
				CyberTrackerPlugIn.log(e1.getMessage(),  e1);
			}
		});
		lnkOp.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		storageViewer = new TableViewer(container, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		GridData sgd = new GridData(SWT.FILL, SWT.FILL, true, true);
		sgd.horizontalIndent = 25;
		sgd.heightHint = 250;
		storageViewer.getControl().setLayoutData(sgd);
		
		storageViewer.getTable().setHeaderVisible(true);
		storageViewer.getTable().setLinesVisible(true);
		final StorageViewerSorter sorter = new StorageViewerSorter();

		final TableViewerColumn colDate = new TableViewerColumn(storageViewer, SWT.NONE);
		colDate.getColumn().setWidth(150);
		colDate.getColumn().setText(StorageColumn.FILEDATE.getName());
		colDate.setLabelProvider(new StorageFileDateLabelProvider());
		colDate.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				sorter.setSortColumn(StorageColumn.FILEDATE, colDate);
			}
		});	
		
		final TableViewerColumn colFirstName = new TableViewerColumn(storageViewer, SWT.NONE);
		colFirstName.getColumn().setWidth(300);
		colFirstName.getColumn().setText(StorageColumn.FILENAME.getName());
		colFirstName.setLabelProvider(new StorageFileNameLabelProvider());
		colFirstName.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				sorter.setSortColumn(StorageColumn.FILENAME, colFirstName);
			}
		});	

		storageViewer.setContentProvider(ArrayContentProvider.getInstance());
		storageViewer.setInput(getStorageFiles());
		storageViewer.setComparator(sorter);
		storageViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				importOptionChanged();
			}
		});

		//by default sort by date desc
		sorter.setSortColumn(StorageColumn.FILEDATE, colDate);
		sorter.setSortColumn(StorageColumn.FILEDATE, colDate);
		
		
		btnFromFile = new Button(container, SWT.RADIO);
		btnFromFile.setSelection(false);
		gd = new GridData(SWT.FILL, SWT.CENTER,true, false);
		gd.horizontalIndent = 10;
		btnFromFile.setLayoutData(gd);
		btnFromFile.setText(Messages.CyberTrackerFileImportDialog_OtherFile);
		btnFromFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnFromFile.getSelection()) {
					btnFromStorage.setSelection(false);
					importOptionChanged();
				}
				
			}
		});

		Composite toFileCmp = new Composite(container, SWT.NONE);
		GridLayout fileCmpLayout = new GridLayout(1, false);
		fileCmpLayout.marginLeft = 15;
		toFileCmp.setLayout(fileCmpLayout);
		toFileCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite fileCmp = new Composite(toFileCmp, SWT.NONE);
		fileCmp.setLayout(new GridLayout(3, false));
		fileCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblFile = new Label(fileCmp, SWT.NONE);
		lblFile.setText(Messages.CyberTrackerExportDialog_Label_File);
		lblFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		txtFile = new Text(fileCmp, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		txtFile.setEditable(false);
		txtFile.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (txtFile.getText() != null && !txtFile.getText().isEmpty()) {
					if (getButton(IDialogConstants.OK_ID) != null) {
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}
				}
			}
		});
		
		btnBrowse = new Button(fileCmp, SWT.NONE);
		btnBrowse.setText(Messages.CyberTrackerExportDialog_Button_Browse);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				files = selectFile();
				if (files != null) {
					StringBuffer text = new StringBuffer();
					for (int i = 0; i < files.length; i++) {
						text.append(files[i].toString());
						if (i+1 < files.length)
							text.append(";"); //$NON-NLS-1$
					}
					txtFile.setText(text.toString());
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		

		importOptionChanged();
		
		setTitle(Messages.CyberTrackerFileImportDialog_Title);
		setMessage(Messages.CyberTrackerFileImportDialog_Message);
		super.getShell().setText(Messages.CyberTrackerFileImportDialog_Title);
		super.setTitleImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.CT_WIZARD_BANNER));
		return composite;
	}

	private Path[] getStorageFiles() {
		Path storageFolder = ICyberTrackerConstants.getStorageFolder(SmartDB.getCurrentConservationArea());
		
		try (Stream<Path> stream = Files.list(storageFolder)){
			List<Path> items = stream.collect(Collectors.toList());
			return items.toArray(new Path[items.size()]);
		}catch (IOException ex) {
			CyberTrackerPlugIn.log(ex.getMessage(), ex);
			return new Path[] {};
		}
	}

	protected void importOptionChanged() {
		lblFile.setEnabled(btnFromFile.getSelection());
		txtFile.setEnabled(btnFromFile.getSelection());
		btnBrowse.setEnabled(btnFromFile.getSelection());
		storageViewer.getControl().setEnabled(btnFromStorage.getSelection());
		Button importBtn = getButton(IDialogConstants.OK_ID);
		if (importBtn != null)
			importBtn.setEnabled((btnFromStorage.getSelection() && !storageViewer.getSelection().isEmpty()) || (btnFromFile.getSelection() && txtFile.getText() != null && !txtFile.getText().isEmpty()));
	}

	private Path[] selectFile() {
		FileDialog fd = new FileDialog(getShell(), SWT.MULTI | SWT.OPEN);
		fd.setFilterExtensions(new String[] {
				"*.xml;*.ctx", //$NON-NLS-1$
				"*.xml", //$NON-NLS-1$
				"*.ctx", //$NON-NLS-1$
				"*.*" //$NON-NLS-1$
		});
		fd.setFilterNames(new String[] {
				Messages.CTPatrolTableContainer_SupportedFiles,
				Messages.CyberTrackerImportDialog_XmlFiles,
				Messages.CTPatrolTableContainer_CyberTrackerFiles,
				Messages.CyberTrackerImportDialog_AllFiles
		});
		String f = fd.open();
		
		if (f != null) {
			Path[] files = new Path[fd.getFileNames().length];
			for (int i = 0; i < fd.getFileNames().length; i ++){
				files[i] = Paths.get(fd.getFilterPath()).resolve(fd.getFileNames()[i]);
			}
			return files;
		}
		return null;
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, Messages.CyberTrackerFileImportDialog_Button_Import, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setFocus();		
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (btnFromStorage.getSelection()) {
				IStructuredSelection selection = (IStructuredSelection)storageViewer.getSelection();
				List<Path> fList = new ArrayList<>();
				for (Iterator<?> i = selection.iterator(); i.hasNext();) {
					Path f = (Path) i.next();
					fList.add(f);
				}
				files = fList.toArray(new Path[selection.size()]);
			} else if (btnFromFile.getSelection()) {
				//nothing (files were already assigned)
			}
			super.setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}
	
	public Path[] getSelectedFiles() {
		return files;
	}

	private final class StorageViewerSorter extends ViewerComparator {
		StorageColumn column;
		private int direction = SWT.UP;
		
		public void setSortColumn(StorageColumn stColumn, TableViewerColumn cViewer){
			if (stColumn == column){
				if (direction == SWT.DOWN){
					direction = SWT.UP;
				}else{
					direction = SWT.DOWN;
				}
			}
			this.column = stColumn;
			storageViewer.getTable().setSortDirection(direction);
			storageViewer.getTable().setSortColumn(cViewer.getColumn());
			storageViewer.refresh();
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (column == null)
				return super.compare(viewer, e1, e2);
			if (e1 instanceof Path && e2 instanceof Path) {
				Path f1 = (Path) e1;
				Path f2 = (Path) e2;
				switch (column) {
				case FILENAME:
					if (direction == SWT.UP) {
						return super.compare(viewer, f1.getFileName().toString(), f2.getFileName().toString());
					} else {
						return -super.compare(viewer, f1.getFileName().toString(), f2.getFileName().toString());
					}
				case FILEDATE:
					try {
						if (direction == SWT.UP) {
							return super.compare(viewer, Files.getLastModifiedTime(f1).toMillis(), Files.getLastModifiedTime(f2).toMillis());
						} else {
							return -super.compare(viewer, Files.getLastModifiedTime(f1).toMillis(), Files.getLastModifiedTime(f2).toMillis());
						}
					}catch (IOException ex) { return 0; }
				default:
					break;
				}
			}
			return super.compare(viewer, e1, e2);
		}
	}
	
	private final class StorageFileNameLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Path) {
				Path file = (Path) element;
				return file.getFileName().toString();
			}
			return super.getText(element);
		}
	}

	private final class StorageFileDateLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Path) {
				Path file = (Path) element;
				try {
					return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format( Files.getLastModifiedTime(file).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() );
				}catch(IOException ex) {
					return ""; //$NON-NLS-1$
				}
			}
			return super.getText(element);
		}
	}
	
}
