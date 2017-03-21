
package com.github.nahuelolgiati.compiler;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import com.github.nahuelolgiati.ClassMapping;
import com.github.nahuelolgiati.DateMapping;
import com.github.nahuelolgiati.EnumMapping;
import com.github.nahuelolgiati.Settings;
import com.github.nahuelolgiati.TsProperty;
import com.github.nahuelolgiati.TsType;
import com.github.nahuelolgiati.TypeProcessor;
import com.github.nahuelolgiati.emitter.TsAccessibilityModifier;
import com.github.nahuelolgiati.emitter.TsAliasModel;
import com.github.nahuelolgiati.emitter.TsBeanModel;
import com.github.nahuelolgiati.emitter.TsCallExpression;
import com.github.nahuelolgiati.emitter.TsConstructorModel;
import com.github.nahuelolgiati.emitter.TsEnumModel;
import com.github.nahuelolgiati.emitter.TsExpression;
import com.github.nahuelolgiati.emitter.TsIdentifierReference;
import com.github.nahuelolgiati.emitter.TsMemberExpression;
import com.github.nahuelolgiati.emitter.TsMethodModel;
import com.github.nahuelolgiati.emitter.TsModel;
import com.github.nahuelolgiati.emitter.TsParameterModel;
import com.github.nahuelolgiati.emitter.TsPropertyModel;
import com.github.nahuelolgiati.emitter.TsReturnStatement;
import com.github.nahuelolgiati.emitter.TsStatement;
import com.github.nahuelolgiati.emitter.TsStringLiteral;
import com.github.nahuelolgiati.emitter.TsTemplateLiteral;
import com.github.nahuelolgiati.emitter.TsThisExpression;
import com.github.nahuelolgiati.parser.BeanModel;
import com.github.nahuelolgiati.parser.EnumModel;
import com.github.nahuelolgiati.parser.JaxrsApplicationModel;
import com.github.nahuelolgiati.parser.JaxrsMethodModel;
import com.github.nahuelolgiati.parser.MethodModel;
import com.github.nahuelolgiati.parser.MethodParameterModel;
import com.github.nahuelolgiati.parser.Model;
import com.github.nahuelolgiati.parser.PathTemplate;
import com.github.nahuelolgiati.parser.PropertyModel;
import com.github.nahuelolgiati.util.Utils;

public class ModelCompiler
{

	private final Settings settings;
	private final TypeProcessor typeProcessor;
	private Map<String[], String> consumesHeadersMap;

	public ModelCompiler(Settings settings, TypeProcessor typeProcessor)
	{
		this.settings = settings;
		this.typeProcessor = typeProcessor;
	}

	public TsModel javaToTypeScript(Model model)
	{
		final SymbolTable symbolTable = new SymbolTable(settings);
		TsModel tsModel = processModel(symbolTable, model);
		tsModel = removeInheritedProperties(symbolTable, tsModel);
		tsModel = addImplementedProperties(symbolTable, tsModel);

		// JAX-RS
		final JaxrsApplicationModel jaxrsApplication = model.getJaxrsApplication() != null ? model.getJaxrsApplication()
				: new JaxrsApplicationModel();
		final Symbol responseSymbol = symbolTable.getSyntheticSymbol("Observable");
		tsModel = createJaxrsClient(symbolTable, tsModel, jaxrsApplication, responseSymbol);

		// dates
		tsModel = transformDates(symbolTable, tsModel);

		// enums
		if (settings.mapEnum == null || settings.mapEnum == EnumMapping.asUnion || settings.mapEnum == EnumMapping.asInlineUnion)
		{
			tsModel = transformEnumsToUnions(tsModel);
		}
		if (settings.mapEnum == EnumMapping.asInlineUnion)
		{
			tsModel = inlineEnums(tsModel, symbolTable);
		}

		// tagged unions
		tsModel = createAndUseTaggedUnions(symbolTable, tsModel);

		symbolTable.resolveSymbolNames();
		tsModel = sortDeclarations(symbolTable, tsModel);
		return tsModel;
	}

