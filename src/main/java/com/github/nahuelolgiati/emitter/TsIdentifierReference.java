
package com.primus.emitter;

import com.primus.Settings;

public class TsIdentifierReference extends TsExpression
{

	private final String identifier;

	public TsIdentifierReference(String identifier)
	{
		this.identifier = identifier;
	}

	public String getIdentifier()
	{
		return identifier;
	}

	@Override
	public String format(Settings settings)
	{
		return identifier;
	}

}
