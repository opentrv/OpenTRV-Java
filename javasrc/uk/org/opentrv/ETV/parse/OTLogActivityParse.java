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

/**Process OpenTRV device log files for key activity..
 * This inspects valve (or valve controller, for split units) JSON log files for:
 * <ul>
 * <li>Calling for heat</li>
 * <li>Energy savings operating (or not, eg for controls).
 * </ul>
 * <p>
 * These are summarised in calendar days, local midnight to local midnight.
 * <p>
 * A couple of log format are supported:
 * <ul>
 * <li>The canonical [ timestamp, controller, map ] form (starts with [).</li>
 * <li>A partially-decrypted form used for data extraction during the ES1 trial (starts with ').</li>
 * </ul>
 */
public final class OTLogActivityParse
    {

    }
