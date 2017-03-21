
package com.github.nahuelolgiati.emitter;

import java.util.List;
import com.github.nahuelolgiati.TsType;

public class TsMethodModel extends TsCallableModel
{

	public TsMethodModel(String name, TsType returnType, List<TsParameterModel> parameters, List<TsStatement> body, List<String> comments)
	{
		super(name, returnType, parameters, body, comments);
	}

}
