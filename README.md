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

## Result example
``` java
import { NgModule, Injectable } from '@angular/core';
import { Headers, Http, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
declare var jQuery: any;
const jsonHeader = new RequestOptions({ headers: new Headers({ 'Content-Type': 'application/json'}) });
const xwwwformurlencodedHeader = new RequestOptions({ headers: new Headers({ 'Content-Type': 'application/x-www-form-urlencoded'}) });
const formdataHeader = new RequestOptions({ headers: new Headers({ 'Content-Type': 'multipart/form-data'}) });

export interface BaseModel extends Manageable, Validable, Describable, Serializable {
    id: number;
}

export interface BaseSimpleModel extends BaseModel {
    description: string;
}

export interface Artist extends BaseSimpleModel {
    artistID: number;
    country: Country;
    userID: number;
    file: File;
}

export interface MultipartFormDataInput extends MultipartInput {
    formDataMap: { [index: string]: InputPart[] };
    formData: { [index: string]: InputPart };
}

export interface InputPart {
    headers: any;
    mediaType: MediaType;
    bodyAsString: string;
    contentTypeFromMessage: boolean;
}

export interface MultipartInput {
    parts: InputPart[];
    preamble: string;
}

export interface MediaType {
    type: string;
    subtype: string;
    parameters: { [index: string]: string };
    wildcardType: boolean;
    wildcardSubtype: boolean;
}

@Injectable()
export class ArtistService {

    constructor(private http: Http) {
    }

    create(arg0: Artist): Observable<any> {
        return this.http.post('rest/artist', arg0, jsonHeader );
    }

    current(): Observable<any> {
        return this.http.get('rest/artist/current');
    }

    delete(id: number): Observable<any> {
        return this.http.delete('rest/artist/' + id);
    }

    get(id: number): Observable<any> {
        return this.http.get('rest/artist/get:' + id);
    }

    getList(): Observable<any> {
        return this.http.get('rest/artist');
    }

    pagedlist(description: string, queryParams?: { firstResult?: number; maxResults?: number; }): Observable<any> {
        return this.http.get('rest/artist/pagedlist:' + description + '?' + jQuery.param(queryParams));
    }

    update(arg0: Artist): Observable<any> {
        return this.http.put('rest/artist', arg0, jsonHeader );
    }

    uploadFile(arg0: MultipartFormDataInput): Observable<any> {
        return this.http.post('rest/artist/submit', arg0, formdataHeader );
    }
}

@NgModule({ providers: [ 
	ArtistService
 ] })
export class AppBackendModule {
}

export type SomeTypeEnum = 'UNO' | 'DOS' | 'TRES'| 'QUIATORCE';
```