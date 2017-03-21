
package com.github.nahuelolgiati.emitter;

import java.util.Iterator;
import java.util.List;
import com.github.nahuelolgiati.Settings;

public class TsTemplateLiteral extends TsExpression
{

	private final List<TsExpression> spans;
	private final TsParameterModel queryParameter;

	public TsTemplateLiteral(List<TsExpression> spans, TsParameterModel queryParameter)
	{
		this.spans = spans;
		this.queryParameter = queryParameter;
	}

	public List<TsExpression> getSpans()
	{
		return spans;
	}

	public TsParameterModel getQueryParameter()
	{
		return queryParameter;
	}

	@Override
	public String format(Settings settings)
	{
		final StringBuilder sb = new StringBuilder();
		for (Iterator<TsExpression> iterator = this.spans.iterator(); iterator.hasNext();)
		{
			final TsExpression span = iterator.next();
			if (span instanceof TsStringLiteral)
			{
				sb.append(span.format(settings));
			}
			else
			{
				sb.append(" + " + span.format(settings));
				if (iterator.hasNext())
				{
					sb.append(" + ");
				}
			}
		}
		if (this.queryParameter != null)
		{
			sb.append(new TsStringLiteral(" + '?' + jQuery.param(queryParams)").getLiteral());
		}
		return sb.toString();
	}

}
