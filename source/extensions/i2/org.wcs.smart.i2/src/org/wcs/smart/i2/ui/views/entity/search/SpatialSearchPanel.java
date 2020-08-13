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
package org.wcs.smart.i2.ui.views.entity.search;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntitySearch;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.search.SpatialEntitySearch;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.dialogs.SelectPointMapDialog;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.ui.CheckBoxDropDown;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;


/**
 * Spatial search panel for entity search view
 * 
 * @author Emily
 *
 */
public class SpatialSearchPanel extends Composite{
	
	private static final String CUSTOM_LOCATION = Messages.SpatialSearchPanel_CustomLabel;
	
	private ComboViewer cmbLocations;
	private CheckBoxDropDown cmbEntityTypeFilters;
	private Text txtMaxDistance;
	private ControlDecoration cdDistance;
	private ControlDecoration cdLocation;
	
	private List<Object> locations;
	private FormToolkit toolkit;
	private EntitySearchView view;
	private Button btnSearch;
	
	
	@Inject
	private IEventBroker eventBroker;
	
	@Inject
	private EPartService partService;
	
	public SpatialSearchPanel(Composite parent, IEclipseContext context, FormToolkit toolkit, EntitySearchView view) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		toolkit.adapt(this);
		
		this.view = view;
		
		ContextInjectionFactory.inject(this, context);
		locations =new ArrayList<>();
		locations.add(CUSTOM_LOCATION);
		
