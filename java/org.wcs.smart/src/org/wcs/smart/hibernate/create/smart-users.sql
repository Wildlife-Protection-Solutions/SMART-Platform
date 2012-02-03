 
 

--Turning on authentication
--Setting and Confirming requireAuthentication
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.connection.requireAuthentication', 'true');
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.authentication.provider', 'BUILTIN');

CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.smart_admin', 'smart_derby');
-- adding a login user
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.login', 'smrt');
--adding manager user
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.manager', 'smrt');
--adding analyst user
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.analyst', 'smrt');
--adding data_entry user
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.data_entry', 'smrt');
		
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.defaultConnectionMode', 'noAccess');
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.fullAccessUsers', 'smart_admin,login,manager,analyst,data_entry');

CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.propertiesOnly', 'true'); 


CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.sqlAuthorization', 'true');