	public TsType javaToTypeScript(Type type)
	{
		final BeanModel beanModel = new BeanModel(Object.class, Object.class, null, null, null, Collections.<Type> emptyList(),
				Collections.singletonList(new PropertyModel("property", type, false, null, null, null)), null);
		final Model model = new Model(Collections.singletonList(beanModel), Collections.<EnumModel<?>> emptyList(), null);
		final TsModel tsModel = javaToTypeScript(model);
		return tsModel.getBeans().get(0).getProperties().get(0).getTsType();
	}

	private TsModel processModel(SymbolTable symbolTable, Model model)
	{
		final Map<Type, List<BeanModel>> children = createChildrenMap(model);
		final List<TsBeanModel> beans = new ArrayList<>();
		for (BeanModel bean : model.getBeans())
		{
			beans.add(processBean(symbolTable, model, children, bean));
		}
		final List<TsEnumModel<?>> enums = new ArrayList<>();
		for (EnumModel<?> enumModel : model.getEnums())
		{
			enums.add(processEnum(symbolTable, enumModel));
		}
		final List<TsAliasModel> typeAliases = new ArrayList<>();
		return new TsModel(beans, enums, typeAliases);
	}

	private Map<Type, List<BeanModel>> createChildrenMap(Model model)
	{
		final Map<Type, List<BeanModel>> children = new LinkedHashMap<>();
		for (BeanModel bean : model.getBeans())
		{
			for (Type ancestor : bean.getParentAndInterfaces())
			{
				if (!children.containsKey(ancestor))
				{
					children.put(ancestor, new ArrayList<BeanModel>());
				}
				children.get(ancestor).add(bean);
			}
		}
		return children;
	}

	private <T> TsBeanModel processBean(SymbolTable symbolTable, Model model, Map<Type, List<BeanModel>> children, BeanModel bean)
	{
		final boolean isClass = !bean.getOrigin().isInterface() && settings.mapClasses == ClassMapping.asClasses;
		final Symbol beanIdentifier = symbolTable.getSymbol(bean.getOrigin());
		final List<TsType.GenericVariableType> typeParameters = new ArrayList<>();
		for (TypeVariable<?> typeParameter : bean.getOrigin().getTypeParameters())
		{
			typeParameters.add(new TsType.GenericVariableType(typeParameter.getName()));
		}
		TsType parentType = typeFromJava(symbolTable, bean.getParent());
		if (parentType != null && parentType.equals(TsType.Any))
		{
			parentType = null;
		}
		final List<TsType> interfaces = new ArrayList<>();
		for (Type aInterface : bean.getInterfaces())
		{
			final TsType interfaceType = typeFromJava(symbolTable, aInterface);
			if (!interfaceType.equals(TsType.Any))
			{
				interfaces.add(interfaceType);
			}
		}
		final List<TsPropertyModel> properties = processProperties(symbolTable, model, bean, "", "");

		if (bean.getDiscriminantProperty() != null && !containsProperty(properties, bean.getDiscriminantProperty()))
		{
			final List<BeanModel> selfAndDescendants = getSelfAndDescendants(bean, children);
			final List<TsType.StringLiteralType> literals = new ArrayList<>();
			for (BeanModel descendant : selfAndDescendants)
			{
				if (descendant.getDiscriminantLiteral() != null)
				{
					literals.add(new TsType.StringLiteralType(descendant.getDiscriminantLiteral()));
				}
			}
			final TsType discriminantType = literals.isEmpty() ? TsType.String : new TsType.UnionType(literals);
			properties.add(0, new TsPropertyModel(bean.getDiscriminantProperty(), discriminantType, settings.declarePropertiesAsReadOnly, null));
		}

		return new TsBeanModel(bean.getOrigin(), isClass, beanIdentifier, typeParameters, parentType, bean.getTaggedUnionClasses(), interfaces,
				properties, null, null, bean.getComments(), null);
	}

