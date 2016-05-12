package de.jetty.wicket.http2.example;

import java.io.File;

import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

public class Runner
{
	public static void main(String[] args) throws Exception
	{
		Server server = new Server();

		// HTTP Configuration
		HttpConfiguration http_config = new HttpConfiguration();
		http_config.setSecureScheme("https");
		http_config.setSecurePort(8443);
		http_config.setSendXPoweredBy(true);
		http_config.setSendServerVersion(true);

		// keytool -keystore keystore -alias jetty -genkey -keyalg RSA
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(new File(".","keystore").getCanonicalPath());
		sslContextFactory.setKeyStorePassword("123456789");
		sslContextFactory.setKeyManagerPassword("123456789");
		sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
		sslContextFactory.setUseCipherSuitesOrder(true);

		// HTTPS Configuration
		HttpConfiguration https_config = new HttpConfiguration(http_config);
		https_config.addCustomizer(new SecureRequestCustomizer());

		// HTTP Connector
		ServerConnector http1 = new ServerConnector(server, new HttpConnectionFactory(http_config),
			new HTTP2CServerConnectionFactory(http_config));
		http1.setPort(8080);
		server.addConnector(http1);

		// HTTP/2 Connection Factory
		HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(https_config);

		NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
		ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
		alpn.setDefaultProtocol(http1.getDefaultProtocol());

		// SSL Connection Factory
		SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

		// HTTP/2 Connector
		ServerConnector http2Connector = new ServerConnector(server, ssl, alpn, http2,
			new HttpConnectionFactory(https_config));
		http2Connector.setPort(8443);
		server.addConnector(http2Connector);

		WebAppContext webAppContext = new WebAppContext();
		webAppContext.setServer(server);
		webAppContext.setContextPath("/");
		webAppContext.setWar("src/main/webapp");
		server.setHandler(webAppContext);

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.addHandler(webAppContext);
		server.setHandler(contexts);
		
		ALPN.debug = false;

		server.start();
		server.join();
	}
}
