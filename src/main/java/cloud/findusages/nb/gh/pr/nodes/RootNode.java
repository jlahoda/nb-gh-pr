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

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.kohsuke.github.GitHub;
import org.netbeans.api.keyring.Keyring;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.InputLine;
import org.openide.actions.PropertiesAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor.Task;
import org.openide.util.Union2;
import org.openide.util.actions.SystemAction;

public final class RootNode extends AbstractNode {

    private final Action[] actions;

    public RootNode() {
        super(new RootChildren());
//            Task[] refresh = new Task[1];
//            refresh[0] = WORKER.create(() -> {
//                try {
//                    GHRateLimit rl = gh.getRateLimit();
//                    StatusDisplayer.getDefault().setStatusText("Limit: " + rl.getRemaining() + "/" + rl.getLimit());
//                } catch (IOException ex) {
//                    Exceptions.printStackTrace(ex);
//                }
//                refresh[0].schedule(10*1000);
//            });
//            refresh[0].schedule(10*1000);
        actions = new Action[]{new AddRepository(), SystemAction.get(PropertiesAction.class)};
    }

    @Override
    public Action[] getActions(boolean context) {
        return actions;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set ghSettings = new Sheet.Set();
        ghSettings.setName("GitHubSettings");
        ghSettings.setDisplayName("GitHub Settings");
        ghSettings.put(new PropertySupport.ReadWrite<String>("username", String.class, "GitHub username", "GitHub username") {
            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                return Common.getPreferencesRoot().get(Common.PROP_USERNAME, "");
            }

            @Override
            public void setValue(String val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                Common.getPreferencesRoot().put(Common.PROP_USERNAME, val);
            }
        });
        ghSettings.put(new PropertySupport.ReadWrite<String>("token", String.class, "GitHub Personal Access Token", "GitHub Personal Access Token") {
            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                char[] token = Keyring.read(Common.PROP_TOKEN);
                return token != null ? new String(token) : "";
            }

            @Override
            public void setValue(String val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                if (val.isEmpty()) {
                    Keyring.delete(Common.PROP_TOKEN);
                } else {
                    Keyring.save(Common.PROP_TOKEN, val.toCharArray(), "OAuth token for GitHub");
                }
                Common.getPreferencesRoot().putInt(Common.PROP_TOKEN_UPDATED, Common.getPreferencesRoot().getInt(Common.PROP_TOKEN_UPDATED, 0) + 1);
            }
        });
        ghSettings.put(new PropertySupport.ReadWrite<Integer>("refreshRate", Integer.class, "Refresh Rate", "Refresh rate in milliseconds. When the given number of milliseconds elapses, the pull requests for all the repositories will be refreshed. Use -1 to disable.") {
            @Override
            public Integer getValue() throws IllegalAccessException, InvocationTargetException {
                return Common.getRefreshRate();
            }

            @Override
            public void setValue(Integer val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                Common.getPreferencesRoot().getInt(Common.PROP_REFRESH_RATE, val);
            }
        });
        sheet.put(ghSettings);
        return sheet;
    }

    private static final class AddRepository extends AbstractAction {

        public AddRepository() {
            super("Add Repository");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            InputLine inp = new InputLine("Repository name: https://github.com/", "Add new repository");
            DialogDisplayer.getDefault().notifyFuture(inp).thenAccept(input -> {
                if (input.getValue() == NotifyDescriptor.OK_OPTION) {
                    Preferences repositories = Common.getPreferencesRoot().node("repositories");
                    String encodedName = input.getInputText().replace('/', ':');
                    try {
                        if (!repositories.nodeExists(encodedName)) {
                            repositories.node(encodedName).put(Common.PROP_LABELS, "");
                        }
                    } catch (BackingStoreException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            });
        }
    }

    private static final class RootChildren extends Children.Keys<Union2<RootChildren.RepositoryKey, Node>> implements PreferenceChangeListener, NodeChangeListener {

        private final Task updateKeys = Common.WORKER.create(() -> {
            String login = Common.getPreferencesRoot().get(Common.PROP_USERNAME, null);
            char[] token = Keyring.read(Common.PROP_TOKEN);
            List<Union2<RootChildren.RepositoryKey, Node>> keys = new ArrayList<>();
            boolean hasAccessTokens = false;
            boolean credentialsValid = false;
            if (login != null && !login.isEmpty() && token != null && token.length > 0) {
                hasAccessTokens = true;

                try {
                    GitHub gh = GitHub.connect(login, new String(token));
                    if (gh.isCredentialValid()) {
                        credentialsValid = true;

                        Preferences repositoriesNode = Common.getPreferencesRoot().node("repositories");
                        for (String repository : repositoriesNode.childrenNames()) {
                            keys.add(Union2.createFirst(new RepositoryKey(login, gh, repository.replace(':', '/'))));
                        }
                    }
                } catch (IOException | BackingStoreException ex) {
                    Common.LOG.log(Level.FINE, null, ex);
                }
            }
            if (!hasAccessTokens) {
                keys.add(Union2.createSecond(new ErrorWarningNode("No GitHub credential provided - please right click inside this window, select Properties and specify login and OAuth token.", true)));
            } else if (!credentialsValid) {
                keys.add(Union2.createSecond(new ErrorWarningNode("Provided credentials are not valid", true)));
            } else if (keys.isEmpty()) {
                keys.add(Union2.createSecond(new ErrorWarningNode("No GitHub repositories specified - please right click inside this window and select Add Repository.", false)));
            }
            setKeys(keys);
        });

        public RootChildren() {
        }

        @Override
        protected Node[] createNodes(Union2<RootChildren.RepositoryKey, Node> key) {
            return new Node[]{key.hasFirst() ? new RepositoryNode(key.first().username, key.first().gh, key.first().repositoryName)
                                             : key.second()};
        }

        @Override
        protected void addNotify() {
            Common.getPreferencesRoot().addPreferenceChangeListener(this);
            Common.getPreferencesRoot().node("repositories").addNodeChangeListener(this);
            updateKeys();
        }

        @Override
        protected void removeNotify() {
            //TODO: better synchronization (what if update is already scheduled/running?)
            Common.getPreferencesRoot().removePreferenceChangeListener(this);
            Common.getPreferencesRoot().node("repositories").removeNodeChangeListener(this);
            setKeys(Collections.emptyList());
        }

        private void updateKeys() {
            updateKeys.schedule(100);
        }

        @Override
        public void preferenceChange(PreferenceChangeEvent evt) {
            updateKeys();
        }

        @Override
        public void childAdded(NodeChangeEvent evt) {
            updateKeys();
        }

        @Override
        public void childRemoved(NodeChangeEvent evt) {
            updateKeys();
        }

        private static final class RepositoryKey {

            private final String username;
            private final GitHub gh;
            private final String repositoryName;

            public RepositoryKey(String username, GitHub gh, String repositoryName) {
                this.username = username;
                this.gh = gh;
                this.repositoryName = repositoryName;
            }

            @Override
            public int hashCode() {
                int hash = 3;
                hash = 37 * hash + Objects.hashCode(this.username);
                hash = 37 * hash + Objects.hashCode(System.identityHashCode(gh));
                hash = 37 * hash + Objects.hashCode(this.repositoryName);
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final RepositoryKey other = (RepositoryKey) obj;
                if (!Objects.equals(this.username, other.username)) {
                    return false;
                }
                if (!Objects.equals(this.repositoryName, other.repositoryName)) {
                    return false;
                }
                return this.gh == other.gh;
            }
        }
    }

    private static final class ErrorWarningNode extends AbstractNode {

        public ErrorWarningNode(String text, boolean error) {
            super(Children.LEAF);
            setDisplayName(text);
            if (error) {
                setIconBaseWithExtension("cloud/findusages/nb/gh/pr/resources/error.png");
            } else {
                setIconBaseWithExtension("cloud/findusages/nb/gh/pr/resources/warning.png");
            }
        }

    }
}
