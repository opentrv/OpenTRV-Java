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

package uk.org.opentrv.ETV.output;

import java.util.function.Function;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationResult;
import uk.org.opentrv.hdd.HDDUtil.HDDMetrics;

/**Generate machine-readable (partial-CVS-line) form for a single result.
 * Stateless.
 */
public final class ETVPerHouseholdComputationResultToCSV
        implements Function<ETVPerHouseholdComputationResult,String>
    {
    /**Produce simple CVS format "house ID,slope,baseload,R^2,n,efficiency gain" eg "12345,1.2,3.5,0.73,156,1.1"; no leading/terminating comma, never null.
     * Returns just the house ID if metrics are not available at all.
     */
    public String apply(ETVPerHouseholdComputationResult r)
        {
        final HDDMetrics hddMetrics = r.getHDDMetrics();
        final Float ratio = r.getRatiokWhPerHDDNotSmartOverSmart();
        return("\""+r.getHouseID()+"\""+((null==hddMetrics)?"":(","+hddMetrics.toCSV()+"," + ((null!=ratio)?ratio:""))));
        }

    /**Produce header for simple CSV format; no leading/terminating comma (nor line-end), never null. */
    public static String headerCSV() { return("\"house ID\","+HDDMetrics.headerCSV()+",\"efficiency gain if computed\""); }
    }
