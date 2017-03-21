
package com.github.nahuelolgiati;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import com.github.nahuelolgiati.parser.SourceType;
import com.github.nahuelolgiati.util.Predicate;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;

public class JaxrsApplicationScanner
{

	public static List<SourceType<Type>> scanJaxrsApplication(Class<?> jaxrsApplicationClass, Predicate<String> isClassNameExcluded)
	{
		try
		{
			System.out.println("Scanning JAX-RS application: " + jaxrsApplicationClass.getName());
			final Constructor<?> constructor = jaxrsApplicationClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			final Application application = (Application) constructor.newInstance();
			final List<Class<?>> resourceClasses = new ArrayList<>();
			for (Class<?> cls : application.getClasses())
			{
				if (cls.isAnnotationPresent(Path.class))
				{
					resourceClasses.add(cls);
				}
			}
			return new JaxrsApplicationScanner().scanJaxrsApplication(jaxrsApplicationClass, resourceClasses, isClassNameExcluded);
		}
		catch (ReflectiveOperationException e)
		{
			throw reportError(e);
		}
	}

	public static List<SourceType<Type>> scanAutomaticJaxrsApplication(ScanResult scanner, Predicate<String> isClassNameExcluded)
	{
		try
		{
			final List<String> namesOfResourceClasses = scanner.getNamesOfClassesWithAnnotation(Path.class);
			final List<Class<?>> resourceClasses = new ArrayList<>();
			for (String className : namesOfResourceClasses)
			{
				resourceClasses.add(Thread.currentThread().getContextClassLoader().loadClass(className));
			}
			System.out.println(String.format("Found %d root resources.", resourceClasses.size()));
			return new JaxrsApplicationScanner().scanJaxrsApplication(null, resourceClasses, isClassNameExcluded);
		}
		catch (ReflectiveOperationException e)
		{
			throw reportError(e);
		}
	}

	private static RuntimeException reportError(ReflectiveOperationException e)
	{
		final String message = "Cannot load JAX-RS application.";
		System.out.println(message);
		return new RuntimeException(message, e);
	}

	List<SourceType<Type>> scanJaxrsApplication(Class<?> applicationClass, List<Class<?>> resourceClasses, Predicate<String> isClassNameExcluded)
	{
		Collections.sort(resourceClasses, new Comparator<Class<?>>()
		{
			@Override
			public int compare(Class<?> o1, Class<?> o2)
			{
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		final List<SourceType<Type>> sourceTypes = new ArrayList<>();
		if (applicationClass != null)
		{
			sourceTypes.add(new SourceType<Type>(applicationClass));
		}
		for (Class<?> resourceClass : resourceClasses)
		{
			if (isClassNameExcluded == null || !isClassNameExcluded.test(resourceClass.getName()))
			{
				sourceTypes.add(new SourceType<Type>(resourceClass));
			}
		}
		return sourceTypes;
	}

}
