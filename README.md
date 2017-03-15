# jaxrs-angular-maven-plugin

This maven plugin is a tool that generate models and services between a java jax-rs project and a typescript angular project.

## Use example
```xml
<plugin>
	<groupId>com.primus</groupId>
	<artifactId>jaxrs-angular-maven-plugin</artifactId>
	<version>01.00.00-SNAPSHOT</version>
	<executions>
		<execution>
			<id>generate</id>
			<goals>
				<goal>generate</goal>
			</goals>
			<configuration>
				<module>AppBackendModule</module>
				<pathPrefix>rest</pathPrefix>
				<serviceNameReplacement>EndPoint|Service</serviceNameReplacement>
				<outputFile>${reelbook.frontend.path}/app/app.backend.ts</outputFile>
				<classPatterns>
					<pattern>com.reelbook.core.model.*</pattern>
					<pattern>com.reelbook.core.model.mpi.*</pattern>
					<pattern>com.reelbook.model.**</pattern>
					<pattern>com.reelbook.core.rest.response.*</pattern>
					<pattern>com.reelbook.rest.endpoint.*</pattern>
				</classPatterns>
				<excludeClassPatterns>
					<pattern>com.reelbook.core.model.*_</pattern>
					<pattern>com.reelbook.core.model.mpi.*_</pattern>
					<pattern>com.reelbook.model.**_</pattern>
				</excludeClassPatterns>
				<excludeClasses>
					<class>com.reelbook.rest.endpoint.TestEndPoint</class>
				</excludeClasses>
			</configuration>
		</execution>
	</executions>
</plugin>
```