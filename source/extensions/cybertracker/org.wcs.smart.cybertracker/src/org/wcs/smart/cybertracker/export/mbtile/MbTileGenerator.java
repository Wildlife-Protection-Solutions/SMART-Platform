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
package org.wcs.smart.cybertracker.export.mbtile;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;

import javax.imageio.ImageIO;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.commands.ChangeCRSCommand;
import org.locationtech.udig.project.render.RenderException;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.ApplicationGIS.DrawMapParameter;
import org.locationtech.udig.project.ui.BoundsStrategy;
import org.locationtech.udig.project.ui.SelectionStyle;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.internal.settings.MapSettings;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/*
 * References:
 * https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java
 * 
 * https://github.com/mapbox/rio-mbtiles/blob/master/mbtiles/scripts/cli.py
 * https://github.com/mapbox/mercantile/blob/master/mercantile/__init__.py 
 */
/**
 * Tools for generating mbtiles from a SMART basemap
 * 
 * @author Emily
 *
 */
public class MbTileGenerator {
	
	private static final int TILESIZE = 256;
	private static final int TILE_TO_RENDER_BUFFER = 10;
	
	private PreparedStatement psInsertTile;
	
	public MbTileGenerator() {
		
	}
	
	
	/**
	 * Determine the total number of tiles that
	 * will be generated for the given bounds,
	 * and zoom levels
	 * @param bounds
	 * @param minzoom
	 * @param maxzoom
	 * @return
	 */
	public int getTileCount(Envelope bounds, int minzoom, int maxzoom) {
		
		int totaltiles = 0;
		
		int[] bndsminTile = ZoomLevel.toTile(bounds.getMinX(), bounds.getMinY(), minzoom);
		int[] bndsmaxTile = ZoomLevel.toTile(bounds.getMaxX(), bounds.getMaxY(), minzoom);
		
		for (int zoom = minzoom; zoom <= maxzoom; zoom ++) {
			
			int startx = bndsminTile[0] * (int)Math.pow(2,  (zoom - minzoom) );
			int starty = bndsmaxTile[1] * (int)Math.pow(2,  (zoom - minzoom) );
			
			int endx = (bndsmaxTile[0] + 1) * (int)Math.pow(2,  (zoom - minzoom) ) - 1;
			int endy = (bndsminTile[1] + 1)* (int)Math.pow(2,  (zoom - minzoom) ) - 1;
			
			totaltiles += (endx - startx + 1) * (endy - starty + 1);
		}
		return totaltiles;
	}
	
	/**
	 * Suggests zoom levels for given envelope (in lat/long)
	 * 
	 * @return array of min and max suggested zoom level
	 */
	public int[] suggestZoomLevels(Envelope env) throws TransformException {
		double ydistance = JTS.orthodromicDistance(new Coordinate(env.getMinX(), env.getMinY()), new Coordinate(env.getMinX(), env.getMaxY()), SmartDB.DATABASE_CRS);
		double xdistance = JTS.orthodromicDistance(new Coordinate(env.getMinX(), env.getMinY()), new Coordinate(env.getMaxX(), env.getMinY()), SmartDB.DATABASE_CRS);
		double dd = 0;
		double totalmeters = 0;
		if (xdistance > ydistance) {
			dd = env.getMaxX() - env.getMinX();
			totalmeters = xdistance;
		}else {
			dd = env.getMaxY() - env.getMinY();
			totalmeters = ydistance;
		}
		
		int tilesRequired = (int)Math.ceil(totalmeters/1280);
		double unitsPerTile = dd / tilesRequired;
		int maxzoom = (int)Math.ceil( Math.log(360 / unitsPerTile) / Math.log(2) );
		int minzoom = (int)Math.ceil( Math.log(360 / dd) / Math.log(2) );
		
		//ensure at least 3 zoom levels
		if (maxzoom < minzoom + 3) maxzoom = minzoom + 3;
		
		if (minzoom < 1) minzoom = 0;
		if (maxzoom > 20) maxzoom = 20;
		
		return new int[] {minzoom, maxzoom};
	}
	

