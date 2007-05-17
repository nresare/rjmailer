package com.voxbiblia.rjmailer.dns;

import junit.framework.TestCase;

/**
 * Tests the Buffer
 */
public class BufferTest
    extends TestCase
{
    public void testParseBEUInt16()
    {
        byte[] buf = new byte[2];
        buf[0] = 18;
        buf[1] = 19;
        assertEquals(4627, Buffer.parseInt16(buf));

        buf[0] = -1;
        buf[1] = -1;
        assertEquals(65535, Buffer.parseInt16(buf));
    }


    public void testReadName()
    {
        byte[] bytes = new byte[] { 6, 'r', 'e', 's', 'a', 'r', 'e', 3, 'c', 'o', 'm', 0 };
        Buffer b = new Buffer(bytes);
        assertEquals("resare.com", b.readName());
    }

    public void testReadNameCompressed()
    {
        byte[] bytes = new byte[] { 6, 'r', 'e', 's', 'a', 'r', 'e',
                3, 'c', 'o', 'm', 0, -64, 0};
        Buffer b = new Buffer(bytes);
        b.skip(12);
        assertEquals("resare.com", b.readName());

    }
}