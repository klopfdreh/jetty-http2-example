package de.jetty.wicket.http2.example;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class HTTP2Application extends WebApplication
{
	@Override
	public Class<? extends Page> getHomePage()
	{
		return HTTP2Page.class;
	}
}