	/**
	 * Generate mbtiles 
	 * @param outputPath output files
	 * @param bounds bounds
	 * @param minzoom minimum zoom level
	 * @param maxzoom maximum zoom level
	 * @param definition basemap definition
	 * 
	 * @param monitor
	 * @throws Exception
	 */
	public void generateMbTiles(Path outputPath, ReferencedEnvelope bounds, int minzoom, int maxzoom, BasemapDefinition definition, IProgressMonitor monitor) throws Exception {

		CoordinateReferenceSystem sphericalMercator = CRS.decode("EPSG:3857", true); //$NON-NLS-1$
		
		int totalWork = getTileCount(bounds, minzoom, maxzoom) + 2;
				
		monitor.beginTask(Messages.MbTileGenerator_TaskName, totalWork);
		monitor.subTask(Messages.MbTileGenerator_SubTask1);

		//set map to spherical mercator
		MapSettings settings = MapSettings.getInstance(definition);
		Map thisMap = ProjectFactory.eINSTANCE.createMap();
		settings.applyTo(thisMap);		
		thisMap.sendCommandSync(new ChangeCRSCommand(sphericalMercator));
		
		monitor.worked(1);
//		minzoom = 1;
//		maxzoom = 5;
		
		Layer lyr = new Layer(Messages.MbTileGenerator_LayerName, bounds, minzoom, maxzoom);
		try(Connection c = getDbConnection(outputPath)) {
			c.createStatement().executeUpdate("BEGIN TRANSACTION"); //$NON-NLS-1$
		
			monitor.subTask(Messages.MbTileGenerator_SubTask2);
			//create & populated metadata table
			createMetadata(c, lyr);

			//create tiles tables
			createTiles(c);
			monitor.worked(1);
			
			//create images here 
			BufferedImage tileimage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
			Graphics2D gc = tileimage.createGraphics();
			
			//populate tiles tables
			for(ZoomLevel zz : lyr.getZooms()) {
				monitor.subTask(MessageFormat.format(Messages.MbTileGenerator_SubTask3, zz.getZoom()));
				
				int totalTiles = zz.getTiles().size();
				int cnt = 0;
				
				int xMinTile = zz.getMinTileX();
				int xMaxTile = zz.getMaxTileX();
				int yMinTile = zz.getMinTileY();
				int yMaxTile = zz.getMaxTileY();

				//process images TILE_TO_RENDER_BUFFER x TILE_TO_RENDER_BUFFER at a time
				for (int x = xMinTile; x <= xMaxTile; x += TILE_TO_RENDER_BUFFER) {
					for (int y = yMinTile; y <= yMaxTile; y += TILE_TO_RENDER_BUFFER) {
						
						Tile minTile = zz.getTile(x, y);
						Tile maxTile = zz.getTile(Math.min(xMaxTile, x + TILE_TO_RENDER_BUFFER - 1), Math.min(yMaxTile, y + TILE_TO_RENDER_BUFFER - 1));
						
						Envelope r1 = minTile.getBoundsMercator();
						r1.expandToInclude(maxTile.getBoundsMercator());
						
						BufferedImage img = new BufferedImage((maxTile.getTileX() - minTile.getTileX()+1)*TILESIZE,  (maxTile.getTileY() - minTile.getTileY()+1)*TILESIZE, BufferedImage.TYPE_INT_RGB);
						Graphics2D gg = img.createGraphics();

						ReferencedEnvelope rr = new ReferencedEnvelope(r1,  sphericalMercator);
						BoundsStrategy bnds = new BoundsStrategy(rr);
						try {
							DrawMapParameter params = new ApplicationGIS.DrawMapParameter(gg,  new Dimension(img.getWidth(),img.getHeight()), thisMap, bnds, 92, SelectionStyle.IGNORE, new NullProgressMonitor());
							ApplicationGIS.drawMap(params);
							
						}catch (Throwable t) {
							
						}
						gg.dispose();
//						ImageIO.write(img, "png", new File("C:\\temp\\mbtiles\\overview_" + zz.getZoom() + "_" + x + "_" + y + ".png"));

						for (int i = 0; i < TILE_TO_RENDER_BUFFER; i ++) {
							for (int j = 0; j < TILE_TO_RENDER_BUFFER; j ++) {
								Tile t = zz.getTile(x+i, y+j);
								if (t == null) continue;
							
								gc.clearRect(0, 0, tileimage.getWidth(), tileimage.getHeight());
								gc.drawImage(img, 0, 0, 256, 256, i * 256, j * 256, (i + 1) * 256, (j + 1) * 256, null);
								writeTile(c, t, tileimage);
								
								cnt++;
								monitor.worked(1);
								monitor.subTask(MessageFormat.format(Messages.MbTileGenerator_SubTask4, + zz.getZoom(), cnt, totalTiles ));
								if (monitor.isCanceled()) return;
							}
						}
						psInsertTile.executeBatch();
					}
				}
			}
			c.createStatement().executeUpdate("COMMIT TRANSACTION"); //$NON-NLS-1$
			gc.dispose();
		}
		
		monitor.done();
	}
	