	private List<TsPropertyModel> processProperties(SymbolTable symbolTable, Model model, BeanModel bean, String prefix, String suffix)
	{
		final List<TsPropertyModel> properties = new ArrayList<>();
		for (PropertyModel property : bean.getProperties())
		{
			boolean pulled = false;
			final PropertyModel.PullProperties pullProperties = property.getPullProperties();
			if (pullProperties != null)
			{
				if (property.getType() instanceof Class<?>)
				{
					final BeanModel pullBean = model.getBean((Class<?>) property.getType());
					if (pullBean != null)
					{
						properties.addAll(
								processProperties(symbolTable, model, pullBean, prefix + pullProperties.prefix, pullProperties.suffix + suffix));
						pulled = true;
					}
				}
			}
			if (!pulled)
			{
				properties.add(processProperty(symbolTable, bean, property, prefix, suffix));
			}
		}
		return properties;
	}

	private static List<BeanModel> getSelfAndDescendants(BeanModel bean, Map<Type, List<BeanModel>> children)
	{
		final List<BeanModel> descendants = new ArrayList<>();
		descendants.add(bean);
		final List<BeanModel> directDescendants = children.get(bean.getOrigin());
		if (directDescendants != null)
		{
			for (BeanModel descendant : directDescendants)
			{
				descendants.addAll(getSelfAndDescendants(descendant, children));
			}
		}
		return descendants;
	}

	private static boolean containsProperty(List<TsPropertyModel> properties, String propertyName)
	{
		for (TsPropertyModel property : properties)
		{
			if (property.getName().equals(propertyName))
			{
				return true;
			}
		}
		return false;
	}

	private TsPropertyModel processProperty(SymbolTable symbolTable, BeanModel bean, PropertyModel property, String prefix, String suffix)
	{
		final TsType type = typeFromJava(symbolTable, property.getType(), property.getName(), bean.getOrigin());
		final TsType tsType = property.isOptional() ? type.optional() : type;
		return new TsPropertyModel(prefix + property.getName() + suffix, tsType, settings.declarePropertiesAsReadOnly, property.getComments());
	}

	private TsEnumModel<?> processEnum(SymbolTable symbolTable, EnumModel<?> enumModel)
	{
		final Symbol beanIdentifier = symbolTable.getSymbol(enumModel.getOrigin());
		return TsEnumModel.fromEnumModel(beanIdentifier, enumModel);
	}

	private TsType typeFromJava(SymbolTable symbolTable, Type javaType)
	{
		return typeFromJava(symbolTable, javaType, null, null);
	}

	private TsType typeFromJava(SymbolTable symbolTable, Type javaType, String usedInProperty, Class<?> usedInClass)
	{
		if (javaType == null)
		{
			return null;
		}
		final TypeProcessor.Context context = new TypeProcessor.Context(symbolTable, typeProcessor);
		final TypeProcessor.Result result = context.processType(javaType);
		if (result != null)
		{
			return result.getTsType();
		}
		else
		{
			if (usedInClass != null && usedInProperty != null)
			{
				System.out.println(
						String.format("Warning: Unsupported type '%s' used in '%s.%s'", javaType, usedInClass.getSimpleName(), usedInProperty));
			}
			else
			{
				System.out.println(String.format("Warning: Unsupported type '%s'", javaType));
			}
			return TsType.Any;
		}
	}

	private TsModel removeInheritedProperties(SymbolTable symbolTable, TsModel tsModel)
	{
		final List<TsBeanModel> beans = new ArrayList<>();
		for (TsBeanModel bean : tsModel.getBeans())
		{
			final Map<String, TsType> inheritedPropertyTypes = getInheritedProperties(symbolTable, tsModel, bean.getParentAndInterfaces());
			final List<TsPropertyModel> properties = new ArrayList<>();
			for (TsPropertyModel property : bean.getProperties())
			{
				if (!Objects.equals(property.getTsType(), inheritedPropertyTypes.get(property.getName())))
				{
					properties.add(property);
				}
			}
			beans.add(bean.withProperties(properties));
		}
		return tsModel.setBeans(beans);
	}

