package org.wcs.smart.ct2smart.run;

import java.io.File;
import java.sql.Connection;
import java.util.List;

import org.wcs.smart.ct2smart.dao.ConnectionUtil;
import org.wcs.smart.ct2smart.matcher.FileUtil;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.ct2smart.patrol.PatrolBuilder;
import org.wcs.smart.ct2smart.patrol.PatrolExtractor;
import org.wcs.smart.ct2smart.xml.parser.TagS;
import org.wcs.smart.patrol.xml.model.PatrolType;

public class CreatePatrols {

	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();
		
		PatrolExtractor extractor = new PatrolExtractor();
		Ct2Smart ct2Smart = FileUtil.loadCt2Smart(new File("match_super_x.xml"));
		PatrolBuilder builder = new PatrolBuilder(ct2Smart);
		
		Connection c = ConnectionUtil.getConnection();
		List<String> dates = extractor.getDates(c);
		for (String date : dates) {
			System.out.println("Extracting patrol for date: " + date);
			List<TagS> sList = extractor.extract(c, date);
			PatrolType p = builder.createPatrol(sList, date);
			FileUtil.write(new File("patrol-" + date.replace('/', '-') + ".xml"), p);
		}

		System.out.println("Done in "+ (double)(System.currentTimeMillis()-start)/1000 +" seconds!!!");
	}

}
