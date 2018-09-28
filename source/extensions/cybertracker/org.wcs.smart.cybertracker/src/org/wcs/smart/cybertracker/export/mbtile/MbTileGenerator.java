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

import javax.imageio.ImageIO;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.render.RenderException;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.ApplicationGIS.DrawMapParameter;
import org.locationtech.udig.project.ui.BoundsStrategy;
import org.locationtech.udig.project.ui.SelectionStyle;
import org.opengis.referencing.operation.TransformException;
import org.wcs.smart.ca.BasemapDefinition;
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
		
		for (int zoom = minzoom; zoom <= maxzoom; zoom ++) {
			int[] minTile = ZoomLevel.toTile(bounds.getMinX(), bounds.getMinY(), zoom);
			int[] maxTile = ZoomLevel.toTile(bounds.getMaxX(), bounds.getMaxY(), zoom);

			int xtile = minTile[0];
			int ytile = minTile[1];

			int xtile2 = maxTile[0];
			int ytile2 = maxTile[1];

			int startx = xtile < 0 ? 0 : xtile;
			int starty = ytile2 < 0 ? 0 : ytile2;
			
			int endx = (int) Math.min(xtile2, Math.pow(2, zoom));
			int endy = (int) Math.min(ytile, Math.pow(2, zoom));
			totaltiles += (endx - startx) * (endy - starty);
		}
		return totaltiles;
	}
	
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
		
		if (minzoom < 1) minzoom = 0;
		if (maxzoom > 20) maxzoom = 20;
		
		return new int[] {minzoom, maxzoom};
	}
	
	
	private PreparedStatement psInsertTile;

	public void generateMbTiles(Path outputPath, ReferencedEnvelope bounds, int minzoom, int maxzoom, BasemapDefinition definition, IProgressMonitor monitor) throws Exception {

		int totalWork = getTileCount(bounds, minzoom, maxzoom) + 2;
				
		monitor.beginTask("Generating MBTiles", totalWork);
		monitor.subTask("creating basemap");
		
		//TODO: set map to epsg 4326
		MapSettings settings = MapSettings.getInstance(definition);
		Map thisMap = ProjectFactory.eINSTANCE.createMap();
		settings.applyTo(thisMap);
		monitor.worked(1);
//		minzoom = 9;
//		maxzoom = 13;
		
		Layer lyr = new Layer("SMART Basemap", bounds, minzoom, maxzoom);
		
		try(Connection c = getDbConnection(outputPath)) {
			c.createStatement().executeUpdate("BEGIN TRANSACTION");
		
			monitor.subTask("generating metadata");
			//create & populated metadata table
			createMetadata(c, lyr);

			//create tiles tables
			createTiles(c);
			monitor.worked(1);
			
			//create images here 
			BufferedImage img = new BufferedImage(TILE_TO_RENDER_BUFFER*TILESIZE,  TILE_TO_RENDER_BUFFER*TILESIZE, BufferedImage.TYPE_INT_RGB);
			Graphics2D gg = img.createGraphics();
			BufferedImage tileimage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
			Graphics2D gc = tileimage.createGraphics();
			
			//populate tiles tables
			for(ZoomLevel zz : lyr.getZooms()) {
				monitor.subTask("processing zoom: " + zz.getZoom());
				
				int totalTiles = zz.getTiles().size();
				int cnt = 0;
				
				int xMinTile = zz.getMinTileX();
				int xMaxTile = zz.getMaxTileX();
				int yMinTile = zz.getMinTileY();
				int yMaxTile = zz.getMaxTileY();

				//process images TILE_TO_RENDER_BUFFER x TILE_TO_RENDER_BUFFER at a time
				for (int x = xMinTile; x <= xMaxTile; x += TILE_TO_RENDER_BUFFER) {
					for (int y = yMinTile; y <= yMaxTile; y += TILE_TO_RENDER_BUFFER) {
						
						//compute bounds for envelope
						Envelope tenv = zz.getTile(x,y).getBoundsLatLong();
//						Tile maxTile = zz.getTile(x+TILE_TO_RENDER_BUFFER, y+TILE_TO_RENDER_BUFFER);
//						if (maxTile != null) {
//							tenv.expandToInclude(maxTile.getBoundsLatLong());
//						}else{
//							Tile temp = new Tile(x+TILE_TO_RENDER_BUFFER, y+TILE_TO_RENDER_BUFFER, zz);
//							tenv.expandToInclude(temp.getBoundsLatLong());
//						}
						
						
						for (int i = 0; i < TILE_TO_RENDER_BUFFER; i ++) {
							for (int j = 0; j < TILE_TO_RENDER_BUFFER; j ++) {
								Tile t = zz.getTile(x+i, y+j);
								if (t != null) {
									tenv.expandToInclude(t.getBoundsLatLong());
								}else {
									Tile temp = new Tile(x+i, y+j, zz);
									tenv.expandToInclude(temp.getBoundsLatLong());
								}
							}
						}
						
						ReferencedEnvelope revn = new ReferencedEnvelope(tenv,  SmartDB.DATABASE_CRS);
						
						gg.clearRect(0, 0, img.getWidth(), img.getHeight());
						BoundsStrategy bnds = new BoundsStrategy(revn);
						DrawMapParameter params = new ApplicationGIS.DrawMapParameter(gg,  new Dimension(img.getWidth(),img.getHeight()), thisMap, bnds, 92, SelectionStyle.IGNORE, new NullProgressMonitor());
						ApplicationGIS.drawMap(params);

						for (int i = 0; i < TILE_TO_RENDER_BUFFER; i ++) {
							for (int j = 0; j < TILE_TO_RENDER_BUFFER; j ++) {
								Tile t = zz.getTile(x+i, y+j);
								if (t == null) continue;
								
								int startx = i * 256;
								int starty = j * 256;
							
								gc.clearRect(0, 0, tileimage.getWidth(), tileimage.getHeight());
								gc.drawImage(img, 0, 0, 256, 256, startx, starty, startx + 256, starty + 256, null);
								
								writeTile(c, t, tileimage);
								cnt++;
								System.out.println(cnt + "/" + totalTiles);
								monitor.worked(1);
								
								//TODO: fix zooming - currently tiles are not correct
								//TODO: support this
								if (monitor.isCanceled()) return;
							}
						}
						psInsertTile.executeBatch();
					}
				}
			}
			c.createStatement().executeUpdate("COMMIT TRANSACTION"); //$NON-NLS-1$
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
//			ImageIO.write(img2, "png", new File("C:\\temp\\mbtiles\\abc_" + t.getZoom().getZoom() + "_" + t.getTileX() + "_" + t.getTileY() + ".png"));
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
