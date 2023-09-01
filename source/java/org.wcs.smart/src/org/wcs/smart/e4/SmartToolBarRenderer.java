package org.wcs.smart.e4;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.e4.ui.internal.workbench.swt.AbstractPartRenderer;
import org.eclipse.e4.ui.internal.workbench.swt.CSSRenderingUtils;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.SideValue;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.renderers.swt.ToolBarManagerRenderer;
import org.eclipse.jface.action.IContributionManagerOverrides;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Extension of the default toolbar manager to display text below icons
 * in toolbar
 *  
 * Create a contribute part.
 */
public class SmartToolBarRenderer extends ToolBarManagerRenderer {

	@Inject
	EModelService modelService;

//	@Override
//	public Object createWidget(final MUIElement element, Object parent) {
//		if (!(element instanceof MToolBar) || !(parent instanceof Composite)) {
//			return null;
//		}
//
//		final MToolBar toolbarModel = (MToolBar) element;
//		ToolBar newTB = createToolbar(toolbarModel, (Composite) parent);
//		bindWidget(element, newTB);
//		processContribution(toolbarModel, toolbarModel.getElementId());
//
//		Control renderedCtrl = newTB;
//		MUIElement parentElement = element.getParent();
//		if (parentElement instanceof MTrimBar) {
//			//for SMART none of the toolbars are draggable
////			if (!element.getTags().contains(IPresentationEngine.NO_MOVE)) {
////				element.getTags().add(IPresentationEngine.DRAGGABLE);
////			}
//			
//			setCSSInfo(element, newTB);
//
//			boolean vertical = false;
//			MTrimBar bar = (MTrimBar) parentElement;
//			vertical = bar.getSide() == SideValue.LEFT || bar.getSide() == SideValue.RIGHT;
//			IEclipseContext parentContext = getContextForParent(element);
//			CSSRenderingUtils cssUtils = parentContext.get(CSSRenderingUtils.class);
//			if (cssUtils != null) {
//				MUIElement modelElement = (MUIElement) newTB.getData(AbstractPartRenderer.OWNING_ME);
//				boolean draggable = ((modelElement != null) && (modelElement.getTags().contains(IPresentationEngine.DRAGGABLE)));
//				if (!draggable) {
//					//had to add this otherwise the toolbar visiblity property does not get set
//					//properly and additional spacers get added to toolbar
//					//on non-windows this seems to cause a lot of errors and is not necessary
//					renderedCtrl = new ImageBasedFrame(newTB.getParent(), newTB, vertical, false);
//					//some sort of image is required for mac/linux
//					Image handleImage = new Image(newTB.getDisplay(), 1, 1);
//					
//					((ImageBasedFrame)renderedCtrl).setImages(null, null, handleImage);
//				}else {
//					renderedCtrl = cssUtils.frameMeIfPossible(newTB, null, vertical, draggable);
//				}
//			}
//		}
//
//		return renderedCtrl;
//	}
	
