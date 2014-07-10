insert into ct_to_smart.element (i, n, uuid) values (?, ?, ?)
insert into ct_to_smart.sighting (i, n, v, s_uuid, uuid) values (?, ?, ?, ?, ?)
insert into ct_to_smart.timertrack (i, n, v, t_uuid, uuid) values (?, ?, ?, ?, ?)




select distinct N, V from CT_TO_SMART.TIMERTRACK where N = 'DeviceId'

select count(distinct V) from CT_TO_SMART.SIGHTING where N = 'team_members'

select distinct V from CT_TO_SMART.SIGHTING where N = 'team_members'


--all locations type locations
select distinct e.n from CT_TO_SMART.ELEMENT e join CT_TO_SMART.SIGHTING s on e.i = s.v where s.N = 'Location'

--all attributes (used as I in A)
select distinct e.* from CT_TO_SMART.ELEMENT e join CT_TO_SMART.SIGHTING s on e.i = s.i


--possible value for given attribute
select distinct v from CT_TO_SMART.SIGHTING where N = '1-5m'
select distinct v from CT_TO_SMART.SIGHTING where N = 'Location'



select  v from CT_TO_SMART.SIGHTING where N = 'Chimpanzee'

----------------------------------------------------------

select distinct v from CT_TO_SMART.SIGHTING where n='Date'

select S_UUID from CT_TO_SMART.SIGHTING where n='Date' and v='1/1/2007'

select * from CT_TO_SMART.SIGHTING where s_uuid in (select S_UUID from CT_TO_SMART.SIGHTING where n='Date' and v='1/1/2007')


--per day per unit
select distinct sd.v, su.v from CT_TO_SMART.SIGHTING sd join CT_TO_SMART.SIGHTING su on sd.s_uuid = su.s_uuid where sd.n='Date' and su.n='Unit_ID'

select s_uuid, i, n, v  from CT_TO_SMART.SIGHTING where s_uuid in (select sd.S_UUID from CT_TO_SMART.SIGHTING sd join CT_TO_SMART.SIGHTING su on sd.s_uuid = su.s_uuid where sd.n='Date' and sd.v='1/1/2007' and su.n='Unit_ID' and su.v='WCS_NIGERIA_1') order by uuid





insert into ct_to_smart.attributes (n, i) select distinct e.n, e.i from CT_TO_SMART.ELEMENT e join CT_TO_SMART.SIGHTING s on e.i = s.i
