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
import java.util.SortedMap;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationSystemStatus;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.SavingEnabledAndDataStatus;
import uk.org.opentrv.ETV.parse.OTLogActivityParse.ValveLogParseResult;

/**Algorithms for data segmentation into control/normal/ignore periods. */
public final class StatusSegmentation
    {
    private StatusSegmentation() { /* Prevent instance instantiation. */ }

    /**Examine the activity and status of the energy-saving devices in a household to decide how to analyse each day.
     * @param devices  collection of devices (eg valves) in household; never null
     * @return  the overall household status by day; never null
     */
    public static ETVPerHouseholdComputationSystemStatus segmentActivity(final Collection<ValveLogParseResult> devices)
        {
        if(null == devices) { throw new IllegalArgumentException(); }

        // Deal with empty input quickly.
        if(devices.isEmpty())
            { return(new ETVPerHouseholdComputationSystemStatus()
                {
                @Override public SortedMap<Integer, SavingEnabledAndDataStatus> getOptionalEnabledAndUsableFlagsByLocalDay()
                    { return(Collections.<Integer, SavingEnabledAndDataStatus>emptySortedMap());  }
                });
            }


        // TODO


        throw new RuntimeException("NOT IMPLEMENTED");
        }
    }
