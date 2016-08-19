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

package uk.org.opentrv.ETV.parse;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**Process OpenTRV device log files for key activity.
 * This contains methods to inspect valve (or valve controller, for split units)
 * JSON log files for:
 * <ul>
 * <li>Calling for heat.</li>
 * <li>Energy savings operating (or not, eg for controls).</li>
 * </ul>
 * <p>
 * These are summarised to calendar days, local midnight to local midnight.
 * <p>
 * A couple of (one-record-per-line) log formats are supported:
 * <ul>
 * <li>The canonical [ timestamp, controller, map ] form (starts with [).</li>
 * <li>A partially-decrypted form used for data extraction during the ES1 trial (starts with ').</li>
 * </ul>
 * In both case the log timestamps are expected to be UTC.
 * <p>
 * These logs are generally performed for one device at a time,
 * then merged given the relationships between those devices
 * (ie all those in one household).
 * <p>
 * These routines are intended to be reasonably robust,
 * eg in the face of possibly slightly mangled log data.
 * <p>
 * The canonical format (c) looks like:
<pre>
[ "2016-08-18T23:57:15Z", "", {"@":"2d1a","+":5,"O":1,"vac|h":1,"b":0,"L":0,"v|%":0} ]
[ "2016-08-18T23:57:28Z", "", {"@":"0d49","+":2,"tT|C":6,"vC|%":646,"T|C16":405} ]
[ "2016-08-18T23:58:28Z", "", {"@":"0d49","+":3,"O":1,"vac|h":3,"B|cV":259,"L":0} ]
[ "2016-08-18T23:59:05Z", "", {"@":"0a45","+":6,"B|cV":262,"L":0,"v|%":0,"tT|C":
</pre>
 * <p>
 * The partially-decrypted (pd) form looks like:
<pre>
'2016-05-12-11:21:45','111.11.11.1','cf 74 II II II II 20 0b 40 09 d8 59 0a e5 75 f3 13 57 a5 94 a2 3b e7 26 99 c4 5a 77 74 6a 6e 2c 5a c2 22 f6 b6 5e 0b 02 31 f2 09 45 57 d4 d9 92 3c 8e 45 95 63 65 5b a3 ff 2f 3d 68 14 80','b''\x00\x10{"tT|C":21,"tS|C":1''','\x00\x10{"tT|C":21,"tS|C":1'
</pre>
 */
public final class OTLogActivityParse
    {
    /**Result of parsing one valve (controller) log.
     * There are several Sets of local calendar days
     * (Integer YYYYMMDD values, from local midnight to local midnight in the household's timezone)
     * indicating for example days in which there was log data
     * and days in which energy savings were being applied (eg temperature setbacks).
     */
    public static interface ValveLogParseResult
        {
        /**Contains an entry for any day for which log data was found; never null but may be empty. */
        Set<Integer> getDaysInWhichDataPresent();
        /**Contains an entry for any day for which evidence of calling for heat was found; never null but may be empty. */
        Set<Integer> getDaysInWhichCallingForHeat();
        /**Contains an entry for any day for which evidence of the energy saving features being enables and/or operating was found; never null but may be empty. */
        Set<Integer> getDaysInWhichEnergySavingActive();
        }

    /**Parses 'c' and 'pd' format valve log files; never null.
     * Quite crude and so is able to parse either format.
     * <p>
     * Intended to be reasonably robust.
     *
     * @param r  line-oriented log file as reader, not closed by routine; never null
     * @param localTimeZoneForDayBoundaries  timezone for household; never null
     */
    public static ValveLogParseResult parseValveLog(final Reader r, final TimeZone localTimeZoneForDayBoundaries) throws IOException
        {
        if(null == r) { throw new IllegalAccessError(); }
        if(null == localTimeZoneForDayBoundaries) { throw new IllegalAccessError(); }

        final Set<Integer> daysInWhichDataPresent = new HashSet<>();
        final Set<Integer> daysInWhichCallingForHeat = new HashSet<>();
        final Set<Integer> daysInWhichEnergySavingActive = new HashSet<>();

        // TODO


        return(new ValveLogParseResult(){
            @Override public Set<Integer> getDaysInWhichDataPresent() { return(daysInWhichDataPresent); }
            @Override  public Set<Integer> getDaysInWhichCallingForHeat() { return(daysInWhichCallingForHeat); }
            @Override public Set<Integer> getDaysInWhichEnergySavingActive() { return(daysInWhichEnergySavingActive); }
            });
        }
    }
