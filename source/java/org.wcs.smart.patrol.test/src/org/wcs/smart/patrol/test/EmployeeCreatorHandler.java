package org.wcs.smart.patrol.test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public class EmployeeCreatorHandler extends AbstractHandler  {

	String[] names = new String[]{
			"Erma Blaze",
			"Karole Gobble",
			"Tamie Mcmurray",
			"Dwayne Petrick",
			"Kymberly Dauphin",
			"Buster Nino",
			"Jeanna Galvez",
			"Margene Romines",
			"Hoyt Rodriquez",
			"Olinda Sirmons",
			"Paola Sieck",
			"Renay Billingsley",
			"Tyree Tuthill",
			"Joette Malia",
			"Melody Ferrier",
			"Holley Metts",
			"Marceline Matheney",
			"Malia Lightner",
			"Vaughn Brasil",
			"Ambrose Zakrzewski",
			"Young Jeanpierre",
			"Becki Lorenzo",
			"Elliot Geno",
			"Lawanda Devos",
			"Keri Collelo",
			"Joshua Fountaine",
			"Paulina Eisenberg",
			"Doretta Rosner",
			"Donya Nickles",
			"Roselee Byford",
			"Sharlene Cotton",
			"Florence Justice",
			"Myriam Lewter",
			"Farrah Buchholz",
			"Traci Karner",
			"Lien Warrington",
			"Oralia Gillen",
			"Angelica Carrozza",
			"Josefa Mandez",
			"Cleo Pardon",
			"Catherine Say",
			"Elmira Ulloa",
			"Shelton Gafford",
			"Lashaun Kmetz",
			"Antony Rizer",
			"Tony Saam",
			"Kandice Carlsen",
			"Albertina Holgate",
			"Briana Zuchowski",
			"Susanna Patnaude",
			"Alta Herdt",
			"Stasia Saia",
			"Junie Strandberg",
			"Tari Fines",
			"Maybell Lothrop",
			"Pedro Tso",
			"Jasmine Finks",
			"Kristal Axelson",
			"Merissa Starbird",
			"Bernice Sabia",
			"Savannah Enderle",
			"Katherina Kofoed",
			"Eustolia Boyer",
			"Christoper Wimbish",
			"Terry Familia",
			"Rhoda Taubman",
			"Rufina Burse",
			"Thaddeus Sproull",
			"Etta Marez",
			"Felisha Krall",
			"Trudy Teachout",
			"Odette Flakes",
			"Socorro Gabrielson",
			"Jaimee Baltimore",
			"Livia Soltys",
			"Randi Marmol",
			"Cheryle Girouard",
			"Sallie Ellison",
			"Dinah Jaso",
			"Jeannie Degroff",
			"Palmer Yurick",
			"Amada Mccallum",
			"Melvina Bohon",
			"Makeda Yawn",
			"Charlotte Fong",
			"Kenna Crosswhite",
			"Emilee Morfin",
			"Kenneth Schmucker",
			"Drema Likes",
			"Candis Studivant",
			"Cherryl Gaal",
			"Mikaela Lyon",
			"Larue Finan",
			"Jayne Lewallen",
			"Alica Wilkison",
			"Jacqualine Bartolotta",
			"Ute Hillock",
			"Klara Fuqua",
			"Tessie Dashiell",
			"Apryl Bracamonte"};

	

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		execute();
		return null;
	}
	
	private void execute() {
		
		Random r = new Random();
		
		List<String> dnames = new ArrayList<>();
		for (String x : names) dnames.add(x);
		
		Set<String> ids = new HashSet<>();
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			for (int i = 0; i < 50; i ++) {
				Employee e = new Employee();
				e.setConservationArea(SmartDB.getCurrentConservationArea());
				
				String name = dnames.get(r.nextInt(dnames.size()-1));
				dnames.remove(name);
				e.setFamilyName(name.split(" ")[1]);
				e.setGivenName(name.split(" ")[0]);
				
				
				e.setBirthDate(LocalDate.of(1920 + r.nextInt(50), r.nextInt(11)+1, r.nextInt(27)+1));
				e.setDateCreated(LocalDate.now());
				e.setStartEmploymentDate(LocalDate.now());
				e.setGender(r.nextInt(100) < 50 ? 'F' : 'M');
				e.setId("E00" + i);
				session.save(e);
				
				if (r.nextInt(50) < 10) {
					String id = name.toLowerCase().replaceAll(" ", "");
					if (id.length() > 15) id = id.substring(0, 14);
					
					if (!ids.contains(id)) {
						e.setSmartUserId(id);
						e.setSmartPassword("smart");
						e.setSmartUserLevelKeys("ADMIN");
						ids.add(id);
					}
				}
			}
			session.getTransaction().commit();
		}
		
	}
}
