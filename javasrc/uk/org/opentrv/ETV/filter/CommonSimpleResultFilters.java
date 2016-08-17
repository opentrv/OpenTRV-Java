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

    /**Minimum typically-acceptable (non-NaN) value for R^2 for daily-sampled data.
     * This is lower than for weekly-sampled data for typical households.
     */
    public static final float MIN_RSQAURED_DAILY_DATA = 0.15f;
    /**True if the HDDMetrics R^2 value is non-NaN and reasonable for daily-sampled data. */
    public static final Predicate<ETVPerHouseholdComputationResult> isOKDailyRSq =
        r -> ((null != r.getHDDMetrics()) && (r.getHDDMetrics().rsqFit >= MIN_RSQAURED_DAILY_DATA));

    /**Minimum acceptable samples (n) for daily-sampled data for one data subset (control or normal).
     * Use twice this for pre-segmented data.
     */
    public static final int MIN_N_DAILY_DATA = 7;
    /**True if the HDDMetrics n value is reasonable for daily-sampled data subset. */
    public static final Predicate<ETVPerHouseholdComputationResult> isEnoughPointsSubset =
        r -> ((null != r.getHDDMetrics()) && (r.getHDDMetrics().n >= MIN_N_DAILY_DATA));
    /**True if the HDDMetrics n value is reasonable for daily-sampled control+normal. */
    public static final Predicate<ETVPerHouseholdComputationResult> isEnoughPointsControlAndNormal =
        r -> ((null != r.getHDDMetrics()) && (r.getHDDMetrics().n >= (2*MIN_N_DAILY_DATA)));

    /**Common result filter combo before segmentation to control/normal for daily-sampled data.
     * Combines common set of noise-rejection predicates.
     * <p>
     * Rejects items with no HDD metrics at all
     * or with very poor or NaN r^2 by daily-data standards.
     */
    public static final Predicate<ETVPerHouseholdComputationResult> goodDailyDataResults =
        hasHDDMetrics.and(isEnoughPointsControlAndNormal).and(isOKDailyRSq);
    }
