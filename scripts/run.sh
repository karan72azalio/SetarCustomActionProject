echo "Enter Neo4j IP Address"
read IPADDRESS
echo "Enter Neo4j password"
read PASSWORD

java -cp uiv-runtime-*.jar -Dloader.path=uiv-lib org.springframework.boot.loader.PropertiesLauncher --logging.level.root=INFO --version=21 --spring.data.neo4j.uri=bolt://${IPADDRESS}:7687 --spring.data.neo4j.password=${PASSWORD} --spring.cache.type=simple
