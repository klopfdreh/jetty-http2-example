package de.jetty.wicket.http2.example;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.collections.ConcurrentHashSet;
import org.apache.wicket.util.time.Time;

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

	private static final SimpleDateFormat headerDateFormat = new SimpleDateFormat(
	    "EEE, dd MMM yyyy HH:mm:ss zzz");

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
	 * The web response of the page to apply the caching information to
	 */
	private WebResponse pageWebResponse;

	/**
	 * The web request of the page to get the caching information from
	 */
	private WebRequest pageWebRequest;

	/**
	 * The page to get the modification time of
	 */
	private Page page;


	public PushHeaderItem(Page page, Response pageResponse, Request pageRequest)
	{
		if (page == null || !(page instanceof WebPage) || pageResponse == null
		    || !(pageResponse instanceof WebResponse))
		{
			throw new WicketRuntimeException(
			    "Please hand over the web page, the web response and the web request to the push header item like \"new PushHeaderItem(this, yourWebPageReponse, yourWebPageRequest)\" - "
			        + "The webPageResponse / webPageRequest can be obtained via \"getRequestCycle().getResponse()\" and placed into the page as field "
			        + "\"private transient Response webPageResponse;\" / \"private transient Request webPageRequest;\"");
		}
		this.pageWebResponse = (WebResponse)pageResponse;
		this.pageWebRequest = (WebRequest)pageRequest;
		this.page = page;
	}

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
	 * Gets the time the page has been modified
	 * 
	 * @return the time the page has been modified
	 */
	private Time getPageModificationTime()
	{
		URL resource = page.getClass().getResource(page.getClass().getSimpleName() + ".html");
		if (resource == null)
		{
			throw new WicketRuntimeException(
			    "The markup to the page couldn't be found: " + page.getClass().getName());
		}
		try
		{
			return Time.valueOf(new Date(resource.openConnection().getLastModified()));
		}
		catch (IOException e)
		{
			throw new WicketRuntimeException(
			    "The time couln't be determined of the markup file of the page: "
			        + page.getClass().getName(),
			    e);
		}
	}

	/**
	 * Applies the cache header item to the response
	 */
	private void applyPageCacheHeader()
	{
		Time pageModificationTime = getPageModificationTime();
		// check modification of page html
		pageWebResponse.setLastModifiedTime(pageModificationTime);
		pageWebResponse.setDateHeader("Expires", pageModificationTime);
		pageWebResponse.setHeader("Cache-Control",
		    "max-age=31536000, public, must-revalidate, proxy-revalidate");
		pageWebResponse.setHeader("Pragma", "public");
	}

	/**
	 * Pushes the previously created URLs to the client
	 */
	@Override
	public void render(Response response)
	{

		// applies
		applyPageCacheHeader();

		HttpServletRequest request = getContainerRequest(RequestCycle.get().getRequest());
		// Check if the protocol is http/2 or http/2.0 to only push the resources in this case
		if (isHttp2(request))
		{

			for (String url : urls)
			{
				// TODO Jetty has to switch to the javax.servlet-api classes and handle
				// SETTINGS_ENABLE_PUSH settings frame value and implement the default API against
				// it.
				try
				{
					Time pageModificationTime = getPageModificationTime();
					String ifModifiedSinceHeader = pageWebRequest.getHeader("If-Modified-Since");

					if (ifModifiedSinceHeader != null)
					{
						Time ifModifiedSinceFromRequestTime = Time
						    .valueOf(headerDateFormat.parse(ifModifiedSinceHeader));
						// If the client modification time is before the page modification time -
						// don't push
						if (ifModifiedSinceFromRequestTime.before(pageModificationTime))
						{
							org.eclipse.jetty.server.Request.getBaseRequest(request)
							    .getPushBuilder().path(url.toString()).push();
						}
					}
					else
					{
						org.eclipse.jetty.server.Request.getBaseRequest(request).getPushBuilder()
						    .path(url.toString()).push();
					}
				}
				catch (ParseException e)
				{
					e.printStackTrace();
				}
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
					Url encoded = new PageParametersEncoder().encodePageParameters(parameters);
					String queryString = encoded.getQueryString();
					url = object.toString() + (queryString != null ? "?" + queryString : "");
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

	/**
	 * Gets the container request
	 * 
	 * @param request
	 *            the wicket request to get the container request from
	 * @return the container request
	 */
	public HttpServletRequest getContainerRequest(Request request)
	{

		return checkHttpServletRequest(request);
	}

	/**
	 * Checks if the given request is a http/2 request
	 * 
	 * @param request
	 *            the request to check if it is a http/2 request
	 * @return if the request is a http/2 request
	 */
	public boolean isHttp2(HttpServletRequest request)
	{
		// detects http/2 and http/2.0
		return request.getProtocol().toLowerCase().contains(HTTP2_PROTOCOL);
	}

	/**
	 * Checks if the container request from the given request is instance of
	 * {@link HttpServletRequest} if not the API of the PushHeaderItem can't be used and a
	 * {@link WicketRuntimeException} is thrown.
	 * 
	 * @param request
	 *            the request to get the container request from. The container request is checked if
	 *            it is instance of {@link HttpServletRequest}
	 * @return the container request get from the given request casted to {@link HttpServletRequest}
	 * @throw {@link WicketRuntimeException} if the container request is not a
	 *        {@link HttpServletRequest}
	 */
	public HttpServletRequest checkHttpServletRequest(Request request)
	{
		Object assumedHttpServletRequest = request.getContainerRequest();
		if (!(assumedHttpServletRequest instanceof HttpServletRequest))
		{
			throw new WicketRuntimeException(
			    "The request is not a HttpServletRequest - the usage of PushHeaderItem is not support in the current environment: "
			        + request.getClass().getName());
		}
		return (HttpServletRequest)assumedHttpServletRequest;
	}
}
