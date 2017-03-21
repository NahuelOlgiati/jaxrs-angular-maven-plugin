
package com.github.nahuelolgiati.emitter;

import com.github.nahuelolgiati.Settings;

public class TsMemberExpression extends TsExpression
{

	private final TsExpression expression;
	private final String identifierName;

	public TsMemberExpression(TsExpression expression, String identifierName)
	{
		this.expression = expression;
		this.identifierName = identifierName;
	}

	public TsExpression getExpression()
	{
		return expression;
	}

	public String getIdentifierName()
	{
		return identifierName;
	}

	@Override
	public String format(Settings settings)
	{
		if (Emitter.isValidIdentifierName(identifierName))
		{
			return expression.format(settings) + "." + identifierName;
		}
		else
		{
			return expression.format(settings) + "[" + Emitter.quote(identifierName, settings) + "]";
		}
	}

}
