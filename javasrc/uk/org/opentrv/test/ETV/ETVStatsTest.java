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

Author(s) / Copyright (s): Damon Hart-Davis 2016
*/

package uk.org.opentrv.test.ETV;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import uk.org.opentrv.ETV.ETVHouseholdGroupSimpleSummaryStats;
import uk.org.opentrv.ETV.ETVHouseholdGroupSimpleSummaryStats.SummaryStats;

/**Testing statistical computations for ETV. */
public class ETVStatsTest
    {
    /**Test for main features of the final simple stats summary. */
    @Test public void testSimpleStatsSummary() throws IOException
        {
        final SummaryStats e = ETVHouseholdGroupSimpleSummaryStats.computeSummaryStats(0, Collections.emptyList());
        assertNotNull(e);
        assertEquals(0, e.getAllHouseholdsCount());
        assertEquals(0, e.getFinalHouseholdsCount());
        assertEquals(0, e.getNormalDayCount());
        }
    }