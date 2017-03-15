
package com.primus;

import java.lang.reflect.Type;
import com.primus.util.Predicate;
import com.primus.util.Utils;

public class ExcludingTypeProcessor implements TypeProcessor
{

	private final Predicate<String> excludeFilter;

	public ExcludingTypeProcessor(Predicate<String> excludeFilter)
	{
		this.excludeFilter = excludeFilter;
	}

	@Override
	public Result processType(Type javaType, Context context)
	{
		final Class<?> rawClass = Utils.getRawClassOrNull(javaType);
		if (rawClass != null && excludeFilter.test(rawClass.getName()))
		{
			return new Result(TsType.Any);
		}
		return null;
	}

}
