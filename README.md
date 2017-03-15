# jaxrs-angular-maven-plugin

This maven plugin is a tool that generate models and services between a java jax-rs project and a typescript angular project.

## Use example
```xml
<plugin>
	<groupId>com.primus</groupId>
	<artifactId>jaxrs-angular-maven-plugin</artifactId>
	<version>X.Y.Z</version>
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
				<outputFile>${angular.application.path}/app/app.backend.ts</outputFile>
				<classPatterns>
					<pattern>com.somepath.core.model.*</pattern>
					<pattern>com.somepath.core.model.mpi.*</pattern>
					<pattern>com.somepath.model.**</pattern>
					<pattern>com.somepath.core.rest.response.*</pattern>
					<pattern>com.somepath.rest.endpoint.*</pattern>
				</classPatterns>
				<excludeClassPatterns>
					<pattern>com.somepath.core.model.*_</pattern>
					<pattern>com.somepath.core.model.mpi.*_</pattern>
					<pattern>com.somepath.model.**_</pattern>
				</excludeClassPatterns>
				<excludeClasses>
					<class>com.somepath.rest.endpoint.TestEndPoint</class>
				</excludeClasses>
			</configuration>
		</execution>
	</executions>
</plugin>
```