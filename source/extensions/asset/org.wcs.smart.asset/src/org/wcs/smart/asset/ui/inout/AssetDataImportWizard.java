/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.inout;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.wizard.Wizard;

/**
 * Wizard for importing asset data from various file formats.
 * 
 * @author Emily
 *
 */
public class AssetDataImportWizard extends Wizard{

	@Inject
	private IEclipseContext context;
	
	public enum Type{
		
		XML("Model Elements From XML File "),
		ASSET_CSV("Assets From CSV File"),
		STATION_CSV("Stations From CSV File"),
		LOCATION_CSV("Locations From CSV File");
		
		String guiName;
		
		Type(String guiName) {
			this.guiName = guiName;
		}
	}
	
	ImportTypePage typePage = null;
	FileWizardPage filePage = null;
	XmlFileWizardPage xmlPage = null;
	AssetMappingPage assetPage = null;
	StationMappingPage stationPage = null;
	LocationMappingPage locationPage = null;
	
	public AssetDataImportWizard() {
		super();
		setWindowTitle("Import Asset Data");
		
	}
	
	@Override
	public boolean canFinish() {
		if (getContainer().getCurrentPage() == null) return false;
		if (!getContainer().getCurrentPage().isPageComplete()) return false;
		if (getContainer().getCurrentPage() == xmlPage ) return true;
		if (getContainer().getCurrentPage() == assetPage ) return true;
		if (getContainer().getCurrentPage() == stationPage ) return true;
		if (getContainer().getCurrentPage() == locationPage ) return true;
		
		return false;
	}
	
	@Override
	public boolean performFinish() {
		
		if (getContainer().getCurrentPage() == xmlPage) {
			return xmlPage.doFinish();
		}
		if (getContainer().getCurrentPage() == assetPage) {
			return assetPage.doFinish();
		}
		if (getContainer().getCurrentPage() == stationPage) {
			return stationPage.doFinish();
		}
		if (getContainer().getCurrentPage() == locationPage) {
			return locationPage.doFinish();
		}
		return false;
		
	}

	public void addPages() {
		filePage = new FileWizardPage();
		xmlPage = new XmlFileWizardPage();
		typePage = new ImportTypePage();
		assetPage = new AssetMappingPage();
		stationPage = new StationMappingPage();
		locationPage = new LocationMappingPage();
		
		ContextInjectionFactory.inject(filePage, context);
		ContextInjectionFactory.inject(xmlPage, context);
		ContextInjectionFactory.inject(typePage, context);
		ContextInjectionFactory.inject(assetPage, context);
		ContextInjectionFactory.inject(stationPage, context);
		ContextInjectionFactory.inject(locationPage, context);
		
		addPage(typePage);
		addPage(filePage);
		addPage(xmlPage);
		addPage(assetPage);
		addPage(stationPage);
		addPage(locationPage);
	}
	
	
}
