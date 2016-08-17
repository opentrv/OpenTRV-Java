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

import java.util.function.Predicate;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationResult;

/**Common filters for eliminating outliers from ETV dataset simple results during processing. */
public final class CommonSimpleResultFilters
    {
    private CommonSimpleResultFilters() { /* Prevent instance instantiation. */ }

    /**True if the HDDMetrics are non-null. */
    public static final Predicate<ETVPerHouseholdComputationResult> hasHDDMetrics = 
        r -> (null != r.getHDDMetrics());
    }
