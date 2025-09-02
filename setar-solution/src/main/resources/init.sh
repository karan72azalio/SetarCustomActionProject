/gen_keystore.sh /keycloak_kong.cer changeit /usr/lib/jvm/jre-11-openjdk/lib/security/cacerts
java $JAVA_OPTS -cp ${jar.name} -Dloader.path=uiv-lib org.springframework.boot.loader.PropertiesLauncher $STARTUP_OPTS --spring.cloud.vault.token=$VAULT_TOKEN
