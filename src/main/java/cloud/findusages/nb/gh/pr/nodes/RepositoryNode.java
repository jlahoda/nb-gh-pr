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
package cloud.findusages.nb.gh.pr.nodes;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.Action;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.openide.actions.DeleteAction;
import org.openide.actions.PropertiesAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor.Task;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;

public final class RepositoryNode extends AbstractNode {

    private final String repositoryName;
    private final Action[] actions;

    public RepositoryNode(String username, GitHub gh, String repositoryName) {
        super(new PullRequestsChildren(username, gh, repositoryName), Lookups.fixed());
        setDisplayName(repositoryName);
        setIconBaseWithExtension("cloud/findusages/nb/gh/pr/resources/repo-16.png");
        this.repositoryName = repositoryName;
        actions = new Action[]{SystemAction.get(DeleteAction.class), null, PropertiesAction.get(PropertiesAction.class)};
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public void destroy() throws IOException {
        try {
            Common.getRepositoryPreferences(repositoryName).removeNode();
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public Action[] getActions(boolean context) {
        return actions;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set ghSettings = new Sheet.Set();
        ghSettings.setName("RepositoryFilter");
        ghSettings.setDisplayName("Repository Filtering");
        ghSettings.put(new PropertySupport.ReadWrite<String>("labels", String.class, "Labels", "A comma-separated list of pull request labels. Pull requests having at least one of these labels will be shown.") {
            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                return Common.getRepositoryPreferences(repositoryName).get(Common.PROP_LABELS, "");
            }

            @Override
            public void setValue(String val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                Common.getRepositoryPreferences(repositoryName).put(Common.PROP_LABELS, val);
            }
        });
        ghSettings.put(new PropertySupport.ReadWrite<Boolean>("enableAll", Boolean.class, "Enable All", "List all pull requests in the given repository") {
            @Override
            public Boolean getValue() throws IllegalAccessException, InvocationTargetException {
                return Common.getRepositoryPreferences(repositoryName).getBoolean(Common.PROP_SHOW_ALL, false);
            }

            @Override
            public void setValue(Boolean val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                Common.getRepositoryPreferences(repositoryName).putBoolean(Common.PROP_SHOW_ALL, val);
            }
        });
        sheet.put(ghSettings);
        return sheet;
    }

    private static final class PullRequestsChildren extends Children.Keys<PullRequestKey> implements PreferenceChangeListener {

        private static final PullRequestKey FAKE_WAIT_KEY = new PullRequestKey(-1);
        private final String username;
        private final GitHub gh;
        private final String repositoryName;
        private final Preferences repositorySettings;
        private final Task refeshTask;
        private final AtomicBoolean visible = new AtomicBoolean();
        private final java.util.Map<Integer, PullRequestKey> prNumber2Key = new HashMap<>();
        private List<PullRequestKey> currentPullRequests = Collections.emptyList();

        public PullRequestsChildren(String username, GitHub gh, String repositoryName) {
            super(true);
            this.username = username;
            this.gh = gh;
            this.repositoryName = repositoryName;
            this.repositorySettings = Common.getRepositoryPreferences(repositoryName);
            this.refeshTask = Common.WORKER.create(() -> {
                if (!visible.get()) {
                    prNumber2Key.clear();
                    doSetKeys(Collections.emptyList());
                    return;
                }
                try {
                    List<PullRequestKey> augmentedKeys = new ArrayList<>(currentPullRequests);
                    augmentedKeys.add(FAKE_WAIT_KEY);
                    setKeys(augmentedKeys);
                    GHRepository repository = gh.getRepository(repositoryName);
                    List<PullRequestKey> filteredPullRequest = new ArrayList<>();
                    for (GHPullRequest pr : repository.getPullRequests(GHIssueState.OPEN)) {
                        if (matchesSettings(pr)) {
                            PullRequestKey key = prNumber2Key.computeIfAbsent(pr.getNumber(), n -> new PullRequestKey(n));

                            key.setPullRequest(pr);
                            filteredPullRequest.add(key);
                        }
                    }
                    doSetKeys(filteredPullRequest);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                PullRequestsChildren c = this;
                int refreshRate = Common.getRefreshRate();
                if (refreshRate >= 1000) {
                    c.refeshTask.schedule(refreshRate);
                }
            });
            repositorySettings.addPreferenceChangeListener(this);
        }

        private void doSetKeys(List<PullRequestKey> pullRequests) {
            this.currentPullRequests = pullRequests;
            setKeys(pullRequests);
        }

        @Override
        @Messages("DN_Refreshing=Refreshing Pull Requests")
        protected Node[] createNodes(PullRequestKey key) {
            Node result;

            if (key == FAKE_WAIT_KEY) {
                result = new WaitNode(Bundle.DN_Refreshing());
            } else {
                result = new PullRequestNode(gh, repositoryName, key);
            }

            return new Node[]{result};
        }

        @Override
        protected void addNotify() {
            visible.set(true);
            refeshTask.schedule(0);
        }

        @Override
        protected void removeNotify() {
            visible.set(false);
            refeshTask.schedule(0);
        }

        @Override
        public void preferenceChange(PreferenceChangeEvent evt) {
            refeshTask.schedule(500);
        }

        private boolean matchesSettings(GHPullRequest pr) throws IOException {
            if (Common.getRepositoryPreferences(repositoryName).getBoolean(Common.PROP_SHOW_ALL, false)) {
                return true;
            }
            String labelSettings = repositorySettings.get(Common.PROP_LABELS, "");
            Set<String> filterLabels = new HashSet<>(Arrays.asList(labelSettings.split(", *")));
            for (GHLabel label : pr.getLabels()) {
                if (filterLabels.contains(label.getName())) {
                    return true;
                }
            }
            return pr.getRequestedReviewers().stream().anyMatch(user -> username.equals(user.getLogin()));
        }
    }

}
