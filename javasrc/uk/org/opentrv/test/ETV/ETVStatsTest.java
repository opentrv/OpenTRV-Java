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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.SortedMap;

import org.junit.Test;

import uk.org.opentrv.ETV.ETVHouseholdGroupSimpleSummaryStats;
import uk.org.opentrv.ETV.ETVHouseholdGroupSimpleSummaryStats.MeanAndPopSD;
import uk.org.opentrv.ETV.ETVHouseholdGroupSimpleSummaryStats.SummaryStats;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationResult;
import uk.org.opentrv.ETV.parse.NBulkInputs;
import uk.org.opentrv.hdd.DDNExtractor;
import uk.org.opentrv.hdd.HDDUtil.HDDMetrics;

/**Testing statistical computations for ETV. */
public class ETVStatsTest
    {
    /**Test for main features of an empty final simple stats summary. */
    @Test public void testEmptySimpleStatsSummary() throws IOException
        {
        // All zeros and NaNs expected for empty data set.
        final SummaryStats e = ETVHouseholdGroupSimpleSummaryStats.computeSummaryStats(0, Collections.emptyList());
        assertNotNull(e);
        assertEquals(0, e.getAllHouseholdsCount());
        assertEquals(0, e.getFinalHouseholdsCount());
        assertEquals(0, e.getNormalDayCount());
        for(final MeanAndPopSD mp : new MeanAndPopSD[]{e.getStatsOverRSquared(), e.getStatsOverSlope(), e.getStatsOverEfficacy()})
            {
            assertNotNull(mp);
            assertTrue(Double.isNaN(mp.mean));
            assertTrue(Double.isNaN(mp.pVariance));
            assertTrue(Double.isNaN(mp.pSD));
            }
        }

    /**Test for main features of a singleton final simple stats summary.
     * All variance/SD should be zero,
     * and all means the same as the input value.
     */
    @Test public void testSingletonSimpleStatsSummary() throws IOException
        {
        // Given a single representative household HDD metrics...
        final ETVPerHouseholdComputationResult hcr = new ETVPerHouseholdComputationResult(){
            @Override public Float getRatiokWhPerHDDNotSmartOverSmart() { return(1.3f); }
            @Override public String getHouseID() { return("TheAvenue"); }
            @Override public HDDMetrics getHDDMetrics() { return(new HDDMetrics(1.5f, 4.0f, 0.8f, 42)); }
            };
        final SummaryStats s = ETVHouseholdGroupSimpleSummaryStats.computeSummaryStats(1, Collections.singleton(hcr));
        assertNotNull(s);
        assertEquals(1, s.getAllHouseholdsCount());
        assertEquals(1, s.getFinalHouseholdsCount());
        assertEquals(42, s.getNormalDayCount());
        for(final MeanAndPopSD mp : new MeanAndPopSD[]{s.getStatsOverRSquared(), s.getStatsOverSlope(), s.getStatsOverEfficacy()})
            {
            assertNotNull(mp);
            assertFalse(Double.isNaN(mp.mean));
            assertFalse(Double.isNaN(mp.pVariance));
            assertFalse(Double.isNaN(mp.pSD));
            assertEquals(0.0, mp.pVariance, 0.01);
            assertEquals(0.0, mp.pSD, 0.01);
            }
        assertEquals(0.8, s.getStatsOverRSquared().mean, 0.01);
        assertEquals(1.5, s.getStatsOverSlope().mean, 0.01);
        assertEquals(1.3, s.getStatsOverEfficacy().mean, 0.01);
        }

    /**Test for main features of a multi-household final simple stats summary.
     * All variance/SD should be +ve.
     */
    @Test public void testMultiSimpleStatsSummary() throws IOException
        {
        // Given representative household HDD metrics...
        final ETVPerHouseholdComputationResult hcr1 = new ETVPerHouseholdComputationResult(){
            @Override public Float getRatiokWhPerHDDNotSmartOverSmart() { return(1.3f); }
            @Override public String getHouseID() { return("TheAvenue"); }
            @Override public HDDMetrics getHDDMetrics() { return(new HDDMetrics(1.5f, 4.0f, 0.8f, 42)); }
            };
        final ETVPerHouseholdComputationResult hcr2 = new ETVPerHouseholdComputationResult(){
            @Override public Float getRatiokWhPerHDDNotSmartOverSmart() { return(1.1f); }
            @Override public String getHouseID() { return("TheGrange"); }
            @Override public HDDMetrics getHDDMetrics() { return(new HDDMetrics(5.5f, 7.0f, 0.6f, 41)); }
            };
        final SummaryStats s = ETVHouseholdGroupSimpleSummaryStats.computeSummaryStats(5, Arrays.asList(hcr1, hcr2));
        assertNotNull(s);
        assertEquals(5, s.getAllHouseholdsCount());
        assertEquals(2, s.getFinalHouseholdsCount());
        assertEquals(83, s.getNormalDayCount());
        for(final MeanAndPopSD mp : new MeanAndPopSD[]{s.getStatsOverRSquared(), s.getStatsOverSlope(), s.getStatsOverEfficacy()})
            {
            assertNotNull(mp);
            assertFalse(Double.isNaN(mp.mean));
            assertFalse(Double.isNaN(mp.pVariance));
            assertFalse(Double.isNaN(mp.pSD));
            assertTrue(mp.pVariance > 0.0);
            assertTrue(mp.pSD > 0.0);
            }
        assertEquals(0.7, s.getStatsOverRSquared().mean, 0.01);
        assertEquals(3.5, s.getStatsOverSlope().mean, 0.01);
        assertEquals(1.2, s.getStatsOverEfficacy().mean, 0.01);
        assertEquals(0.1, s.getStatsOverRSquared().pSD, 0.01);
        assertEquals(2.0, s.getStatsOverSlope().pSD, 0.01);
        assertEquals(0.1, s.getStatsOverEfficacy().pSD, 0.01);
        }

    /**Sample (real, EGLL) HDD data for just large enough for a meaningful computation. */
    static final String HDDsample = "Date,HDD,% Estimated,\"EGLL 15.5C base, source www.degreedays.net (using temperature data from www.wunderground.com)\"\n" +
        "2016-01-01,10.2,0\n" +
        "2016-01-02,5.6,0\n" +
        "2016-01-03,7.8,0\n" +
        "2016-01-04,8,0\n" +
        "2016-01-05,7.8,1\n" +
        "2016-01-06,9.1,0\n" +
        "2016-01-07,8.3,0\n" +
        "2016-01-08,9.8,0\n" +
        "2016-01-09,6.8,0\n" +
        "2016-01-10,9.4,0\n" +
        "2016-01-11,9.6,0\n" +
        "2016-01-12,9.9,0\n" +
        "2016-01-13,10.8,0\n" +
        "2016-01-14,11.7,0\n" +
        "2016-01-15,12.7,0\n";

    /**Test a synthetic data set for correct efficacy computation. */
    @Test public void testSyntheticRatioComputation() throws IOException
        {
        final SortedMap<Integer, Float> hdd = DDNExtractor.extractSimpleHDD(new StringReader(HDDsample), NBulkInputs.STD_BASE_TEMP_C).getMap();
        assertEquals(15, hdd.size());





        }
    }