	private TsModel addImplementedProperties(SymbolTable symbolTable, TsModel tsModel)
	{
		final List<TsBeanModel> beans = new ArrayList<>();
		for (TsBeanModel bean : tsModel.getBeans())
		{
			if (bean.isClass())
			{
				final List<TsPropertyModel> resultProperties = new ArrayList<>(bean.getProperties());

				final Set<String> classPropertyNames = new LinkedHashSet<>();
				for (TsPropertyModel property : bean.getProperties())
				{
					classPropertyNames.add(property.getName());
				}
				classPropertyNames.addAll(getInheritedProperties(symbolTable, tsModel, Utils.listFromNullable(bean.getParent())).keySet());

				final List<TsPropertyModel> implementedProperties = getImplementedProperties(symbolTable, tsModel, bean.getInterfaces());
				Collections.reverse(implementedProperties);
				for (TsPropertyModel implementedProperty : implementedProperties)
				{
					if (!classPropertyNames.contains(implementedProperty.getName()))
					{
						resultProperties.add(0, implementedProperty);
						classPropertyNames.add(implementedProperty.getName());
					}
				}

				beans.add(bean.withProperties(resultProperties));
			}
			else
			{
				beans.add(bean);
			}
		}
		return tsModel.setBeans(beans);
	}

	private static Map<String, TsType> getInheritedProperties(SymbolTable symbolTable, TsModel tsModel, List<TsType> parents)
	{
		final Map<String, TsType> properties = new LinkedHashMap<>();
		for (TsType parentType : parents)
		{
			final TsBeanModel parent = tsModel.getBean(getOriginClass(symbolTable, parentType));
			if (parent != null)
			{
				properties.putAll(getInheritedProperties(symbolTable, tsModel, parent.getParentAndInterfaces()));
				for (TsPropertyModel property : parent.getProperties())
				{
					properties.put(property.getName(), property.getTsType());
				}
			}
		}
		return properties;
	}

	private static List<TsPropertyModel> getImplementedProperties(SymbolTable symbolTable, TsModel tsModel, List<TsType> interfaces)
	{
		final List<TsPropertyModel> properties = new ArrayList<>();
		for (TsType aInterface : interfaces)
		{
			final TsBeanModel bean = tsModel.getBean(getOriginClass(symbolTable, aInterface));
			if (bean != null)
			{
				properties.addAll(getImplementedProperties(symbolTable, tsModel, bean.getInterfaces()));
				properties.addAll(bean.getProperties());
			}
		}
		return properties;
	}

	private TsModel createJaxrsClient(SymbolTable symbolTable, TsModel tsModel, JaxrsApplicationModel jaxrsApplication, Symbol responseSymbol)
	{

		settings.importDeclarations = new ArrayList<>();
		settings.importDeclarations.add("import { NgModule, Injectable } from '@angular/core'");
		settings.importDeclarations.add("import { Headers, Http, RequestOptions } from '@angular/http'");
		settings.importDeclarations.add("import { Observable } from 'rxjs/Rx'");
		settings.importDeclarations.add("declare var jQuery: any");
		consumesHeadersMap = getConsumesHeadersMap(MediaType.APPLICATION_JSON, MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_FORM_URLENCODED);
		settings.importDeclarations.addAll(consumesHeadersMap.values());

		final List<TsBeanModel> processJaxrsServices = processJaxrsServices(jaxrsApplication, symbolTable, responseSymbol, true);
		tsModel.getBeans().addAll(processJaxrsServices);

		tsModel.getBeans().add(getClientModule(symbolTable, processJaxrsServices));

		return tsModel;
	}

	private static Map<String[], String> getConsumesHeadersMap(String... mediaTypes)
	{
		HashMap<String[], String> consumesHeadersMap = new HashMap<>();
		for (String mediaType : mediaTypes)
		{
			final String identifier = mediaType.substring(mediaType.indexOf("/") + 1).replace("-", "") + "Header ";
			String[] key = {mediaType, identifier};
			consumesHeadersMap.put(key,
					"const " + identifier + "= new RequestOptions({ headers: new Headers({ 'Content-Type': '" + mediaType + "'}) })");
		}
		return consumesHeadersMap;
	}

