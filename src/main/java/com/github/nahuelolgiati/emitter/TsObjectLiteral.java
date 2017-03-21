
package com.primus.emitter;

import java.util.*;
import com.primus.Settings;
import com.primus.util.Utils;

public class TsObjectLiteral extends TsExpression
{

	private final List<TsPropertyDefinition> propertyDefinitions;

	public TsObjectLiteral(TsPropertyDefinition... propertyDefinitions)
	{
		this(Utils.removeNulls(Arrays.asList(propertyDefinitions)));
	}

	public TsObjectLiteral(List<TsPropertyDefinition> propertyDefinitions)
	{
		this.propertyDefinitions = propertyDefinitions;
	}

	public List<TsPropertyDefinition> getPropertyDefinitions()
	{
		return propertyDefinitions;
	}

	@Override
	public String format(Settings settings)
	{
		final List<String> props = new ArrayList<>();
		for (TsPropertyDefinition property : propertyDefinitions)
		{
			props.add(property.format(settings));
		}
		if (props.isEmpty())
		{
			return "{}";
		}
		else
		{
			return "{ " + Utils.join(props, ", ") + " }";
		}

	}

}
