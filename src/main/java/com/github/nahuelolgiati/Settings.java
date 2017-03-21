
package com.github.nahuelolgiati;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.github.nahuelolgiati.emitter.Emitter;
import com.github.nahuelolgiati.util.Predicate;

public class Settings
{
	public String newline = String.format("%n");
	public String quotes = "'";
	public String indentString = "    ";
	public String module = null;
	public String pathPrefix = null;
	public String namespace = null;
	public String umdNamespace = null;
	private Predicate<String> excludeFilter = null;
	public boolean declarePropertiesAsOptional = false;
	public boolean declarePropertiesAsReadOnly = false;
	public String removeTypeNamePrefix = null;
	public String removeTypeNameSuffix = null;
	public String addTypeNamePrefix = null;
	public String addTypeNameSuffix = null;
	public String serviceNameReplacement = null;
	public Map<String, String> customTypeNaming = new LinkedHashMap<>();
	public String customTypeNamingFunction = null;
	public List<String> referencedFiles = new ArrayList<>();
	public List<String> importDeclarations = new ArrayList<>();
	public Map<String, String> customTypeMappings = new LinkedHashMap<>();
	public DateMapping mapDate; // default is DateMapping.asDate
	public EnumMapping mapEnum; // default is EnumMapping.asUnion
	public ClassMapping mapClasses; // default is ClassMapping.asInterfaces
	public boolean disableTaggedUnions = false;
	public TypeProcessor customTypeProcessor = null;
	public boolean sortDeclarations = false;
	public boolean sortTypeDeclarations = false;
	public List<Class<? extends Annotation>> includePropertyAnnotations = new ArrayList<>();
	public List<Class<? extends Annotation>> optionalAnnotations = new ArrayList<>();
	public boolean displaySerializerWarning = true;
	public boolean disableJackson2ModuleDiscovery = false;
	public ClassLoader classLoader = null;

	public void loadCustomTypeProcessor(ClassLoader classLoader, String customTypeProcessor)
	{
		if (customTypeProcessor != null)
		{
			this.customTypeProcessor = loadInstance(classLoader, customTypeProcessor, TypeProcessor.class);
		}
	}

	public void loadIncludePropertyAnnotations(ClassLoader classLoader, List<String> includePropertyAnnotations)
	{
		if (includePropertyAnnotations != null)
		{
			this.includePropertyAnnotations = loadClasses(classLoader, includePropertyAnnotations, Annotation.class);
		}
	}

	public void loadOptionalAnnotations(ClassLoader classLoader, List<String> optionalAnnotations)
	{
		if (optionalAnnotations != null)
		{
			this.optionalAnnotations = loadClasses(classLoader, optionalAnnotations, Annotation.class);
		}
	}

	public static Map<String, String> convertToMap(List<String> mappings)
	{
		final Map<String, String> result = new LinkedHashMap<>();
		if (mappings != null)
		{
			for (String mapping : mappings)
			{
				final String[] values = mapping.split(":", 2);
				if (values.length < 2)
				{
					throw new RuntimeException("Invalid mapping format: " + mapping);
				}
				result.put(values[0].trim(), values[1].trim());
			}
		}
		return result;
	}

	public void validate()
	{
		if (module == null)
		{
			throw new RuntimeException("module is a required parameter. ");
		}
		if (umdNamespace != null && !Emitter.isValidIdentifierName(umdNamespace))
		{
			throw new RuntimeException("Value of 'umdNamespace' parameter is not valid identifier: " + umdNamespace + ". ");
		}
	}

	public String getDefaultNpmVersion()
	{
		return "1.0.0";
	}

	public Predicate<String> getExcludeFilter()
	{
		if (excludeFilter == null)
		{
			setExcludeFilter(null, null);
		}
		return excludeFilter;
	}

	public void setExcludeFilter(List<String> excludedClasses, List<String> excludedClassPatterns)
	{
		this.excludeFilter = createExcludeFilter(excludedClasses, excludedClassPatterns);
	}

	public static Predicate<String> createExcludeFilter(List<String> excludedClasses, List<String> excludedClassPatterns)
	{
		final Set<String> names = new LinkedHashSet<>(excludedClasses != null ? excludedClasses : Collections.<String> emptyList());
		final List<Pattern> patterns = Input.globsToRegexps(excludedClassPatterns != null ? excludedClassPatterns : Collections.<String> emptyList());
		return new Predicate<String>()
		{
			@Override
			public boolean test(String className)
			{
				return names.contains(className) || Input.classNameMatches(className, patterns);
			}
		};
	}

	private static <T> List<Class<? extends T>> loadClasses(ClassLoader classLoader, List<String> classNames, Class<T> requiredClassType)
	{
		if (classNames == null)
		{
			return null;
		}
		try
		{
			final List<Class<? extends T>> classes = new ArrayList<>();
			for (String className : classNames)
			{
				System.out.println("Loading class " + className);
				final Class<?> loadedClass = classLoader.loadClass(className);
				if (requiredClassType.isAssignableFrom(loadedClass))
				{
					@SuppressWarnings("unchecked")
					final Class<? extends T> castedClass = (Class<? extends T>) loadedClass;
					classes.add(castedClass);
				}
				else
				{
					throw new RuntimeException(String.format("Class '%s' is not assignable to '%s'.", loadedClass, requiredClassType));
				}
			}
			return classes;
		}
		catch (ReflectiveOperationException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static <T> T loadInstance(ClassLoader classLoader, String className, Class<T> requiredType)
	{
		try
		{
			System.out.println("Loading class " + className);
			return requiredType.cast(classLoader.loadClass(className).newInstance());
		}
		catch (ReflectiveOperationException e)
		{
			throw new RuntimeException(e);
		}
	}

}
