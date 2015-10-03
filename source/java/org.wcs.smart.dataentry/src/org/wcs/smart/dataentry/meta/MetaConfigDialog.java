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
package org.wcs.smart.dataentry.meta;

import java.util.Map;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * The CyberTracker dialog for managing patrol meta screen
 * that will be shown in generated CyberTracker application
 * 
 * @author elitvin
 * @since 2.0.0
 */
public abstract class MetaConfigDialog<T> extends AbstractPropertyJHeaderDialog {
	
	private String title;
	private String message;
	
	private TableViewer modelListViewer;

	private Composite infoInnerPanel;
	private Composite emptyComposite;

	private Map<T, ScreenOptionComposite> screenComposites;
	
	public MetaConfigDialog(Shell shell, String title, String message) {
		super(shell, title);
		this.title = title;
		this.message = message;
	}

	protected abstract Map<T, ScreenOption> getOptionsMap();
	
	protected abstract T[] getOptionsToShow();
	
	protected abstract Map<T, ScreenOptionComposite> buildOptionComposites(Composite infoInnerPanel, IScreenOptionChangeListener listener);
	
	protected abstract LabelProvider getMetaTypeLabelProvider();
	
	@Override
	protected Composite createContent(Composite parent) {
		
		SashForm container = new SashForm(parent, SWT.HORIZONTAL);

		modelListViewer = new TableViewer(container, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		modelListViewer.setLabelProvider(getMetaTypeLabelProvider());
		modelListViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelListViewer.setInput(getOptionsToShow());
		modelListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		modelListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateRightPanel();
			}
		});

		Composite rightPanel = new Composite(container, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		infoInnerPanel = new Composite(rightPanel, SWT.NONE);
		infoInnerPanel.setLayout(new StackLayout());
		infoInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		emptyComposite = new Composite(infoInnerPanel, SWT.NONE);
		emptyComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

		IScreenOptionChangeListener listener = new IScreenOptionChangeListener() {
			@Override
			public void screenOptionChanged() {
				setChangesMade(true);
				StackLayout stackLayout = ((StackLayout)infoInnerPanel.getLayout());
				updateMessage((Composite)stackLayout.topControl);
			}
		};
		
		screenComposites = buildOptionComposites(infoInnerPanel, listener);

		container.setWeights(new int[]{40,60});
		
		setTitle(title);
		setMessage(message);
		return container;
	}

	private void updateRightPanel() {
		IStructuredSelection selection = (IStructuredSelection) modelListViewer.getSelection();
		Object meta = selection.getFirstElement();

		StackLayout stackLayout = ((StackLayout)infoInnerPanel.getLayout());
		Composite cmp = screenComposites.get(meta);
		updateMessage(cmp);
		if (cmp != null) {
			stackLayout.topControl = cmp;
		} else {
			stackLayout.topControl = emptyComposite;
		}
		infoInnerPanel.layout();
	}

	private void updateMessage(Composite editComposite) {
		if (editComposite instanceof ScreenOptionComposite) {
			ScreenOptionComposite soc = (ScreenOptionComposite) editComposite;
			String msg = soc.validate();
			if (msg != null) {
				setMessage(msg, IMessageProvider.ERROR);
				return;
			}
		}
		setMessage(message);
	}
	
	private boolean validate() {
		for (T option : getOptionsToShow()) {
			Composite cmp = screenComposites.get(option);
			if (cmp instanceof ScreenOptionComposite) {
				ScreenOptionComposite soc = (ScreenOptionComposite) cmp;
				if (soc.validate() != null) {
					modelListViewer.setSelection(new StructuredSelection(option));
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	protected boolean performSave() {
		if (!validate())
			return false;
		
		Session session = getSession();
		session.beginTransaction();
		try {
			for (ScreenOption so : getOptionsMap().values()) {
				session.saveOrUpdate(so);
			}
			session.getTransaction().commit();
			setChangesMade(false);
			return true;
		} catch (Exception ex) {
			session.getTransaction().rollback();
			SmartPlugIn.displayLog(Messages.MetaConfigDialog_SaveError + "\n"+ ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return false;
		}
	}

}
