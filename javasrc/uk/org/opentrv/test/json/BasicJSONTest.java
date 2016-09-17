package uk.org.opentrv.test.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import uk.org.opentrv.comms.json.JSONStatsLineStreamReader;
import uk.org.opentrv.comms.statshandlers.builtin.SimpleFileLoggingStatsHandler;

public class BasicJSONTest
    {
    /**Example from https://code.google.com/p/json-simple/wiki/EncodingExamples to test basic behaviour. */
    @Test public void testEncExample()
        {
        final JSONObject obj = new JSONObject();
        obj.put("name", "foo");
        obj.put("num", new Integer(100));
        obj.put("balance", new Double(1000.21));
        obj.put("is_vip", new Boolean(true));
        obj.put("nickname", null);
        final String result = String.valueOf(obj);
//        System.out.print(obj);
        assertEquals("{\"balance\":1000.21,\"is_vip\":true,\"num\":100,\"name\":\"foo\",\"nickname\":null}", result);
        }

    /**Decoding plausible input. */
    @Test public void testDecExample() throws Exception
        {
        final String in = "{\"id\":\"c2e0\",\"t|C16\":332,\"RH|%\":65,\"l\":254,\"o\":2,\"b|cV\":323}";
        final JSONParser parser = new JSONParser();
        final Map json = (Map)parser.parse(in);
        assertNull(json.get("bogus"));
        assertEquals("c2e0", json.get("id"));
        assertEquals(323, ((Number)json.get("b|cV")).intValue());
        assertNotNull(json.get("l"));
        assertEquals(254, ((Number)json.get("l")).intValue());
        }

    /**Decoding broken input. */
    @Test public void testBrokenExample()
        {
        // DHD20141123: one common-ish failure mode seen over RF is truncation of the frame.
        final String truncated = "{\"id\":\"c2e0\",\"t|C16\":332,\"RH|%\":65,\"l\":254,\"o";
        final JSONParser parser = new JSONParser();
        try { parser.parse(truncated); fail("should have been rejected"); } catch(final ParseException e) { /* expected */ }
        try { parser.parse("}"); fail("should have been rejected"); } catch(final ParseException e) { /* expected */ }
        try { parser.parse(""); fail("should have been rejected"); } catch(final ParseException e) { /* expected */ }
        try { parser.parse("@abc"); fail("should have been rejected"); } catch(final ParseException e) { /* expected */ }
        }

    /**Test of conversion of raw leaf-node JSON to JSON-array log line. */
    @Test public void testJSONLoggingSimple() throws Exception
        {
        for(final String concID : new String[]{"", Long.toString((rnd.nextLong()>>>1),36)} )
            {
            final String line = SimpleFileLoggingStatsHandler.wrapLeafJSONAsArrayLogLine(1416763674255L, concID, "{}");
            assertEquals("[ \"2014-11-23T17:27:54Z\", \""+concID+"\", {} ]", line);
            final JSONParser parser = new JSONParser();
            final Object lO = parser.parse(line);
            assertTrue(lO instanceof List);
            final List l = (List)lO;
            assertEquals("2014-11-23T17:27:54Z", l.get(0));
            assertEquals(concID, l.get(1));
            assertTrue(l.get(2) instanceof Map);
            }
        }

    /**Sample of [ timestamp, concentratorID, lightweightNodeJSON ] format, line oriented. */
    public static final String StreamedJSONSample1 =
        "[ \"2014-12-19T14:58:20Z\", \"\", {\"@\":\"2d1a\",\"+\":7,\"v|%\":0,\"tT|C\":7,\"O\":1,\"vac|h\":6} ]\n" +
        "[ \"2014-12-19T14:58:28Z\", \"\", {\"@\":\"3015\",\"+\":2,\"T|C16\":290,\"L\":255,\"B|mV\":2567} ]\n" +
        "[ \"2014-12-19T14:59:50Z\", \"\", {\"@\":\"0a45\",\"+\":5,\"B|mV\":3315,\"v|%\":0,\"tT|C\":7,\"O\":1} ]\n" +
        "[ \"2014-12-19T15:00:06Z\", \"\", {\"@\":\"414a\",\"+\":3,\"B|mV\":3315,\"v|%\":0,\"tT|C\":7,\"O\":1} ]\n" +
        "[ \"2014-12-19T15:00:20Z\", \"\", {\"@\":\"2d1a\",\"+\":0,\"L\":120,\"T|C16\":305,\"B|mV\":3315} ]\n" +
        "[ \"2014-12-19T15:00:28Z\", \"\", {\"@\":\"3015\",\"+\":3,\"v|%\":0,\"tT|C\":12,\"O\":1,\"vac|h\":6} ]\n" +
        "[ \"2014-12-19T15:01:28Z\", \"\", {\"@\":\"3015\",\"+\":4,\"B|mV\":2550,\"T|C16\":290,\"H|%\":80} ]\n" +
        "[ \"2014-12-19T15:01:50Z\", \"\", {\"@\":\"0a45\",\"+\":7,\"L\":206,\"B|mV\":3315,\"v|%\":0,\"tT|C\":7} ]\n" +
        "[ \"2014-12-19T15:02:06Z\", \"\", {\"@\":\"414a\",\"+\":4,\"L\":142,\"vac|h\":6,\"T|C16\":278} ]\n" +
        "[ \"2014-12-19T15:02:10Z\", \"\", {\"@\":\"0d49\",\"+\":4,\"L\":239,\"vac|h\":7,\"T|C16\":290} ]\n";

    /**Test extraction to space-separated text 3-column output of single stat by node ID and stat name. */
    @Test public void extractStreamingStatTest() throws Exception
        {
        // Test filter just by field.
        // Empty input.
        try(final BufferedReader br = new BufferedReader(new JSONStatsLineStreamReader(new StringReader(""), "v|%")))
            {
            assertNull(br.readLine());
            }
        // Non-empty input.
        try(final BufferedReader br = new BufferedReader(new JSONStatsLineStreamReader(new StringReader(StreamedJSONSample1), "v|%")))
            {
            assertEquals("2014-12-19T14:58:20Z 2d1a 0", br.readLine());
            assertEquals("2014-12-19T14:59:50Z 0a45 0", br.readLine());
            assertEquals("2014-12-19T15:00:06Z 414a 0", br.readLine());
            assertEquals("2014-12-19T15:00:28Z 3015 0", br.readLine());
            assertEquals("2014-12-19T15:01:50Z 0a45 0", br.readLine());
            assertNull(br.readLine());
            }

        // Test filter by field and ID.
        try(final BufferedReader br = new BufferedReader(new JSONStatsLineStreamReader(new StringReader(StreamedJSONSample1), "v|%", "414a")))
            {
            assertEquals("2014-12-19T15:00:06Z 414a 0", br.readLine());
            assertNull(br.readLine());
            }

        // Test of single-char reads.
        try(final Reader r = new JSONStatsLineStreamReader(new StringReader(StreamedJSONSample1), "v|%"))
            {
            assertEquals('2', r.read());
            assertEquals('0', r.read());
            assertEquals('1', r.read());
            assertEquals('4', r.read());
            }
        }

    /**Sample of [ timestamp, concentratorID, lightweightNodeJSON ] format, line oriented, longer IDs. */
    public static final String StreamedJSONSample2 =
        "[ \"2016-09-17T07:38:55Z\", \"\", {\"@\":\"91ACF3CFF388D4E0\",\"+\":5,\"O\":2,\"tT|C\":18,\"T|C16\":341} ]\n" + 
        "[ \"2016-09-17T07:39:35Z\", \"\", {\"@\":\"96F0CED3B4E690E8\",\"+\":2,\"L\":34,\"tT|C\":19,\"T|C16\":341} ]\n" + 
        "[ \"2016-09-17T07:39:51Z\", \"\", {\"@\":\"819C99B4B9BD84BB\",\"+\":14,\"L\":195,\"T|C16\":234,\"O\":1} ]\n" + 
        "[ \"2016-09-17T07:41:06Z\", \"\", {\"@\":\"FEDA88A08188E083\",\"+\":11,\"tS|C\":0,\"vC|%\":0,\"gE\":11} ]\n" + 
        "[ \"2016-09-17T07:41:38Z\", \"\", {\"@\":\"E68EF783B0EFCBBB\",\"+\":8,\"T|C16\":372,\"b\":0} ]\n" + 
        "[ \"2016-09-17T07:41:56Z\", \"\", {\"@\":\"FA97A8A7B7D2D3B6\",\"+\":0,\"H|%\":60,\"tT|C\":16} ]\n" + 
        "[ \"2016-09-17T07:42:34Z\", \"\", {\"@\":\"A9B2F7C089EECD89\",\"+\":5,\"T|C16\":335,\"H|%\":62,\"O\":1} ]\n" + 
        "[ \"2016-09-17T07:42:40Z\", \"\", {\"@\":\"E091B7DC8FEDC7A9\",\"+\":1,\"tT|C\":14,\"tS|C\":4} ]\n" + 
        "[ \"2016-09-17T07:42:43Z\", \"\", {\"@\":\"91ACF3CFF388D4E0\",\"+\":6,\"H|%\":60,\"O\":2,\"vac|h\":0} ]\n" + 
        "[ \"2016-09-17T07:43:38Z\", \"\", {\"@\":\"96F0CED3B4E690E8\",\"+\":3,\"H|%\":61,\"O\":1,\"B|cV\":266} ]\n";

    /**OK local PRNG. */
    private static final Random rnd = new Random();
    }
