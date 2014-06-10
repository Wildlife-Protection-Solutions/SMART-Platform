package org.wcs.smart.ct2smart.run;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import org.wcs.smart.ct2smart.dao.ConnectionUtil;
import org.wcs.smart.ct2smart.matcher.FileUtil;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.ct2smart.patrol.PatrolBuilder;
import org.wcs.smart.ct2smart.patrol.PatrolExtractor;
import org.wcs.smart.ct2smart.xml.parser.TagS;
import org.wcs.smart.ct2smart.xml.parser.TagT;
import org.wcs.smart.patrol.xml.model.PatrolType;

public class CreatePatrols {

	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();
		
		PatrolExtractor extractor = new PatrolExtractor();
		Ct2Smart ct2Smart = FileUtil.loadCt2Smart(new File("match_super_x.xml"));
		PatrolBuilder builder = new PatrolBuilder(ct2Smart);
		
		Connection c = ConnectionUtil.getConnection();
		String[] uniqueId = new String[] {"Date", "Unit_ID", "DeviceId"};
		String[] uniqueValues = new String[uniqueId.length];
		ResultSet rs = extractor.getUniqueGroups(c, uniqueId);
		while (rs.next()) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < uniqueValues.length; i++) {
				uniqueValues[i] = rs.getString(i+1);
				sb.append(uniqueValues[i]).append("  ");
			}
			System.out.println("Extracting patrol for: " + sb);
			List<TagS> sList = extractor.extractS(c, uniqueId, uniqueValues);
			List<TagT> tList = extractor.extractT(c, uniqueValues[2], uniqueValues[0]);
			PatrolType p = builder.createPatrol(sList, tList);
			p.setId(uniqueValues[1] + "-patrol-" + uniqueValues[0].replace('/', '-'));
			
			FileUtil.write(new File(p.getId() + ".xml"), p);
		}
		rs.close();
		
		System.out.println("Done in "+ (double)(System.currentTimeMillis()-start)/1000 +" seconds!!!");
	}

}
