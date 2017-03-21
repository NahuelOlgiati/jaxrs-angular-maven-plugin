
package com.github.nahuelolgiati.emitter;

import java.util.List;
import com.github.nahuelolgiati.compiler.EnumKind;
import com.github.nahuelolgiati.compiler.EnumMemberModel;
import com.github.nahuelolgiati.compiler.Symbol;
import com.github.nahuelolgiati.parser.EnumModel;

// T extends String | Number
public class TsEnumModel<T> extends TsDeclarationModel
{

	private final EnumKind<T> kind;
	private final List<EnumMemberModel<T>> members;

	public TsEnumModel(Class<?> origin, Symbol name, EnumKind<T> kind, List<EnumMemberModel<T>> members, List<String> comments)
	{
		super(origin, name, comments);
		this.kind = kind;
		this.members = members;
	}

	public static <T> TsEnumModel<T> fromEnumModel(Symbol name, EnumModel<T> enumModel)
	{
		return new TsEnumModel<>(enumModel.getOrigin(), name, enumModel.getKind(), enumModel.getMembers(), enumModel.getComments());
	}

	public EnumKind<T> getKind()
	{
		return kind;
	}

	public List<EnumMemberModel<T>> getMembers()
	{
		return members;
	}

}
