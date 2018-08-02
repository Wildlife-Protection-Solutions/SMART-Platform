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
import org.eclipse.gef.layout.ILayoutAlgorithm;
import org.eclipse.gef.layout.algorithms.HorizontalShiftAlgorithm;
import org.eclipse.gef.layout.algorithms.RadialLayoutAlgorithm;
import org.eclipse.gef.layout.algorithms.SpaceTreeLayoutAlgorithm;
import org.eclipse.gef.layout.algorithms.SpringLayoutAlgorithm;
import org.eclipse.gef.layout.algorithms.SugiyamaLayoutAlgorithm;
import org.eclipse.gef.layout.algorithms.TreeLayoutAlgorithm;
import org.eclipse.gef.zest.fx.jface.ZestContentViewer;
import org.eclipse.gef.zest.fx.jface.ZestFxJFaceModule;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
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

	private FormToolkit toolkit;

	private RelationshipGraphFilterComposite cmpFilter;
	private ComboViewer cmbStyle;
	private ComboViewer cmbLayout;
	private ZestContentViewer graphViewer;
	private RelationshipGraphLabelProvider graphLabelProvider;
	private RelationshipGraphContentProvider graphContentProvider;
	
	private IntelEntity[] roots;

	private ILayoutAlgorithm defaultLayoutAlogorithm;
	private Map<ILayoutAlgorithm, String> layoutAlgorithms;

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

	private RelationshipDiagramStyleListLoadJob loadStyleListJob = new RelationshipDiagramStyleListLoadJob() {
		@Override
		protected void processData(List<RelationshipDiagramStyle> styles) {
			Display.getDefault().syncExec(new Runnable() {
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
		initLayoutAlgorithms();
		this.toolkit = toolkit;
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = layout.verticalSpacing = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}

	private void initLayoutAlgorithms() {
		defaultLayoutAlogorithm = new RadialLayoutAlgorithm();
		layoutAlgorithms = new HashMap<>();
		layoutAlgorithms.put(defaultLayoutAlogorithm, Messages.RelationshipGraphComposite_LayoutAlogorithm_Radial);
		layoutAlgorithms.put(new SpringLayoutAlgorithm(), Messages.RelationshipGraphComposite_LayoutAlogorithm_Spring);
		layoutAlgorithms.put(new TreeLayoutAlgorithm(), Messages.RelationshipGraphComposite_LayoutAlogorithm_Tree);
		layoutAlgorithms.put(new SugiyamaLayoutAlgorithm(), Messages.RelationshipGraphComposite_LayoutAlogorithm_Sugiyama);
		layoutAlgorithms.put(new SpaceTreeLayoutAlgorithm(), Messages.RelationshipGraphComposite_LayoutAlogorithm_SpaceTree);
		layoutAlgorithms.put(new HorizontalShiftAlgorithm(), Messages.RelationshipGraphComposite_LayoutAlogorithm_HorizontalShift);
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
		topSection.setClient(topCmp);

		cmpFilter = new RelationshipGraphFilterComposite(topCmp);
		
		Composite rightCmp = toolkit.createComposite(topCmp, SWT.NONE);
		rightCmp.setLayout(new GridLayout(2, false));
		rightCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)rightCmp.getLayoutData()).minimumWidth = 200; //NOTE: need this to fix layout issues caused by CheckBoxDropDown located in RelationshipGraphFilterComposite
		
		toolkit.createLabel(rightCmp, Messages.RelationshipGraphComposite_Style);

		cmbStyle = new ComboViewer(rightCmp, SWT.READ_ONLY | SWT.BORDER);
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
		cmbLayout.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbLayout.setContentProvider(ArrayContentProvider.getInstance());
		cmbLayout.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return layoutAlgorithms.get(element);
			}
		});
		List<Entry<ILayoutAlgorithm, String>> algEntries = new ArrayList<>(layoutAlgorithms.entrySet());
		Collections.sort(algEntries, (e1, e2) -> e1.getValue().compareTo(e2.getValue()));
		cmbLayout.setInput(algEntries.stream().map(e -> e.getKey()).toArray());
		cmbLayout.setSelection(new StructuredSelection(defaultLayoutAlogorithm));
		cmbLayout.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) cmbLayout.getStructuredSelection();
				if (selection != null && !selection.isEmpty()) {
					ILayoutAlgorithm layoutAlgorithm = (ILayoutAlgorithm) selection.getFirstElement();
					graphViewer.setLayoutAlgorithm(layoutAlgorithm);
					String id = getPreferencesId();
					if (id != null && !id.isEmpty()) {
						Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(PREFERENCE_LAYOUT_ALGORITHM_KEY + id, layoutAlgorithm.getClass().getCanonicalName());
					}
					graphViewer.refresh();
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
	
	private void refreshGraphContent() {
		loadGraphDataJob.cancel();
		loadGraphDataJob.setRoots(roots);
		loadGraphDataJob.setFilterData(cmpFilter.getFilterData());
		loadGraphDataJob.schedule();
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
			for (ILayoutAlgorithm layoutAlgorithm : layoutAlgorithms.keySet()) {
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
		IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		eventBroker.unsubscribe(styleHandler);
		eventBroker.unsubscribe(entityHandler);
	}

}
