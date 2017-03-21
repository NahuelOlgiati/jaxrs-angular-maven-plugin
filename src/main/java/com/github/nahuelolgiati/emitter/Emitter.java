
package com.github.nahuelolgiati.emitter;

import java.io.*;
import java.util.*;
import com.github.nahuelolgiati.*;
import com.github.nahuelolgiati.compiler.EnumKind;
import com.github.nahuelolgiati.compiler.EnumMemberModel;
import com.github.nahuelolgiati.util.Utils;

public class Emitter
{

	private final Settings settings;
	private Writer writer;
	private boolean forceExportKeyword;
	private int indent;

	public Emitter(Settings settings)
	{
		this.settings = settings;
	}

	public void emit(TsModel model, Writer output, String outputName, boolean closeOutput, boolean forceExportKeyword, int initialIndentationLevel)
	{
		this.writer = output;
		this.forceExportKeyword = forceExportKeyword;
		this.indent = initialIndentationLevel;
		if (outputName != null)
		{
			System.out.println("Writing declarations to: " + outputName);
		}
		emitReferences();
		emitImports();
		emitNamespace(model);
		emitUmdNamespace();
		if (closeOutput)
		{
			close();
		}
	}

	private void emitReferences()
	{
		if (settings.referencedFiles != null && !settings.referencedFiles.isEmpty())
		{
			writeNewLine();
			for (String reference : settings.referencedFiles)
			{
				writeIndentedLine("/// <reference path=" + quote(reference, settings) + " />");
			}
		}
	}

	private void emitImports()
	{
		if (settings.importDeclarations != null && !settings.importDeclarations.isEmpty())
		{
			writeNewLine();
			for (String importDeclaration : settings.importDeclarations)
			{
				writeIndentedLine(importDeclaration + ";");
			}
		}
	}

	private void emitNamespace(TsModel model)
	{
		if (settings.namespace != null)
		{
			writeNewLine();
			String prefix = "export ";
			writeIndentedLine(prefix + "namespace " + settings.namespace + " {");
			indent++;
			emitElements(model, true, false);
			indent--;
			writeNewLine();
			writeIndentedLine("}");
		}
		else
		{
			emitElements(model, true, false);
		}
	}

	private void emitElements(TsModel model, boolean exportKeyword, boolean declareKeyword)
	{
		exportKeyword = exportKeyword || forceExportKeyword;
		emitBeans(model, exportKeyword);
		emitTypeAliases(model, exportKeyword);
		emitNumberEnums(model, exportKeyword, declareKeyword);
	}

	private void emitBeans(TsModel model, boolean exportKeyword)
	{
		for (TsBeanModel bean : model.getBeans())
		{
			writeNewLine();
			emitAnnotations(bean.getAnnotations());
			emitComments(bean.getComments());
			final String declarationType = bean.isClass() ? "class" : "interface";
			final String typeParameters = bean.getTypeParameters().isEmpty() ? "" : "<" + Utils.join(bean.getTypeParameters(), ", ") + ">";
			final List<TsType> extendsList = bean.getExtendsList();
			final List<TsType> implementsList = bean.getImplementsList();
			final String extendsClause = extendsList.isEmpty() ? "" : " extends " + Utils.join(extendsList, ", ");
			final String implementsClause = implementsList.isEmpty() ? "" : " implements " + Utils.join(implementsList, ", ");
			writeIndentedLine(exportKeyword, declarationType + " " + bean.getName() + typeParameters + extendsClause + implementsClause + " {");
			indent++;
			for (TsPropertyModel property : bean.getProperties())
			{
				emitProperty(property);
			}
			if (bean.getConstructor() != null)
			{
				emitCallable(bean.getConstructor());
			}
			for (TsMethodModel method : bean.getMethods())
			{
				emitCallable(method);
			}
			indent--;
			writeIndentedLine("}");
		}
	}

	private void emitProperty(TsPropertyModel property)
	{
		emitComments(property.getComments());
		final TsType tsType = property.getTsType();
		final String readonly = property.readonly ? "readonly " : "";
		final String questionMark = settings.declarePropertiesAsOptional || (tsType instanceof TsType.OptionalType) ? "?" : "";
		writeIndentedLine(readonly + quoteIfNeeded(property.getName(), settings) + questionMark + ": " + tsType.format(settings) + ";");
	}

	public static String quoteIfNeeded(String name, Settings settings)
	{
		return isValidIdentifierName(name) ? name : quote(name, settings);
	}

	public static String quote(String value, Settings settings)
	{
		return settings.quotes + value + settings.quotes;
	}

