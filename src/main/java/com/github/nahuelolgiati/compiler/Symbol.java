
package com.github.nahuelolgiati.compiler;

public class Symbol
{

	protected String name;

	public Symbol(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}

}
