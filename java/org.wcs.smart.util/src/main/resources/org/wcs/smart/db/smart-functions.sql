CREATE FUNCTION smart.trimHkeyToLevel(level integer, hkey long varchar) returns varchar(32672)
LANGUAGE JAVA
deterministic 
external name 'org.wcs.smart.ca.datamodel.Category.trimHkeyToLevel'
PARAMETER STYLE JAVA
NO SQL 
RETURNS NULL ON NULL INPUT;


CREATE FUNCTION smart.hkeyLength(hkey long varchar) returns integer
LANGUAGE JAVA
deterministic 
external name 'org.wcs.smart.ca.datamodel.Category.hkeyLength'
PARAMETER STYLE JAVA
NO SQL 
RETURNS NULL ON NULL INPUT;

CREATE FUNCTION smart.pointInPolygon(x double, y double, wkb blob) returns boolean
LANGUAGE JAVA
deterministic
external name 'org.wcs.smart.util.GeometryUtils.pointInPolygon'
PARAMETER STYLE JAVA
NO SQL
RETURNS NULL ON NULL INPUT;


CREATE FUNCTION smart.intersects(wkb1 blob, wkb2 blob) returns boolean
LANGUAGE JAVA
deterministic
external name 'org.wcs.smart.util.GeometryUtils.intersects'
PARAMETER STYLE JAVA
NO SQL
RETURNS NULL ON NULL INPUT;


CREATE FUNCTION smart.computeTileId(x double, y double, destCrsWkt varchar(32672), originX double, originY double, gridSize double)
 returns varchar(32672)
LANGUAGE JAVA
deterministic 
external name 'org.wcs.smart.util.ReprojectUtils.computeTileId'
PARAMETER STYLE JAVA
NO SQL 
RETURNS NULL ON NULL INPUT;