		createControls();
	}
	
	private boolean validate() {
		if (!IntelSecurityManager.INSTANCE.canViewEntityAny()) {
			btnSearch.setEnabled(false);
			return false;
		}
	
		Object location = cmbLocations.getStructuredSelection().getFirstElement() ;
		boolean ok = (location instanceof RecordEditorInput || location instanceof CustomLocation);
		if (!ok) {
			cdLocation.show();
			cdLocation.setDescriptionText(Messages.SpatialSearchPanel_LocationsRequired);
		}else {
			cdLocation.hide();
		}
		try {
			cdDistance.hide();
			Double d = Double.parseDouble(txtMaxDistance.getText());
			if (d < 0) {
				cdDistance.setDescriptionText(Messages.SpatialSearchPanel_InvalidDistance);
				cdDistance.show();
				ok = false;
			}
		}catch (Exception ex) {
			cdDistance.setDescriptionText(Messages.SpatialSearchPanel_InvalidDistance1);
			cdDistance.show();
			ok = false;
		}
		btnSearch.setEnabled(ok);
		return ok;
	}
	
	public String getEntityTypeFilters() {
		StringBuilder sb = new StringBuilder();
		for (Object x : cmbEntityTypeFilters.getCheckObjects()) {
			if (x instanceof IntelEntityTypeAttribute) {
				IntelEntityTypeAttribute a = (IntelEntityTypeAttribute)x;
				sb.append(a.getEntityType().getKeyId());
				sb.append(SpatialEntitySearch.ATTRIBUTE_SEPERATOR);
				sb.append(a.getAttribute().getKeyId());
				sb.append(IntelEntitySearch.SEPARATOR);
			}
		}
		if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	public Double getDistance() {
		return Double.parseDouble(txtMaxDistance.getText());
	}
	public void refresh() {
		cmbEntityTypeFilters.setInput(Collections.singletonList(DialogConstants.LOADING_TEXT));
		loadEntityTypes.schedule();	
	}
	
	private void doSearch() {
		Object element = cmbLocations.getStructuredSelection().getFirstElement();
		if (element instanceof RecordEditorInput) {
			view.doSpatialSearch(((RecordEditorInput) element).getUuid());
		}else if (element instanceof CustomLocation) {
			view.doSpatialSearch(((CustomLocation) element).getGeomtry());
		}
	}
	
	public void selectRecord(UUID recordUuid) {
		Object r = cmbLocations.getStructuredSelection().getFirstElement();
		if (r instanceof RecordEditorInput && ((RecordEditorInput) r).getUuid().equals(recordUuid)) return;
		
		RecordEditorInput temp = new RecordEditorInput(null, recordUuid,  null,  null,  null, null);
		cmbLocations.setSelection(new StructuredSelection(temp));
	}
	private void createControls() {
		setLayout(new GridLayout(2, false));
		
		Label l = toolkit.createLabel(this, Messages.SpatialSearchPanel_LocationsLabel, SWT.NONE);
		cdLocation = new ControlDecoration(l, SWT.RIGHT | SWT.TOP);
		cdLocation.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdLocation.setShowHover(true);
		cdLocation.setDescriptionText(Messages.SpatialSearchPanel_LocationsRequired);
		cdLocation.show();
		
		Composite temp = toolkit.createComposite(this);
		temp.setLayout(new GridLayout(2, false));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		cmbLocations = new ComboViewer(temp, SWT.DROP_DOWN | SWT.READ_ONLY);
		toolkit.adapt(cmbLocations.getControl(), true, true);
		cmbLocations.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbLocations.setContentProvider(ArrayContentProvider.getInstance());
		cmbLocations.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof RecordEditorInput) {
					return ((RecordEditorInput) element).getName();
				}else if (element == CUSTOM_LOCATION) {
					return CUSTOM_LOCATION;
				}else if (element instanceof CustomLocation) {
					return ((CustomLocation) element).getLabel();
				}
				return super.getText(element);
			}
		});
		cmbLocations.setInput(locations);
		
		Hyperlink elink = toolkit.createHyperlink(temp, Messages.SpatialSearchPanel_EditLabel, SWT.NONE);
		elink.setEnabled(false);
		elink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				Object x = cmbLocations.getStructuredSelection().getFirstElement();
				if (!(x instanceof CustomLocation)) return;
				CustomLocation toEdit = (CustomLocation)x;
				SelectPointMapDialog pointOnMap = new SelectPointMapDialog(getShell()) {
					@Override
					protected void createButtonsForButtonBar(Composite parent) {
						createButton(parent, -2, DialogConstants.DELETE_BUTTON_TEXT, false);
						createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
						createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
					}
					protected void buttonPressed(int buttonId) {
						if (buttonId == IDialogConstants.OK_ID || buttonId == IDialogConstants.CANCEL_ID) {
							super.buttonPressed(buttonId);
							return;
						}
						setReturnCode(-2);
						close();
					}
					
				};
				pointOnMap.setInitPoint(toEdit.x, toEdit.y);
				int code = pointOnMap.open();
				if (code == Window.OK) {
					toEdit.x = pointOnMap.getPoint().getX();
					toEdit.y = pointOnMap.getPoint().getY();
					cmbLocations.refresh();
					cmbLocations.setSelection(new StructuredSelection(toEdit));
				}else if (code == -2) {
					locations.remove(toEdit);
					cmbLocations.refresh();
				}
			}
		});
		
		cmbLocations.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (cmbLocations.getStructuredSelection().getFirstElement() instanceof CustomLocation) {
					elink.setEnabled(true);
				}else {
					elink.setEnabled(false);
				}
				if (cmbLocations.getStructuredSelection().getFirstElement() == CUSTOM_LOCATION) {
					SelectPointMapDialog pointOnMap = new SelectPointMapDialog(getShell()) {
						
					};
					if (pointOnMap.open() == Window.OK) {
						double x = pointOnMap.getPoint().getX();
						double y = pointOnMap.getPoint().getY();
						
						CustomLocation location = new CustomLocation(x,y);
						locations.add(location);
						cmbLocations.refresh();
						cmbLocations.setSelection(new StructuredSelection(location));
					}
				}
				if (validate()) {
					if (view.isSpatialActive()) doSearch();
				}
				
			}
		});
		
		l = toolkit.createLabel(this, Messages.SpatialSearchPanel_FiltersLabel, SWT.NONE);
		l.setToolTipText(Messages.SpatialSearchPanel_entitytypetooltip);
		
		cmbEntityTypeFilters = new CheckBoxDropDown(this);
		toolkit.adapt(cmbEntityTypeFilters.getParent(), true, true);
		cmbEntityTypeFilters.getParent().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbEntityTypeFilters.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityTypeFilters.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelEntityTypeAttribute) {
					IntelEntityTypeAttribute ii = (IntelEntityTypeAttribute)element;
					return ii.getEntityType().getName() + " [" + ii.getAttribute().getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return super.getText(element);
			}
		});
		cmbEntityTypeFilters.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (view.isSpatialActive()) doSearch();
			}
		});
		
		
		l = toolkit.createLabel(this, Messages.SpatialSearchPanel_DistanceLabel, SWT.NONE);
		l.setToolTipText(Messages.SpatialSearchPanel_DistanceTooltip);
		txtMaxDistance = toolkit.createText(this, "2000"); //$NON-NLS-1$
		txtMaxDistance.addListener(SWT.Modify, e->validate());
		txtMaxDistance.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		cdDistance = new ControlDecoration(l, SWT.RIGHT | SWT.TOP);
		cdDistance.hide();
		cdDistance.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdDistance.setShowHover(true);
		
		
		btnSearch = toolkit.createButton(this, Messages.SpatialSearchPanel_SearchLabel, SWT.PUSH);
		btnSearch.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 2, 1));
		btnSearch.addListener(SWT.Selection, (event)->{
			doSearch();
		});
		btnSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntityAny());
		btnSearch.setEnabled(false);
		
		loadEntityTypes.schedule();
		EventHandler refresh = e->{
			cmbEntityTypeFilters.setInput(Collections.singletonList(DialogConstants.LOADING_TEXT));
			loadEntityTypes.schedule();	
		};
		
		EventHandler onPartClose = e->{
			if (e.getProperty(UIEvents.EventTags.WIDGET) == null) {
				Object x = e.getProperty(UIEvents.EventTags.ELEMENT);
				if (x instanceof MPart ) {
					Object lpart = E3Utils.getSourceObject( ((MPart)x) ) ;
					if (lpart instanceof RecordEditor) {
						locations.remove(  ((RecordEditor)lpart).getEditorInput() );
						cmbLocations.refresh();
					}
				}
			}
		};
		eventBroker.subscribe(UIEvents.UIElement.TOPIC_WIDGET, onPartClose);

		EventHandler recordRenameHandler = e->cmbLocations.refresh();
		eventBroker.subscribe(IntelEvents.RECORD_MODIFIED, recordRenameHandler);
		
		IPartListener partListener = new IPartListener() {
			
			@Override
			public void partVisible(MPart part) {}
			
			@Override
			public void partHidden(MPart part) {}
			
			@Override
			public void partDeactivated(MPart part) {}
			
			@Override
			public void partBroughtToTop(MPart part) {}
			
			@Override
			public void partActivated(MPart part) {
				Object lpart = E3Utils.getSourceObject(part) ;
				if (lpart instanceof RecordEditor) {
					RecordEditorInput in = (RecordEditorInput) ((RecordEditor)lpart).getEditorInput();
					if (!locations.contains(in)) locations.add( in );
					cmbLocations.refresh();
				}
			}
		};
		partService.addPartListener(partListener);
		
		addListener(SWT.Dispose, e->{
			eventBroker.unsubscribe(refresh);
			eventBroker.unsubscribe(onPartClose);
			eventBroker.unsubscribe(recordRenameHandler);
			partService.removePartListener(partListener);
		});
		
		
	}
	
	
	private Job loadEntityTypes = new Job(Messages.SpatialSearchPanel_loadingtypesjobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<IntelEntityTypeAttribute> filterOptions = new ArrayList<>();
			List<IntelProfile> profiles = ProfilesManager.INSTANCE.getActiveProfiles().stream().filter(e->IntelSecurityManager.INSTANCE.canViewEntities(e)).collect(Collectors.toList());
			if (!profiles.isEmpty()) {
				try(Session session = HibernateManager.openSession()){
					Query<IntelEntityTypeAttribute> q = session.createQuery("SELECT DISTINCT a FROM IntelEntityTypeAttribute a join a.id.entityType t join t.profiles p join p.id.profile pp WHERE pp IN (:profiles) and a.id.attribute.conservationArea = :ca and a.id.attribute.type = :type", IntelEntityTypeAttribute.class); //$NON-NLS-1$
					q.setParameter("ca",  SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
					q.setParameter("type",  IntelAttribute.AttributeType.POSITION); //$NON-NLS-1$
					q.setParameterList("profiles",  profiles); //$NON-NLS-1$
					
					filterOptions.addAll(q.list());
					filterOptions.forEach(a->{
						a.getAttribute().getName();
						a.getEntityType().getName();
					});
				}
			}
			Display.getDefault().asyncExec(()->{
				cmbEntityTypeFilters.setInput(filterOptions);
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
	private class CustomLocation {
		public double x;
		public double y;
		public CustomLocation(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		public String getLabel() {
			DecimalFormat df = new DecimalFormat("#.####"); //$NON-NLS-1$
			return "(" + df.format(x) + ", " + df.format(y) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
		public Geometry getGeomtry() {
			return GeometryFactoryProvider.getFactory().createPoint(new Coordinate(x,y));
		}
	}
}
