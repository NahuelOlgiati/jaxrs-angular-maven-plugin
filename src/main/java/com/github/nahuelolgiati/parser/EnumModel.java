
package com.github.nahuelolgiati.parser;

import java.util.*;
import com.github.nahuelolgiati.compiler.EnumKind;
import com.github.nahuelolgiati.compiler.EnumMemberModel;

// T extends String | Number
public class EnumModel<T> extends DeclarationModel
{

	private final EnumKind<T> kind;
	private final List<EnumMemberModel<T>> members;

	public EnumModel(Class<?> origin, EnumKind<T> kind, List<EnumMemberModel<T>> members, List<String> comments)
	{
		super(origin, comments);
		this.kind = kind;
		this.members = members;
	}

	public EnumKind<T> getKind()
	{
		return kind;
	}

	public List<EnumMemberModel<T>> getMembers()
	{
		return members;
	}

	public EnumModel<T> withMembers(List<EnumMemberModel<T>> members)
	{
		return new EnumModel<>(origin, kind, members, comments);
	}

	@Override
	public EnumModel<T> withComments(List<String> comments)
	{
		return new EnumModel<>(origin, kind, members, comments);
	}

}
