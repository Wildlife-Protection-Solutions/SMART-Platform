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
package org.wcs.smart.map.internal;

import java.awt.Graphics2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.locationtech.udig.catalog.URLUtils;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.render.RenderException;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.ApplicationGIS.DrawMapParameter;
import org.locationtech.udig.project.ui.SelectionStyle;
import org.opengis.referencing.operation.TransformException;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;
/**
 * A copy of the udig export map to image wizard that implements custom export 
 * options.  In particular this wizard will only export the current map
 * and you can optionally choose to preserve bounds or scale when exporting.
 * 
 * @author Emily
 *
 */
public class ExportMapToImageWizard extends Wizard implements IExportWizard {

	private ExportMapWizardPage mapSelectorPage;

	public ExportMapToImageWizard() {
		setWindowTitle(Messages.ExportMapToImageWizard_WindowTitle);
		setDialogSettings(SmartPlugIn.getDefault().getDialogSettings());
		
		setDefaultPageImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.WIZBAN_EXPORT_IMAGE));
		mapSelectorPage = new ExportMapWizardPage(); 
		addPage(mapSelectorPage);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		mapSelectorPage.setSelection(selection);

	}

	@Override
	public boolean performFinish() {
		final Collection<String> errors = new ArrayList<String>();
       
        try {
            getContainer().run(false, true, new IRunnableWithProgress(){

                public void run( IProgressMonitor monitor ) throws InvocationTargetException,
                        InterruptedException {

                    IMap map = mapSelectorPage.getMap();

                    monitor.beginTask(Messages.ExportMapToImageWizard_Progress_Exporting, 1);
                    
                    try {
                    	if (!exportMap(map, new SubProgressMonitor(monitor, 3))){
                    		errors.add(null);
                    	}
                    } catch (RenderException e) {
                           Object[] args = new Object[]{map.getName(), e.getLocalizedMessage()};
                            String pattern = Messages.ExportMapToImageWizard_RenderError;
                            errors.add(MessageFormat.format(pattern, args));
                        } catch (IOException e) {
                            errors.add(Messages.ExportMapToImageWizard_RenderError2
                                    + e.getLocalizedMessage());
                        } catch (TransformException e) {
                            errors
                                    .add(Messages.ExportMapToImageWizard_WorldFileError1);
                        } catch (NoninvertibleTransformException e) {
                            errors
                                    .add(Messages.ExportMapToImageWizard_WorldFileError2);
                        } catch (RuntimeException e) {
                            errors
                                    .add(Messages.ExportMapToImageWizard_Error
                                            + e.getLocalizedMessage());
                        }
                    
                }
            });
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (!errors.isEmpty()) {
        	if (errors.iterator().next() != null){
        		((WizardPage) getContainer().getCurrentPage())
                    .setErrorMessage(Messages.ExportMapToImageWizard_Error2
                            + errors.iterator().next());
        	}
            return false;
        }
        mapSelectorPage.saveLastSelection();
        
        return true;
	}

	private boolean exportMap(IMap map, IProgressMonitor monitor)
			throws RenderException, IOException, TransformException,
			NoninvertibleTransformException {

		monitor.beginTask(Messages.ExportMapToImageWizard_Progress_Rendering, 3);
		monitor.setTaskName(MessageFormat.format(Messages.ExportMapToImageWizard_Progress_Prepare, new Object[] { map.getName() }));
		File destination = determineDestinationFile(map);
		if (destination == null) {
			return false;
		}

		int width = mapSelectorPage.getWidth();
		int height = mapSelectorPage.getHeight();

		// gdavis - ARGB won't output proper background color for non-alpha
		// supporting
		// image types like jpg. Since the resulting image contains no alpha,
		// RGB works
		// fine for all formats.
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB); // .TYPE_INT_ARGB);

		Graphics2D g = image.createGraphics();

		IMap renderedMap;
		try {
			monitor.worked(1);
			monitor.setTaskName(MessageFormat.format(Messages.ExportMapToImageWizard_Progress_RenderingMap,  new Object[] { map.getName() }));

			DrawMapParameter drawMapParameter = new DrawMapParameter(g,
					new java.awt.Dimension(width, height), map,
					mapSelectorPage.getBoundsStrategy(),
					mapSelectorPage.getSelectedFormat().getDPI(),SelectionStyle.IGNORE,
					monitor);

			renderedMap = ApplicationGIS.drawMap(drawMapParameter);
		} finally {
			g.dispose();
		}
		monitor.worked(1);
		monitor.setTaskName(MessageFormat.format(Messages.ExportMapToImageWizard_Progress_WriteDisk, new Object[] { map.getName() }));
		mapSelectorPage.getSelectedFormat().write(renderedMap, image, destination);


		monitor.done();
		return true;
	}

	private File determineDestinationFile(IMap map) {
		File exportDir = mapSelectorPage.getOutputDir();
		if (!exportDir.exists()){
			boolean createDir = MessageDialog.openQuestion(getContainer()
					.getShell(), Messages.ExportMapToImageWizard_OverwriteDialogTitle,MessageFormat.format(Messages.ExportMapToImageWizard_OutputDirDoesNotExist, new Object[]{exportDir.toString()}));
			if (!createDir){
				return null;
			}else{
				if (!SmartUtils.createDirectory(exportDir)){
					return null;
				}
			}
		}
		if (!exportDir.isDirectory()){
			MessageDialog.openError(getContainer().getShell(), Messages.ExportMapToImageWizard_OverwriteDialogTitle, Messages.ExportMapToImageWizard_InvalidOutputDir + exportDir.toString());
			return null;			
		}
		
		String name = URLUtils.cleanFilename(mapSelectorPage.getFileName());
		File destination = addSuffix(new File(exportDir, name));
		if (destination.exists()) {
			boolean overwrite = !MessageDialog.openQuestion(getContainer()
					.getShell(), Messages.ExportMapToImageWizard_OverwriteDialogTitle,Messages.ExportMapToImageWizard_OverwriteDialogMessage + destination.getName());

			if (!overwrite) {
				if (!destination.delete()) {
					destination = selectFile(destination, Messages.ExportMapToImageWizard_OverwriteError);
				}

			} else {
				destination = selectFile(destination,Messages.ExportMapToImageWizard_ChooseFilenameMessage);
			}
		}

		if (destination == null) {
			return null;
		}

		return addSuffix(destination);
	}

	private File addSuffix(File file) {
		String path = stripEndSlash(file.getPath());

		File destination;
		String extension = mapSelectorPage.getSelectedFormat().getExtension();
		if (!path.endsWith(extension)) {
			destination = new File(path + "." + extension); //$NON-NLS-1$
		} else {
			return file;
		}
		return destination;
	}

	private String stripEndSlash(String path) {
		if (path.endsWith("/")) //$NON-NLS-1$
			return stripEndSlash(path.substring(0, path.length() - 1));
		return path;
	}

	private File selectFile(File destination, String string) {
		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(string);
		dialog.setFilterPath(destination.getParent());
		dialog.setFileName(destination.getName());
		String file = dialog.open();
		if (file == null) {
			destination = null;
		} else {
			destination = new File(file);
		}
		return destination;
	}
}
