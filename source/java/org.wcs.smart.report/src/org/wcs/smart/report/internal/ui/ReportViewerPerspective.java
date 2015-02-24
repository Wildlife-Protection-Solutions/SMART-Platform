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
package org.wcs.smart.report.internal.ui;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.Event;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.internal.ui.viewer.ReportView;
import org.wcs.smart.ui.ConservationAreaListView;

/**
 * View report perspective
 * @author egouge
 * @since 1.0.0
 */
public class ReportViewerPerspective  implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.report.ReportViewerPerspective"; //$NON-NLS-1$

	public static final String VIEWER_AREA_ID = "org.wcs.smart.report.viewer"; //$NON-NLS-1$
	/**
	 * Constructs a new Default layout engine.
	 */

	public ReportViewerPerspective() {
		super();
	}

	/**
	 * Defines the initial layout for a perspective.
	 * 
	 * Implementors of this method may add additional views to a perspective.
	 * The perspective already contains an editor folder with
	 * <code>ID = ILayoutFactory.ID_EDITORS</code>. Add additional views to the
	 * perspective in reference to the editor folder.
	 * 
	 * This method is only called when a new perspective is created. If an old
	 * perspective is restored from a persistence file then this method is not
	 * called.
	 * 
	 * @param layout
	 *            the factory used to add views to the perspective
	 */
	public void createInitialLayout(IPageLayout layout) {
		defineLayout(layout);
		
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		ContextInjectionFactory.inject(this, ctx);
	}

	/**
	 * Defines the initial layout for a page.
	 */
	private void defineLayout(IPageLayout layout) {
		// Editors are placed for free.
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		
		layout.addView(ReportListView.ID, IPageLayout.LEFT, (float)0.26, editorArea);
		layout.getViewLayout(ReportListView.ID).setCloseable(false);
		
		IFolderLayout reportsArea = layout.createFolder(VIEWER_AREA_ID, IPageLayout.RIGHT, 0.26f, ReportListView.ID);
		reportsArea.addPlaceholder(ReportView.ID + ":*"); //$NON-NLS-1$
		
		
		if (SmartDB.isMultipleAnalysis()){
			layout.addView(ConservationAreaListView.ID, IPageLayout.TOP, 0.3f, ReportListView.ID);
		}
	}
	
	@Inject
	@Optional
	private void perspectiveOpened(@UIEventTopic(UIEvents.UILifeCycle.PERSPECTIVE_OPENED) Event event, IEclipseContext ctx){
		if ( ((MPerspective)event.getProperty(UIEvents.EventTags.ELEMENT)).getElementId().equals(ID)){
			updateTags(ctx);
		}
	}
	
	@Inject
	@Optional
	private void perspectiveReset(@UIEventTopic(UIEvents.UILifeCycle.PERSPECTIVE_RESET) Event event, IEclipseContext ctx){
		if ( ((MPerspective)event.getProperty(UIEvents.EventTags.ELEMENT)).getElementId().equals(ID)){
			updateTags(ctx);
		}
	}
	
	/**
	 * ensure the viewer area is always visible
	 * @param ctx
	 */
	private void updateTags(IEclipseContext ctx){
		MApplication app = ctx.get(MApplication.class);
		EModelService mService = ctx.get(EModelService.class);
		MPartStack stack = (MPartStack) mService.find(ReportViewerPerspective.VIEWER_AREA_ID, app);
		if (stack != null && !stack.getTags().contains(IPresentationEngine.NO_AUTO_COLLAPSE)){
			stack.getTags().add(IPresentationEngine.NO_AUTO_COLLAPSE);
			stack.setToBeRendered(true);
		}
	}
}