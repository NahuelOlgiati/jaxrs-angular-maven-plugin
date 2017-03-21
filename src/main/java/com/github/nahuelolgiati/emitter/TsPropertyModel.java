
package com.github.nahuelolgiati.emitter;

import java.util.List;
import com.github.nahuelolgiati.TsProperty;
import com.github.nahuelolgiati.TsType;

public class TsPropertyModel extends TsProperty implements Comparable<TsProperty>
{

	public final boolean readonly;
	public final List<String> comments;

	public TsPropertyModel(String name, TsType tsType, boolean readonly, List<String> comments)
	{
		super(name, tsType);
		this.readonly = readonly;
		this.comments = comments;
	}

	public List<String> getComments()
	{
		return comments;
	}

	public TsPropertyModel setTsType(TsType type)
	{
		return new TsPropertyModel(getName(), type, readonly, getComments());
	}

	@Override
	public int compareTo(TsProperty o)
	{
		return name.compareTo(o.getName());
	}

	@Override
	public String toString()
	{
		return "TsPropertyModel{" + "name=" + name + ", tsType=" + tsType + '}';
	}

}
