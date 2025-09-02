#!/bin/bash
uivJar=$1  #input arg

etcdctl_get() {
	curlparams="-s --cacert /opt/etcd_certs/ca.pem $ETCD_ACCESS_URL/v2/keys"
	res=`curl $curlparams$1 | sed 's/.*\"value\":\"\(.*\)\",.*/\1/'`
	echo $res
}

boltUrl=`etcdctl_get /services/Neo4J/boltURI`
neo4jUser=`etcdctl_get /config/Neo4J/userName`
neo4jPassword=`etcdctl_get /config/Neo4J/password`
logConfigFile=`etcdctl_get /config/log/log4j_configFile_path`
#echo "after return :: $boltUrl $neo4jUser $neo4jPassword $logConfigFile"

java -Dlogging.config=$logConfigFile -Dspring.data.neo4j.uri=$boltUrl -Dspring.data.neo4j.username=$neo4jUser -Dspring.data.neo4j.password=$neo4jPassword -jar $uivJar