	// https://github.com/Microsoft/TypeScript/blob/master/doc/spec.md#2.2.2
	// http://www.ecma-international.org/ecma-262/6.0/index.html#sec-names-and-keywords
	public static boolean isValidIdentifierName(String name)
	{
		if (name == null || name.isEmpty())
		{
			return false;
		}
		final char start = name.charAt(0);
		if (!Character.isUnicodeIdentifierStart(start) && start != '$' && start != '_')
		{
			return false;
		}
		for (char c : name.substring(1).toCharArray())
		{
			if (!Character.isUnicodeIdentifierPart(c) && c != '$' && c != '_' && c != '\u200C' && c != '\u200D')
			{
				return false;
			}
		}
		return true;
	}

	private void emitCallable(TsCallableModel method)
	{
		writeNewLine();
		emitComments(method.getComments());
		final List<String> parameters = new ArrayList<>();
		for (TsParameterModel parameter : method.getParameters())
		{
			final String access = parameter.getAccessibilityModifier() != null ? parameter.getAccessibilityModifier().format() + " " : "";
			final String questionMark = (parameter.getTsType() instanceof TsType.OptionalType) ? "?" : "";
			final String type = parameter.getTsType() != null ? ": " + parameter.getTsType() : "";
			parameters.add(access + parameter.getName() + questionMark + type);
		}
		final String type = method.getReturnType() != null ? ": " + method.getReturnType() : "";
		final String signature = method.getName() + "(" + Utils.join(parameters, ", ") + ")" + type;
		if (method.getBody() != null)
		{
			writeIndentedLine(signature + " {");
			indent++;
			emitStatements(method.getBody());
			indent--;
			writeIndentedLine("}");
		}
		else
		{
			writeIndentedLine(signature + ";");
		}
	}

	private void emitStatements(List<TsStatement> statements)
	{
		for (TsStatement statement : statements)
		{
			if (statement instanceof TsReturnStatement)
			{
				final TsReturnStatement returnStatement = (TsReturnStatement) statement;
				if (returnStatement.getExpression() != null)
				{
					writeIndentedLine("return " + returnStatement.getExpression().format(settings) + ";");
				}
				else
				{
					writeIndentedLine("return;");
				}
			}
		}
	}

	private void emitTypeAliases(TsModel model, boolean exportKeyword)
	{
		for (TsAliasModel alias : model.getTypeAliases())
		{
			writeNewLine();
			emitComments(alias.getComments());
			final String genericParameters = alias.getTypeParameters().isEmpty() ? "" : "<" + Utils.join(alias.getTypeParameters(), ", ") + ">";
			writeIndentedLine(exportKeyword, "type " + alias.getName() + genericParameters + " = " + alias.getDefinition().format(settings) + ";");
		}
	}

	private void emitNumberEnums(TsModel model, boolean exportKeyword, boolean declareKeyword)
	{
		final ArrayList<TsEnumModel<?>> enums = settings.mapEnum == EnumMapping.asNumberBasedEnum ? new ArrayList<>(model.getEnums())
				: new ArrayList<TsEnumModel<?>>(model.getEnums(EnumKind.NumberBased));
		for (TsEnumModel<?> enumModel : enums)
		{
			writeNewLine();
			emitComments(enumModel.getComments());
			writeIndentedLine(exportKeyword, (declareKeyword ? "declare " : "") + "const enum " + enumModel.getName() + " {");
			indent++;
			for (EnumMemberModel<?> member : enumModel.getMembers())
			{
				emitComments(member.getComments());
				final String initializer = enumModel.getKind() == EnumKind.NumberBased ? " = " + member.getEnumValue() : "";
				writeIndentedLine(member.getPropertyName() + initializer + ",");
			}
			indent--;
			writeIndentedLine("}");
		}
	}

	private void emitUmdNamespace()
	{
		if (settings.umdNamespace != null)
		{
			writeNewLine();
			writeIndentedLine("export as namespace " + settings.umdNamespace + ";");
		}
	}

	private void emitAnnotations(List<String> annotations)
	{
		if (annotations != null)
		{
			for (String annotation : annotations)
			{
				writeIndentedLine(annotation);
			}
		}
	}

	private void emitComments(List<String> comments)
	{
		if (comments != null)
		{
			writeIndentedLine("/**");
			for (String comment : comments)
			{
				writeIndentedLine(" * " + comment);
			}
			writeIndentedLine(" */");
		}
	}

	private void writeIndentedLine(boolean exportKeyword, String line)
	{
		writeIndentedLine((exportKeyword ? "export " : "") + line);
	}

	private void writeIndentedLine(String line)
	{
		try
		{
			if (!line.isEmpty())
			{
				for (int i = 0; i < indent; i++)
				{
					writer.write(settings.indentString);
				}
			}
			writer.write(line);
			writeNewLine();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void writeNewLine()
	{
		try
		{
			writer.write(settings.newline);
			writer.flush();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void close()
	{
		try
		{
			writer.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

}
