package com.voxbiblia.rjmailer.dns;

import junit.framework.TestCase;

/**
 * Tests Resolver
 */
public class ResolverTest
    extends TestCase
{
    public void testCtor()
            throws Exception
    {
        Resolver r = new Resolver("193.14.119.138");
    }

    public void testResolve()
    {
        Resolver r = new Resolver("193.14.119.138");
        r.resolve(new Query("resare.com"));
    }

}
