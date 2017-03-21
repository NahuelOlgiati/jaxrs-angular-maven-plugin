
package com.primus.emitter;

import com.primus.Settings;

public class TsStringLiteral extends TsExpression
{

	private final String literal;

	public TsStringLiteral(String literal)
	{
		this.literal = literal;
	}

	public String getLiteral()
	{
		return literal;
	}

	@Override
	public String format(Settings settings)
	{
		return Emitter.quote(literal, settings);
	}

}
