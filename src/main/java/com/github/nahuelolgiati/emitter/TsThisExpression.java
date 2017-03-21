
package com.primus.emitter;

import com.primus.Settings;

public class TsThisExpression extends TsExpression
{

	@Override
	public String format(Settings settings)
	{
		return "this";
	}

}
