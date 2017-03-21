
package com.primus.parser;

import java.lang.reflect.Type;
import java.util.*;

public class JaxrsMethodModel extends MethodModel
{

	private final String httpMethod;
	private final String[] consumes;
	private final String path;
	private final List<MethodParameterModel> pathParams;
	private final List<MethodParameterModel> formParams;
	private final List<MethodParameterModel> queryParams;
	private final MethodParameterModel entityParam;

	public JaxrsMethodModel(Class<?> originClass, String name, Type returnType, String httpMethod, String[] consumes, String path,
			List<MethodParameterModel> pathParams, List<MethodParameterModel> formParams, List<MethodParameterModel> queryParams,
			MethodParameterModel entityParam)
	{
		super(originClass, name, null, returnType);
		this.httpMethod = httpMethod;
		this.consumes = consumes;
		this.path = path;
		this.pathParams = pathParams;
		this.formParams = formParams;
		this.queryParams = queryParams;
		this.entityParam = entityParam;
	}

	public String getHttpMethod()
	{
		return httpMethod;
	}

	public String[] getConsumes()
	{
		return consumes;
	}

	public String getPath()
	{
		return path;
	}

	public List<MethodParameterModel> getPathParams()
	{
		return pathParams;
	}

	public List<MethodParameterModel> getFormParams()
	{
		return formParams;
	}

	public List<MethodParameterModel> getQueryParams()
	{
		return queryParams;
	}

	public MethodParameterModel getEntityParam()
	{
		return entityParam;
	}

}
