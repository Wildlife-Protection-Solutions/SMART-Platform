/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.ui.map;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.style.sld.SLDContent;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.SmartStyle;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.udig.style.MapLayerDefaultStyle;
import org.wcs.smart.udig.style.StyleImageProducer;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.util.UuidUtils;

/**
 * 
 * Composite for configuring different default styles for various map layers.
 * 
 * Plugins define the layers they want to configure via extension point. This
 * page allows them to match the layer to a default style.
 * 
 * @author Emily
 * @since 8.0.0
 */
public class DefaultMapLayerStylesComposite extends Composite {

	private List<SmartStyle> currentStyles;
	private Map<String, String> properties;

	private boolean isEditable = true;

	public DefaultMapLayerStylesComposite(Composite parent, int style) {
		this(parent, style, true);
	}

	public DefaultMapLayerStylesComposite(Composite parent, int style, boolean isEditable) {
		super(parent, style);
		this.isEditable = isEditable;
		create();
	}

	private void fireModified() {
		Event event = new Event();
		event.widget = this;
		for (Listener x : getListeners(SWT.Modify)) {
			x.handleEvent(event);
		}
	}

	private void create() {

		setLayout(new GridLayout());

		List<MapLayerDefaultStyle> defaults = Collections.emptyList();
		try {
			defaults = StyleManager.INSTANCE.getDefaultStyleMapLayers();
			defaults.sort((a,b)->{
				if (a.getMapName().equals(b.getMapName())) return Collator.getInstance().compare(a.getLayerName(), b.getLayerName());
				return Collator.getInstance().compare(a.getMapName(), b.getMapName());
			});
			
		} catch (CoreException ex) {
			SmartPlugIn.displayLog(ex.getMessage(), ex);
		}

		load();

		Composite header = new Composite(this, SWT.NONE);
		header.setLayout(new GridLayout(3, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout) header.getLayout()).marginWidth = 0;
		((GridLayout) header.getLayout()).marginHeight = 0;
		((GridLayout) header.getLayout()).horizontalSpacing = 15;

		Label h1 = new Label(header, SWT.NONE);
		h1.setText("Map");
		h1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Label h2 = new Label(header, SWT.NONE);
		h2.setText("Layer");
		h2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Label h3 = new Label(header, SWT.NONE);
		h3.setText("Style");
		h3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		FontData fd = h1.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(header.getShell().getDisplay(), fd);
		header.addListener(SWT.Dispose, e -> boldFont.dispose());
		h1.setFont(boldFont);
		h2.setFont(boldFont);
		h3.setFont(boldFont);

		ScrolledComposite scroll = new ScrolledComposite(this, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite c1 = new Composite(scroll, SWT.NONE);
		scroll.setContent(c1);

		c1.setLayout(new GridLayout(3, false));
		((GridLayout) c1.getLayout()).marginWidth = 0;
		((GridLayout) c1.getLayout()).marginHeight = 0;
		((GridLayout) c1.getLayout()).horizontalSpacing = 15;
		((GridLayout) c1.getLayout()).verticalSpacing = 8;
		c1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		List<SmartStyle> styles = currentStyles;
		List<Object> options = new ArrayList<>(styles);
		options.add(0, ""); //$NON-NLS-1$

		SmartStyleLabelProvider sslp = new SmartStyleLabelProvider() {
			@Override
			public Image getImage(Object element) {
				if (element instanceof SmartStyle){
					return super.getImage(element);
				}
				return null;
			}
		};
		
		
		List<TableComboViewer> viewers = new ArrayList<>();
		for (MapLayerDefaultStyle s : defaults) {
			Label l = new Label(c1, SWT.WRAP);
			l.setText(s.getMapName());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			
			l = new Label(c1, SWT.WRAP);
			l.setText(s.getLayerName());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			
			TableComboViewer cmbStyle = new TableComboViewer(c1, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
			viewers.add(cmbStyle);
			cmbStyle.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			((GridData) cmbStyle.getControl().getLayoutData()).widthHint = 200;
			cmbStyle.setContentProvider(ArrayContentProvider.getInstance());
			cmbStyle.setLabelProvider(sslp);
			cmbStyle.setInput(options);
			cmbStyle.getControl().setEnabled(isEditable);

			if (properties.containsKey(s.getKey()) && properties.get(s.getKey()) != null) {

				UUID uuid = UuidUtils.stringToUuid(properties.get(s.getKey()));
				for (SmartStyle ss : styles) {
					if (ss.getUuid().equals(uuid))
						cmbStyle.setSelection(new StructuredSelection(ss));
				}
			}

			cmbStyle.addSelectionChangedListener(e -> {
				Object sel = cmbStyle.getStructuredSelection().getFirstElement();
				if (sel instanceof SmartStyle) {
					properties.put(s.getKey(), UuidUtils.uuidToString(((SmartStyle) sel).getUuid()));
				} else {
					properties.put(s.getKey(), null);
				}
				fireModified();
			});
		}

		c1.setSize(c1.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		c1.layout();

		scroll.addListener(SWT.Resize, e -> {
			((GridData) h1.getLayoutData()).widthHint = c1.getChildren()[0].getBounds().width;
			((GridData) h2.getLayoutData()).widthHint = c1.getChildren()[1].getBounds().width;
			((GridData) h3.getLayoutData()).widthHint = c1.getChildren()[2].getBounds().width;
		});
		((GridData) h1.getLayoutData()).widthHint = c1.getChildren()[0].getBounds().width;
		((GridData) h2.getLayoutData()).widthHint = c1.getChildren()[1].getBounds().width;
		((GridData) h3.getLayoutData()).widthHint = c1.getChildren()[2].getBounds().width;

		//create style glyphs
		Job createGlphs = new Job("create glyphs") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (SmartStyle s : styles) {
					try {
						StyleBlackboard parsed = StyleManager.INSTANCE.fromString(s.getStyleString());
						Style sld = (Style) parsed.get(SLDContent.ID);
						if (sld != null)
							sslp.setGlyph(s, StyleImageProducer.INSTANCE.createImage(sld));
					} catch (Exception e) {
						SmartPlugIn.log(e.getMessage(), e);
					}

				}
				Display.getDefault().asyncExec(()->viewers.forEach(v->v.refresh()));
				return Status.OK_STATUS;
			}
			
		};
		createGlphs.schedule();
	}

	private void load() {
		try (Session session = HibernateManager.openSession()) {
			currentStyles = QueryFactory.buildQuery(session, SmartStyle.class,
					new Object[] { "conservationArea", SmartDB.getCurrentConservationArea() }).list(); //$NON-NLS-1$

			properties = StyleManager.INSTANCE.getDefaultStyles(SmartDB.getCurrentConservationArea(), session);

		}
	}

	public void save() throws Exception {
		try (Session session = HibernateManager.openSession()) {
			session.beginTransaction();
			try {
				StyleManager.INSTANCE.setDefaultStyles(SmartDB.getCurrentConservationArea(), properties, session);
				session.getTransaction().commit();
			} catch (Exception ex) {
				if (session.getTransaction().isActive())
					session.getTransaction().rollback();
			}

		}
	}
}
