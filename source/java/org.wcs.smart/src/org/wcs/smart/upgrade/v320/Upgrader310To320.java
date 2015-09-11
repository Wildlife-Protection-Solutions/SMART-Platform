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
package org.wcs.smart.upgrade.v320;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB.DbUser;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;

/**
 * Upgrades from database version 310 to 320.
 *
 * @author Emily
 *
 */
public class Upgrader310To320 implements IDatabaseUpgrader {
	
	private String dbUrl = null;
	
	public void upgrade(final IProgressMonitor monitor) {
		monitor.subTask(Messages.Upgrader310To320_ProgressMessage);
		final Session s = HibernateManager.openSession();
		try{
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					try {
						dbUrl = c.getMetaData().getURL();
						c.setAutoCommit(false);
						upgrade(c, s, monitor);
					} catch (final Exception e) {
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPlugIn.displayLog(Messages.Upgrader310To320_ErrorMessage, e);
							}
						});
					} finally {
						c.setAutoCommit(true);
					}
				}
			});
		}finally{
			s.close();
		}
		
		/* do a hard derby upgrade */ 
		try{
			//disconnect 
			HibernateManager.endSessionFactory(true);
			//perform hard upgrade
			DriverManager.getConnection(dbUrl + ";create=false;upgrade=true;user=" + DbUser.ADMIN.getUserName() + ";password=" + DbUser.ADMIN.getPassword()); //$NON-NLS-1$ //$NON-NLS-2$ 
			DerbyHibernateExtensions.shutDown(true);
		}catch(Exception ex){
			SmartPlugIn.log("Could not perform hard derby upgrade.", ex); //$NON-NLS-1$
		}
	}

	private void upgrade(Connection c, Session session, IProgressMonitor monitor) throws Exception {
		String[] sql = new String[]{
				"alter table smart.obs_waypoint_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.obs_observation_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.obs_gridded_query add column style long varchar",  //$NON-NLS-1$
				
				"alter table smart.observation_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.waypoint_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.patrol_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.gridded_query add column style long varchar", //$NON-NLS-1$
				
				"create table smart.map_styles (uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, style_string long varchar not null, primary key (uuid))", //$NON-NLS-1$
				"alter table smart.map_styles add constraint mapstyle_ca_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(UUID) ON UPDATE RESTRICT ON DELETE CASCADE", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.map_styles to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.map_styles to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.map_styles to analyst", //$NON-NLS-1$

				//for the move from the net.refractions.udig namespace to the org.locationtech.udig namespace
				"CREATE FUNCTION REPLACEALL(STR LONG VARCHAR, OLD LONG VARCHAR, NEW LONG VARCHAR) RETURNS LONG VARCHAR LANGUAGE JAVA DETERMINISTIC EXTERNAL NAME 'org.wcs.smart.util.SmartUtils.replaceAll' PARAMETER STYLE JAVA NO SQL RETURNS NULL ON NULL INPUT", //$NON-NLS-1$
				"update smart.saved_maps set map_def = replaceall(map_def, 'net.refractions.udig', 'org.locationtech.udig')", //$NON-NLS-1$

				"alter table smart.cm_attribute_tree_node add column cm_attribute_uuid CHAR(16) FOR BIT DATA", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node add column dm_attribute_uuid CHAR(16) FOR BIT DATA", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node add column parent_uuid CHAR(16) FOR BIT DATA", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node add column node_order SMALLINT", //$NON-NLS-1$

				"UPDATE smart.cm_attribute_tree_node SET NODE_ORDER=0", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node alter column DM_TREE_NODE_UUID NULL", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute_tree_node ADD CONSTRAINT cm_attribute_tree_node_parent_uuid_fk FOREIGN KEY (PARENT_UUID) REFERENCES smart.cm_attribute_tree_node (UUID) ON UPDATE RESTRICT ON DELETE CASCADE", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute_tree_node ADD CONSTRAINT cm_attribute_tree_node_cm_attribute_uuid_fk FOREIGN KEY (CM_ATTRIBUTE_UUID) REFERENCES smart.cm_attribute (UUID) ON UPDATE RESTRICT ON DELETE CASCADE", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute_tree_node ADD CONSTRAINT cm_attribute_tree_node_dm_attribute_uuid_fk FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES smart.dm_attribute (UUID) ON UPDATE RESTRICT ON DELETE CASCADE", //$NON-NLS-1$
		
				//fix constraint cascade options; we have to use the restrict for deleting data model elements; but this must be done after 
				//waypoint ca cascade delete foreign key constraint 
				"ALTER TABLE smart.wp_observation_attributes DROP CONSTRAINT observation_attribute_att_uuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.wp_observation_attributes DROP CONSTRAINT observation_attribute_att_list_uuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.wp_observation_attributes DROP CONSTRAINT observation_attribute_att_tree_uuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.wp_observation DROP CONSTRAINT observation_category_uuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.waypoint DROP CONSTRAINT waypoint_ca_uuid_fk", //$NON-NLS-1$

				"ALTER TABLE smart.wp_observation_attributes ADD CONSTRAINT observation_attribute_att_uuid_fk FOREIGN KEY (ATTRIBUTE_UUID) REFERENCES smart.dm_attribute (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.wp_observation_attributes ADD CONSTRAINT observation_attribute_att_list_uuid_fk FOREIGN KEY (LIST_ELEMENT_UUID) REFERENCES smart.dm_attribute_list (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.wp_observation_attributes ADD CONSTRAINT observation_attribute_att_tree_uuid_fk FOREIGN KEY (TREE_NODE_UUID) REFERENCES smart.dm_attribute_tree (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.wp_observation ADD CONSTRAINT observation_category_uuid_fk FOREIGN KEY (CATEGORY_UUID) REFERENCES smart.dm_category (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.waypoint ADD CONSTRAINT waypoint_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (UUID) ON UPDATE RESTRICT ON DELETE CASCADE" //$NON-NLS-1$
		};
		
		for (String s : sql){
			c.createStatement().execute(s);
		}
				
		/* VERSION UDATE */ 
		String ssql = "update smart.db_version set version = '3.2.0' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$
		c.createStatement().execute(ssql);
		
		c.commit();
	}

	/**
	 * Performs the post processing upgrades.  This code has to be
	 * performed after all other upgrades/install processes to ensure
	 * all tables are installed in the databases so species delete check
	 * can be execute without missing tables.
	 * 
	 * @param monitor
	 */
	public void postProcess(IProgressMonitor monitor) {
		monitor.subTask(Messages.Upgrader310To320_ProgressMessage);
		final Session s = HibernateManager.openSession();
		try{
			/* Species Cleanup - ticket #1118 */
			s.beginTransaction();
			cleanUpSpecies(s, monitor);
			s.getTransaction().commit();
				
			// configurable model trees; do this after the species clean up to 
			//improve performance
			monitor.subTask(Messages.Upgrader310To320_ProgressMessage + ": " + Messages.Upgrader310To320_upgradingDBMsg); //$NON-NLS-1$
			CmUpgrader310To320 cmUpgrader = new CmUpgrader310To320();
			cmUpgrader.upgrade(s);
		}finally{
			s.close();
		}
	}
	public static void cleanUpSpecies(Session session, IProgressMonitor monitor){
		List<?> data = session.createCriteria(Attribute.class)
				.add(Restrictions.eq("keyId", "species")) //$NON-NLS-1$ //$NON-NLS-2$
				.list();
		
		//no species
		if (data.size() == 0) return;
		
		boolean yesToAll = false;
		
		//for each species attribute
		for (int i = 0; i < data.size(); i ++){
			final Attribute species = (Attribute) data.get(i);
		
			//	i) More than 50 species in the database AND
			//	ii) More than 30% of the species are unused
			
			Long numSpecies = (Long)session.createCriteria(AttributeTreeNode.class)
					.add(Restrictions.eq("attribute", species)) //$NON-NLS-1$
					.setProjection(Projections.rowCount())
					.uniqueResult();
		
			Query q = session.createQuery(
				"SELECT count(distinct poa.attributeTreeNode) FROM WaypointObservationAttribute poa WHERE poa.attributeTreeNode.attribute = :attribute "); //$NON-NLS-1$
			q.setParameter("attribute", species); //$NON-NLS-1$
			Long numSpeciesUsed = (Long)q.uniqueResult();
			
			if (numSpecies < 50 || ((numSpecies - numSpeciesUsed) / ((float)numSpecies)) < .3){
				//skip this species attribute and try the next one
				continue;
			}
			
			final int[] cont = new int[]{-1};
			if (!yesToAll){
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
						//ask the user
						MessageDialog md = new MessageDialog(
								Display.getDefault().getActiveShell(),
								Messages.Upgrader310To320_SpeciesUpgradeTitle,
								null,
								MessageFormat.format(Messages.Upgrader310To320_SpeciesUpgradeQuestion, new Object[]{species.getConservationArea().getName()}),
								MessageDialog.WARNING,
								new String[]{IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL},
								0);
						cont[0] = md.open();
					}	
				});
				if (cont[0] == 2){
					//skip this species and move to the next one
					continue;
				}else if (cont[0] == 1){
					yesToAll = true;
				}
			}
			
			//we have more than 50 species and more than 30% of them are not used
			//remove all the unused ones
	
			String msg = MessageFormat.format(Messages.Upgrader310To320_SpeciesUpgradeMsg, 
					species.getConservationArea().getId());
			monitor.subTask(msg);
			List<Integer> status = new ArrayList<Integer>();
			status.add(0);
			status.add(numSpecies.intValue());
			List<AttributeTreeNode> roots = new ArrayList<AttributeTreeNode>(species.getTree());
			for (AttributeTreeNode n : roots){
				processTreeNode(n, session, monitor, status, msg);
			}
		}
		session.flush();
	}
	
	
	
	private static boolean processTreeNode(AttributeTreeNode node, Session session, IProgressMonitor monitor, List<Integer> status, String msg){
		boolean kids = true;
		monitor.subTask(msg + " (" + status.get(0) + " / " + status.get(1) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		status.set(0, status.get(0) + 1);
		List<AttributeTreeNode> kidsToProcess = new ArrayList<AttributeTreeNode>(node.getChildren());
		for (AttributeTreeNode n : kidsToProcess){
			if (!processTreeNode(n, session, monitor, status, msg)){
				kids = false;
			}
		}
		//not all kids could be removed so we cannot remove any parent
		if (!kids) return false;
		
		boolean canDelete = true;
		try{
			canDelete = DeleteManager.canDelete(node, session);
		}catch (Exception ex){
			ex.printStackTrace();
			//cannot delete me
			return false;
		}
		if (!canDelete) return false;
		
		//remove any configurable model lablel (cm nodes will deleted through cascade) 
		Query q= session.createQuery("DELETE FROM Label WHERE id.element IN (SELECT uuid FROM CmAttributeTreeNode WHERE dmTreeNode = :node)"); //$NON-NLS-1$
		q.setParameter("node", node); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete me
		if (node.getParent() != null){
			node.getParent().getChildren().remove(node);
			node.getParent().getActiveChildren().remove(node);
		}else{
			node.getAttribute().getTree().remove(node);
		}
		node.setParent(null);
		//delete labels
		for (Label l : node.getNames()){
			session.delete(l);
		}
		node.getNames().clear();
		
		
		session.delete(node);
		session.flush();
		return true;
	}
	
}
