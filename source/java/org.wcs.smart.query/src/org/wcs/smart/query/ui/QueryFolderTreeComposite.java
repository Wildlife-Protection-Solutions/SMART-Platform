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
package org.wcs.smart.query.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.querylist.AddFolderHandler;
import org.wcs.smart.query.ui.querylist.NameCellEditor;
import org.wcs.smart.query.ui.querylist.QueryListContentProvider;
import org.wcs.smart.query.ui.querylist.QueryListLabelProvider;
import org.wcs.smart.query.ui.querylist.SavedQueryTree;

/**
 * A composite which contains the list of query folders
 * and allows users to add folders, delete folders and rename
 * folders.  
 *  
 * @author egouge
 * @since 1.0.0
 */
public class QueryFolderTreeComposite extends Composite{

	private Button btnAddFolder;
	private TreeViewer tblViewer;
	private boolean includeSharedFolders;
	
	private IQueryListener folderChanged = new QueryListenerAdapter() {

		@Override
		public void folderModified(int eventType, Object object) {
			getShell().getDisplay().syncExec(new Runnable(){
				@Override
				public void run() {
					tblViewer.refresh();
				}});
		}
		
	};
	/**
	 * 
	 */
	public QueryFolderTreeComposite(Composite parent, boolean inculdeSharedFolders) {
		super(parent, SWT.NONE);
		this.includeSharedFolders = inculdeSharedFolders;
		createComposite();
	}
	
	/**
	 * Adds a selection changed listener to the underlying 
	 * tree.
	 * @param listener
	 */
	public void addSelectionListener(ISelectionChangedListener listener){
		tblViewer.addSelectionChangedListener(listener);
	}
	
	/**
	 * @return the current selection
	 */
	public IStructuredSelection getSelection(){
		return (IStructuredSelection) tblViewer.getSelection();
	}
	private void createComposite(){
		QueryEventManager.getInstance().addListener(folderChanged);
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				QueryEventManager.getInstance().removeListener(folderChanged);
			}
		});
		setLayout(new GridLayout(1, false));
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		tblViewer = new TreeViewer(this, SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd.heightHint = 300;
		tblViewer.getTree().setLayoutData(gd);
		tblViewer.setContentProvider(new QueryListContentProvider(false));
		tblViewer.setLabelProvider(new QueryListLabelProvider());
		tblViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!tblViewer.getSelection().isEmpty()){
					btnAddFolder.setEnabled(true);
				}else{
					btnAddFolder.setEnabled(false);
				}
			}
		});
	
		tblViewer.setCellEditors(new CellEditor[] { new TextCellEditor(tblViewer.getTree()) });
		tblViewer.setColumnProperties(new String[] { "col1" }); //$NON-NLS-1$
		tblViewer.setCellModifier(new NameCellEditor(tblViewer));
		
		new TreeViewerFocusCellManager(tblViewer, new FocusCellOwnerDrawHighlighter(tblViewer));
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tblViewer) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
						return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION ||
								event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TreeViewerEditor.create(tblViewer, actSupport, ColumnViewerEditor.DEFAULT);

		Menu mnu = new Menu(tblViewer.getControl());
		MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setText(Messages.QueryFolderTreeComposite_AddFolderButton);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.setEnabled(false);
		miAdd.addListener(SWT.Selection, e->addFolder());
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				miAdd.setEnabled(!tblViewer.getStructuredSelection().isEmpty());
			}
			@Override
			public void menuHidden(MenuEvent e) {	
			}
		});
		tblViewer.getControl().setMenu(mnu);
		
		btnAddFolder = new Button(this, SWT.NONE);
		btnAddFolder.setText(Messages.QueryFolderTreeComposite_AddFolderButton);
		btnAddFolder.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAddFolder.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAddFolder.setEnabled(false);
		btnAddFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addFolder();
			}
		});
		
		HashMap<Integer, List<QueryFolder>> data = new HashMap<Integer, List<QueryFolder>> ();
		if (includeSharedFolders){
			data.put(QueryListContentProvider.FOLDER_KEY, SavedQueryTree.getInstance().getFolders());
		}else{
			//remove shared folders
			List<QueryFolder> folders = new ArrayList<QueryFolder>();
			for (QueryFolder folder : SavedQueryTree.getInstance().getFolders()){
				if (folder.getEmployee() != null){
					folders.add(folder);
				}
			}
			data.put(QueryListContentProvider.FOLDER_KEY, folders);
		}
		tblViewer.setInput(data);
		
		tblViewer.refresh();
		tblViewer.expandToLevel(2);
		
	}

	private void addFolder() {
		if (!tblViewer.getSelection().isEmpty()){
			QueryFolder selectedFolder = (QueryFolder) tblViewer.getStructuredSelection().getFirstElement();
			QueryFolder newFolder = AddFolderHandler.addQueryFolder(selectedFolder);
			if (newFolder != null){
				QueryEventManager.getInstance().fireFolderAdded(newFolder);
			}
			tblViewer.refresh();
			tblViewer.editElement(newFolder, 0);
		}
	}
}
