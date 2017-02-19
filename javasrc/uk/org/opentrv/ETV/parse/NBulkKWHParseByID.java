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
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationInputKWh;

/**Get heating fuel energy consumption (kWh) by whole local days (local midnight-to-midnight) from bulk data.
 * Days may not be contiguous.
 * <p>
 * Extracts from bulk data from the secondary meter provider.
 * <p>
 * The ID to extract data for has to be supplied to the constructor.
 * <p>
 * Format sample, initial few lines:
<pre>
house_id,received_timestamp,device_timestamp,energy,temperature
1002,1456790560,1456790400,306.48,-3
1002,1456791348,1456791300,306.48,-3
1002,1456792442,1456792200,306.48,-3
</pre>
 * <p>
 * No assumption is made about ordering by house_id,
 * but values for a given house ID must be in rising order by device_timestamp.
 * <p>
 * <ul>
 * <li>energy is assumed to be in kWh (cumulative)</li>
 * <li>device_timestamp is assumed to be in UTC seconds</li>
 * </ul>
 * <p>
 * Allows/skips header rows after the first,
 * eg to allow data files to be concatenated by date.
 */
public final class NBulkKWHParseByID implements ETVPerHouseholdComputationInputKWh
    {
    /**Default time zone assumed for this data for UK based homes. */
    public static final TimeZone DEFAULT_NB_TIMEZONE = TimeZone.getTimeZone("Europe/London");

    /**Maximum number of minutes tolerance (before local midnight) to accept reading; range [1,59].
     * Note that even reading mid-evening once per day or once per week is probably OK too!
     * But given the nature of this data we can insist on a better fit to HDD data.
     */
    public static final int EPSILON_MIN = 30;

    /**Extract set of all distinct (non-negative integer) IDs in the supplied data.
     * @param  r  bulk input data, close()d by by this routine on completion; never null
     * @return set of distinct non-negative house IDs; may be empty but never null
     */
    public static final Set<Integer> extractIDs(final Reader r) throws IOException
        {
        if(null == r) { throw new IllegalArgumentException(); }

        final Set<Integer> result = new HashSet<>();

        // Wrap with a by-line reader and arrange to close() when done...
        try(final LineNumberReader l = new LineNumberReader(r))
            {
            final String header = l.readLine();
            if(null == header) { throw new IOException("missing header row"); }
            final String hf[] = header.split(",");
            if(hf.length < 5) { throw new IOException("too few fields in header row"); }
            if((hf[0].length() > 0) && (Character.isDigit(hf[0].charAt(0)))) { throw new IOException("leading numeric not text in header row"); }

            // Read data rows just to extract the house ID [0].
            String row;
            while(null != (row = l.readLine()))
                {
                // Allow extra header lines/rows to be skipped silently.
                if(row.startsWith("house_id,")) { continue; }
                final String rf[] = row.split(",");
                if(rf.length < 4) { throw new IOException("too few fields in row " + l.getLineNumber()); }
                result.add(Integer.parseInt(rf[0], 10));
                }
            };

        return(result);
        }

    /**House/meter ID to filter for; +ve. */
    private final int meterID;

    /**Reader for CSV; never null but may be closed. */
    private final Reader r;

    /**Time zone of house; never null. */
    private final TimeZone tz;

    /**Create instance with the house/meter ID to filter for and CSV input Reader.
     * Reader will be closed by getKWHByLocalDay()
     * so this is one shot and a new instance of this class
     * is needed if the data is to be read again.
     */
    public NBulkKWHParseByID(final int meterID, final Reader r, final TimeZone tz)
        {
        if(meterID < 0) { throw new IllegalArgumentException(); }
        if(null == r) { throw new IllegalArgumentException(); }
        if(null == tz) { throw new IllegalArgumentException(); }
        this.meterID = meterID;
        this.r = r;
        this.tz = tz;
        }

    /**Create instance with the house/meter ID to filter for and CSV input Reader and default UK time zone.
     * Reader will be closed by getKWHByLocalDay()
     * so this is one shot and a new instance of this class
     * is needed if the data is to be read again.
     */
    public NBulkKWHParseByID(final int meterID, final Reader r)
        { this(meterID, r, DEFAULT_NB_TIMEZONE); }

    /**Interval heating fuel consumption (kWh) by whole local days; never null.
     * @return  never null though may be empty
     * @throws IOException  in case of failure, eg parse problems
     */
    @Override public SortedMap<Integer, Float> getKWhByLocalDay() throws IOException
        {
        // Read first line, usually:
        //     house_id,received_timestamp,device_timestamp,energy,temperature
        // Simply check that the header exists, has (at least) 5 fields, and does not start with a digit.
        // An empty file is not acceptable (ie indicates a problem).

        final SortedMap<Integer, Float> result = new TreeMap<>();

        // Wrap with a by-line reader and arrange to close() when done...
        try(final LineNumberReader l = new LineNumberReader(r))
            {
            final String header = l.readLine();
            if(null == header) { throw new IOException("missing header row"); }
            final String hf[] = header.split(",");
            if(hf.length < 5) { throw new IOException("too few fields in header row"); }
            if((hf[0].length() > 0) && (Character.isDigit(hf[0].charAt(0)))) { throw new IOException("leading numeric not text in header row"); }

            // Avoid multiple parses of ID; check with a String comparison.
            final String sID = Integer.toString(meterID);

            // Read data rows...
            // Filter by house ID [0], use device_timestamp [2] and energy [3].
            // This will need to accumulate energy for an entire day in the local time zone,
            // taking the last value from the previous day from the last value for the current day,
            // both values needing to be acceptably close to (ie possibly just after) midnight.
            String row;
            int currentDayYYYYMMDD = -1;
            Float kWhAtStartOfCurrentDay = null;
            final Calendar latestDeviceTimestamp = Calendar.getInstance(tz);
            latestDeviceTimestamp.setTime(new Date(0));
            while(null != (row = l.readLine()))
                {
                // Allow extra header lines/rows to be skipped silently.
                if(row.startsWith("house_id,")) { continue; }
                // Now split into columns.
                final String rf[] = row.split(",");
                if(rf.length < 4) { throw new IOException("too few fields in row " + l.getLineNumber()); }
                if(!sID.equals(rf[0])) { continue; }
                final long device_timestamp = Long.parseLong(rf[2], 10);
                final float energy = Float.parseFloat(rf[3]);
                final long dtsms = 1000L * device_timestamp;
                // Verify that device time moves monotonically forwards...
                // Allow a repeated device timestamp (omit repeat row).
                // ASSUME that the value has not changed for now, eg:
//                X,1481277863,1481277600,5184.53,11
//                X,1481280530,1481278500,5184.53,10
//                X,1481280553,1481278500,5184.53,10
//                X,1481280792,1481279400,5184.53,11
                final long lDTm;
                if((-1 != currentDayYYYYMMDD) && (dtsms <= (lDTm = latestDeviceTimestamp.getTimeInMillis())))
                    {
                	// Ignore/skip repeat/duplicate device timestamp.
                	if(dtsms == lDTm)
                	    {
                		System.err.println("WARNING: duplicate device timestamp at row " + l.getLineNumber() + ": " + row);
                		continue;
                		}
                	throw new IOException("device time gone backwards at row " + l.getLineNumber() + ": " + row + " vs " + latestDeviceTimestamp);
                	}
                // Now convert to local date (and time) allowing for time zone.
                // Measurement days are local midnight to local midnight.
                latestDeviceTimestamp.setTimeInMillis(dtsms);
                final int todayYYYYMMDD =
                    (latestDeviceTimestamp.get(Calendar.YEAR)*10000) +
                    ((latestDeviceTimestamp.get(Calendar.MONTH)+1)*100) +
                    (latestDeviceTimestamp.get(Calendar.DAY_OF_MONTH));
                final boolean newDay = (todayYYYYMMDD != currentDayYYYYMMDD);
                if(newDay)
                    {
                    // Rolled directly to following day with no gap?
//                    final boolean followingDay = ((-1 != currentDayYYYYMMDD) &&
//                        (((currentDayYYYYMMDD+1) == todayYYYYMMDD) ||
//                        (1 == Util.daysBetweenDateKeys(currentDayYYYYMMDD, todayYYYYMMDD))));
                    // Sufficiently close to start of day to treat as midnight
                    // for computing a day interval energy consumption?
                    final boolean closeEnoughToStartOfDay =
                       ((0 == latestDeviceTimestamp.get(Calendar.HOUR_OF_DAY)) &&
                        (EPSILON_MIN >= latestDeviceTimestamp.get(Calendar.MINUTE)));
                    if(!closeEnoughToStartOfDay)
                        {
                        // If not close enough to use, just null the 'start of day' reading.
                        kWhAtStartOfCurrentDay = null;
                        }
                    else
                        {
                        // If the start-of-day value is present then compute the interval.
                        if(null != kWhAtStartOfCurrentDay)
                            {
                            final float dayUse = energy - kWhAtStartOfCurrentDay;
                            result.put(currentDayYYYYMMDD, dayUse);
                            }
                        kWhAtStartOfCurrentDay = energy;
                        }
                    // In any case, note the new day.
                    currentDayYYYYMMDD = todayYYYYMMDD;
                    }

//                final SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd-HH:mm");
//                fmt.setCalendar(latestDeviceTimestamp);
//                final String dateFormatted = fmt.format(latestDeviceTimestamp.getTime());
//System.out.println(dateFormatted);
                }
            }

        return(result);
        }

    }
