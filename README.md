# jetty-http2-example
This is a small setup of jetty / http2 / Apache Wicket to test experimental features. To run follow these steps

1. Download the corresponding ALPN Boot version for your jdk http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html
2. Before you launch the Runner class (in package: src/test/java/de/jetty/wicket/http2/example) apply -Xbootclasspath/p:/&lt;path_to_jar&gt;/alpn-boot-&lt;version&gt;.jar to your vm args
3. Access the server via https://127.0.0.1:8443/
