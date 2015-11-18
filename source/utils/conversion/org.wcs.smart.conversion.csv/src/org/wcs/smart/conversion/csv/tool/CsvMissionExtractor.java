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
package org.wcs.smart.conversion.csv.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.conversion.csv.handler.ProcessingActionHandler;
import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.tag.TagS;
import org.wcs.smart.conversion.tool.MatchSession;
import org.wcs.smart.conversion.tool.MissionBuilder;
import org.wcs.smart.conversion.util.FileUtil;
import org.wcs.smart.er.xml.model.missions.MissionType;

/**
 * @author elitvin
 * @since 3.2.0
 */
public class CsvMissionExtractor extends AbstractCsvExtractor {

	private static final Logger logger = LogManager.getLogger(ProcessingActionHandler.class); 

	public CsvMissionExtractor(Connection c) throws SQLException {
		super(c);
	}

	public void extract(String folder, MatchSession session, DataModelLookup dmLookup) throws Exception {
		MissionBuilder builder = new MissionBuilder(session, dmLookup);
		try {
			PrintStream inf = new PrintStream(new FileOutputStream(folder + "\\warnings.log")); //$NON-NLS-1$
			PrintStream err = new PrintStream(new FileOutputStream(folder + "\\errors.log")); //$NON-NLS-1$
			builder.setExtraOutputs(err, inf);
		} catch (FileNotFoundException e) {
			logger.error("Unable to create files for logging in selected folder.", e); //$NON-NLS-1$
			builder.clearExtraOutputs();
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Mission generation", "Unable to create files for logging in selected folder.");
		}

		try {
			String[] uniqueId = getUniqueIds(session.getSmartMapping());
			String[] columnNames = getCsvColumns(uniqueId);
			String[] uniqueValues = new String[uniqueId.length];
			ResultSet rs = getUniqueGroups(columnNames);
			while (rs.next()) {
				for (int i = 0; i < uniqueValues.length; i++) {
					uniqueValues[i] = rs.getString(i+1);
				}
				List<TagS> sList = extractS(columnNames, uniqueValues);
//				List<TagT> tList = extractT(uniqueValues[2], uniqueValues[0]);
				String id = buildId(uniqueValues);
				MissionType mission = builder.createMission(sList, id);
				builder.buildTracksFromWp(mission);
				builder.removeEmptyWayoints(mission);
				
				FileUtil.write(new File(folder + "\\" + mission.getId() + ".xml"), mission); //$NON-NLS-1$ //$NON-NLS-2$
			}
			rs.close();
		} finally {
			builder.clearExtraOutputs();
		}
	}
}