	private List<TsBeanModel> processJaxrsServices(JaxrsApplicationModel jaxrsApplication, SymbolTable symbolTable, Symbol responseSymbol,
			boolean implement)
	{
		final List<TsBeanModel> services = new ArrayList<>();
		final String applicationPath = jaxrsApplication.getApplicationPath();
		final Map<String, List<JaxrsMethodModel>> serviceMethodMap = groupingByServiceName(jaxrsApplication.getMethods());

		for (Entry<String, List<JaxrsMethodModel>> entry : serviceMethodMap.entrySet())
		{
			final String serviceName = entry.getKey();
			final List<JaxrsMethodModel> methods = entry.getValue();

			List<TsMethodModel> tsMethodModelList = new ArrayList<>();
			for (JaxrsMethodModel method : methods)
			{
				tsMethodModelList.add(processJaxrsMethod(symbolTable, applicationPath, responseSymbol, method, implement));
			}

			// Http Symbol
			final Symbol angularHttpSymbol = symbolTable.getSyntheticSymbol("Http");

			// application client class
			final TsConstructorModel constructor = new TsConstructorModel(
					Arrays.asList(new TsParameterModel(TsAccessibilityModifier.Private, "http", new TsType.ReferenceType(angularHttpSymbol))),
					Collections.<TsStatement> emptyList(), null);

			final TsBeanModel clientModel = new TsBeanModel(Application.class, true, symbolTable.getSyntheticSymbol(serviceName), null, null, null,
					null, null, constructor, tsMethodModelList, null, Utils.listFromNullable("@Injectable()"));
			services.add(clientModel);
		}

		return services;
	}

	private TsBeanModel getClientModule(SymbolTable symbolTable, final List<TsBeanModel> processJaxrsServices)
	{
		String module = "@NgModule({ providers: [ ";
		for (Iterator<TsBeanModel> iterator = processJaxrsServices.iterator(); iterator.hasNext();)
		{
			module += settings.newline + "\t" + iterator.next().getName();
			if (iterator.hasNext())
			{
				module += ",";
			}
		}
		module += settings.newline + " ] })";

		final TsBeanModel clientModule = new TsBeanModel(Application.class, true, symbolTable.getSyntheticSymbol(settings.module), null, null, null,
				null, null, null, null, null, Arrays.asList(module));
		return clientModule;
	}

	private Map<String, List<JaxrsMethodModel>> groupingByServiceName(List<JaxrsMethodModel> methods)
	{
		final Map<String, List<JaxrsMethodModel>> serviceMethodMap = new LinkedHashMap<>();
		for (JaxrsMethodModel method : methods)
		{
			final String className = method.getOriginClass().getSimpleName();
			final String serviceName = settings.serviceNameReplacement != null ? replaceServiceName(className) : className;
			if (serviceMethodMap.containsKey(serviceName))
			{
				serviceMethodMap.get(serviceName).add(method);
			}
			else
			{
				final List<JaxrsMethodModel> mapMethods = new ArrayList<>();
				mapMethods.add(method);
				serviceMethodMap.put(serviceName, mapMethods);
			}
		}
		return serviceMethodMap;
	}

	private String replaceServiceName(String className)
	{
		String oldChar = settings.serviceNameReplacement.substring(0, settings.serviceNameReplacement.indexOf("|"));
		String newChar = settings.serviceNameReplacement.substring(settings.serviceNameReplacement.indexOf("|") + 1,
				settings.serviceNameReplacement.length());
		return className.replace(oldChar, newChar);
	}

