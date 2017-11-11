package org.wcs.smart.asset.ui.views.stationlocation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttributeValue;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.asset.model.AssetMetadataMapping.AssetField;
import org.wcs.smart.asset.ui.AttributeFieldEditor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class StationLocationDetailsPage {

	private static final String LOCATION_KEY = "STATION";

	private StationLocationEditor parentEditor;
	
//	private Label lblPosition;
	private List<AttributeFieldEditor> fieldEditors;
	
	private Composite attributePanel;
	private FormToolkit toolkit;
	
	private AssetStationLocationAttributeValue tmpLocationAttribute;
	private AttributeFieldEditor locFieldEditor;
	
	private boolean isInitializing = false;
	
	public StationLocationDetailsPage(StationLocationEditor editor) {
		this.parentEditor = editor;
		
		AssetAttribute tmp = new AssetAttribute();
		tmp.setType(AttributeType.POSITION);
		tmp.setName("Position");
		tmpLocationAttribute = new AssetStationLocationAttributeValue();
		tmpLocationAttribute.setAttribute(tmp);
	}
	
	public void createControl(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		Composite toppanel = toolkit.createComposite(panel, SWT.BORDER);
		toppanel.setLayout(new GridLayout(3, false));
		toppanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		locFieldEditor = new AttributeFieldEditor(toppanel, tmpLocationAttribute.getAttribute());
		locFieldEditor.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!locFieldEditor.isValid()) return;
				if (isInitializing) return;
				if (locFieldEditor.updateValue(tmpLocationAttribute)) {
					AssetStationLocation location = (AssetStationLocation) attributePanel.getData(LOCATION_KEY);
					if (location != null) {
						location.setX(tmpLocationAttribute.getNumberValue());
						location.setY(tmpLocationAttribute.getNumberValue2());
					}
				}
				parentEditor.setDirty(true);
				
			}
		});

		Composite attributeComp = toolkit.createComposite(panel, SWT.BORDER);
		attributeComp.setLayout(new GridLayout());
		attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label l = toolkit.createLabel(attributeComp, "Attributes");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.setFont(boldFont);
		l.addListener(SWT.Dispose,  e-> boldFont.dispose());
		
		ScrolledComposite attributes = new ScrolledComposite(attributeComp,  SWT.V_SCROLL);
		attributes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributes.setExpandHorizontal(true);
		attributes.setExpandVertical(true);
		
		toolkit.adapt(attributes);
		attributePanel = toolkit.createComposite(attributes);
		attributes.setContent(attributePanel);
		attributePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributePanel.setLayout(new GridLayout());
	}
	
	public void initializeAttributes(AssetStationLocation location) {

		tmpLocationAttribute.setNumberValue(location.getX());
		tmpLocationAttribute.setNumberValue2(location.getY());
		
		try {
			isInitializing = true;
			locFieldEditor.initControl(tmpLocationAttribute);
			attributePanel.setData(LOCATION_KEY, location);
		}finally {
			isInitializing = false;
		}
		
		for (Control c : attributePanel.getChildren()) c.dispose();
		fieldEditors = new ArrayList<>();
		
		ScrolledComposite scroll = new ScrolledComposite(attributePanel, SWT.V_SCROLL);
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(scroll);
		
		Composite attributes = toolkit.createComposite(scroll);
		attributes.setLayout(new GridLayout(2, false));
		scroll.setContent(attributes);
		
		List<AssetStationLocationAttribute> locationAttributes = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			locationAttributes.addAll(
					session.createQuery("FROM AssetStationLocationAttribute WHERE attribute.conservationArea = :ca")
					.setParameter("ca",  SmartDB.getCurrentConservationArea())
					.list());
			locationAttributes.forEach(ss->{
				ss.getAttribute().getName();
				if (ss.getAttribute().getAttributeList() != null) ss.getAttribute().getAttributeList().forEach(li->li.getName());
			});
		}
		
		for (AssetStationLocationAttribute attribute : locationAttributes) {
			
			AttributeFieldEditor editor = new AttributeFieldEditor(attributes, attribute.getAttribute());
			editor.adapt(toolkit);
			fieldEditors.add(editor);
			if (editor.getTextAttributeControl() != null) {
				editor.getTextAttributeControl().addListener(SWT.Resize, e-> scroll.setMinSize(attributes.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
			}
			
			for (AssetStationLocationAttributeValue v : location.getAttributeValues()) {
				if (v.getAttribute().equals(attribute.getAttribute())) editor.initControl(v);
			}
			
			editor.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (!editor.isValid()) return;
					AssetStationLocationAttributeValue toUpdate = null;
					for (AssetStationLocationAttributeValue v : location.getAttributeValues()) {
						if (v.getAttribute().equals(editor.getAttribute())) {
							toUpdate = v;
							break;
						}
					}
					boolean isNew = false;
					if (toUpdate == null) {
						isNew = true;
						toUpdate = new AssetStationLocationAttributeValue();
						toUpdate.setStationLocation(location);
						toUpdate.setAttribute(editor.getAttribute());
					}
					if (editor.updateValue(toUpdate)) {
						if (isNew) location.getAttributeValues().add(toUpdate);
					}else {
						if (!isNew) location.getAttributeValues().remove(toUpdate);
					}
					parentEditor.setDirty(true);
					
				}
			});
		}
		scroll.setMinSize(attributes.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
}
