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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.GridLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.RadialLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.diagram.style.RelationshipDiagramStyleLabelProvider;
import org.wcs.smart.i2.diagram.style.RelationshipDiagramStyleListLoadJob;
import org.wcs.smart.i2.diagram.style.RelationshipDiagramStyleLoadJob;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

/**
 * Composite that contains controls related to relationship graph visualization.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphComposite extends Composite {
	
	private static final String PREFERENCE_DEPTH_KEY            = "org.wcs.smart.i2.diagram.filter.depth."; //$NON-NLS-1$
	private static final String PREFERENCE_STYLE_KEY            = "org.wcs.smart.i2.diagram.filter.style."; //$NON-NLS-1$
	private static final String PREFERENCE_LAYOUT_ALGORITHM_KEY = "org.wcs.smart.i2.diagram.filter.layoutalgorithm."; //$NON-NLS-1$

	private IEditorPart parentEditor;
	private FormToolkit toolkit;

	private RelationshipGraphFilterComposite cmpFilter;
	private ComboViewer cmbStyle;
	private ComboViewer cmbLayout;
	private GraphViewer graphViewer;
	private RelationshipGraphLabelProvider graphLabelProvider;
	private RelationshipGraphContentProvider graphContentProvider;
	
	private IntelEntity[] roots;
	private IRelationshipGraphData graphDelayedInput;
	private boolean graphDelayedRefresh;

	private LayoutAlgorithm defaultLayoutAlogorithm;
	private Map<LayoutAlgorithm, String> layoutAlgorithms;

	private EventHandler styleHandler = new EventHandler() {
		@Override
		public void handleEvent(Event event) {
			Object data = event.getProperty(IEventBroker.DATA);
			if (data instanceof List) {
				updateStylesInput((List<?>) data);
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
			Object input = graphDelayedInput != null ? graphDelayedInput : graphViewer.getInput();
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
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!RelationshipGraphComposite.this.isDisposed()) {
						delayedUpdateGraph(graphData, false);
					}
				}		
			});
		}
	};

	private RelationshipDiagramStyleListLoadJob loadStyleListJob = new RelationshipDiagramStyleListLoadJob() {
		@Override
		protected void processData(List<RelationshipDiagramStyle> styles) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					updateStylesInput(styles);
				}		
			});
		}
	};
	
	private RelationshipDiagramStyleLoadJob loadStyleJob = new RelationshipDiagramStyleLoadJob() {
		@Override
		protected void processData(RelationshipDiagramStyle style) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!RelationshipGraphComposite.this.isDisposed()) {
						graphLabelProvider.setStyle(style);
						delayedUpdateGraph(null, true);
					}
				}		
			});
		}
	};

	private IPartListener2 partListener = new IPartListener2() {
		
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			if (partRef.getPart(false) == parentEditor) {
				delayedUpdateGraph(graphDelayedInput, graphDelayedRefresh);
			}
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {}
		
		@Override
		public void partVisible(IWorkbenchPartReference partRef) {}
		
		@Override
		public void partOpened(IWorkbenchPartReference partRef) {}
		
		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {}
		
		@Override
		public void partHidden(IWorkbenchPartReference partRef) {}
		
		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {}
		
		@Override
		public void partClosed(IWorkbenchPartReference partRef) {}
		
	};
	
	
	public RelationshipGraphComposite(Composite parent, FormToolkit toolkit, IEditorPart parentEditor) {
		super(parent, SWT.NONE);
		this.parentEditor = parentEditor;
		initLayoutAlgorithms();
		this.toolkit = toolkit;
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = layout.verticalSpacing = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}

	private void initLayoutAlgorithms() {
		defaultLayoutAlogorithm = new RadialLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING );
		layoutAlgorithms = new HashMap<>();
		layoutAlgorithms.put(defaultLayoutAlogorithm, Messages.RelationshipGraphComposite_LayoutAlogorithm_Radial);
		layoutAlgorithms.put(new GridLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING ), Messages.RelationshipGraphComposite_LayoutAlogorithm_Grid);
		layoutAlgorithms.put(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING ), Messages.RelationshipGraphComposite_LayoutAlogorithm_Spring);
		layoutAlgorithms.put(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING ), Messages.RelationshipGraphComposite_LayoutAlogorithm_Tree);
	}

	private void createContent(Composite parent) {
		Section topSection = toolkit.createSection(parent, Section.TWISTIE | Section.TITLE_BAR | Section.EXPANDED);
		topSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		topSection.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				topSection.getParent().layout(true, true);
			}
		});
		topSection.setText(Messages.RelationshipGraphComposite_Settings);
		
		Composite topCmp = toolkit.createComposite(topSection, SWT.NONE);
		topCmp.setLayout(new GridLayout(2, false));
		topCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)topCmp.getLayout()).marginWidth = 0;
		((GridLayout)topCmp.getLayout()).marginHeight = 0;
		topSection.setClient(topCmp);

		cmpFilter = new RelationshipGraphFilterComposite(topCmp);
		
		Group rightCmp = new Group(topCmp, SWT.NONE);
		rightCmp.setText(Messages.RelationshipGraphComposite_LayoutLbl);
		rightCmp.setLayout(new GridLayout(2, false));
		rightCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)rightCmp.getLayoutData()).minimumWidth = 200; //NOTE: need this to fix layout issues caused by CheckBoxDropDown located in RelationshipGraphFilterComposite
		
		toolkit.createLabel(rightCmp, Messages.RelationshipGraphComposite_Style);

		cmbStyle = new ComboViewer(rightCmp, SWT.READ_ONLY | SWT.BORDER);
		cmbStyle.getControl().setBackground(cmbStyle.getControl().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbStyle.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbStyle.setContentProvider(ArrayContentProvider.getInstance());
		cmbStyle.setLabelProvider(new RelationshipDiagramStyleLabelProvider());
		cmbStyle.setInput(Arrays.asList(DialogConstants.LOADING_TEXT));
		cmbStyle.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) cmbStyle.getStructuredSelection();
				loadStyleJob.cancel();
				RelationshipDiagramStyle st = (RelationshipDiagramStyle)selection.getFirstElement();
				if (st != null) {
					String id = getPreferencesId();
					if (id != null && !id.isEmpty()) {
						Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(PREFERENCE_STYLE_KEY + id, UuidUtils.uuidToString(st.getUuid()));
					}
					loadStyleJob.setUuid(st.getUuid());
					loadStyleJob.schedule();
				}
			}
		});

		toolkit.createLabel(rightCmp, Messages.RelationshipGraphComposite_Layout);

		cmbLayout = new ComboViewer(rightCmp, SWT.READ_ONLY | SWT.BORDER);
		cmbLayout.getControl().setBackground(cmbLayout.getControl().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbLayout.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbLayout.setContentProvider(ArrayContentProvider.getInstance());
		cmbLayout.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return layoutAlgorithms.get(element);
			}
		});
		List<Entry<LayoutAlgorithm, String>> algEntries = new ArrayList<>(layoutAlgorithms.entrySet());
		Collections.sort(algEntries, (e1, e2) -> e1.getValue().compareTo(e2.getValue()));
		cmbLayout.setInput(algEntries.stream().map(e -> e.getKey()).toArray());
		cmbLayout.setSelection(new StructuredSelection(defaultLayoutAlogorithm));
		cmbLayout.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) cmbLayout.getStructuredSelection();
				if (selection != null && !selection.isEmpty()) {
					LayoutAlgorithm layoutAlgorithm = (LayoutAlgorithm) selection.getFirstElement();
					graphViewer.setLayoutAlgorithm(layoutAlgorithm);
					String id = getPreferencesId();
					if (id != null && !id.isEmpty()) {
						Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(PREFERENCE_LAYOUT_ALGORITHM_KEY + id, layoutAlgorithm.getClass().getCanonicalName());
					}
					graphViewer.applyLayout();
				}
			}
		});
		
		
		Composite mainCmp = toolkit.createComposite(parent, SWT.NONE);
		mainCmp.setLayout(new FillLayout(SWT.VERTICAL));
		mainCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		graphContentProvider = new RelationshipGraphContentProvider();
		graphLabelProvider = new RelationshipGraphLabelProvider(graphContentProvider);
		graphViewer = new GraphViewer(mainCmp, SWT.NONE);
		graphViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		graphViewer.createControl(mainCmp, SWT.NONE);
		
		graphViewer.setContentProvider(graphContentProvider);
		graphViewer.setLabelProvider(graphLabelProvider);
		graphViewer.setLayoutAlgorithm(defaultLayoutAlogorithm);

		cmpFilter.addFilterChangeListener(new IRelationshipGraphFilterChangeListener() {
			@Override
			public void filterChanged(RelationshipGraphFilterData filterData) {
				String id = getPreferencesId();
				if (id != null && !id.isEmpty()) {
					Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(PREFERENCE_DEPTH_KEY + id, filterData.getDepth());
				}
				refreshGraphContent();
			}
		});
		
		loadStyleListJob.schedule();

		IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		eventBroker.subscribe(IntelEvents.GRAPH_STYLESET_CHANGED, styleHandler);
		eventBroker.subscribe(IntelEvents.ENTITY_ALL, entityHandler);

		parentEditor.getSite().getPage().addPartListener(partListener);
	}

	public void setInput(IntelEntity... entity) {
		roots = entity;
		applyPreferences();
		refreshGraphContent();
	}

	private void updateStylesInput(List<?> newStyles) {
		if (cmbStyle.getControl().isDisposed()) {
			return;
		}
		IStructuredSelection selection = (IStructuredSelection) cmbStyle.getStructuredSelection();
		Object selObj = selection.getFirstElement();
		cmbStyle.setInput(newStyles);
		if (selObj == null) {
			String id = getPreferencesId();
			applyPreferenceStyle(id);
		}
		if (!newStyles.contains(selObj)) {
			selObj = newStyles.get(0);
		}
		cmbStyle.setSelection(new StructuredSelection(selObj));
	}

	private void delayedUpdateGraph(IRelationshipGraphData input, boolean refresh) {
		IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (part == parentEditor) {
			//graph is in active editor and we need to set input right away
			graphDelayedInput = null; //reset as it is not relevant
			graphDelayedRefresh = false;
			if (input != null) {
				graphViewer.setInput(input);
			} 
			if (refresh) {
				graphViewer.getControl().setBackground(graphViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WHITE));
				graphViewer.refresh();
			}
			
		} else {
			//graph is in non-active editor and can delay update for graph input
			if (input != null) {
				graphDelayedInput = input;
			}
			graphDelayedRefresh = graphDelayedRefresh || refresh;
		}
	}
	
	private void refreshGraphContent() {
		loadGraphDataJob.cancel();
		if (!this.isDisposed()) {
			loadGraphDataJob.setRoots(roots);
			loadGraphDataJob.setFilterData(cmpFilter.getFilterData());
			loadGraphDataJob.schedule();
		}
	}

	private void applyPreferences() {
		String id = getPreferencesId();
		applyPreferenceDepth(id);
		applyPreferenceLayoutAlgorithm(id);
		applyPreferenceStyle(id);
	}

	private void applyPreferenceDepth(String id) {
		if (id == null || id.isEmpty()) return;
		int depth = Intelligence2PlugIn.getDefault().getPreferenceStore().getInt(PREFERENCE_DEPTH_KEY + id);
		if (depth != 0) {
			cmpFilter.setFilterDepth(depth);
		}
	}

	private void applyPreferenceLayoutAlgorithm(String id) {
		if (id == null || id.isEmpty()) return;
		String layoutAlgClass = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_LAYOUT_ALGORITHM_KEY + id);
		if (layoutAlgClass != null && !layoutAlgClass.isEmpty()) {
			for (LayoutAlgorithm layoutAlgorithm : layoutAlgorithms.keySet()) {
				if (layoutAlgClass.equals(layoutAlgorithm.getClass().getCanonicalName())) {
					cmbLayout.setSelection(new StructuredSelection(layoutAlgorithm));
					return;
				}
			}
		}
	}

	private void applyPreferenceStyle(String id) {
		if (id == null || id.isEmpty()) return;
		String styleUuidStr = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_STYLE_KEY + id);
		if (styleUuidStr != null && !styleUuidStr.isEmpty()) {
			Object input = cmbStyle.getInput();
			if (input instanceof List<?>) {
				List<?> styleList = (List<?>) input;
				UUID styleUuid = UuidUtils.stringToUuid(styleUuidStr);
				for (Object obj : styleList) {
					if (obj instanceof UuidItem) {
						UuidItem style = (UuidItem) obj;
						if (styleUuid.equals(style.getUuid())) {
							cmbStyle.setSelection(new StructuredSelection(style));
							return;
						}
					}
				}
			}
		}
	}
	
	private String getPreferencesId() {
		return roots != null && roots.length == 1 && roots[0].getUuid() != null ? UuidUtils.uuidToString(roots[0].getUuid()) : null;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		parentEditor.getSite().getPage().removePartListener(partListener);
		IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		eventBroker.unsubscribe(styleHandler);
		eventBroker.unsubscribe(entityHandler);
	}

}