	private TsMethodModel processJaxrsMethod(SymbolTable symbolTable, String applicationPath, Symbol responseSymbol, JaxrsMethodModel method,
			boolean implement)
	{
		final String path = Utils.joinPath(getPathWithPrefix(applicationPath), method.getPath());
		final PathTemplate pathTemplate = PathTemplate.parse(path);
		final List<TsParameterModel> parameters = new ArrayList<>();
		// path params
		for (MethodParameterModel parameter : method.getPathParams())
		{
			parameters.add(processParameter(symbolTable, method, parameter));
		}
		// entity param
		if (method.getEntityParam() != null)
		{
			parameters.add(processParameter(symbolTable, method, method.getEntityParam()));
		}
		// form params
		final List<MethodParameterModel> formParams = method.getFormParams();
		final TsParameterModel formParameter;
		if (formParams != null && !formParams.isEmpty())
		{
			final List<TsProperty> properties = new ArrayList<>();
			for (MethodParameterModel queryParam : formParams)
			{
				final TsType type = typeFromJava(symbolTable, queryParam.getType(), method.getName(), method.getOriginClass());
				properties.add(new TsProperty(queryParam.getName(), new TsType.OptionalType(type)));
			}
			formParameter = new TsParameterModel("formParams", new TsType.OptionalType(new TsType.ObjectType(properties)));
			parameters.add(formParameter);
		}
		else
		{
			formParameter = null;
		}
		// query params
		final List<MethodParameterModel> queryParams = method.getQueryParams();
		final TsParameterModel queryParameter;
		if (queryParams != null && !queryParams.isEmpty())
		{
			final List<TsProperty> properties = new ArrayList<>();
			for (MethodParameterModel queryParam : queryParams)
			{
				final TsType type = typeFromJava(symbolTable, queryParam.getType(), method.getName(), method.getOriginClass());
				properties.add(new TsProperty(queryParam.getName(), new TsType.OptionalType(type)));
			}
			queryParameter = new TsParameterModel("queryParams", new TsType.OptionalType(new TsType.ObjectType(properties)));
			parameters.add(queryParameter);
		}
		else
		{
			queryParameter = null;
		}
		// return type
		final TsType returnType = typeFromJava(symbolTable, method.getReturnType(), method.getName(), method.getOriginClass());
		final TsType wrappedReturnType = new TsType.GenericReferenceType(responseSymbol, returnType);

		// implementation
		final List<TsStatement> body;
		if (implement)
		{

			body = new ArrayList<>();
			TsCallExpression tsCallExpression = new TsCallExpression(
					new TsMemberExpression(new TsMemberExpression(new TsThisExpression(), "http"), method.getHttpMethod().toLowerCase()),
					Utils.removeNulls(Arrays.asList(processPathTemplate(pathTemplate, queryParameter),
							method.getEntityParam() != null ? new TsIdentifierReference(method.getEntityParam().getName()) : null,
							formParameter != null ? new TsIdentifierReference("jQuery.param(formParams)") : null, getConsumes(method))));

			body.add(new TsReturnStatement(tsCallExpression));
		}
		else
		{
			body = null;
		}
		// method
		final TsMethodModel tsMethodModel = new TsMethodModel(method.getName(), wrappedReturnType, parameters, body, null);
		return tsMethodModel;
	}

	private String getPathWithPrefix(String applicationPath)
	{
		String pathWithPrefix = null;
		pathWithPrefix = applicationPath != null ? applicationPath : "";
		if (pathWithPrefix != "")
		{
			pathWithPrefix = settings.pathPrefix != null ? pathWithPrefix + "/" + settings.pathPrefix + "/" : pathWithPrefix;
		}
		else
		{
			pathWithPrefix = settings.pathPrefix != null ? settings.pathPrefix + "/" : pathWithPrefix;
		}
		return pathWithPrefix;
	}

	private TsIdentifierReference getConsumes(JaxrsMethodModel method)
	{
		TsIdentifierReference consumes = null;
		if (method.getConsumes() != null)
		{
			for (String consume : method.getConsumes())
			{
				for (String[] entry : this.consumesHeadersMap.keySet())
				{
					if (consume.equals(entry[0]))
					{
						return new TsIdentifierReference(entry[1]);
					}
				}
			}
		}
		return consumes;
	}

	private TsParameterModel processParameter(SymbolTable symbolTable, MethodModel method, MethodParameterModel parameter)
	{
		final TsType parameterType = typeFromJava(symbolTable, parameter.getType(), method.getName(), method.getOriginClass());
		return new TsParameterModel(parameter.getName(), parameterType);
	}

