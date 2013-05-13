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

import net.refractions.udig.catalog.URLUtils;
import net.refractions.udig.project.IMap;
import net.refractions.udig.project.render.RenderException;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.ApplicationGIS.DrawMapParameter;
import net.refractions.udig.project.ui.SelectionStyle;

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
import org.opengis.referencing.operation.TransformException;
import org.wcs.smart.SmartPlugIn;

public class ExportMapToImageWizard extends Wizard implements IExportWizard {

	private ExportMapWizardPage mapSelectorPage;

	public ExportMapToImageWizard() {
		setWindowTitle("Export map as image");
		setDialogSettings(SmartPlugIn.getDefault().getDialogSettings());

		String title = null; // will use default title
		//	        ImageDescriptor banner = ProjectUIPlugin.getDefault().getImageDescriptor(Icons.WIZBAN + "exportimage_wiz.gif"); //$NON-NLS-1$
		// setDefaultPageImageDescriptor(banner);
		mapSelectorPage = new ExportMapWizardPage("Select Map", title, null); //$NON-NLS-1$
		addPage(mapSelectorPage);
		// addPage(imageSettingsPage);
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

                    monitor.beginTask("Exporting Map", 1);
                    
                    try {
                    	exportMap(map, new SubProgressMonitor(monitor, 3));
                    } catch (RenderException e) {
                           Object[] args = new Object[]{map.getName(), e.getLocalizedMessage()};
                            String pattern = "Error occurred while rendering {0} -> {1}";
                            errors.add(MessageFormat.format(pattern, args));
                        } catch (IOException e) {
                            errors.add("Error occurred while attempting to write the rendered image to a  file:"
                                    + e.getLocalizedMessage());
                        } catch (TransformException e) {
                            errors
                                    .add("Failed to create world file.  This image can not be used as a Raster file in uDig");
                        } catch (NoninvertibleTransformException e) {
                            errors
                                    .add("Failed to create world file.  This image can not be used as a Raster file in uDig");
                        } catch (RuntimeException e) {
                            errors
                                    .add("An unexpected failure occurred: "
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
            ((WizardPage) getContainer().getCurrentPage())
                    .setErrorMessage("Error were generated while exporting map. "
                            + errors.iterator().next());
            return false;
        }
        mapSelectorPage.saveLastSelection();
        
        return true;
	}

	private void exportMap(IMap map, IProgressMonitor monitor)
			throws RenderException, IOException, TransformException,
			NoninvertibleTransformException {

		monitor.beginTask("Rendering", 3);
		String pattern = "Preparing {0} for export.";
		Object[] args = new Object[] { map.getName() };
		monitor.setTaskName(MessageFormat.format(pattern, args));
		File destination = determineDestinationFile(map);
		if (destination == null) {
			return;
		}

		int width = mapSelectorPage.getWidth(map.getViewportModel()
				.getWidth(), map.getViewportModel().getHeight());
		int height = mapSelectorPage.getHeight(map.getViewportModel()
				.getWidth(), map.getViewportModel().getHeight());

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
			pattern = "Rendering {0}";
			args = new Object[] { map.getName() };
			monitor.setTaskName(MessageFormat.format(pattern, args));

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
		pattern = "Writing {0} to disk.";
		args = new Object[] { map.getName() };
		monitor.setTaskName(MessageFormat.format(pattern, args));
		mapSelectorPage.getSelectedFormat().write(renderedMap, image, destination);


		monitor.done();
	}

	private File determineDestinationFile(IMap map) {
		File exportDir = mapSelectorPage.getOutputDir();
		String name = URLUtils.cleanFilename(map.getName());
		File destination = addSuffix(new File(exportDir, name));
		if (destination.exists()) {
			boolean overwrite = !MessageDialog.openQuestion(getContainer()
					.getShell(), "Export Map","Overwrite existing file? " + destination.getName());

			if (!overwrite) {
				if (!destination.delete()) {
					destination = selectFile(destination, "Unable to delete existing file.");
				}

			} else {
				destination = selectFile(destination,"Choose new filename");
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
