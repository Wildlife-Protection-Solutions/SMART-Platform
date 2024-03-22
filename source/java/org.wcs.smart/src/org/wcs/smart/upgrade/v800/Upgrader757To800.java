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
package org.wcs.smart.upgrade.v800;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.Collator;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.upgrade.AbstractInteralDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.util.UuidUtils;

/**
 * 7.0.0 to 7.5.0 upgrader
 * 
 * @author Emily
 *
 */
public class Upgrader757To800 extends AbstractInteralDatabaseUpgrader { 
	
	private Exception thrownException = null;

	private HashMap<ConservationArea, String> caTimeZoneMapping;
	
	
	public HashMap<ConservationArea, String> getCaTimeZoneMapping(){
		return this.caTimeZoneMapping;
	}
	
	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(Messages.Upgrader700To741_UpgradeMsg, UpgradeEngine.UpgradeFromVersion.V800.fromVersion, UpgradeEngine.UpgradeFromVersion.V800.toVersion));  
		thrownException = null;
		
		try(Session s = HibernateManager.openSession()){
			
			//open a dialog to confirm timezone settings for Conservation Area
			List<ConservationArea> cas = s.createQuery("FROM ConservationArea WHERE uuid != :ccaa ", ConservationArea.class) //$NON-NLS-1$
					.setParameter("ccaa", ConservationArea.MULTIPLE_CA) //$NON-NLS-1$
					.list();
			
			Display.getDefault().syncExec(()->{
				TimeZoneDialog dialog = new TimeZoneDialog(Display.getDefault().getActiveShell(), cas);
				dialog.open();
				caTimeZoneMapping = dialog.getMappings();			
				SmartContext.INSTANCE.setClass(Upgrader757To800.class, this);	
			});
			
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					s.beginTransaction();
					try {
						c.setAutoCommit(false);
						upgrade(c, monitor);
						c.setAutoCommit(true);
						s.getTransaction().commit();
					} catch (final Exception e) {
						thrownException = new Exception(MessageFormat.format(Messages.Upgrader700To741_UpgradeErrorMsage, UpgradeEngine.UpgradeFromVersion.V800.fromVersion, UpgradeEngine.UpgradeFromVersion.V800.toVersion), e); 
					}
				}
			});
		}
		if (thrownException != null)
			throw thrownException;

		monitor.done();
	}

	private void upgrade(Connection c, IProgressMonitor monitor)
			throws Exception {
		
		
		
		//drop entity plugin tables if they exists
		String[] sql = new String[] {
			"drop table smart.entity_gridded_query",  //$NON-NLS-1$
			"drop table smart.entity_observation_query",  //$NON-NLS-1$
			"drop table smart.entity_summary_query",  //$NON-NLS-1$
			"drop table smart.entity_waypoint_query",  //$NON-NLS-1$
			"drop table smart.entity_attribute_value",  //$NON-NLS-1$
			"drop table smart.entity",  //$NON-NLS-1$
			"drop table smart.entity_attribute",  //$NON-NLS-1$
			"drop table smart.entity_type"  //$NON-NLS-1$
		};
		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			try {
				c.createStatement().execute(s);
			}catch (Exception ex) {
				//don't worry if the table doesn't exists
			}
		}
		
		sql = new String[] {
		
			"delete from smart.db_version where plugin_id = 'org.wcs.smart.entity' or plugin_id = 'org.wcs.smart.entity.query'",  //$NON-NLS-1$
				
			//remove icon, iconfiles from conservation areas that are not referenced
			//leave any custom icons if they are used or not  
			"DELETE FROM smart.ICONFILE WHERE icon_uuid not in (select distinct icon_uuid from smart.dm_attribute where icon_uuid is not null union  select distinct icon_uuid from smart.DM_ATTRIBUTE_LIST where icon_uuid is not null union select distinct icon_uuid from smart.DM_ATTRIBUTE_TREE where icon_uuid is not null union select distinct icon_uuid from smart.DM_CATEGORY where icon_uuid is not null ) and  filename like 'platform%'", //$NON-NLS-1$
			"DELETE FROM smart.ICON WHERE uuid not in (SELECT icon_uuid FROM smart.iconfile)", //$NON-NLS-1$
				
			//add icon key to patrol attributes
			//modifications to constraints required
			"alter table smart.patrol_mandate drop constraint PATROL_MANDATE_CA_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_mandate ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER table smart.patrol_mandate ADD CONSTRAINT pm_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

			"ALTER TABLE smart.patrol_transport DROP CONSTRAINT PATROL_TRANSPORT_CA_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_transport ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER table smart.patrol_transport ADD CONSTRAINT ptransport_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
			"ALTER TABLE smart.team DROP CONSTRAINT TEAM_CA_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE smart.team ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER table smart.team ADD CONSTRAINT team_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
			"ALTER TABLE smart.patrol_attribute DROP CONSTRAINT PATROL_ATT_CA_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER table smart.patrol_attribute ADD CONSTRAINT patrol_attribute_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
			"ALTER TABLE smart.patrol_attribute_list ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_list ADD CONSTRAINT patrol_attribute_list_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE",  //$NON-NLS-1$
				
			"ALTER TABLE smart.station DROP CONSTRAINT STATION_CA_UUID_FK",   //$NON-NLS-1$
			"ALTER TABLE smart.station ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER TABLE smart.station ADD CONSTRAINT station_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE",  //$NON-NLS-1$
				
			"ALTER TABLE smart.iconset DROP CONSTRAINT iconset_cauuid_fk", //$NON-NLS-1$
			"ALTER TABLE smart.icon DROP CONSTRAINT icon_cauuid_fk", //$NON-NLS-1$
			"ALTER TABLE smart.iconset ADD CONSTRAINT iconset_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.icon ADD CONSTRAINT icon_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
			"ALTER TABLE SMART.PATROL_MANDATE ADD CONSTRAINT PATROL_MANDATE_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE SMART.patrol_transport ADD CONSTRAINT patrol_Transport_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE SMART.team ADD CONSTRAINT TEAM_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE SMART.station ADD CONSTRAINT STATION_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
			//increase size of property field
			"ALTER TABLE smart.conservation_area_property alter column value set data type varchar(32672)", //$NON-NLS-1$
				
			//geometry support
			"ALTER TABLE smart.wp_observation_attributes add column geom BLOB", //$NON-NLS-1$
			"ALTER TABLE smart.wp_observation_attributes add column number_value_2 double", //$NON-NLS-1$
				
			//hibernate 6 employee uuid cannot conflict with ccaa uuid
			"update smart.employee set uuid = x'00000000000000000000000000000001' where uuid = x'00000000000000000000000000000000'", //$NON-NLS-1$

			//waypoint last modified for timestamp change to utc
			"CREATE FUNCTION smart.localTsToUtcTs(ts timestamp, zoneid varchar(256)) returns timestamp LANGUAGE JAVA deterministic external name 'org.wcs.smart.util.DerbyUtils.localToUtc' PARAMETER STYLE JAVA NO SQL RETURNS NULL ON NULL INPUT", //$NON-NLS-1$
			

			//attachment tags
			"CREATE TABLE smart.attachment_tag (uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, keyid varchar(128) not null, primary key (uuid), unique(ca_uuid, keyid))", //$NON-NLS-1$
			"ALTER TABLE smart.attachment_tag  ADD CONSTRAINT attachment_tag_ca_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"CREATE TABLE SMART.attachment_tag_link (uuid char(16) for bit data not null, obs_attachment_uuid char(16) for bit data, wp_attachment_uuid char(16) for bit data, tag_uuid char(16) for bit data not null, primary key (uuid))", //$NON-NLS-1$
			
			"ALTER TABLE smart.attachment_tag_link  ADD CONSTRAINT attachment_tag_link_tag_uuid_fk FOREIGN KEY (tag_uuid) REFERENCES smart.attachment_tag(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.attachment_tag_link  ADD CONSTRAINT attachment_tag_link_obs_attachment_uuid_fk FOREIGN KEY (obs_attachment_uuid) REFERENCES smart.observation_attachment(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.attachment_tag_link  ADD CONSTRAINT attachment_tag_link_wp_attachment_uuid_fk FOREIGN KEY (wp_attachment_uuid) REFERENCES smart.wp_attachments(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

			"ALTER TABLE smart.cm_node add column attachment_tags long varchar", //$NON-NLS-1$

		};
		
		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}

		//use timezone setting for each ca to update
		HashMap<ConservationArea, String> caTimeZoneMapping = SmartContext.INSTANCE.getClass(Upgrader757To800.class).getCaTimeZoneMapping();
		for (Entry<ConservationArea, String> entry : caTimeZoneMapping.entrySet()) {
			String query = "update smart.waypoint set last_modified = smart.localTsToUtcTs(last_modified, '" + entry.getValue() + "') where ca_uuid = x'" + UuidUtils.uuidToString(entry.getKey().getUuid())+ "' and last_modified is not null"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			SmartPlugIn.logInfo(query);
			c.createStatement().execute(query);
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V800.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
	}

	
	class TimeZoneDialog extends SmartStyledTitleDialog{

		private List<ConservationArea> cas;
		private HashMap<ConservationArea, String> zoneMapping;
		
		private List<String> allZones = new ArrayList<>(ZoneId.getAvailableZoneIds());
		
		public TimeZoneDialog(Shell parent, List<ConservationArea> cas) {
			super(parent);
			this.cas = cas;
			this.zoneMapping = new HashMap<>();
			for (ConservationArea ca : cas) {
				zoneMapping.put(ca, ZoneId.systemDefault().getId());
			}
		
			LocalDateTime now = LocalDateTime.now();
			allZones.sort((a,b)->{
				ZoneId za = ZoneId.of(a);
				ZoneId zb = ZoneId.of(b);
				Integer seca = za.getRules().getOffset(now).getTotalSeconds();
				Integer secb = zb.getRules().getOffset(now).getTotalSeconds();
				if (seca.intValue() != secb.intValue()) return Integer.compare(seca, secb);
				String d1 = za.getDisplayName(TextStyle.SHORT, Locale.getDefault());
				String d2 = zb.getDisplayName(TextStyle.SHORT, Locale.getDefault());
				return Collator.getInstance().compare(d1, d2);
			});
		}
		
		@Override
		public Control createDialogArea(Composite parent){
			Composite composite = (Composite) super.createDialogArea(parent);

			Composite outer  = new Composite(composite, SWT.NONE);
			outer.setLayout(new GridLayout());
			outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			//Create an outer composite for spacing
			ScrolledComposite scrolled = new ScrolledComposite(outer, SWT.V_SCROLL | SWT.NONE);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			scrolled.setLayoutData(gd);
			
			// always show the focus control
			scrolled.setShowFocusedControl(true);
			scrolled.setExpandHorizontal(true);
			scrolled.setExpandVertical(true);
			
			Composite main = new Composite(scrolled, SWT.NONE);
			main.setLayout(new GridLayout(2, false));
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			
			for (ConservationArea ca : cas) {
				Label l = new Label(main, SWT.NONE);
				l.setText(ca.getNameLabel());
				
				ComboViewer cmbViewer = createCombo( main);
				cmbViewer.addSelectionChangedListener(e->{
					zoneMapping.put(ca, (String)cmbViewer.getStructuredSelection().getFirstElement());
				});
			}
			
			setMessage("Select the most appropriate timezone for each Conservation Area"); 
			setTitle("Timezone Mapping"); 
			getShell().setText("Timezone Mapping");
			
			
			scrolled.setContent(main);
			Point pnt = scrolled.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			scrolled.setMinSize(pnt);
			((GridData)scrolled.getLayoutData()).heightHint = Math.min(250, pnt.y);
			
			return composite; 
		}
		
		private ComboViewer createCombo(Composite parent){
			ComboViewer cmbTz = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbTz.setContentProvider(ArrayContentProvider.getInstance());
			cmbTz.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					ZoneId zone = ZoneId.of((String)element);
					String offset = zone.getRules().getOffset(LocalDateTime.now()).getDisplayName(TextStyle.SHORT, Locale.getDefault());
					String label = zone.getDisplayName(TextStyle.SHORT, Locale.getDefault());
					label += " (" +  zone.getId(); //$NON-NLS-1$
					label += " UTC" + offset + ") "; //$NON-NLS-1$ //$NON-NLS-2$
					return label;
				}
			});
			
			cmbTz.setInput(allZones);
			cmbTz.setSelection(new StructuredSelection(ZoneId.systemDefault().getId()));
			
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);			
			cmbTz.getCombo().setLayoutData(gd);
				
			return cmbTz;
		}
		

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		}
		
		public HashMap<ConservationArea, String> getMappings(){
			return this.zoneMapping;
		}
		
	}
}