	private static TsTemplateLiteral processPathTemplate(PathTemplate pathTemplate, TsParameterModel queryParameter)
	{
		final List<TsExpression> spans = new ArrayList<>();
		for (PathTemplate.Part part : pathTemplate.getParts())
		{
			if (part instanceof PathTemplate.Literal)
			{
				final PathTemplate.Literal literal = (PathTemplate.Literal) part;
				spans.add(new TsStringLiteral(literal.getLiteral()));
			}
			if (part instanceof PathTemplate.Parameter)
			{
				final PathTemplate.Parameter parameter = (PathTemplate.Parameter) part;
				spans.add(new TsIdentifierReference(parameter.getName()));
			}
		}
		return new TsTemplateLiteral(spans, queryParameter);
	}

	private TsModel transformDates(SymbolTable symbolTable, TsModel tsModel)
	{
		final TsAliasModel dateAsNumber = new TsAliasModel(null, symbolTable.getSyntheticSymbol("DateAsNumber"), null, TsType.Number, null);
		final TsAliasModel dateAsString = new TsAliasModel(null, symbolTable.getSyntheticSymbol("DateAsString"), null, TsType.String, null);
		final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
		final TsModel model = transformBeanPropertyTypes(tsModel, new TsType.Transformer()
		{
			@Override
			public TsType transform(TsType type)
			{
				if (type == TsType.Date)
				{
					if (settings.mapDate == DateMapping.asNumber)
					{
						typeAliases.add(dateAsNumber);
						return new TsType.ReferenceType(dateAsNumber.getName());
					}
					if (settings.mapDate == DateMapping.asString)
					{
						typeAliases.add(dateAsString);
						return new TsType.ReferenceType(dateAsString.getName());
					}
				}
				return type;

			}
		});
		return model.setTypeAliases(new ArrayList<>(typeAliases));
	}

	private TsModel transformEnumsToUnions(TsModel tsModel)
	{
		final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
		for (TsEnumModel<String> enumModel : tsModel.getEnums(EnumKind.StringBased))
		{
			final List<TsType> values = new ArrayList<>();
			for (EnumMemberModel<String> member : enumModel.getMembers())
			{
				values.add(new TsType.StringLiteralType(member.getEnumValue()));
			}
			final TsType union = new TsType.UnionType(values);
			typeAliases.add(new TsAliasModel(enumModel.getOrigin(), enumModel.getName(), null, union, enumModel.getComments()));
		}
		return tsModel.setTypeAliases(new ArrayList<>(typeAliases));
	}

	private TsModel inlineEnums(final TsModel tsModel, final SymbolTable symbolTable)
	{
		final Set<TsAliasModel> inlinedAliases = new LinkedHashSet<>();
		final TsModel newTsModel = transformBeanPropertyTypes(tsModel, new TsType.Transformer()
		{
			@Override
			public TsType transform(TsType tsType)
			{
				if (tsType instanceof TsType.EnumReferenceType)
				{
					final TsAliasModel alias = tsModel.getTypeAlias(getOriginClass(symbolTable, tsType));
					if (alias != null)
					{
						inlinedAliases.add(alias);
						return alias.getDefinition();
					}
				}
				return tsType;
			}
		});
		final ArrayList<TsAliasModel> aliases = new ArrayList<>(tsModel.getTypeAliases());
		aliases.removeAll(inlinedAliases);
		return newTsModel.setTypeAliases(aliases);
	}

	private TsModel createAndUseTaggedUnions(final SymbolTable symbolTable, TsModel tsModel)
	{
		if (settings.disableTaggedUnions)
		{
			return tsModel;
		}
		// create tagged unions
		final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
		for (TsBeanModel bean : tsModel.getBeans())
		{
			if (!bean.getTaggedUnionClasses().isEmpty())
			{
				final Symbol unionName = symbolTable.getSymbol(bean.getOrigin(), "Union");
				final List<TsType> unionTypes = new ArrayList<>();
				for (Class<?> cls : bean.getTaggedUnionClasses())
				{
					final TsType type = new TsType.ReferenceType(symbolTable.getSymbol(cls));
					unionTypes.add(type);
				}
				final TsType.UnionType union = new TsType.UnionType(unionTypes);
				typeAliases.add(new TsAliasModel(bean.getOrigin(), unionName, null, union, null));
			}
		}
		// use tagged unions
		final TsModel model = transformBeanPropertyTypes(tsModel, new TsType.Transformer()
		{
			@Override
			public TsType transform(TsType tsType)
			{
				final Class<?> cls = getOriginClass(symbolTable, tsType);
				if (cls != null && !(tsType instanceof TsType.GenericReferenceType))
				{
					final Symbol unionSymbol = symbolTable.hasSymbol(cls, "Union");
					if (unionSymbol != null)
					{
						return new TsType.ReferenceType(unionSymbol);
					}
				}
				return tsType;
			}
		});
		return model.setTypeAliases(new ArrayList<>(typeAliases));
	}

