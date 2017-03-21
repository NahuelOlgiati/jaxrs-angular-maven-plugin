
package com.github.nahuelolgiati.parser;

import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.List;

public class PropertyModel
{

	private final String name;
	private final Type type;
	private final boolean optional;
	private final Member originalMember;
	private final PullProperties pullProperties;
	private final List<String> comments;

	public static class PullProperties
	{
		public final String prefix;
		public final String suffix;

		public PullProperties(String prefix, String suffix)
		{
			this.prefix = prefix;
			this.suffix = suffix;
		}
	}

	public PropertyModel(String name, Type type, boolean optional, Member originalMember, PullProperties pullProperties, List<String> comments)
	{
		this.name = name;
		this.type = type;
		this.optional = optional;
		this.originalMember = originalMember;
		this.pullProperties = pullProperties;
		this.comments = comments;
	}

	public String getName()
	{
		return name;
	}

	public Type getType()
	{
		return type;
	}

	public boolean isOptional()
	{
		return optional;
	}

	public Member getOriginalMember()
	{
		return originalMember;
	}

	public PropertyModel originalMember(Member originalMember)
	{
		return new PropertyModel(name, type, optional, originalMember, pullProperties, comments);
	}

	public PullProperties getPullProperties()
	{
		return pullProperties;
	}

	public List<String> getComments()
	{
		return comments;
	}

	public PropertyModel withComments(List<String> comments)
	{
		return new PropertyModel(name, type, optional, originalMember, pullProperties, comments);
	}

	@Override
	public String toString()
	{
		return "PropertyModel{" + "name=" + name + ", type=" + type + "}";
	}

}
