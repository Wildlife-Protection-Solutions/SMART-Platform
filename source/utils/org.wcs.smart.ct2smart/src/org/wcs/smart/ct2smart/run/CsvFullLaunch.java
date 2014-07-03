package org.wcs.smart.ct2smart.run;

import java.io.File;
import java.sql.Connection;

import org.wcs.smart.ct2smart.dao.ConnectionUtil;
import org.wcs.smart.ct2smart.db.CsvDbLoader;
import org.wcs.smart.ct2smart.matcher.CsvMatchFileBuilder;
import org.wcs.smart.ct2smart.matcher.FileUtil;
import org.wcs.smart.ct2smart.matcher.MatchFileBuilder;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;

public class CsvFullLaunch {

	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();

		Connection c = ConnectionUtil.getConnection();
//		File file = new File("d:\\dev\\data\\CyberTracker Data_Jan-2014\\test.xml");
		File file = new File("d:\\dev\\data\\CyberTracker Data_Jan-2014\\test_short_super.xml");

		CsvDbLoader.getInstance().load(file, c);

		System.out.println("CSV DB loaded in "+ (double)(System.currentTimeMillis()-start)/1000 +" seconds!!!");

		start = System.currentTimeMillis();

		CsvMatchFileBuilder matchBuilder = new CsvMatchFileBuilder();
		Ct2Smart ct2Smart = matchBuilder.create(c);
		FileUtil.write(new File("match_super_csv.xml"), ct2Smart);

		System.out.println("CSV based file created in "+ (double)(System.currentTimeMillis()-start)/1000 +" seconds!!!");
	}

}
