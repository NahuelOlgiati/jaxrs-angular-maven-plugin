
package com.primus;

import java.lang.reflect.Type;
import java.util.*;
import com.primus.util.Utils;

public class CustomMappingTypeProcessor implements TypeProcessor
{

	private final Map<String, String> customMappings;

	public CustomMappingTypeProcessor(Map<String, String> customMappings)
	{
		this.customMappings = customMappings;
	}

	@Override
	public Result processType(Type javaType, Context context)
	{
		final Class<?> rawClass = Utils.getRawClassOrNull(javaType);
		if (rawClass != null)
		{
			final String tsTypeName = customMappings.get(rawClass.getName());
			if (tsTypeName != null)
			{
				return new Result(new TsType.BasicType(tsTypeName));
			}
		}
		return null;
	}

}
