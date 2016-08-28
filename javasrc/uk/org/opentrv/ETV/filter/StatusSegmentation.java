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

package uk.org.opentrv.ETV.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationSystemStatus;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.SavingEnabledAndDataStatus;
import uk.org.opentrv.ETV.parse.OTLogActivityParse.ValveLogParseResult;

/**Algorithms for data segmentation into control/normal/ignore periods.
 * These algorithms are important to the robustness of the efficacy analysis.
 */
public final class StatusSegmentation
    {
    private StatusSegmentation() { /* Prevent instance instantiation. */ }

    /**Examine the activity and status of the energy-saving devices in a household to decide how to analyse each day.
     * This can use days where any device in the household
     * is calling for heat AND reporting its energy saving status (enabled or disabled).
     * <p>
     * Within those days, any that are marked as having energy saving features disabled
     * for the majority of devices can be regarded as control days;
     * where the majority have energy saving features enabled are normal days.
     *
     * @param devices  collection of devices (eg valves) in household; never null
     * @return  the overall household status by day; never null
     */
    public static ETVPerHouseholdComputationSystemStatus segmentActivity(final Collection<ValveLogParseResult> devices)
        {
        if(null == devices) { throw new IllegalArgumentException(); }

//        // Deal with empty input quickly (result has no usable days).
//        if(devices.isEmpty())
//            {
//            return(new ETVPerHouseholdComputationSystemStatus(){
//                @Override public SortedMap<Integer, SavingEnabledAndDataStatus> getOptionalEnabledAndUsableFlagsByLocalDay()
//                    { return(Collections.<Integer, SavingEnabledAndDataStatus>emptySortedMap());  }
//                });
//            }

        final SortedMap<Integer, SavingEnabledAndDataStatus> result = new TreeMap<>();

        // Potentially usable days: where any device in the household
        // is calling for heat AND reporting its energy saving status (enabled or disabled)
        final Set<Integer> potentiallyUsableDays = new HashSet<>();
        for(final ValveLogParseResult vlpr : devices)
            {
            final Set<Integer> s = new HashSet<>(vlpr.getDaysInWhichCallingForHeat());
            s.retainAll(vlpr.getDaysInWhichEnergySavingStatsReported());
            potentiallyUsableDays.addAll(s);
            }

        // TODO


        return(new ETVPerHouseholdComputationSystemStatus(){
            @Override public SortedMap<Integer, SavingEnabledAndDataStatus> getOptionalEnabledAndUsableFlagsByLocalDay()
                { return(Collections.unmodifiableSortedMap(result));  }
            });
        }
    }
