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
package org.wcs.smart.query.common.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.ui.image.AttachmentTable;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryEditCommand;
import org.wcs.smart.query.model.IQueryResultInfoProvider;

/**
 * Results page for displaying a list of observations represented
 * by a IPagedImageResultSet query results.
 * 
 * @author Emily
 *
 */
public class QueryResultsImagePage extends EditorPart  implements AttachmentTable.IMenuCreator{
	
	private static final String PREFERENCE_ICONSIZE = "org.wcs.smart.query.imagesize"; //$NON-NLS-1$
	
	private enum IconSize{
		SMALL(100, Messages.QueryResultsImagePage_SmallIconSize), 
		MEDIUM(150, Messages.QueryResultsImagePage_MediumIconSize),
		LARGE(200, Messages.QueryResultsImagePage_LargeIconSize);
		
		int size;
		String label;
		
		IconSize(int size, String label){
			this.size = size;
			this.label = label;
		}
	}
	
	private IconSize iconSize = IconSize.MEDIUM;

	private AttachmentTable imageTable;	
	private Label lblNumImages;
	private Label lblNumSelected;
	private Label iconSizeLabel;
	
	private QueryResultsEditor editor;
	
	public QueryResultsImagePage(QueryResultsEditor parent) {
		super();
		this.editor = parent;
		
		String size = QueryPlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_ICONSIZE);
		if (size != null) {
			try {
				iconSize = IconSize.valueOf(size.toUpperCase(Locale.ROOT));
			}catch (Throwable t) {}
		}
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.setSite(site);
		super.setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private void setIconSize(IconSize newSize){
		this.iconSize = newSize;
		if (imageTable != null) {
			imageTable.setThumbnailSize(iconSize.size);
		}
		iconSizeLabel.setText(newSize.label);
		iconSizeLabel.getParent().getParent().layout(true, true);
		
		QueryPlugIn.getDefault().getPreferenceStore().putValue(PREFERENCE_ICONSIZE, iconSize.name());
	}
	
	
	
	@Override
	public void createPartControl(Composite parent) {

		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		parent.addListener(SWT.Dispose, e->toolkit.dispose());
		
		Composite outer = toolkit.createComposite(parent);
		outer.setLayout(new GridLayout());
		
		//header that contains the number of results, number selected 
		//and the icon size 
		Composite header = toolkit.createComposite(outer, SWT.BORDER);
		header.setLayout(new GridLayout(4, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lblNumImages = toolkit.createLabel(header,""); //$NON-NLS-1$
		lblNumImages.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Label spacer = toolkit.createLabel(header,"", SWT.SEPARATOR | SWT.VERTICAL); //$NON-NLS-1$
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)spacer.getLayoutData()).heightHint = lblNumImages.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		lblNumSelected = toolkit.createLabel(header, ""); //$NON-NLS-1$
		lblNumSelected.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Composite iconSize = toolkit.createComposite(header);
		iconSize.setLayout(new GridLayout(2, false));
		iconSize.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		((GridLayout)iconSize.getLayout()).marginWidth = 0;
		((GridLayout)iconSize.getLayout()).marginHeight = 0;
		
		iconSizeLabel = toolkit.createLabel(iconSize, this.iconSize.label);
		iconSizeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		iconSizeLabel.setToolTipText(Messages.QueryResultsImagePage_changeSizeTooltip);
		
		Button btnDown = toolkit.createButton(iconSize, "", SWT.ARROW | SWT.DOWN); //$NON-NLS-1$
		btnDown.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		btnDown.setToolTipText(Messages.QueryResultsImagePage_changeSizeTooltip);
		Menu mnuIconSize = new Menu(btnDown);
		btnDown.addDisposeListener(e->mnuIconSize.dispose());
		
		MenuItem small = new MenuItem(mnuIconSize, SWT.RADIO);
		MenuItem medium = new MenuItem(mnuIconSize, SWT.RADIO);
		MenuItem large = new MenuItem(mnuIconSize, SWT.RADIO);
		small.setText(IconSize.SMALL.label);
		medium.setText(IconSize.MEDIUM.label);
		large.setText(IconSize.LARGE.label);
		
		if(this.iconSize == IconSize.SMALL){
			small.setSelection(true);
		}else if (this.iconSize == IconSize.MEDIUM){
			medium.setSelection(true);
		}else if (this.iconSize == IconSize.LARGE){
			large.setSelection(true);
		}
		small.addListener(SWT.Selection, e-> {if (small.getSelection()) setIconSize(IconSize.SMALL);});
		medium.addListener(SWT.Selection, e-> {if (medium.getSelection()) setIconSize(IconSize.MEDIUM);});
		large.addListener(SWT.Selection, e-> {if (large.getSelection()) setIconSize(IconSize.LARGE);});
		btnDown.addListener(SWT.MouseDown, e->mnuIconSize.setVisible(true));
		iconSizeLabel.addListener(SWT.MouseDown, e->mnuIconSize.setVisible(true));
		
		//image results table
		imageTable = new AttachmentTable(outer, toolkit, this);
		imageTable.setThumbnailSize(this.iconSize.size);
		imageTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		imageTable.addListener(SWT.Selection, e->{
			lblNumSelected.setText(MessageFormat.format(Messages.QueryResultsImagePage_selectedLabel, imageTable.getSelection().size()));
			lblNumSelected.getParent().getParent().layout(true, true);
		});
	}
	