	private TsModel sortDeclarations(SymbolTable symbolTable, TsModel tsModel)
	{
		final List<TsBeanModel> beans = tsModel.getBeans();
		final List<TsAliasModel> aliases = tsModel.getTypeAliases();
		final List<TsEnumModel<?>> enums = tsModel.getEnums();
		if (settings.sortDeclarations)
		{
			for (TsBeanModel bean : beans)
			{
				Collections.sort(bean.getProperties());
			}
		}
		if (settings.sortDeclarations || settings.sortTypeDeclarations)
		{
			Collections.sort(beans);
			Collections.sort(aliases);
			Collections.sort(enums);
		}
		final LinkedHashSet<TsBeanModel> orderedBeans = new LinkedHashSet<>();
		for (TsBeanModel bean : beans)
		{
			addOrderedClass(symbolTable, tsModel, bean, orderedBeans);
		}
		return tsModel.setBeans(new ArrayList<>(orderedBeans)).setTypeAliases(aliases).setEnums(enums);
	}

	private static void addOrderedClass(SymbolTable symbolTable, TsModel tsModel, TsBeanModel bean, LinkedHashSet<TsBeanModel> orderedBeans)
	{
		// for classes first add their parents to ordered list
		if (bean.isClass() && bean.getParent() != null)
		{
			final TsBeanModel parentBean = tsModel.getBean(getOriginClass(symbolTable, bean.getParent()));
			if (parentBean != null)
			{
				addOrderedClass(symbolTable, tsModel, parentBean, orderedBeans);
			}
		}
		// add current bean to the ordered list
		orderedBeans.add(bean);
	}

	private static TsModel transformBeanPropertyTypes(TsModel tsModel, TsType.Transformer transformer)
	{
		final List<TsBeanModel> newBeans = new ArrayList<>();
		for (TsBeanModel bean : tsModel.getBeans())
		{
			final List<TsPropertyModel> newProperties = new ArrayList<>();
			for (TsPropertyModel property : bean.getProperties())
			{
				final TsType newType = TsType.transformTsType(property.getTsType(), transformer);
				newProperties.add(property.setTsType(newType));
			}
			final List<TsMethodModel> newMethods = new ArrayList<>();
			for (TsMethodModel method : bean.getMethods())
			{
				final List<TsParameterModel> newParameters = new ArrayList<>();
				for (TsParameterModel parameter : method.getParameters())
				{
					final TsType newParameterType = TsType.transformTsType(parameter.getTsType(), transformer);
					newParameters.add(new TsParameterModel(parameter.getAccessibilityModifier(), parameter.getName(), newParameterType));
				}
				final TsType newReturnType = TsType.transformTsType(method.getReturnType(), transformer);
				newMethods.add(new TsMethodModel(method.getName(), newReturnType, newParameters, method.getBody(), method.getComments()));
			}
			newBeans.add(bean.withProperties(newProperties).withMethods(newMethods));
		}
		return tsModel.setBeans(newBeans);
	}

	private static Class<?> getOriginClass(SymbolTable symbolTable, TsType type)
	{
		if (type instanceof TsType.ReferenceType)
		{
			final TsType.ReferenceType referenceType = (TsType.ReferenceType) type;
			return symbolTable.getSymbolClass(referenceType.symbol);
		}
		return null;
	}
}
