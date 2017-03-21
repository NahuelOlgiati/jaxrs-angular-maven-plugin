
package com.github.nahuelolgiati.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathTemplate
{

	private final List<Part> parts;

	private PathTemplate(List<Part> parts)
	{
		this.parts = parts;
	}

	public List<Part> getParts()
	{
		return parts;
	}

	public static PathTemplate parse(String path)
	{
		final List<Part> parts = new ArrayList<>();
		final Matcher matcher = Pattern.compile("\\{\\s*(?<ParamName>\\w[\\w\\.-]*)\\s*(:\\s*(?<ParamRegex>[^{}\\s]+)\\s*)?\\}").matcher(path);
		int index = 0;
		while (matcher.find())
		{
			if (matcher.start() > index)
			{
				parts.add(new Literal(path.substring(index, matcher.start())));
			}
			parts.add(new Parameter(matcher.group("ParamName"), matcher.group("ParamRegex")));
			index = matcher.end();
		}
		if (index < path.length())
		{
			parts.add(new Literal(path.substring(index, path.length())));
		}
		return new PathTemplate(parts);
	}

	public String format(String parameterLeftDelimiter, String parameterRightDelimiter, boolean includeParameterRegex)
	{
		final StringBuilder sb = new StringBuilder();
		for (Part part : parts)
		{
			if (part instanceof Literal)
			{
				final Literal literal = (Literal) part;
				sb.append(literal.literal);
			}
			if (part instanceof Parameter)
			{
				final Parameter parameter = (Parameter) part;
				sb.append(parameterLeftDelimiter);
				sb.append(parameter.name);
				if (includeParameterRegex && parameter.regex != null)
				{
					sb.append(":");
					sb.append(parameter.regex);
				}
				sb.append(parameterRightDelimiter);
			}
		}
		return sb.toString();
	}

	public static abstract class Part
	{
	}

	public static class Literal extends Part
	{
		private final String literal;

		public Literal(String literal)
		{
			this.literal = literal;
		}

		public String getLiteral()
		{
			return literal;
		}
	}

	public static class Parameter extends Part
	{
		private final String name;
		private final String regex;

		public Parameter(String name, String regex)
		{
			this.name = name;
			this.regex = regex;
		}

		public String getName()
		{
			return name;
		}

		public String getRegex()
		{
			return regex;
		}
	}

}
