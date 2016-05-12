package de.jetty.wicket.http2.example;

import org.apache.wicket.Component;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * The object to be pushed. See the urlFor methods of {@link Component} to know what can be used in
 * addition to {@link String}.
 * 
 * @author Tobias Soloschenko
 */
public class PushItem
{
	private Object object;

	private PageParameters pageParameters;

	public PushItem(Object object, PageParameters pageParameters)
	{
		this.object = object;
		this.pageParameters = pageParameters;
	}

	public PushItem(Object object)
	{
		this.object = object;
	}
	
	public PushItem()
	{
	}

	public Object getObject()
	{
		return object;
	}

	public PushItem setObject(Object object)
	{
		this.object = object;
		return this;
	}

	public PageParameters getPageParameters()
	{
		return pageParameters;
	}

	public PushItem setPageParameters(PageParameters pageParameters)
	{
		this.pageParameters = pageParameters;
		return this;
	}

}
