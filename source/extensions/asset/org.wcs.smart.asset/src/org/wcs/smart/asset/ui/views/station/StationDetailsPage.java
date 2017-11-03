package org.wcs.smart.asset.ui.views.station;

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
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.asset.ui.AttributeFieldEditor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class StationDetailsPage {

	private StationEditor parentEditor;
	
	private Label lblPosition;
	private List<AttributeFieldEditor> fieldEditors;
	
	private Composite attributePanel;
	private FormToolkit toolkit;
	
	public StationDetailsPage(StationEditor editor) {
		this.parentEditor = editor;
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
		
		Label l = toolkit.createLabel(toppanel, "Position: ");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		lblPosition = toolkit.createLabel(toppanel, "");
		lblPosition.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite attributeComp = toolkit.createComposite(panel, SWT.BORDER);
		attributeComp.setLayout(new GridLayout());
		attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		l = toolkit.createLabel(attributeComp, "Attributes");
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
	
	public void initializeAttributes(AssetStation station) {
		
		String pnt = MessageFormat.format("POINT({0} {1})", station.getX(), station.getY());
		
		//TODO: crs
//		if (crs == null || crs == GeometryUtils.SMART_CRS || CRS.equalsIgnoreMetadata(crs, GeometryUtils.SMART_CRS)){
//			return "POINT( " + getNumberValue() +" " + getNumberValue2() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		}else{
//			try{
//				Coordinate c = ReprojectUtils.reproject(getNumberValue(), getNumberValue2(), GeometryUtils.SMART_CRS, crs);
//				return "POINT( " + c.x + " " + c.y + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//			}catch (Exception ex){
//				return "ERROR: " + ex.getMessage(); //$NON-NLS-1$
//			}
//		}
		
		lblPosition.setText(pnt);
		
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
		
		List<AssetStationAttribute> stationAttributes = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			stationAttributes.addAll(
					session.createQuery("FROM AssetStationAttribute WHERE attribute.conservationArea = :ca")
					.setParameter("ca",  SmartDB.getCurrentConservationArea())
					.list());
			stationAttributes.forEach(ss->{
				ss.getAttribute().getName();
				if (ss.getAttribute().getAttributeList() != null) ss.getAttribute().getAttributeList().forEach(li->li.getName());
			});
		}
		
		for (AssetStationAttribute attribute : stationAttributes) {
			
			AttributeFieldEditor editor = new AttributeFieldEditor(attributes, attribute.getAttribute());
			editor.adapt(toolkit);
			fieldEditors.add(editor);
			if (editor.getTextAttributeControl() != null) {
				editor.getTextAttributeControl().addListener(SWT.Resize, e-> scroll.setMinSize(attributes.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
			}
			
			for (AssetStationAttributeValue v : station.getAttributeValues()) {
				if (v.getAttribute().equals(attribute.getAttribute())) editor.initControl(v);
			}
			
			editor.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (!editor.isValid()) return;
					AssetStationAttributeValue toUpdate = null;
					for (AssetStationAttributeValue v : station.getAttributeValues()) {
						if (v.getAttribute().equals(editor.getAttribute())) {
							toUpdate = v;
							break;
						}
					}
					boolean isNew = false;
					if (toUpdate == null) {
						isNew = true;
						toUpdate = new AssetStationAttributeValue();
						toUpdate.setStation(station);
						toUpdate.setAttribute(editor.getAttribute());
					}
					if (editor.updateValue(toUpdate)) {
						if (isNew) station.getAttributeValues().add(toUpdate);
					}else {
						if (!isNew) station.getAttributeValues().remove(toUpdate);
					}
					parentEditor.setDirty(true);
					
				}
			});
		}
		scroll.setMinSize(attributes.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
}
