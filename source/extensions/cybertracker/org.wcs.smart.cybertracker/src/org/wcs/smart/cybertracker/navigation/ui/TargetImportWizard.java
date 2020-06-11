/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.navigation.ui;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.model.NavigationTarget;
import org.wcs.smart.cybertracker.navigation.ExportNavigationManager;

/**
 * Wizard for importing targets into a navigation layer.
 * 
 * @author Emily
 *
 */
public class TargetImportWizard extends Wizard {

	private SelectTargetProviderPage page1;
	private List<INavigationLayerTargetProvider> providers;
	private ITargetEditor editor;
	
	public TargetImportWizard(ITargetEditor editor) {
		this.editor = editor;
		providers = ExportNavigationManager.INSTANCE.getTargetProviders();
		providers.sort((a,b)->Collator.getInstance().compare(a.getTypeName(),  b.getTypeName()));
	}
	
	public List<INavigationLayerTargetProvider> getTargetProviders() {
		return this.providers;
	}
	
	@Override
	public void addPages() {
		page1 = new SelectTargetProviderPage();
		
		super.addPage(page1);
		for (INavigationLayerTargetProvider pp : providers) {
			for (WizardPage page : pp.getPages()) {
				super.addPage(page);
			}
		}
		
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean canFinish() {
		if (page1.getProvider() == null) return false;
		return page1.getProvider().canFinish();
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		return super.getNextPage(page);
	}
	
	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		return super.getPreviousPage(page);
	}
		
	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					List<NavigationTarget> targets = page1.getProvider().getTargets(monitor);
					editor.addTargets(targets);
				}
			});
		} catch (Exception ex) {
			CyberTrackerPlugIn.log(ex.getMessage(), ex);
			return false;
		}
		
		return true;
	}
	
    
}
