package de.jetty.wicket.http2.example;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.collections.ConcurrentHashSet;
import org.eclipse.jetty.server.Request;

/**
 * A push header item to be used in the http/2 context and to reduce the latency of the web
 * application
 * 
 * @author Tobias Soloschenko
 *
 */
public class PushHeaderItem extends HeaderItem
{
	private static final long serialVersionUID = 1L;

	private Set<String> urls = new ConcurrentHashSet<String>(new TreeSet<String>());

	/**
	 * Uses the URLs that has already been pushed to the client to ensure not to push them again
	 */
	@Override
	public Iterable<?> getRenderTokens()
	{
		return urls;
	}

	/**
	 * Pushes the previously created URLs to the client
	 */
	@Override
	public void render(Response response)
	{
		for (String url : urls)
		{
			// TODO This has to be replaced with the final javax.servlet.api interface (HttpServletRequest)
			((Request)RequestCycle.get().getRequest().getContainerRequest()).getPushBuilder()
				.path(url.toString()).push();
		}
	}

	/**
	 * Creates a URL and pushes the resource to the client - this is only supported if http2 is
	 * enabled
	 * 
	 * @param object
	 *            the object to create the URL for - if it is a String this is taken as a URL
	 *            directly
	 * @param parameters
	 *            the parameters to be applied to create the URL
	 */
	@SuppressWarnings("unchecked")
	public PushHeaderItem push(List<PushItem> pushItems)
	{
		for (PushItem pushItem : pushItems)
		{
			Object object = pushItem.getObject();
			PageParameters parameters = pushItem.getPageParameters();

			if (object == null)
			{
				throw new WicketRuntimeException(
					"Please provide an object to the items to be pushed, so that the url can be created for the given resource.");
			}

			RequestCycle requestCycle = RequestCycle.get();
			CharSequence url = null;
			if (object instanceof ResourceReference)
			{
				url = requestCycle.urlFor((ResourceReference)object, parameters);
			}
			else if (object instanceof Class)
			{
				url = requestCycle.urlFor((Class<? extends Page>)object, parameters);
			}
			else if (object instanceof IRequestHandler)
			{
				url = requestCycle.urlFor((IRequestHandler)object);
			}
			else
			{
				url = object.toString();
			}

			if (url.toString().equals("."))
			{
				url = "/";
			}
			else if (url.toString().startsWith("."))
			{
				url = url.toString().substring(1);
			}

			urls.add(url.toString());
		}
		return this;
	}
}
