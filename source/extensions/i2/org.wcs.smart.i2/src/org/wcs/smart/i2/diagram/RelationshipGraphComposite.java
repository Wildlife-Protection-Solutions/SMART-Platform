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
package org.wcs.smart.i2.diagram;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.gef.layout.algorithms.RadialLayoutAlgorithm;
import org.eclipse.gef.zest.fx.jface.ZestContentViewer;
import org.eclipse.gef.zest.fx.jface.ZestFxJFaceModule;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.i2.RelationshipDiagramManager;
import org.wcs.smart.i2.diagram.style.RelationshipDiagramStyleLabelProvider;
import org.wcs.smart.i2.diagram.style.RelationshipDiagramStyleLoadJob;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;

/**
 * Composite that contains controls related to relationship graph visualization.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphComposite extends Composite {

	private FormToolkit toolkit;

	private RelationshipGraphFilterComposite cmpFilter;
	private ComboViewer cmbStyle;
	private ZestContentViewer graphViewer;
	private RelationshipGraphLabelProvider graphLabelProvider;
	private RelationshipGraphContentProvider graphContentProvider;
	
	private IntelEntity[] roots;


	private EventHandler styleHandler = new EventHandler() {
		@Override
		public void handleEvent(Event event) {
			Object data = event.getProperty(IEventBroker.DATA);
			if (data instanceof List) {
				IStructuredSelection selection = (IStructuredSelection) cmbStyle.getStructuredSelection();
				Object selObj = selection.getFirstElement();
				List<?> lst = (List<?>) data;
				cmbStyle.setInput(lst);
				if (!lst.contains(selObj)) {
					selObj = lst.get(0);
				}
				cmbStyle.setSelection(new StructuredSelection(selObj));
			}
		}
	};

	private EventHandler entityHandler = new EventHandler() {
		@Override
		public void handleEvent(Event event) {
			Object data = event.getProperty(IEventBroker.DATA);
			if (data instanceof List) {
				processList((List<?>)data);
			} else {
				processList(Arrays.asList(data));
			}
		}
		
		private void processList(List<?> entities) {
			Object input = graphViewer.getInput();
			if (input instanceof IRelationshipGraphData) {
				IRelationshipGraphData graphData = (IRelationshipGraphData) input;
				Set<?> eSet = new HashSet<>(Arrays.asList(graphData.getEntities()));
				eSet.retainAll(entities);
				if (!eSet.isEmpty()) {
					refreshGraphContent();
				}
			}
		}
	};
	
	private RelationshipGraphLoadDataJob loadGraphDataJob = new RelationshipGraphLoadDataJob() {
		@Override
		protected void processData(IRelationshipGraphData graphData) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					if (!RelationshipGraphComposite.this.isDisposed()) {
						graphViewer.setInput(graphData);
					}
				}		
			});
		}
	};
	
	private RelationshipDiagramStyleLoadJob loadStyleJob = new RelationshipDiagramStyleLoadJob() {
		@Override
		protected void processData(RelationshipDiagramStyle style) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					if (!RelationshipGraphComposite.this.isDisposed()) {
						graphLabelProvider.setStyle(style);
						graphViewer.refresh();
					}
				}		
			});
		}
	};

	public RelationshipGraphComposite(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = layout.verticalSpacing = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}

	private void createContent(Composite parent) {
		Composite topCmp = toolkit.createComposite(parent, SWT.NONE);
		topCmp.setLayout(new GridLayout(2, false));
		topCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		cmpFilter = new RelationshipGraphFilterComposite(topCmp);
		
		Composite styleCmp = toolkit.createComposite(topCmp, SWT.NONE);
		styleCmp.setLayout(new GridLayout(2, false));
		styleCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)styleCmp.getLayoutData()).minimumWidth = 200; //NOTE: need this to fix layout issues caused by CheckBoxDropDown located in RelationshipGraphFilterComposite
		
		List<RelationshipDiagramStyle> stylesList = RelationshipDiagramManager.INSTANCE.loadStyles(getShell());

		toolkit.createLabel(styleCmp, "Style:");

		cmbStyle = new ComboViewer(styleCmp, SWT.READ_ONLY | SWT.BORDER);
		cmbStyle.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbStyle.setContentProvider(ArrayContentProvider.getInstance());
		cmbStyle.setLabelProvider(new RelationshipDiagramStyleLabelProvider());
		cmbStyle.setInput(stylesList);
		cmbStyle.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) cmbStyle.getStructuredSelection();
				loadStyleJob.cancel();
				RelationshipDiagramStyle st = (RelationshipDiagramStyle)selection.getFirstElement();
				if (st != null) {
					loadStyleJob.setUuid(st.getUuid());
					loadStyleJob.schedule();
				}
			}
		});

		Composite mainCmp = new Composite(parent, SWT.NONE);
		StackLayout stackLayout = new StackLayout();
		stackLayout.marginHeight = stackLayout.marginWidth = 0;
		mainCmp.setLayout(stackLayout);
		mainCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite sizingCmp = toolkit.createComposite(mainCmp, SWT.NONE);
		sizingCmp.setLayout(new GridLayout());

		Composite graphCmp = toolkit.createComposite(mainCmp, SWT.NONE);
		graphCmp.setLayout(new FillLayout(SWT.VERTICAL));

		((StackLayout)mainCmp.getLayout()).topControl = graphCmp; //NOTE: this is a hack to coordinate sizing of composites with GridLayout and FillLayout

		graphContentProvider = new RelationshipGraphContentProvider();
		graphLabelProvider = new RelationshipGraphLabelProvider(graphContentProvider);
		
		graphViewer = new ZestContentViewer(new ZestFxJFaceModule());
		graphViewer.createControl(graphCmp, SWT.NONE);
		graphViewer.setContentProvider(graphContentProvider);
		graphViewer.setLabelProvider(graphLabelProvider);
		graphViewer.setLayoutAlgorithm(new RadialLayoutAlgorithm());

		if (!stylesList.isEmpty()) {
			//TODO: do we want to persist selected style?
			cmbStyle.setSelection(new StructuredSelection(stylesList.get(0)));
		}

		cmpFilter.addFilterChangeListener(new IRelationshipGraphFilterChangeListener() {
			@Override
			public void filterChanged(RelationshipGraphFilterData filterData) {
				refreshGraphContent();
			}
		});

		IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		eventBroker.subscribe(IntelEvents.GRAPH_STYLESET_CHANGED, styleHandler);
		eventBroker.subscribe(IntelEvents.ENTITY_ALL, entityHandler);
		
	}

	public void setInput(IntelEntity... entity) {
		roots = entity;
		refreshGraphContent();
	}
	
	private void refreshGraphContent() {
		loadGraphDataJob.cancel();
		loadGraphDataJob.setRoots(roots);
		loadGraphDataJob.setFilterData(cmpFilter.getFilterData());
		loadGraphDataJob.schedule();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		eventBroker.unsubscribe(styleHandler);
		eventBroker.unsubscribe(entityHandler);
	}

}
