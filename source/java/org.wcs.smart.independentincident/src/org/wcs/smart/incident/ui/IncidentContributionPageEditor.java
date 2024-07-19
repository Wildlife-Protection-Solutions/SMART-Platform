/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.incident.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;

/**
 * The contribution page of the mission editor.  This page
 * is only added if there exists at least on plugin that
 * implements the IMissionEditorContribution extension point.
 * 
 * @author Emily
 * @since 8.1.0
 *
 */
public class IncidentContributionPageEditor extends EditorPart{

	private IncidentEditor editor = null;
	private FormToolkit toolkit;
	
	/**
	 * Creates new page
	 * @param editor
	 */
	public IncidentContributionPageEditor(IncidentEditor editor){
		this.editor = editor;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void dispose(){
		super.dispose();
		toolkit.dispose();
	}
	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		ScrolledForm main = toolkit.createScrolledForm(parent);
		
		main.getBody().setLayout(new GridLayout(1, false));
		main.getBody().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		String canEdit = editor.canEdit();
		if (canEdit != null){
			Composite warning = toolkit.createComposite(main.getBody());
			warning.setLayout(new GridLayout(2, false));
			Label lblImage = toolkit.createLabel(warning, null, SWT.NONE);
			Image x = editor.getSite().getWorkbenchWindow().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			lblImage.setImage(x);
			Label lblWarning = toolkit.createLabel(warning, "", SWT.NONE); //$NON-NLS-1$
			lblWarning.setText(MessageFormat.format(Messages.IncidentContributionPageEditor_cannotedit, new Object[]{ canEdit }));
		}
		
		List<IIncidentEditorContribution> parts = findContributions();
	
		for (IIncidentEditorContribution part : parts){
			final Section sec = toolkit.createSection(main.getBody(), Section.TWISTIE | Section.TITLE_BAR | Section.EXPANDED);
			sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			sec.addExpansionListener(new ExpansionAdapter() {
				@Override
				public void expansionStateChanged(ExpansionEvent e) {
					if (sec.isExpanded()){
						sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));			
					}else{
						sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					}
					sec.getParent().layout(true, true);
				}
			});
			sec.setText(part.getName());
			Composite info = part.createControl(toolkit, sec, editor.canEdit()==null);
			sec.setClient(info);

			part.setIncident(editor.getIncident());
		}
	}

	@Override
	public void setFocus() {

	}
	
	private List<IIncidentEditorContribution> findContributions(){
		List<IIncidentEditorContribution> items = new ArrayList<IIncidentEditorContribution>();
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IIncidentEditorContribution.EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals("uieditor")){ //$NON-NLS-1$
					IIncidentEditorContribution page = (IIncidentEditorContribution)e.createExecutableExtension("class"); //$NON-NLS-1$
					items.add(page);
				}
			}
		}catch (Exception ex){
			IncidentPlugIn.displayLog(ex.getMessage(), ex);
		}
		return items;
	}
	
	/**
	 * 
	 * @return <code>true</code> if there exists at least one
	 * extension to appear on this page.
	 */
	public static boolean hasContributions(){
		if (Platform.getExtensionRegistry() == null) return false;
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IIncidentEditorContribution.EXTENSION_ID);
		if (config.length > 0){
			for (IConfigurationElement e : config) {
				if (e.getName().equals("uieditor")){ //$NON-NLS-1$
					return true;
				}
			}
		}
		return false;
	}

}
