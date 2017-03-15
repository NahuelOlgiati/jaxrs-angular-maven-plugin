
package com.primus.emitter;

import java.util.List;
import com.primus.TsType;

public class TsMethodModel extends TsCallableModel
{

	public TsMethodModel(String name, TsType returnType, List<TsParameterModel> parameters, List<TsStatement> body, List<String> comments)
	{
		super(name, returnType, parameters, body, comments);
	}

}
