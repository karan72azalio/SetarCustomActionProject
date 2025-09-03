echo off
set /p IPADDRESS=Enter Neo4j IP Address?:
set /p PASSWORD=Enter Neo4j password?:

java -cp * -Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Dloader.path=uiv-lib org.springframework.boot.loader.PropertiesLauncher --logging.level.root=INFO --version=21 --spring.data.neo4j.uri=bolt://%IPADDRESS%:7687 --spring.data.neo4j.password=%PASSWORD% --spring.cache.type=simple --uiv.neo4j.ogm.autoIndexMode=NONE