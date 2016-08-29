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
import java.io.LineNumberReader;
import java.io.Reader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationSystemStatus;
import uk.org.opentrv.ETV.filter.StatusSegmentation;
import uk.org.opentrv.hdd.HDDUtil;

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
     * indicating for example days in which there was any log data
     * and days in which energy savings were reported (on or off) and being applied (eg temperature setbacks).
     */
    public static interface ValveLogParseResult
        {
        /**Contains an entry for any day for which log data was found; never null but may be empty. */
        Set<Integer> getDaysInWhichDataPresent();
        /**Contains an entry for any day for which evidence of calling for heat was found; never null but may be empty. */
        Set<Integer> getDaysInWhichCallingForHeat();
        /**Contains an entry for any day for which evidence of the energy saving features being reported, on or off, was found; never null but may be empty. */
        Set<Integer> getDaysInWhichEnergySavingStatsReported();
        /**Contains an entry for any day for which evidence of the energy saving features being enabled and/or operating was found; never null but may be empty.
         * Subset of getDaysInWhichEnergySavingStatsReported(), may be equal.
         */
        Set<Integer> getDaysInWhichEnergySavingActive();
        }

//    /**UTC (GMT) timezone. */
//    private static final TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");

    /**Valve open percentage field name in log files. */
    public static final String FIELD_VALVE_PC_OPEN = "v|%";

    /**Temperature setback Celsius field name in log files. */
    public static final String FIELD_TEMP_SETBACK_C = "tS|C";

    /**Valve open (non-zero percentage) regex. */
    public static final Pattern REGEX_VALVE_PC_OPEN = Pattern.compile(".*\"v\\|%\":[1-9].*");

    /**Temperature setback (present, any non-negative value) regex. */
    public static final Pattern REGEX_TEMP_SETBACK_REPORTED = Pattern.compile(".*\"tS\\|C\":[0-9].*");

    /**Temperature setback (+ve non-zero degrees) regex. */
    public static final Pattern REGEX_TEMP_SETBACK_C = Pattern.compile(".*\"tS\\|C\":[1-9].*");

    /**Parses 'c' and 'pd' format valve log files; never null.
     * Intended to parse/interpret the logs of TRV1 valves, eg software released ~2016H1.
     * <p>
     * Quite crude and so is able to parse either format.
     * <p>
     * Intended to be reasonably robust.
     * <p>
     * Uses timestamp to work out daysInWhichDataPresent.
     * <p>
     * Uses tS|C > 0 to detect automated setback and thus daysInWhichEnergySavingActive.
     * <p>
     * Uses presence of tS|C (of any value) to detect setback reporting and thus daysInWhichEnergySavingStatsReported.
     * Can be used as an alternative to daysInWhichDataPresent
     * to validate days where tS|C is available as a metric.
     * <p>
     * Uses v|% > 0 to detect call for heat and thus daysInWhichCallingForHeat.
     *
     * @param r  line-oriented log file as reader, not closed by routine; never null
     * @param localTimeZoneForDayBoundaries  timezone for household; never null
     */
    public static ValveLogParseResult parseTRV1ValveLog(final Reader r, final TimeZone localTimeZoneForDayBoundaries) throws IOException
        {
        if(null == r) { throw new IllegalAccessError(); }
        if(null == localTimeZoneForDayBoundaries) { throw new IllegalAccessError(); }

        final Set<Integer> daysInWhichDataPresent = new HashSet<>();
        final Set<Integer> daysInWhichCallingForHeat = new HashSet<>();
        final Set<Integer> daysInWhichEnergySavingStatsReported = new HashSet<>();
        final Set<Integer> daysInWhichEnergySavingActive = new HashSet<>();

        final LineNumberReader lr = new LineNumberReader(r);

        String line;
        while(null != (line = lr.readLine()))
            {
            // Quietly ignore blank lines.
            if(0 == line.length()) { continue; }

            // Crudely deduce the line type from the first character.
            final char firstChar = line.charAt(0);
            final boolean isCanon = ('[' == firstChar);
            if(!isCanon && ('\'' != firstChar))
                {
                System.err.println("Unrecognised valve log line type at line "+lr.getLineNumber()+ ": skipping");
                continue;
                }

            // Parse the (UTC) timestamp to get the underlying time.
            // Also extract the JSON stats map/object.
            final long time;
            final JSONObject leafObject;
            boolean valveOpen = false;
            boolean tSCPresent = false;
            boolean tempSetback = false;
            if(isCanon)
                {
                // Parse the input and prepare the new string output.
                final Object o = JSONValue.parse(line); // FIXME: use retained parser for efficiency.
                if(!(o instanceof JSONArray)) { System.err.println("input line is not a JSON array: " + line); continue; }
                final JSONArray array = (JSONArray)o;
                if(3 != array.size()) { System.err.println("input line JSON array has wrong number of elements: " + line); continue; }
                if(!(array.get(0) instanceof String)) { System.err.println("input line timestamp ([0]) is not a string: " + line); continue; }
                final String timeStamp = (String) array.get(0);
                if(!(array.get(2) instanceof JSONObject)) { System.err.println("input line leaf JSON ([2]) is not an object/map: " + line); continue; }
                leafObject = (JSONObject)array.get(2);
                // Parse the timestamp...
                final Instant instant = Instant.parse(timeStamp);
                time = instant.getEpochSecond() * 1000L;
                // Check the important fields.
                final Object pFv = leafObject.get(FIELD_VALVE_PC_OPEN);
                if((pFv instanceof Number) && (((Number)pFv).intValue() > 0)) { valveOpen = true; }
                final Object pFs = leafObject.get(FIELD_TEMP_SETBACK_C);
                // Slight optimisation handling tSC values...
                final boolean pFsIsNumber = pFs instanceof Number;
                if(pFsIsNumber) { tSCPresent = true; }
                if(pFsIsNumber && (((Number)pFs).intValue() > 0)) { tempSetback = true; }
                }
            else
                {
                // Parse the timestamp at start of line:
                //    '2016-05-12-11:21:45'
                if('\'' != line.charAt(20))
                    {
                    System.err.println("Bad timestamp at line "+lr.getLineNumber()+ ": skipping");
                    continue;
                    }
                // Convert to UTC format for parsing (ASSUMES TIMESTAMP IS UTC).
                final String timeStamp = line.substring(1, 11) + 'T' + line.substring(12, 20) + 'Z';
                final Instant instant = Instant.parse(timeStamp);
                time = instant.getEpochSecond() * 1000L;
                // Look for the appropriate (non-zero) fields with regexes.
                // This relies on the matches being sufficiently specific to not match anything unwanted.
                if(REGEX_VALVE_PC_OPEN.matcher(line).matches()) { valveOpen = true; }
                // Slight optimisation handling tSC values...
                final boolean tSCp = REGEX_TEMP_SETBACK_REPORTED.matcher(line).matches();
                if(tSCp) { tSCPresent = true; }
                if(tSCp && REGEX_TEMP_SETBACK_C.matcher(line).matches()) { tempSetback = true; }
                }

            // Extract date for household's local timezone.
            final Calendar cal = Calendar.getInstance(localTimeZoneForDayBoundaries);
            cal.setTime(new Date(time));
            final Integer key = HDDUtil.keyFromDate(cal);
            daysInWhichDataPresent.add(key);

            if(valveOpen) { daysInWhichCallingForHeat.add(key); }
            if(tSCPresent) { daysInWhichEnergySavingStatsReported.add(key); }
            if(tempSetback) { daysInWhichEnergySavingActive.add(key); }
            }

        return(new ValveLogParseResult(){
            @Override public Set<Integer> getDaysInWhichDataPresent() { return(daysInWhichDataPresent); }
            @Override public Set<Integer> getDaysInWhichCallingForHeat() { return(daysInWhichCallingForHeat); }
            @Override public Set<Integer> getDaysInWhichEnergySavingStatsReported() { return(daysInWhichEnergySavingStatsReported); }
            @Override public Set<Integer> getDaysInWhichEnergySavingActive() { return(daysInWhichEnergySavingActive); }
            });
        }

    /**Relative path within log data directory to grouping CSV file. */
    public static final String LOGDIR_PATH_TO_GROUPING_CSV = "grouping.csv";

    /**Read/parse (as a list of records) the grouping CSV file that names devices and groups them into households; never null but may be empty.
     * Closes the Reader when finished.
     *
     * @throws  IOException if file cannot be read
     */
    private static List<String> loadGroupingCSVAsList(final Function<String, Reader> dataReader)
        throws IOException
        {
        try(LineNumberReader lr = new LineNumberReader(dataReader.apply(LOGDIR_PATH_TO_GROUPING_CSV)))
            {
            final ArrayList<String> result = new ArrayList<>();
            String line;
            while(null != (line = lr.readLine()))
                {
                result.add(line);
                }
            result.trimToSize();
            return(Collections.unmodifiableList(result));
            }
        }

    /**Read/parse (as a map from household to a Set of its valves/devices) the grouping CSV file; never null but may be empty.
     * Format:
     * <pre>
HouseID,deviceID,secondaryDeviceID(2ndaryIDOfHouseIfDeviceIDBlank)
5013,,16WW
5013,2d1a
5013,414a
5013,0d49
5013,0a45
5013,3015
S001,,synthetic1
S001,synthd
     * </pre>
     * <p>
     * No valve can belong to more than one household, though this is not yet verified.
     * <p>
     * Closes the Reader when finished.
     *
     * @throws  IOException if file cannot be read or parsed
     */
    public static Map<String, Set<String>> loadGroupingCSVAsMap(final Function<String, Reader> dataReader)
        throws IOException
        {
        final List<String> rl = loadGroupingCSVAsList(dataReader);
        final Map<String, Set<String>> result = new HashMap<>();

        // Skip header row if present.
        final List<String> l = (rl.get(0).startsWith("HouseID,") ? rl.subList(1, rl.size()) : rl);

        // Collect valves by household.
        for(final String record : l)
            {
            final String cols[] = record.split(",");
            if((cols.length < 2) || (cols.length > 3)) { throw new IOException("bad cols, expecting 2 or 3: " + record); }
            final String houseID = cols[0];
            if("".equals(houseID)) { throw new IOException("bad (empty) houseID: " + record); }
            final String valveID = cols[1];
            // Ignore row with no valve ID.
            if("".equals(valveID)) { continue; }
            // Create appropriate entry.
            if(!result.containsKey(houseID)) { result.put(houseID, new HashSet<String>()); }
            result.get(houseID).add(valveID);
            }

        // Check that no valve was associated with more than one household.
        // Union size should be sum of all constituent sets, ie no overlap.
        int nPerHousehold = 0;
        final Set<String> unionValveIDs = new HashSet<>();
        for(final Set<String> h : result.values())
            {
            nPerHousehold += h.size();
            unionValveIDs.addAll(h);
            }
        if(unionValveIDs.size() != nPerHousehold)
            { throw new IOException("bad groupings, at least one valve in more than one household"); }

        return(result);
        }

    /**Find and analyse the log if any for the given valve (identified by primary or secondary ID); null if not found.
     * @param dataReader  smart reader Function; never null
     * @param valvePrimaryID  primary valve ID; never null
     * @param valveSecondaryID  secondary valve ID; may be null
     * @return  result of analysis, or null if no data could be found
     */
    public static ValveLogParseResult findAndAnalyseLog(final Function<String, Reader> dataReader, final TimeZone localTimeZoneForDayBoundaries, final String valvePrimaryID, final String valveSecondaryID)
        {
        if(null == dataReader) { throw new IllegalArgumentException(); }
        if(null == valvePrimaryID) { throw new IllegalArgumentException(); }
        if(null == localTimeZoneForDayBoundaries) { throw new IllegalArgumentException(); }

        // Try all combinations of name with and without compression
        // for single-file format.
        final String[] endings = { "blah", "json.gz", "dlog.gz", "json", "dlog" };
        for(final String e : endings)
            {
            final String filename = valvePrimaryID + "." + e;
            try {
                // Stop as soon as one succeeds.
                try(final Reader r = dataReader.apply(filename))
                    { return(parseTRV1ValveLog(r, localTimeZoneForDayBoundaries)); }
                }
            catch(final Exception e1) { /* ignore */ }
            }

        // TODO: read from multi-device dump if no stand-alone file found...

        return(null); // Not found.
        }

    /**Load an analyse a household's devices together.
     * Loads the per-device data,
     * does the initial parse per device,
     * combines and returns the result.
     * <p>
     * Note: this silently omits any device for which no data at all is found,
     * which may have a bearing on (say) quora/majority calculations when combining.
     *
     * @return  the combined result
     */
    public static ETVPerHouseholdComputationSystemStatus analyseHouseLogs(final Function<String, Reader> dataReader, final TimeZone localTimeZoneForDayBoundaries, final Collection<String> devices)
        {
        // Per-device log parse results.
        final List<ValveLogParseResult> perDevice = new ArrayList<>(devices.size());

        for(final String valveID : devices)
            {
            final ValveLogParseResult vlpr = findAndAnalyseLog(dataReader, localTimeZoneForDayBoundaries, valveID, null);
            // Silently OMIT any device for which there is no data at all.
            if(null != vlpr) { perDevice.add(vlpr); }
            }

        return(StatusSegmentation.segmentActivity(perDevice));
        }

    /**Read/parse an entire set of log records and produce per-household sets of dates for segmentation and analysis; never null but may be empty.
     * Passes a null restrictToHouseholds value, thus loading data for all households.
     */
    public static Map<String, ETVPerHouseholdComputationSystemStatus> loadAndParseAllOTLogs(final Function<String, Reader> dataReader, final TimeZone localTimeZoneForDayBoundaries)
        throws IOException
        { return(loadAndParseAllOTLogs(dataReader, localTimeZoneForDayBoundaries, null)); }

    /**Read/parse an entire set of log records and produce per-household sets of dates for segmentation and analysis; never null but may be empty.
     * Given a Functor that takes relative path name and returns a Reader of line-oriented records:
     * <ol>
     * <li>Read the grouping CSV file that names devices and groups them into households.</li>
     * <li>Read and parse each device log file, trying various locations for the data.</li>
     * <li>Collate the data into households and work out which days are usable,
     *     and which have energy-saving features enabled or not (the latter being controls).</li>
     * </ol>
     * <p>
     * A day may not be usable if, for example,
     * only a minority of devices are reporting energy-saving status,
     * or valves are reporting an intermediate mixture of enabled and disabled status.
     *
     * @param localTimeZoneForDayBoundaries  timezone for all households in dataset; never null
     * @param restrictToHouseholds  if not null, ignore households not included in this set;
     *     can cut down processing time on households already filtered from consideration
     * @return  map from house ID to ETVPerHouseholdComputationSystemStatus;
     *     never null but may be empty
     */
    public static Map<String, ETVPerHouseholdComputationSystemStatus> loadAndParseAllOTLogs(final Function<String, Reader> dataReader, final TimeZone localTimeZoneForDayBoundaries, final Set<String> restrictToHouseholds)
        throws IOException
        {
        // Load groupings: abort with exception if not possible.
        final Map<String, Set<String>> gm = loadGroupingCSVAsMap(dataReader);

        final Map<String, ETVPerHouseholdComputationSystemStatus> result = new HashMap<>(gm.size() * 2);

        // House by house, load parse, and analyse logs as a group.
        // Create a segmented view for the household as a whole.
        for(final String houseID : gm.keySet())
            {
            if((null != restrictToHouseholds) && !restrictToHouseholds.contains(houseID)) { continue; }
            final ETVPerHouseholdComputationSystemStatus houseStatus = analyseHouseLogs(dataReader, localTimeZoneForDayBoundaries, gm.get(houseID));
            result.put(houseID, houseStatus);
            }

        return(result);
        }
    }
