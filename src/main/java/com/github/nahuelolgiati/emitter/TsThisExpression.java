
package com.github.nahuelolgiati.emitter;

import com.github.nahuelolgiati.Settings;

public class TsThisExpression extends TsExpression
{

	@Override
	public String format(Settings settings)
	{
		return "this";
	}

}
