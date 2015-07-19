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

Author(s) / Copyright (s): Damon Hart-Davis 2015
*/
package uk.org.opentrv.comms.statshandlers.builtin;

import java.io.IOException;
import java.util.Map;

import uk.org.opentrv.comms.statshandlers.StatsHandler;
import uk.org.opentrv.comms.statshandlers.StatsMessageWithMetadata;


/**Minimal StatsHandler that simply remembers the last string value(s) passed. */
public final class DummyStatsHandler implements StatsHandler
    {
    /**Last remote stats received or null of none. */
    private StatsMessageWithMetadata lastStatsMessageWithMetadata;

    /**Empty constructor. */
    public DummyStatsHandler() {}

    /**Config aware constructor. */
    public DummyStatsHandler(final Map config) {}
    
    /**Get last remote stats received or null of none. */
    public StatsMessageWithMetadata getLastStatsMessageWithMetadata() { return(lastStatsMessageWithMetadata); }

    @Override public void processStatsMessage(final StatsMessageWithMetadata swmd) throws IOException
        { lastStatsMessageWithMetadata = swmd; }
    }
