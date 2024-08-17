/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package info.lahoda.nb.gh.pr.nodes;

import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

public class Common {

    public static final RequestProcessor WORKER = new RequestProcessor(PullRequestNode.class.getName(), 1, false, false);
    public static final Logger LOG = Logger.getLogger(PullRequestNode.class.getPackage().getName());
    public static final String PROP_USERNAME = "username";
    public static final String PROP_TOKEN = "oauthToken";
    public static final String PROP_LABELS = "labels";
    public static final String PROP_SHOW_ALL = "showAll";
    public static final String PROP_REFRESH_RATE = "labels";

    public static Preferences getPreferencesRoot() {
        return NbPreferences.forModule(PullRequestNode.class);
    }

    public static int getRefreshRate() {
        return getPreferencesRoot().getInt(PROP_REFRESH_RATE, 10 * 60 * 1000);
    }
    public static Preferences getRepositoryPreferences(String repositoryName) {
        return Common.getPreferencesRoot().node("repositories/" + repositoryName.replace('/', ':'));
    }
}
