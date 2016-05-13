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
 * TODO org.eclipse.jetty.server.Request has to be replaced with the final javax.servlet.api
 * interface (HttpServletRequest)
 * 
 * @author Tobias Soloschenko
 *
 */
public class PushHeaderItem extends HeaderItem
{
	private static final long serialVersionUID = 1L;

	/**
	 * The http2 protocol string
	 */
	public static final String HTTP2_PROTOCOL = "http/2";

	/**
	 * The token suffix to be used in this header item
	 */
	private static final String TOKEN_SUFFIX = HTTP2_PROTOCOL + "_pushed";

	/**
	 * The URLs of resources to be pushed to the client
	 */
	private Set<String> urls = new ConcurrentHashSet<String>(new TreeSet<String>());

	/**
	 * Uses the URLs that has already been pushed to the client to ensure not to push them again
	 */
	@Override
	public Iterable<?> getRenderTokens()
	{
		Set<String> tokens = new TreeSet<String>();
		for (String url : urls)
		{
			tokens.add(url + TOKEN_SUFFIX);
		}
		return tokens;
	}

	/**
	 * Pushes the previously created URLs to the client
	 */
	@Override
	public void render(Response response)
	{
		Request request = getContainerRequest(RequestCycle.get().getRequest());
		// Check if the protocol is http/2 or http/2.0 to only push the resources in this case
		if (isHttp2(request))
		{
			for (String url : urls)
			{
				request.getPushBuilder().path(url.toString()).push();
			}
		}
	}

	/**
	 * Creates a URL and pushes the resource to the client - this is only supported if http2 is
	 * enabled
	 * 
	 * @param pushItems
	 *            a list of items to be pushed to the client
	 * @return the current push header item
	 */
	@SuppressWarnings("unchecked")
	public PushHeaderItem push(List<PushItem> pushItems)
	{
		RequestCycle requestCycle = RequestCycle.get();
		if (isHttp2(getContainerRequest(requestCycle.getRequest())))
			for (PushItem pushItem : pushItems)
			{
				Object object = pushItem.getObject();
				PageParameters parameters = pushItem.getPageParameters();
				String suffix = pushItem.getSuffix();

				if (object == null)
				{
					throw new WicketRuntimeException(
						"Please provide an object to the items to be pushed, so that the url can be created for the given resource.");
				}

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

				if (suffix != null)
				{
					url = url.toString()
						+ (url.toString().contains("?") ? "&" + suffix : "?" + suffix);
				}

				urls.add(url.toString());
			}
		return this;
	}

	/**
	 * Gets the container request
	 * 
	 * @param request
	 *            the wicket request to get the container request from
	 * @return the container request
	 */
	public Request getContainerRequest(org.apache.wicket.request.Request request)
	{

		return (Request)request.getContainerRequest();
	}

	/**
	 * Checks if the given request is a http/2 request
	 * 
	 * @param request
	 *            the request to check if it is a http/2 request
	 * @return if the request is a http/2 request
	 */
	public boolean isHttp2(Request request)
	{
		// detects http/2 and http/2.0
		return request.getProtocol().toLowerCase().contains(HTTP2_PROTOCOL);
	}
}
