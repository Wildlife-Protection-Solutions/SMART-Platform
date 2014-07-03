package org.wcs.smart.ct2smart.run;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.xml.bind.JAXBException;

import org.wcs.smart.ct2smart.dao.ConnectionUtil;
import org.wcs.smart.ct2smart.db.DbLoader;
import org.wcs.smart.ct2smart.matcher.FileUtil;
import org.wcs.smart.ct2smart.matcher.MatchFileBuilder;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;

/**
 * @author elitvin
 * @since 3.0.0
 */
@Deprecated
public class FullLaunch {

	public static void main(String[] args) throws SQLException, JAXBException, IOException {
		long start = System.currentTimeMillis();
		Connection c = ConnectionUtil.getConnection();
		
		DbLoader loader = new DbLoader();
//		loader.load(new File("d:\\dev\\data\\CyberTracker Data_Jan-2014\\test.xml"), c);
		loader.load(new File("d:\\dev\\data\\CyberTracker Data_Jan-2014\\test_short_super.xml"), c);

		System.out.println("DB loaded in "+ (double)(System.currentTimeMillis()-start)/1000 +" seconds!!!");
		start = System.currentTimeMillis();

		MatchFileBuilder matchBuilder = new MatchFileBuilder();
		Ct2Smart ct2Smart = matchBuilder.create(c);
		FileUtil.write(new File("match_super.xml"), ct2Smart);

		System.out.println("File created in "+ (double)(System.currentTimeMillis()-start)/1000 +" seconds!!!");
	}
}
