package de.jetty.wicket.http2.example.resources;

import org.apache.wicket.request.resource.CssResourceReference;

public class TestResourceReference extends CssResourceReference
{

	private static final long serialVersionUID = 1L;

	private static TestResourceReference testResourceReference;

	private TestResourceReference()
	{
		super(TestResourceReference.class, "TestResourceReference.css");
	}

	public static synchronized TestResourceReference getInstance()
	{
		if (testResourceReference == null)
		{
			testResourceReference = new TestResourceReference();
		}
		return testResourceReference;
	}

}
