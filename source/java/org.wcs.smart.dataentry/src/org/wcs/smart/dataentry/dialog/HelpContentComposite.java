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
package org.wcs.smart.dataentry.dialog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.dataentry.dialog.composite.AbstractInfoComposite.IModelChangedListener;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttribute.HelpImageLocation;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for collecting help info for configurable model attributes.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class HelpContentComposite extends Composite {

	private static int IMAGE_SIZE = 150;

	private HelpSettings copyOptions = null;

	private Text txtText;
	private ComboViewer cmbImageLoc;
	private Canvas lblImg;

	private CmAttribute attribute;

	private Image imgCache;
	private Path imgPath;

	private IModelChangedListener listener;

	private ToolBar tb;
	private ToolItem tiCopy, tiPaste, tiAssignAll, tiImport;

	private List<ConfigurableModel> otherConfigurableModels = new ArrayList<>();
	
	private Shell previewShell;
	private Shell importShell;

	private boolean fireChanged = true;
	
	public HelpContentComposite(Composite parent, IModelChangedListener listener) {
		super(parent, SWT.NONE);
		this.listener = listener;
		createContent();
		this.attribute = null;
	}

	private void createContent() {
		setLayout(new GridLayout(1, false));
		((GridLayout) getLayout()).marginWidth = 0;
		((GridLayout) getLayout()).marginHeight = 0;

		Composite header = SmartUiUtils.createHeaderLabel(this, Messages.HelpContentComposite_ImageLblSection);
		header.setLayout(new GridLayout(2, false));
		((GridLayout) header.getLayout()).marginHeight = 0;
		header.getChildren()[0].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		tb = new ToolBar(header, SWT.FLAT);

		tiCopy = new ToolItem(tb, SWT.NONE);
		tiCopy.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY));
		tiCopy.setToolTipText(Messages.HelpContentComposite_copytooltip);
		tiCopy.setEnabled(true);
		tiCopy.addListener(SWT.Selection, e -> {
			copyOptions = new HelpSettings(txtText.getText(),
					(HelpImageLocation) cmbImageLoc.getStructuredSelection().getFirstElement(), imgPath);
			tiPaste.setEnabled(true);
		});

		tiPaste = new ToolItem(tb, SWT.NONE);
		tiPaste.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_PASTE));
		tiPaste.setToolTipText(Messages.HelpContentComposite_pastetooltip);
		tiPaste.setEnabled(false);
		tiPaste.addListener(SWT.Selection, e -> {
			if (copyOptions == null)
				return;
			txtText.setText(copyOptions.text);
			attribute.setHelpText(copyOptions.text);
			cmbImageLoc.setSelection(new StructuredSelection(copyOptions.location));
			attribute.setHelpImageLocation(copyOptions.location);

			attribute.setImportHelpFile(copyOptions.imagePath);
			refreshImage();
			fireChanged();
		});

		tiAssignAll = new ToolItem(tb, SWT.NONE);
		tiAssignAll.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CREATECOPY_ICON));
		tiAssignAll.setToolTipText(Messages.HelpContentComposite_assignalltooltip);
		tiAssignAll.addListener(SWT.Selection, e -> assignAll());

		tiImport = new ToolItem(tb, SWT.NONE);
		tiImport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
		tiImport.setToolTipText(Messages.HelpContentComposite_importhelptooltip);
		tiImport.addListener(SWT.Selection, e -> importHelp());
		tiImport.setEnabled(false);
		Composite temp = new Composite(this, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		((GridLayout) temp.getLayout()).marginWidth = 0;
		((GridLayout) temp.getLayout()).marginHeight = 0;
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		lblImg = new Canvas(temp, SWT.BORDER);
		lblImg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData) lblImg.getLayoutData()).widthHint = IMAGE_SIZE;
		((GridData) lblImg.getLayoutData()).heightHint = IMAGE_SIZE;

		lblImg.addListener(SWT.Dispose, e -> {
			if (imgCache != null) {
				imgCache.dispose();
				imgCache = null;
			}
		});
		lblImg.addListener(SWT.Paint, e -> {
			if (imgPath == null) {
				String text = Messages.HelpContentComposite_NoImageText;
				Point size = e.gc.textExtent(text);
				e.gc.drawText(text, (lblImg.getBounds().width - size.x) / 2, (lblImg.getBounds().height / 2) - size.y);
				return;
			}
			if (imgCache != null) {
				e.gc.drawImage(imgCache, 0, 0);
				return;
			}
			String text = Messages.HelpContentComposite_FormatNotSupportedText;
			Point size = e.gc.textExtent(text);
			e.gc.drawText(text, (lblImg.getBounds().width - size.x) / 2, (lblImg.getBounds().height / 2) - size.y);
		});

		Composite btnComp = new Composite(temp, SWT.NONE);
		btnComp.setLayout(new GridLayout());
		((GridLayout) btnComp.getLayout()).marginWidth = 0;
		((GridLayout) btnComp.getLayout()).marginHeight = 0;
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		cmbImageLoc = new ComboViewer(btnComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbImageLoc.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbImageLoc.setContentProvider(ArrayContentProvider.getInstance());
		cmbImageLoc.setInput(CmAttribute.HelpImageLocation.values());
		cmbImageLoc.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				switch (((CmAttribute.HelpImageLocation) element)) {
				case BEFORE:
					return Messages.HelpContentComposite_DisplayBeforeTextOp;
				case AFTER:
					return Messages.HelpContentComposite_DisplayAfterOp;
				}
				return super.getText(element);
			}
		});
		cmbImageLoc.setSelection(new StructuredSelection(CmAttribute.HelpImageLocation.BEFORE));
		cmbImageLoc.addSelectionChangedListener(e -> {
			if (attribute == null)
				return;
			attribute.setHelpImageLocation((HelpImageLocation) cmbImageLoc.getStructuredSelection().getFirstElement());
			fireChanged();
		});

		Button btnSelect = new Button(btnComp, SWT.NONE);
		btnSelect.setText(Messages.HelpContentComposite_SelectImageBtn);
		btnSelect.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnSelect.addListener(SWT.Selection, e -> selectImageFile());
		btnSelect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Button btnClear = new Button(btnComp, SWT.NONE);
		btnClear.setText(Messages.HelpContentComposite_ClearImageBtn);
		btnClear.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnClear.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnClear.addListener(SWT.Selection, e -> {
			if (attribute != null) {
				attribute.setImportHelpFile(null);
				attribute.setHelpFormat(null);
			}
			refreshImage();
			fireChanged();
		});

		SmartUiUtils.createHeaderLabel(this, Messages.HelpContentComposite_TextSectionHeader);

		txtText = new Text(this, SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
		txtText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		txtText.addListener(SWT.Modify, e -> {
			if (attribute == null)
				return;
			attribute.setHelpText(txtText.getText());
			fireChanged();
		});

		Button btnPreview = new Button(this, SWT.NONE);
		btnPreview.setText(Messages.HelpContentComposite_previewbutton);
		btnPreview.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnPreview.addListener(SWT.Selection, e -> preview());
		btnPreview.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}

	private void fireChanged() {
		if (!fireChanged) return;
		listener.modelChanged();

	}
	private void preview() {
		if (previewShell == null || previewShell.isDisposed()) {
			previewShell = new Shell(getShell());
			previewShell.setLayout(new GridLayout());
			((GridLayout) previewShell.getLayout()).marginWidth = 0;
			((GridLayout) previewShell.getLayout()).marginHeight = 0;
			Browser b = new Browser(previewShell, SWT.NONE);
			b.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			previewShell.setSize(400, 600);
		}
		((Browser) previewShell.getChildren()[0]).setText(attribute.getHelpTextAsHtml(true), true);
		previewShell.open();
	}

	public void setAttribute(CmAttribute attribute) {
		try {
			fireChanged = false;
			this.attribute = attribute;
			if (imgCache != null) {
				imgCache.dispose();
				imgCache = null;
			}

			if (attribute != null) {
				txtText.setText(attribute.getHelpText() == null ? "" : attribute.getHelpText()); //$NON-NLS-1$
				if (attribute.getHelpImageLocation() != null) {
					cmbImageLoc.setSelection(new StructuredSelection(attribute.getHelpImageLocation()));
				}
				tiAssignAll.setToolTipText(MessageFormat
						.format(Messages.HelpContentComposite_assignalltooltip2, attribute.getName()));
				loadCm.schedule();
			} else {
				txtText.setText(""); //$NON-NLS-1$
				cmbImageLoc.setSelection(new StructuredSelection(CmAttribute.HelpImageLocation.BEFORE));
			}
			refreshImage();
		}finally {
			fireChanged = true;
		}
	}

	private void importHelp() {
		importShell = new Shell(getShell(), SWT.PRIMARY_MODAL | SWT.RESIZE );
		importShell.setLayout(new GridLayout(2, true));
		((GridLayout)importShell.getLayout()).marginWidth = 3;
		((GridLayout)importShell.getLayout()).marginHeight = 3;
		importShell.setBackground(importShell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
//		Composite outer = new Composite(importShell, SWT.NONE);
//		outer.setLayout(new GridLayout());
//		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		((GridLayout)outer.getLayout()).marginWidth = 0;
//		((GridLayout)outer.getLayout()).marginHeight = 0;
//		outer.setBackground(outer.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Composite wrapper = new Composite(importShell, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TableViewer lstCm = new TableViewer(wrapper, SWT.BORDER);
		lstCm.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstCm.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ConfigurableModel) return ((ConfigurableModel)element).getName();
				return super.getText(element);
			}
		});
		TableColumn tc = new TableColumn(lstCm.getTable(), SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		layout.setColumnData(tc, new ColumnWeightData(1));
		wrapper.setLayout(layout);
		lstCm.setContentProvider(ArrayContentProvider.getInstance());
		lstCm.setInput(otherConfigurableModels);
		
		TableViewer lstAttribute = new TableViewer(importShell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		lstAttribute.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstAttribute.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof CmAttribute) {
					StringBuilder sb = new StringBuilder();
					CmAttribute a = (CmAttribute)element;
					sb.append(a.getName());
					sb.append(" ("); //$NON-NLS-1$
					List<String> cats = new ArrayList<>();
					CmNode node = a.getNode();
					while(node != null) {
						cats.add(0, node.getName());
						node = node.getParent();
					}
					for (String s : cats) sb.append(s + " - "); //$NON-NLS-1$
					sb.deleteCharAt(sb.length() - 1);
					sb.deleteCharAt(sb.length() - 1);
					sb.deleteCharAt(sb.length() - 1);
					sb.append(")"); //$NON-NLS-1$
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		lstAttribute.setContentProvider(ArrayContentProvider.getInstance());
		lstAttribute.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object x = lstAttribute.getStructuredSelection().getFirstElement();
				if (x == null) return;
				if (!(x instanceof CmAttribute)) return;
				CmAttribute a = (CmAttribute)x;
				attribute.setHelpText(a.getHelpText());
				attribute.setHelpImageLocation(a.getHelpImageLocation());
				attribute.setImportHelpFile(a.getHelpImage());
				
				txtText.setText(attribute.getHelpText() == null ? "" : attribute.getHelpText()); //$NON-NLS-1$
				if (attribute.getHelpImageLocation() != null) {
					cmbImageLoc.setSelection(new StructuredSelection(attribute.getHelpImageLocation()));
				}
				imgPath = attribute.getImportHelpFile();
				refreshImage();
				fireChanged();
				importShell.dispose();
				importShell = null;
			}
		});
		
		lstCm.addSelectionChangedListener(e->{
			lstAttribute.setInput(DialogConstants.LOADING_TEXT);
			
			Job j = new Job(Messages.HelpContentComposite_loadingattributesjob) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Object[] x = new Object[] {null};
					Display.getDefault().syncExec(()->{
						x[0] = lstCm.getStructuredSelection().getFirstElement();	
					});
					
					if (!(x[0] instanceof ConfigurableModel)) return Status.OK_STATUS;
					List<CmAttribute> nodes = new ArrayList<>();
					final ConfigurableModel cm = (ConfigurableModel)x[0];
					try(Session session = HibernateManager.openSession()){
						session.beginTransaction();
						
						session.doWork(new Work() {
							@Override
							public void execute(Connection c) throws SQLException {
								int iso = c.getTransactionIsolation();
								c.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
								
								try {
									ConfigurableModel cm2 = session.get(ConfigurableModel.class, cm.getUuid());
									List<CmNode> nn = new ArrayList<>();
									nn.addAll(cm2.getNodes());
									while(!nn.isEmpty()) {
										CmNode n = nn.remove(0);
										if (n.getChildren() != null) nn.addAll(n.getChildren());
										if (n.getCmAttributes() != null) {
											for (CmAttribute att : n.getCmAttributes()) {
												if (att.getAttribute().equals(attribute.getAttribute())) {
													nodes.add(att);
												}
											}
										}
									}
									for (CmAttribute n : nodes) {
										n.getName();
										n.getNode().getName();
									}
								}finally {
									c.setTransactionIsolation(iso);
								}
								session.getTransaction().rollback();
							}
						});
						
					}
					
					Display.getDefault().asyncExec(()->{
						if (lstAttribute.getControl().isDisposed()) return;
						if (nodes.isEmpty()) {
							lstAttribute.setInput(new String[] {MessageFormat.format(Messages.HelpContentComposite_NoAttributeFound, attribute.getName(), cm.getName())});
						}else {
							lstAttribute.setInput(nodes);
						}
					});
					return Status.OK_STATUS;
				}		
			};
			
			
			j.schedule();
		});
		
		Button btnClose = new Button(importShell, SWT.PUSH);
		btnClose.setText(IDialogConstants.CLOSE_LABEL);
		btnClose.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		((GridData)btnClose.getLayoutData()).widthHint = (int)(btnClose.computeSize(SWT.DEFAULT, SWT.DEFAULT).x * 1.5);
