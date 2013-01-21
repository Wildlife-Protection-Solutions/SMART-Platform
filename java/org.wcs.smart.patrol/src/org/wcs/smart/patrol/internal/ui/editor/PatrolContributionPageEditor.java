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
package org.wcs.smart.patrol.internal.ui.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;

/**
 * The contribution page of the patrol editor.  This page
 * is only added if there exists at least on plugin that
 * implements the IPatrolEditorContribution extension point.
 * 
 * @author Emily
 *
 */
public class PatrolContributionPageEditor extends EditorPart{

	
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private PatrolEditor editor = null;
	
	/**
	 * Creates new page
	 * @param editor
	 */
	public PatrolContributionPageEditor(PatrolEditor editor){
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
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		List<IPatrolEditorContribution> parts = findContributions();
		for (IPatrolEditorContribution part : parts){
			final Section sec = toolkit.createSection(main, Section.TWISTIE | Section.TITLE_BAR | Section.EXPANDED);
			sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			sec.addExpansionListener(new ExpansionAdapter() {
				@Override
				public void expansionStateChanged(ExpansionEvent e) {
					if (sec.isExpanded()){
						sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));			
					}else{
						sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					}
					sec.getParent().layout();
				}
			});
			sec.setText(part.getName());
			Composite info = part.createControl(toolkit, sec);
			sec.setClient(info);
			part.setPatrol(editor.getPatrol());
		}
	}

	@Override
	public void setFocus() {

	}
	
	private List<IPatrolEditorContribution> findContributions(){
		List<IPatrolEditorContribution> items = new ArrayList<IPatrolEditorContribution>();
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IPatrolEditorContribution.EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				IPatrolEditorContribution page = (IPatrolEditorContribution)e.createExecutableExtension("class"); //$NON-NLS-1$
				items.add(page);
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.CreatePatrolWizard_ErrorCreatingWizardPages, ex);
			return null;
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
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IPatrolEditorContribution.EXTENSION_ID);
		if (config.length > 0){
			return true;
		}
		return false;
	}

}
