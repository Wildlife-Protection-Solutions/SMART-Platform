update smart.iconfile set filename = 'platform:/plugin/org.wcs.smart/images/datamodel/color/Rocks_and_minerals_icon.svg' where filename = 'platform:/plugin/org.wcs.smart/images/datamodel/color/Rocks & minerals_icon.svg';

update smart.iconfile set filename = 'platform:/plugin/org.wcs.smart/images/datamodel/line/Rocks_and_minerals_icon.svg' where filename = 'platform:/plugin/org.wcs.smart/images/datamodel/line/Rocks & minerals_icon.svg';

update smart.iconfile set filename = 'platform:/plugin/org.wcs.smart/images/datamodel/black/Rocks_and_minerals_icon.svg' where filename = 'platform:/plugin/org.wcs.smart/images/datamodel/black/Rocks & minerals_icon.svg';

update smart.iconfile set filename = 'platform:/plugin/org.wcs.smart/images/datamodel/color/Infrastructure_and_roads_icon.svg' where filename = 'platform:/plugin/org.wcs.smart/images/datamodel/color/Infrastructure & roads_icon.svg';

update smart.iconfile set filename = 'platform:/plugin/org.wcs.smart/images/datamodel/line/Infrastructure_and_roads_icon.svg' where filename = 'platform:/plugin/org.wcs.smart/images/datamodel/line/Infrastructure & roads_icon.svg';

update smart.iconfile set filename = 'platform:/plugin/org.wcs.smart/images/datamodel/black/Infrastructure_and_roads_icon.svg' where filename = 'platform:/plugin/org.wcs.smart/images/datamodel/black/Infrastructure & roads_icon.svg';

update smart.iconfile set filename = 'platform:/plugin/org.wcs.smart/images/datamodel/color/Weapons_and_Gear_seized_icon.svg' where filename = 'platform:/plugin/org.wcs.smart/images/datamodel/color/Weapons & Gear_seized_icon.svg';

update smart.iconfile set filename = 'platform:/plugin/org.wcs.smart/images/datamodel/line/Weapons_and_Gear_seized_icon.svg' where filename = 'platform:/plugin/org.wcs.smart/images/datamodel/line/Weapons & Gear_seized_icon.svg';

update smart.iconfile set filename = 'platform:/plugin/org.wcs.smart/images/datamodel/black/Weapons_and_Gear_seized_icon.svg' where filename = 'platform:/plugin/org.wcs.smart/images/datamodel/black/Weapons & Gear_seized_icon.svg';


--update versions
update connect.connect_plugin_version set version = '7.5.1' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '7.5.1' where plugin_id = 'org.wcs.smart';
update connect.connect_version set version = '7.5.1', last_updated = now();