//		btnClose.setBackground(outer.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnClose.addListener(SWT.Selection, e->{importShell.dispose();importShell=null;});
		importShell.setSize(500, 200);
		Point p = tb.getLocation();
		p.x = p.x + tb.getSize().x - 500;
		p.y = p.y + tb.getSize().y;
		p = tb.getParent().toDisplay(p);
	
		importShell.setLocation( p );
		
		importShell.open();
		
	
	}

	private void assignAll() {
		if (this.attribute == null)
			return;

		ConfigurableModel cm = attribute.getNode().getModel();
		List<CmNode> toProcess = new ArrayList<>();
		toProcess.addAll(cm.getNodes());
		while (!toProcess.isEmpty()) {
			CmNode node = toProcess.remove(0);

			if (node.getChildren() != null)
				toProcess.addAll(node.getChildren());

			if (node.getCmAttributes() == null)
				continue;
			for (CmAttribute a : node.getCmAttributes()) {
				if (a.equals(this.attribute))
					continue;
				if (a.getAttribute().equals(this.attribute.getAttribute())) {
					// copy settings
					a.setHelpText(this.attribute.getHelpText());
					a.setHelpImageLocation(this.attribute.getHelpImageLocation());
					a.setImportHelpFile(attribute.getImportHelpFile() != null ? attribute.getImportHelpFile()
							: attribute.getHelpImage());
				}
			}
		}
		fireChanged();
	}

	private void refreshImage() {
		if (imgCache != null) {
			imgCache.dispose();
			imgCache = null;
		}
		imgPath = null;
		if (attribute != null) {
			if (attribute.getImportHelpFile() != null) {
				imgPath = attribute.getImportHelpFile();
			} else {
				imgPath = attribute.getHelpImage();
			}
			if (imgPath != null) {
				imgCache = SmartUtils.getImage(imgPath, IMAGE_SIZE);
			}
		}

		getDisplay().asyncExec(() -> lblImg.redraw());
	}

	private void selectImageFile() {
		FileDialog fd = new FileDialog(getShell());
		String imageextensions = "*.png;*.jpeg;*.jpg;*.svg"; //$NON-NLS-1$
		fd.setFilterExtensions(new String[] { imageextensions, "*.*" }); //$NON-NLS-1$
		fd.setFilterNames(
				new String[] { MessageFormat.format(Messages.HelpContentComposite_ImageFilesOp, imageextensions),
						Messages.HelpContentComposite_AllFilesOp });
		String filename = fd.open();
		if (filename == null)
			return;

		Path p = Paths.get(filename);
		attribute.setImportHelpFile(p);
		attribute.setHelpImageLocation((HelpImageLocation) cmbImageLoc.getStructuredSelection().getFirstElement());
		refreshImage();
		fireChanged();
	}
	
	Job loadCm = new Job(Messages.HelpContentComposite_loadingcmjobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (!otherConfigurableModels.isEmpty()) return Status.OK_STATUS;
			CmAttribute temp = attribute;
			if (temp == null) return Status.OK_STATUS;
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				session.doWork(new Work() {
					
					@Override
					public void execute(Connection c) throws SQLException {
						int iso = c.getTransactionIsolation();
						c.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
						
						try {
							otherConfigurableModels.addAll(QueryFactory.buildQuery(session, ConfigurableModel.class, 
									new Object[] {"conservationArea", temp.getNode().getModel().getConservationArea()}) //$NON-NLS-1$
									.list());
							otherConfigurableModels.remove(temp.getNode().getModel());
							otherConfigurableModels.forEach(e->e.getName());
						}finally {
							c.setTransactionIsolation(iso);
						}
						
					}
				
					
				});
				session.getTransaction().rollback();
				
			}
			Display.getDefault().asyncExec(()->{
				if (!otherConfigurableModels.isEmpty()) tiImport.setEnabled(true);
			});
			return Status.OK_STATUS;
		}
	};
	

	private class HelpSettings {
		String text;
		CmAttribute.HelpImageLocation location;
		Path imagePath;

		public HelpSettings(String text, CmAttribute.HelpImageLocation location, Path imagePath) {
			this.text = text;
			this.location = location;
			this.imagePath = imagePath;
		}
	}
}