	/*
	 * write tile to mbtiles set
	 */
	private void writeTile(Connection c, Tile t, BufferedImage image) throws SQLException, IOException, RenderException {
		
		psInsertTile.setInt(1, t.getZoom().getZoom());
		psInsertTile.setInt(2, t.getTileX());
		int y = (int)(Math.pow(2,  t.getZoom().getZoom()) - t.getTileY()) - 1;
		psInsertTile.setInt(3, y);
		
		try(ByteArrayOutputStream bout = new ByteArrayOutputStream()){
//			ImageIO.write(image, "png", new File("C:\\temp\\mbtiles\\abc_" + t.getZoom().getZoom() + "_" + t.getTileX() + "_" + t.getTileY() + ".png"));
			ImageIO.write(image, "png", bout); //$NON-NLS-1$
			psInsertTile.setBytes(4, bout.toByteArray());
		}
		psInsertTile.addBatch();
	}
	
	/*
	 * create tiles table
	 */
	private void createTiles(Connection c) throws SQLException {
		c.createStatement().executeUpdate("CREATE TABLE tiles (zoom_level integer, tile_column integer, tile_row integer, tile_data blob)"); //$NON-NLS-1$

		//create insert prepared statement
		psInsertTile = c.prepareStatement("INSERT INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)");  //$NON-NLS-1$
	}
	
	/*
	 * create metadata table
	 */
	private void createMetadata(Connection c, Layer lyr) throws SQLException {
	
		c.createStatement().executeUpdate("CREATE TABLE metadata (name text, value text)"); //$NON-NLS-1$
		
		PreparedStatement ps = c.prepareStatement("INSERT INTO metadata (name, value) VALUES (?, ?)"); //$NON-NLS-1$
		
		ps.setString(1, "name"); //$NON-NLS-1$
		ps.setString(2, lyr.getName());
		ps.executeUpdate();
		
		ps.setString(1, "type"); //$NON-NLS-1$
		ps.setString(2, "baselayer"); //$NON-NLS-1$
		ps.executeUpdate();
		
		ps.setString(1, "version"); //$NON-NLS-1$
		ps.setString(2, "1.2"); //$NON-NLS-1$
		ps.executeUpdate();
		
		ps.setString(1, "description"); //$NON-NLS-1$
		ps.setString(2, "The SMART basemap exported with the CyberTracker package."); //$NON-NLS-1$
		ps.executeUpdate();
		
		ps.setString(1, "format"); //$NON-NLS-1$
		ps.setString(2, "png"); //$NON-NLS-1$
		ps.executeUpdate();
		
		StringBuilder sb = new StringBuilder();
		sb.append(lyr.getBounds().getMinX());
		sb.append(","); //$NON-NLS-1$
		sb.append(lyr.getBounds().getMinY());
		sb.append(","); //$NON-NLS-1$
		sb.append(lyr.getBounds().getMaxX());
		sb.append(","); //$NON-NLS-1$
		sb.append(lyr.getBounds().getMaxY());
		
		ps.setString(1, "bounds"); //$NON-NLS-1$
		ps.setString(2, sb.toString());
		ps.executeUpdate();
		
		double xc = lyr.getBounds().centre().x;
		double yc = lyr.getBounds().centre().y;
		
		ps.setString(1, "center"); //$NON-NLS-1$
		ps.setString(2, xc + "," + yc); //$NON-NLS-1$
		ps.executeUpdate();
		
		ps.setString(1, "minzoom"); //$NON-NLS-1$
		ps.setString(2, String.valueOf(lyr.getMinZoom()));
		ps.executeUpdate();
		
		ps.setString(1, "maxzoom"); //$NON-NLS-1$
		ps.setString(2, String.valueOf(lyr.getMaxZoom()));
		ps.executeUpdate();
	}
	
	
	/*
	 * connect to sqllite db
	 */
	private Connection getDbConnection(Path file) throws IOException, SQLException, ClassNotFoundException {
		Path temp = Paths.get(file.toString());
		if (Files.exists(temp)) Files.delete(temp);
		Class.forName("org.sqlite.JDBC"); //$NON-NLS-1$
		return DriverManager.getConnection("jdbc:sqlite:" + file); //$NON-NLS-1$
	}
	
}
