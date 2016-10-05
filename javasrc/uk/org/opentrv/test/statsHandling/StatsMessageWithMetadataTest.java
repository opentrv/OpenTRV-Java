/*
The OpenTRV project licenses this file to you
under the Apache Licence, Version 2.0 (the "Licence");
you may not use this file except in compliance
with the Licence. You may obtain a copy of the Licence at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the Licence is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the Licence for the
specific language governing permissions and limitations
under the Licence.

Author(s) / Copyright (s): Damon Hart-Davis 2015
*/
package uk.org.opentrv.test.statsHandling;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import uk.org.opentrv.comms.statshandlers.StatsMessageWithMetadata;

public class StatsMessageWithMetadataTest
    {
    /**Test JSON representation.
     * TODO-971: canary test that was incorrectly passing in UK timezone (GMT/GMT+1), not in French (GMT+1/GMT+2),
     * and was amended by Bruno G 2016/08 to correctly fail in UK timezone again (during BST).
     * 
     * @throws ParseException
     */
    @Test
    public void testAsJSON() throws ParseException
        {
        // % date --date='@1430933201'
        // Wed May  6 17:26:41 UTC 2015
        final long t = 1430933201034L; // ~2015/05/06 18:26 BST aka 2015/05/06 17:26 UTC
        final StatsMessageWithMetadata sm1 = new StatsMessageWithMetadata("{}", t, true);
        assertEquals(t, sm1.timestamp);
        final String sm1s = sm1.asJSONArrayString();
        assertEquals("[\"2015-05-06T17:26:41Z\",\"{}\",true]", sm1s);
        final JSONParser parser = new JSONParser();
        final List<?> jsonArray = (List<?>) parser.parse(sm1s);
        assertEquals("2015-05-06T17:26:41Z", jsonArray.get(0));
        assertEquals("{}", jsonArray.get(1));
        assertEquals(Boolean.TRUE, jsonArray.get(2));
        assertEquals("[\"2015-05-06T17:26:41Z\",\"{}\",true]", sm1s);
        final StatsMessageWithMetadata sm2 = new StatsMessageWithMetadata("@ABCD;\"2", t, true);
        final String sm2s = sm2.asJSONArrayString();
        assertEquals("[\"2015-05-06T17:26:41Z\",\"@ABCD;\\\"2\",true]", sm2s);
        }
    }
