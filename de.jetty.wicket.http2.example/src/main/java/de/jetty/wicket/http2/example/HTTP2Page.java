package de.jetty.wicket.http2.example;

import java.util.Arrays;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

import de.jetty.wicket.http2.example.resources.TestResourceReference;

public class HTTP2Page extends WebPage
{

	private static final long serialVersionUID = 1L;

	public HTTP2Page()
	{
		add(new Label("label", "Hallo"));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		TestResourceReference instance = TestResourceReference.getInstance();
		response.render(CssHeaderItem.forReference(instance));
		response.render(new PushHeaderItem().push(Arrays.asList(new PushItem(instance))));
	}
}
