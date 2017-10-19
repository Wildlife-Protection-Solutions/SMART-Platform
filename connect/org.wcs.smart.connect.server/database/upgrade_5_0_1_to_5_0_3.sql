CREATE OR REPLACE FUNCTION smart.computeHoursPoly(polygon bytea, linestring bytea) RETURNS double precision AS $$
DECLARE
  ls geometry;
  p geometry;
  value double precision;
  ctime double precision;
  clength double precision;
  i integer;
  pnttemp geometry;
  pnttemp2 geometry;
  lstemp geometry;
BEGIN
	ls := st_geomfromwkb(linestring);
	p := st_geomfromwkb(polygon);
	
	--wholly contained use entire time
	IF not st_isvalid(ls) and st_length(ls) = 0 THEN
		pnttemp = st_pointn(ls, 1);
		IF (smart.pointinpolygon(st_x(pnttemp),st_y(pnttemp), p)) THEN
			RETURN (st_z(st_endpoint(ls)) - st_z(st_startpoint(ls))) / 3600000.0;
		END IF;
		RETURN 0;
	END IF;
	
	IF (st_contains(p, ls)) THEN
		return (st_z(st_endpoint(ls)) - st_z(st_startpoint(ls))) / 3600000.0;
	END IF;
	
	value := 0;
	FOR i in 1..ST_NumPoints(ls)-1 LOOP
		pnttemp := st_pointn(ls, i);
		pnttemp2 := st_pointn(ls, i+1);
		lstemp := st_makeline(pnttemp, pnttemp2);	
		IF (NOT st_intersects(st_envelope(ls), st_envelope(lstemp))) THEN
			--do nothing; outside envelope
		ELSE
			IF (ST_COVERS(p, lstemp)) THEN
				value := value + st_z(pnttemp2) - st_z(pnttemp);
			ELSIF (ST_INTERSECTS(p, lstemp)) THEN
				ctime := st_z(pnttemp2) - st_z(pnttemp);
				clength := st_distance(pnttemp, pnttemp2);
				IF (clength = 0) THEN
					--points are the same and intersect so include the entire time
					value := value + ctime;
				ELSE
					--part in part out so linearly interpolate
					value := value + (ctime * (st_length(st_intersection(p, lstemp)) / clength));
				END IF;
			END IF;
		END IF;
	END LOOP;
	RETURN value / 3600000.0;
END;
$$LANGUAGE plpgsql;


update connect.connect_version set version = '5.0.3';