
package com.github.nahuelolgiati.emitter;

public enum TsAccessibilityModifier
{

	Public,
	Private,
	Protected;

	public String format()
	{
		return name().toLowerCase();
	}

}