	@Override
	public Object createWidget(final MUIElement element, Object parent) {
		if (!(element instanceof MToolBar) || !(parent instanceof Composite)) {
			return null;
		}

		Composite toolbarComposite = (Composite) parent;
		// Composite which contains toolbar needs to have a separate class to allow the
		// CSS engine to target it
		IStylingEngine engine = getContextForParent(element).get(IStylingEngine.class);
		if (engine != null) {
			String cssClass = WidgetElement.getCSSClass(toolbarComposite);
			if (cssClass != null && !cssClass.isEmpty()) {
				if (!cssClass.contains("ToolbarComposite")) {//$NON-NLS-1$
					cssClass = cssClass + " ToolbarComposite"; //$NON-NLS-1$
				}
			} else {
				cssClass = "ToolbarComposite"; //$NON-NLS-1$
			}
			engine.setClassname(toolbarComposite, cssClass);
		}

		final MToolBar toolbarModel = (MToolBar) element;
		ToolBar newTB = createToolbar(toolbarModel, toolbarComposite);
		bindWidget(element, newTB);
		processContribution(toolbarModel, toolbarModel.getElementId());

		Control renderedCtrl = newTB;
		MUIElement parentElement = element.getParent();
		if (parentElement instanceof MTrimBar) {
			//for SMART none of the toolbars are draggable
			if (!element.getTags().contains(IPresentationEngine.NO_MOVE)) {
				element.getTags().add(IPresentationEngine.DRAGGABLE);
			}

			setCSSInfo(element, newTB);


			boolean vertical = false;
			MTrimBar bar = (MTrimBar) parentElement;
			vertical = bar.getSide() == SideValue.LEFT || bar.getSide() == SideValue.RIGHT;
			IEclipseContext parentContext = getContextForParent(element);

			CSSRenderingUtils cssUtils = parentContext.get(CSSRenderingUtils.class);
			if (cssUtils != null) {
				MUIElement modelElement = (MUIElement) newTB.getData(AbstractPartRenderer.OWNING_ME);
				boolean draggable = ((modelElement != null) && (modelElement.getTags().contains(IPresentationEngine.DRAGGABLE)));
				renderedCtrl = cssUtils.frameMeIfPossible(newTB, null, vertical, draggable);
			}
		}

		return renderedCtrl;
	}

//	private Image initDragHandleResource() {
//		Bundle bundle = org.eclipse.e4.ui.internal.workbench.swt.WorkbenchSWTActivator.getDefault().getBundle();
//		IPath path = new Path("$ws$/images/dragHandle.png");
//		URL url = FileLocator.find(bundle, path, null);
//		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
//		if (desc != null)
//			JFaceResources.getImageRegistry().put( "org.eclipse.e4.ui.workbench.swt.DRAG_HANDLE", desc);
//		return JFaceResources.getImage( "org.eclipse.e4.ui.workbench.swt.DRAG_HANDLE");
//	}
	
	private ToolBar createToolbar(final MUIElement element, Composite parent) {
		int orientation = getOrientation(element);
		int style = orientation | SWT.WRAP | SWT.FLAT ;
		
		ToolBarManager manager = getManager((MToolBar) element);
		if (manager == null) {
			manager = new ToolBarManager(style);
			IContributionManagerOverrides overrides = null;
			MApplicationElement parentElement = element.getParent();
			if (parentElement == null) {
				parentElement = modelService.getContainer(element);
			}

			if (parentElement != null) {
				overrides = (IContributionManagerOverrides) parentElement.getTransientData().get(
						IContributionManagerOverrides.class.getName());
			}

			manager.setOverrides(overrides);
			linkModelToManager((MToolBar) element, manager);
		} else {
			ToolBar toolBar = manager.getControl();
			if (toolBar != null && !toolBar.isDisposed() && (toolBar.getStyle() & orientation) == 0) {
				toolBar.dispose();
			}
			manager.setStyle(style);
		}
		ToolBar btoolbar = manager.createControl(parent);
		btoolbar.setData(manager);
		btoolbar.setData(AbstractPartRenderer.OWNING_ME, element);
		
		btoolbar.requestLayout();
		
		if (element.getElementId().startsWith("vertical.org.wcs.smart")) {		
			FontData f = btoolbar.getFont().getFontData()[0];
			
			f.setHeight(f.getHeight() - 1);
			Font smaller = new Font(btoolbar.getDisplay(), f);
			btoolbar.setFont(smaller);
			btoolbar.addListener(SWT.Dispose, e->smaller.dispose());
		}
		return btoolbar;
	}

	int getOrientation(final MUIElement element) {
		MUIElement theParent = element.getParent();
		if (theParent instanceof MTrimBar) {
			MTrimBar trimContainer = (MTrimBar) theParent;
			SideValue side = trimContainer.getSide();
			if (side.getValue() == SideValue.LEFT_VALUE || side.getValue() == SideValue.RIGHT_VALUE) {
				return SWT.VERTICAL;
			}
		}
		if (element.getElementId().equals("org.wcs.smart.perspectives")){
			return SWT.HORIZONTAL;	
		}else if (element.getElementId().startsWith("vertical.org.wcs.smart")) {
			return SWT.VERTICAL | SWT.RIGHT;	
		}
		return SWT.HORIZONTAL;
	}

}
