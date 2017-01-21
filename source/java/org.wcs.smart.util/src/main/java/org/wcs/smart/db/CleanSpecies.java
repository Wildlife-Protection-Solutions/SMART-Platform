package org.wcs.smart.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.tree.TreeNode;

public class CleanSpecies {

	private static final String database = "C:\\temp\\Gabon\\smart.3.3.1.Gabon_LargeSpecies\\smart.3.3.1.Gabon_LargeSpecies\\smart\\data\\database\\smartdb";
	private static final String user = "smart_admin";
	private static final String password = "smart_derby";
	
	public static Connection createConnection() 	throws Exception {
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		
//		System.out.println("jdbc:derby:" + database
//				+ ";create=true;user=" + user + ";password=" + password + ";");
		
		String connectionString = "jdbc:derby:" + database 
				+ ";user=" + user + ";password=" + password +";";
		
		return DriverManager.getConnection(connectionString);
	}
	
	public static void main(String[] args) throws Exception{
		CleanSpecies cs = new CleanSpecies();
		cs.c = createConnection();
		cs.getSpeciesAttributes();
		
	}
	
	private Connection c;
	
	private void getSpeciesAttributes() throws SQLException{
		List<byte[]> species = new ArrayList<>();
		try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid FROM smart.dm_attribute where keyId = 'species'")){
			while(rs.next()){
				species.add(rs.getBytes(1));
			}
		}
		
		c.setAutoCommit(false);
		//start transaction
		for (byte[] specie: species){
			processAttribute(specie);
			c.commit();
		}
	}
	
	private void processAttribute(byte[] speciesAttribute) throws SQLException{
		System.out.println("finding nodes: " + speciesAttribute.toString());
		
		//used keys
		PreparedStatement usedkeys = c.prepareStatement("select hkey from smart.dm_attribute_tree where attribute_uuid = ? and uuid in (select distinct tree_node_uuid from smart.WP_OBSERVATION_ATTRIBUTES where tree_node_uuid is not null and attribute_uuid = ?)");
		usedkeys.setBytes(1, speciesAttribute);
		usedkeys.setBytes(2, speciesAttribute);
		HashSet<String> usedKeys = new HashSet<>();
		
		try(ResultSet rs = usedkeys.executeQuery()){
			while(rs.next()){
				String hkey = rs.getString(1);
				//put all parent parts too
				while(hkey.indexOf('.') > 0){
					hkey = hkey.substring(0, hkey.lastIndexOf('.'));
					usedKeys.add(hkey + ".");
				}
			}
		}
		
		
		PreparedStatement notused = c.prepareStatement("select uuid, hkey from smart.dm_attribute_tree where attribute_uuid = ? ");
		notused.setBytes(1, speciesAttribute);
		
		List<TreeNode> toDelete = new ArrayList<TreeNode>();
		try(ResultSet rs = notused.executeQuery()){
			while(rs.next()){
				String hkey = rs.getString(2);
				if (!usedKeys.contains(hkey)){
					toDelete.add(new TreeNode(rs.getBytes(1), rs.getString(2)));
				}
			}
		}
				
		PreparedStatement deleteLabel = c.prepareStatement("DELETE FROM smart.i18n_label where element_uuid = ?");
		PreparedStatement deleteNode = c.prepareStatement("DELETE FROM smart.dm_attribute_tree where uuid = ?");
		int cnt = 0;
		for (TreeNode d : toDelete){
			if (cnt % 10 == 0){
				System.out.println("processing " + cnt + "/" + toDelete.size());
			}
			cnt++;
			deleteLabel.setBytes(1, d.uuid);
			deleteNode.setBytes(1, d.uuid);
			
			try{
				deleteNode.executeUpdate();
				deleteLabel.executeUpdate();
			}catch (Exception ex){
				System.out.println("unable to delete tree node: " + ex.getMessage());
			}
		}
		
	
	}
	
	private class TreeNode{
		byte[] uuid;
		int length;
		
		public TreeNode(byte[] uuid, String hkey){
			this.uuid = uuid;
			this.length = hkey.length() - hkey.replaceAll("\\.", "").length();
		}
	}
}
