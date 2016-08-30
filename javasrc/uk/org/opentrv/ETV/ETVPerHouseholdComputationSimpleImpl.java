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
import java.util.EnumMap;
import java.util.SortedMap;
import java.util.SortedSet;

import uk.org.opentrv.hdd.ConsumptionHDDTuple;
import uk.org.opentrv.hdd.ContinuousDailyHDD;
import uk.org.opentrv.hdd.HDDUtil;
import uk.org.opentrv.hdd.HDDUtil.HDDMetrics;

/**Simple computation implementation for one household, no efficacy.
 * This can do a simple computation to find overall kWh/HDD
 * from the supplied house's data,
 * ignoring (not computing) change in efficiency with equipment operation.
 * <p>
 * This ignores
 * <p>
 * May not work if input data is discontinuous,
 * or iff energy data date range is not completely within HDD data date range.
 * <p>
 * This class is a stateless singleton.
 */
public final class ETVPerHouseholdComputationSimpleImpl implements ETVPerHouseholdComputation
    {
    // Lazy-creation singleton.
    private ETVPerHouseholdComputationSimpleImpl() { /* prevent direct instance creation. */ }
    private static class ETVPerHouseholdComputationSimpleImplHolder { static final ETVPerHouseholdComputationSimpleImpl INSTANCE = new ETVPerHouseholdComputationSimpleImpl(); }
    public static ETVPerHouseholdComputationSimpleImpl getInstance() { return(ETVPerHouseholdComputationSimpleImplHolder.INSTANCE); }

    @Override
    public ETVPerHouseholdComputationResult apply(final ETVPerHouseholdComputationInput in) throws IllegalArgumentException
        {
        if(null == in) { throw new IllegalArgumentException(); }

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

    /**Split energy data by savings-features type (enabled/disabled).
     * Computes the heating-efficiency metric (kWh/HDD) separately
     * for the cases where energy saving measures are
     * enabled (normal) and disabled (control).
     *
     * @param in   per-day raw input energy and HDD and status values; never null
     * @return  mapping from Enabled and Disabled (not DontUse) to the HDD metrics and efficacy measure;
     *    never null though may be empty
     */
    public static EnumMap<SavingEnabledAndDataStatus, ETVPerHouseholdComputationResult> splitData(
        final ETVPerHouseholdComputationInput in)
        {

        // TODO



        throw new RuntimeException("NOT IMPLEMENTED");
        }

    }
