package de.jetty.wicket.http2.example;

import java.util.Arrays;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.http.WebResponse;

import de.jetty.wicket.http2.example.resources.TestResourceReference;

public class HTTP2Page extends WebPage
{
	private static final long serialVersionUID = 1L;

	private transient Response webPageResponse;

	private transient Request webPageRequest;

	public HTTP2Page()
	{
		webPageResponse = getRequestCycle().getResponse();
		webPageRequest = getRequestCycle().getRequest();
		add(new Label("label", "Hallo"));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		TestResourceReference instance = TestResourceReference.getInstance();
		response.render(CssHeaderItem.forReference(instance));
		response.render(new PushHeaderItem(this, webPageResponse, webPageRequest)
		    .push(Arrays.asList(new PushItem(instance))));
	}

	@Override
	protected void setHeaders(WebResponse response)
	{
		// NOOP just disable caching
	}
}
