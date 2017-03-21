
package com.github.nahuelolgiati;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import com.github.nahuelolgiati.compiler.ModelCompiler;
import com.github.nahuelolgiati.emitter.Emitter;
import com.github.nahuelolgiati.emitter.TsModel;
import com.github.nahuelolgiati.parser.JaxbParser;
import com.github.nahuelolgiati.parser.Model;
import com.github.nahuelolgiati.parser.ModelParser;

public class TypeScriptGenerator
{

	public static final String Version = getVersion();

	private final Settings settings;
	private TypeProcessor typeProcessor = null;
	private ModelParser modelParser = null;
	private ModelCompiler modelCompiler = null;
	private Emitter emitter = null;

	public TypeScriptGenerator()
	{
		this(new Settings());
	}

	public TypeScriptGenerator(Settings settings)
	{
		this.settings = settings;
		settings.validate();
	}

	public static void printVersion()
	{
		System.out.println("Running jaxrs-angular-maven-plugin version " + Version);
	}

	public String generateTypeScript(Input input)
	{
		final StringWriter stringWriter = new StringWriter();
		generateTypeScript(input, Output.to(stringWriter));
		return stringWriter.toString();
	}

	public void generateTypeScript(Input input, Output output)
	{
		generateTypeScript(input, output, false, 0);
	}

	public void generateEmbeddableTypeScript(Input input, Output output, boolean addExportKeyword, int initialIndentationLevel)
	{
		generateTypeScript(input, output, addExportKeyword, initialIndentationLevel);
	}

	private void generateTypeScript(Input input, Output output, boolean forceExportKeyword, int initialIndentationLevel)
	{
		final Model model = getModelParser().parseModel(input.getSourceTypes());
		final TsModel tsModel = getModelCompiler().javaToTypeScript(model);
		getEmitter().emit(tsModel, output.getWriter(), output.getName(), output.shouldCloseWriter(), forceExportKeyword, initialIndentationLevel);
	}

	public TypeProcessor getTypeProcessor()
	{
		if (typeProcessor == null)
		{
			final List<TypeProcessor> processors = new ArrayList<>();
			processors.add(new ExcludingTypeProcessor(settings.getExcludeFilter()));
			if (settings.customTypeProcessor != null)
			{
				processors.add(settings.customTypeProcessor);
			}
			processors.add(new CustomMappingTypeProcessor(settings.customTypeMappings));
			processors.add(new DefaultTypeProcessor());
			typeProcessor = new TypeProcessor.Chain(processors);
		}
		return typeProcessor;
	}

	public ModelParser getModelParser()
	{
		if (modelParser == null)
		{
			modelParser = new JaxbParser(settings, getTypeProcessor());
		}
		return modelParser;
	}

	public ModelCompiler getModelCompiler()
	{
		if (modelCompiler == null)
		{
			modelCompiler = new ModelCompiler(settings, getTypeProcessor());
		}
		return modelCompiler;
	}

	public Emitter getEmitter()
	{
		if (emitter == null)
		{
			emitter = new Emitter(settings);
		}
		return emitter;
	}

	private static String getVersion()
	{
		try
		{
			final InputStream inputStream = TypeScriptGenerator.class
					.getResourceAsStream("/META-INF/maven/com.primus/jaxrs-angular-maven-plugin/pom.properties");
			if (inputStream != null)
			{
				final Properties properties = new Properties();
				properties.load(inputStream);
				return (String) properties.get("version");
			}
			return null;
		}
		catch (Exception e)
		{
			return null;
		}
	}

}
