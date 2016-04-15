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
package org.wcs.smart.birt.ui;

import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.designer.internal.ui.util.UIUtil;
import org.eclipse.birt.report.designer.ui.editors.IReportProvider;
import org.eclipse.birt.report.designer.ui.editors.MultiPageReportEditor;
import org.eclipse.birt.report.model.api.IVersionInfo;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.wcs.smart.birt.Activator;

/**
 * BIRT Report editor for SMART birt editors.  This editor looks
 * for the appropriate editor manager based on the editor input and calls
 * the required functions.  See reportEditManager extension point.
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class RCPMultiPageReportEditor extends MultiPageReportEditor {

	
	/**
	 * The ID of the Report Editor
	 */
	public static final String REPROT_EDITOR_ID = "org.eclipse.birt.report.designer.ui.editors.ReportEditor"; //$NON-NLS-1$
	
	private IReportEditorManager manager;
	
	public RCPMultiPageReportEditor(){
		super();
	}
	
	@Override
	public void refreshMarkers(IEditorInput input) throws CoreException {
		ModuleHandle reportDesignHandle = getModel();
		if (reportDesignHandle != null) {
			reportDesignHandle.checkReport();
		}
	}
	
	
	private IReportEditorManager getManager(){
		if (manager == null){
			try{
			if (Platform.getExtensionRegistry() != null){
				IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IReportEditorManager.EXTENSION_ID);
				for (IConfigurationElement e : config) {
					if (getEditorInput().getClass().getName().equals(e.getAttribute("EditorInput"))){ //$NON-NLS-1$
						manager = (IReportEditorManager) e.createExecutableExtension("EditorManager"); //$NON-NLS-1$
						break;
					}
				}
			}
			}catch (Exception ex){
				Activator.log(ex);
			}
		}
		return manager;
	}
	
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.designer.ui.editors.MultiPageReportEditor#init
	 * (org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		getSite().getWorkbenchWindow().getPartService().addPartListener(this);
		if (getManager() != null){
			getManager().init(this);
		}
	}

	
	@Override
	public void addPages(){
		/*
		 * This is copied from the BIRT MultiPageReportEditor to fix the version checking library resource.  I 
		 * could not figure out how to get the checkVersion function to use the correct
		 * ResourceLocator, so I copied the functionality here and specifically used the required locator
		 */
		List<?> formPageList = EditorContributorManager.getInstance( )
				.getEditorContributor( getEditorSite( ).getId( ) ).formPageList;
		boolean error = false;

		// For back compatible only.
		// Provide warning message to let user select if the auto convert needs
		// See bugzilla bug 136536 for detail.
		String fileName = getProvider( ).getInputPath( getEditorInput( ) ).toOSString( );
		List<?> message = SmartModuleUtils.checkVersion( fileName );
		if ( message.size( ) > 0 ){
			IVersionInfo info = (IVersionInfo) message.get( 0 );

			if ( !MessageDialog.openConfirm( UIUtil.getDefaultShell( ),
					"Warning",  //$NON-NLS-1$
					info.getLocalizedMessage( ) ) ){
				for ( Iterator<?> iter = formPageList.iterator( ); iter.hasNext( ); ){
					FormPageDef pagedef = (FormPageDef) iter.next( );
					if ( XMLSourcePage_ID.equals( pagedef.id ) ){
						try{
							addPage( pagedef.createPage( ), pagedef.displayName );
							break;
						}catch ( Exception e ){}
					}
				}
				return;
			}
		}
		
		UIUtil.processSessionResourceFolder( getEditorInput( ),
				UIUtil.getProjectFromInput( getEditorInput( ) ),
				null );
		// load the model first here, so consequent pages can directly use it
		// without reloading
		getProvider( ).getReportModuleHandle( getEditorInput( ) );

		for ( Iterator<?> iter = formPageList.iterator( ); iter.hasNext( ); ){
			FormPageDef pagedef = (FormPageDef) iter.next( );
			try{
				addPage( pagedef.createPage( ), pagedef.displayName );
			}catch ( Exception e ){
				error = true;
			}
		}

		if (error) {
			setActivePage(XMLSourcePage_ID);
		}

		if (getManager() != null) {
			getManager().addPages();
		}
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.designer.ui.editors.MultiPageReportEditor#dispose
	 * ()
	 */
	public void dispose() {
		getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		if (getManager() != null){
			getManager().dispose();
		}
		
		super.dispose();
	}

	public void doSave(IProgressMonitor monitor) {
		if (getManager() != null){
			getManager().doSave(monitor);
		}
	}

	public void doSaveParent(IProgressMonitor monitor){
		super.doSave(monitor);
	}
	
	public void setPartName(String partName){
		super.setPartName(partName);
	}
	
	
	@Override
	/**
	 * Overrides the default save method to save a copy of the report to the 
	 * database and create a new file.
	 */
	public void doSaveAs() {
		if (getManager() != null){
			getManager().doSaveAs();
		}
	}
	
	public IReportProvider getProvider( )
	{
		return super.getProvider();
	}

	public void firePropertyChange(int propertyId){
		super.firePropertyChange(propertyId);
	}
	
	
}