	public void setResult(IPagedImageResultSet results) {
		imageTable.setResultSet(results);
		Display.getDefault().asyncExec(()->{
			if (results == null) {
				lblNumImages.setText(""); //$NON-NLS-1$
				lblNumSelected.setText(""); //$NON-NLS-1$
			}else {
				lblNumImages.setText(MessageFormat.format(Messages.QueryResultsImagePage_countLabel, results.getImageCount()));
				lblNumSelected.setText(MessageFormat.format(Messages.QueryResultsImagePage_selectedLabel, 0));
			}
			lblNumImages.getParent().layout(true, true);
		});
		
	}
	
	@Override
	public Menu createMenu(Composite parent) {
		Menu thumbMenu = new Menu(parent);
		
		thumbMenu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent evt) {
				for (MenuItem mi : thumbMenu.getItems()) mi.dispose();
				if (editor.getQuery() != null && imageTable.getSelection().size() > 0) {
					IQueryResultInfoProvider[] provider = QueryTypeManager.INSTANCE.findQueryType(editor.getQuery().getTypeKey()).getResultProviders();
					for (IQueryResultInfoProvider p : provider) {
						if (!(p instanceof IQueryEditCommand)) {
							MenuItem mnuItem = new MenuItem(thumbMenu, SWT.Deactivate);
							mnuItem.setText(p.getName());
							mnuItem.setImage(p.getImage());
							mnuItem.addListener(SWT.Selection, e->{
								p.doWork(imageTable.getSelection().get(0));
							});
						}
					}
					new MenuItem(thumbMenu, SWT.SEPARATOR);
				}
				MenuItem mnuExport = new MenuItem(thumbMenu, SWT.DEFAULT);
				mnuExport.setText(Messages.QueryResultsImagePage_ExportMenuItem);
				mnuExport.addListener(SWT.Selection, e->{
					DirectoryDialog dd = new DirectoryDialog(parent.getShell());
					dd.setText(Messages.QueryResultsImagePage_ExportDialogTitle);
					dd.setMessage(Messages.QueryResultsImagePage_ExportDialogMessage);
					String directory = dd.open();
					if (directory == null) return;
					
					Path exportPath = Paths.get(directory);
					if (!Files.exists(exportPath)) {
						try {
							Files.createDirectories(exportPath);
						} catch (IOException e1) {
							QueryPlugIn.displayLog(MessageFormat.format(Messages.QueryResultsImagePage_CouldNotCreateDirectory, e1.getMessage()), e1);
							return;
						}
					}
					
					int cnt = 0;
					int size = 0;
					for (IAttachmentResultItem a : imageTable.getSelection()) {
						size++;

						//check duplicate filenames
						String name = a.getAttachment().getFilename();
						String prefix = name;
						String suffix = name;
						int index = prefix.lastIndexOf("."); //$NON-NLS-1$
						if (index > 0) {
							prefix = prefix.substring(0, index);
							suffix = name.substring(index);
						}
						Path outputFile = exportPath.resolve(prefix + suffix);
						int cnter = 1;
						while(Files.exists(outputFile)) {
							outputFile = exportPath.resolve(prefix + "_" + cnter + suffix); //$NON-NLS-1$
							cnter ++;
						}
						
						try {
							EncryptUtils.decryptAttachment(a.getAttachment(), outputFile);
							cnt ++;
						}catch (Exception e1) {
							QueryPlugIn.displayLog(MessageFormat.format(Messages.QueryResultsImagePage_CouldNotExport, a.getAttachment().getFilename(), e1.getMessage()), e1);
						}
					}
					MessageDialog.openInformation(parent.getShell(), Messages.QueryResultsImagePage_ExportComplete, MessageFormat.format(Messages.QueryResultsImagePage_ExportCompleteMsg, cnt, size,exportPath.toString()));
				});
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
				
			}
		});
		
		
		return thumbMenu;
	}

	@Override
	public void setFocus() {
	}

}
