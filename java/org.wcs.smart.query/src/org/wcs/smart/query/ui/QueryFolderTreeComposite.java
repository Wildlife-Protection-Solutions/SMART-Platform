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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.query.IQueryFolderListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.querylist.AddFolderHandler;
import org.wcs.smart.query.ui.querylist.FolderNameCellEditor;
import org.wcs.smart.query.ui.querylist.QueryListLabelProvider;
import org.wcs.smart.query.ui.querylist.QueryListViewContentProvider;
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
		setLayout(new GridLayout(1, false));
		
		tblViewer = new TreeViewer(this, SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd.heightHint = 300;
		tblViewer.getTree().setLayoutData(gd);
		tblViewer.setContentProvider(new QueryListViewContentProvider(false));
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
		tblViewer.setColumnProperties(new String[] { "col1" });
		tblViewer.setCellModifier(new FolderNameCellEditor(tblViewer));
		
		new TreeViewerFocusCellManager(tblViewer, new FocusCellOwnerDrawHighlighter(tblViewer));
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tblViewer) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
						return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION ||
								event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TreeViewerEditor.create(tblViewer, actSupport, ColumnViewerEditor.DEFAULT);

		
		btnAddFolder = new Button(this, SWT.NONE);
		btnAddFolder.setText("Create New Folder");
		btnAddFolder.setEnabled(false);
		btnAddFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!tblViewer.getSelection().isEmpty()){
					QueryFolder selectedFolder = (QueryFolder) ((IStructuredSelection)tblViewer.getSelection()).getFirstElement();
					QueryFolder newFolder = AddFolderHandler.addQueryFolder(selectedFolder);
					if (newFolder != null){
						QueryEventManager.getInstance().fireFolderChangedListeners(IQueryFolderListener.FOLDER_ADDED, newFolder);
					}
					tblViewer.refresh();
					
					tblViewer.editElement(newFolder, 0);
				}
			}
		});
		
		HashMap<Integer, List<QueryFolder>> data = new HashMap<Integer, List<QueryFolder>> ();
		if (includeSharedFolders){
			data.put(QueryListViewContentProvider.FOLDER_KEY, SavedQueryTree.getInstance().getFolders());
		}else{
			//remove shared folders
			List<QueryFolder> folders = new ArrayList<QueryFolder>();
			for (QueryFolder folder : SavedQueryTree.getInstance().getFolders()){
				if (folder.getEmployee() != null){
					folders.add(folder);
				}
			}
			data.put(QueryListViewContentProvider.FOLDER_KEY, folders);
		}
		tblViewer.setInput(data);
		
		tblViewer.refresh();
		tblViewer.expandToLevel(2);
		
	}

}
