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

package uk.org.opentrv.ETV;

import java.io.IOException;
import java.util.SortedMap;
import java.util.SortedSet;

import uk.org.opentrv.ETV.filter.StatusSegmentation;
import uk.org.opentrv.hdd.ConsumptionHDDTuple;
import uk.org.opentrv.hdd.ContinuousDailyHDD;
import uk.org.opentrv.hdd.HDDUtil;
import uk.org.opentrv.hdd.HDDUtil.HDDMetrics;

/**Simple computation implementation for one household.
 * This can do a simple computation to find overall kWh/HDD
 * from the supplied house's data,
 * ignoring (not computing) change in efficiency with equipment operation
 * if status information (Enabled/Disabled/DontUse) is not supplied.
 * <p>
 * May fail if input data is discontinuous.
 * <p>
 * May fail iff energy data date range is not completely within HDD data date range.
 * <p>
 * This class is a stateless singleton.
 */
public final class ETVPerHouseholdComputationSimpleImpl implements ETVPerHouseholdComputation
    {
    // Lazy-creation singleton.
    private ETVPerHouseholdComputationSimpleImpl() { /* prevent direct instance creation. */ }
    private static class ETVPerHouseholdComputationSimpleImplHolder { static final ETVPerHouseholdComputationSimpleImpl INSTANCE = new ETVPerHouseholdComputationSimpleImpl(); }
    public static ETVPerHouseholdComputationSimpleImpl getInstance() { return(ETVPerHouseholdComputationSimpleImplHolder.INSTANCE); }

    /**Computes result over all energy data supplied (ignores status); never null. */
    private ETVPerHouseholdComputationResult all(final ETVPerHouseholdComputationInput in) throws IllegalArgumentException
        {
        // FIXME: not meeting contract if HDD data discontinuous; should check.
        final ContinuousDailyHDD cdh = new ContinuousDailyHDD()
            {
            @Override public SortedMap<Integer, Float> getMap() { try { return(in.getHDDByLocalDay()); } catch(final IOException e) { throw new IllegalArgumentException(e); } }
            @Override public float getBaseTemperatureAsFloat() { return(in.getBaseTemperatureAsFloat()); }
            };

            final SortedSet<ConsumptionHDDTuple> combined;
        try {
            final SortedMap<Integer, Float> kWhByLocalDay = in.getKWhByLocalDay();
            if(kWhByLocalDay.isEmpty())
                {
                // Return 'not computable' result.
                return(new ETVPerHouseholdComputationResult() {
                    @Override public String getHouseID() { return(in.getHouseID()); }
                    @Override public HDDMetrics getHDDMetrics() { return(null); }
                    @Override public Float getRatiokWhPerHDDNotSmartOverSmart() { return(null); }
                    });
                }
            combined = HDDUtil.combineDailyIntervalReadingsWithHDD(kWhByLocalDay, cdh);
            }
        catch(final IOException e) { throw new IllegalArgumentException(e); }

        final HDDMetrics metrics = HDDUtil.computeHDDMetrics(combined);

        return(new ETVPerHouseholdComputationResult() {
            @Override public String getHouseID() { return(in.getHouseID()); }
            @Override public HDDMetrics getHDDMetrics() { return(metrics); }
            // Efficacy computation not implemented for simple analysis.
            @Override public Float getRatiokWhPerHDDNotSmartOverSmart() { return(null); }
            });
        }

    @Override
    public ETVPerHouseholdComputationResult apply(final ETVPerHouseholdComputationInput in) throws IllegalArgumentException
        {
        if(null == in) { throw new IllegalArgumentException(); }

        // Simple case, with no segmentation/status.
        final SortedMap<Integer, SavingEnabledAndDataStatus> statuses = in.getOptionalEnabledAndUsableFlagsByLocalDay();
        if(null == statuses) { return(all(in)); }

        // With status/segmentation
        // run for both Enabled and Disabled cases,
        // and combine the results.
        final ETVPerHouseholdComputationInput inE = StatusSegmentation.filterByStatus(in, SavingEnabledAndDataStatus.Enabled);
        final ETVPerHouseholdComputationResult rE = all(inE);
        final ETVPerHouseholdComputationInput inD = StatusSegmentation.filterByStatus(in, SavingEnabledAndDataStatus.Disabled);
        final ETVPerHouseholdComputationResult rD = all(inD);

        // Compute the efficacy, energy-saving features disabled over enabled, > 1.0 is good.
        final HDDMetrics hddMetricsE = rE.getHDDMetrics();
        final float efficacy = rD.getHDDMetrics().slopeEnergyPerHDD / rE.getHDDMetrics().slopeEnergyPerHDD;

        // Return HDD stats for 'enabled' state, with ratio computed D/E.
        return(new ETVPerHouseholdComputationResult() {
            @Override public String getHouseID() { return(in.getHouseID()); }
            @Override public HDDMetrics getHDDMetrics() { return(hddMetricsE); }
            @Override public Float getRatiokWhPerHDDNotSmartOverSmart() { return(efficacy); }
            });
        }
    }
