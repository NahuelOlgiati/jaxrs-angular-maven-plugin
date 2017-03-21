
package com.primus.emitter;

import java.util.*;
import com.primus.TsType;
import com.primus.compiler.Symbol;

public class TsAliasModel extends TsDeclarationModel
{

	private final List<TsType.GenericVariableType> typeParameters;
	private final TsType definition;

	public TsAliasModel(Class<?> origin, Symbol name, List<TsType.GenericVariableType> typeParameters, TsType definition, List<String> comments)
	{
		super(origin, name, comments);
		this.typeParameters = typeParameters != null ? typeParameters : Collections.<TsType.GenericVariableType> emptyList();
		this.definition = definition;
	}

	public List<TsType.GenericVariableType> getTypeParameters()
	{
		return typeParameters;
	}

	public TsType getDefinition()
	{
		return definition;
	}

}
