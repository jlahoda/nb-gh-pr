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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.Action;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.openide.actions.OpenAction;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileUtil;
import org.openide.modules.Places;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;

@Messages({
    "# {0} - PR number",
    "# {1} - PR title",
    "TITLE_PR=#{0}: {1}"
})
public class PullRequestNode extends PullRequestKeyBasedNode {

    public PullRequestNode(GitHub gh, String repositoryName, PullRequestKey key) {
        super(key, new PRKeys(repositoryName, key), Lookup.EMPTY);
        setIconBaseWithExtension("info/lahoda/nb/gh/pr/resources/github-pullrequest.png");
    }

    @Override
    protected void configureNodeFromPullRequest(GHPullRequest pr) {
        String title = Bundle.TITLE_PR(String.valueOf(pr.getNumber()), pr.getTitle());
        setDisplayName(title);
        setShortDescription(pr.getBody());
    }

    private static final class PRKeys extends Children.Keys<Node> {

        private final String repositoryName;
        private final PullRequestKey key;

        public PRKeys(String repositoryName, PullRequestKey key) {
            this.repositoryName = repositoryName;
            this.key = key;
        }

        @Override
        protected Node[] createNodes(Node k) {
            return new Node[] {k};
        }

        @Override
        protected void addNotify() {
            List<Node> children = new ArrayList<>();
            children.add(new NodeImpl(key, "info/lahoda/nb/gh/pr/resources/github-overview.png", Lookups.fixed(new OpenCookie() {
                @Override
                public void open() {
                    //TODO: asynchronous
                    try {
                        GHPullRequest pr = key.getPullRequest();
                        File targetFile = Places.getCacheSubfile("gh/pr/" + repositoryName + "/" + pr.getNumber() + ".md");

                        try (InputStream in = new ByteArrayInputStream(pr.getBody().getBytes())) {
                            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }

                        OpenCookie oc = FileUtil.toFileObject(targetFile).getLookup().lookup(OpenCookie.class);

                        if (oc != null) {
                            oc.open();
                        }
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }), SystemAction.get(OpenAction.class)) {
                @Override
                protected void configureNodeFromPullRequest(GHPullRequest pr) {
                    String body = pr.getBody();
                    int lineEnd = body.indexOf('\n');

                    if (lineEnd == (-1)) {
                        lineEnd = body.length();
                    }

                    setDisplayName(body.substring(0, lineEnd));
                    setShortDescription(body);
                }
            });

            children.add(new NodeImpl(key, "info/lahoda/nb/gh/pr/resources/github-user.png", Lookup.EMPTY) {
                @Override
                protected void configureNodeFromPullRequest(GHPullRequest pr) {
                    String user = "<unknown>";
                    try {
                        GHUser ghUser = pr.getUser();
                        user = ghUser.getName() + " (" + ghUser.getLogin() + ")";
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    setDisplayName(user);
                }
            });
            children.add(new NodeImpl(key, "info/lahoda/nb/gh/pr/resources/github-diff.png", Lookups.fixed(new OpenCookie() {
                @Override
                public void open() {
                    //TODO: asynchronous
                    try {
                        GHPullRequest pr = key.getPullRequest();
                        URL url = pr.getPatchUrl();
                        File targetFile = Places.getCacheSubfile("gh/pr/" + repositoryName + "/" + pr.getNumber() + ".diff");

                        try (InputStream in = url.openStream()) {
                            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }

                        OpenCookie oc = FileUtil.toFileObject(targetFile).getLookup().lookup(OpenCookie.class);

                        if (oc != null) {
                            oc.open();
                        }
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }), SystemAction.get(OpenAction.class)) {
                @Override
                protected void configureNodeFromPullRequest(GHPullRequest pr) {
                    String changedFiles = "<unknown>";
                    try {
                        changedFiles = String.valueOf(pr.getChangedFiles());
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    setDisplayName(changedFiles + " changed files");
                }
            });
            children.add(new NodeImpl(key, "info/lahoda/nb/gh/pr/resources/github-tags.png", Lookup.EMPTY) {
                @Override
                protected void configureNodeFromPullRequest(GHPullRequest pr) {
                    setDisplayName(pr.getLabels().stream().map(l -> l.getName()).collect(Collectors.joining(", ")));
                }
            });

            setKeys(children);
        }

    }

    private static abstract class NodeImpl extends PullRequestKeyBasedNode {

        private final Action defaultAction;
        private final Action[] actions;

        public NodeImpl(PullRequestKey key, String iconBase, Lookup lookup, Action... actions) {
            super(key, Children.LEAF, lookup);
            setIconBaseWithExtension(iconBase);
            if (actions.length > 0) {
                defaultAction = actions[0];
            } else {
                defaultAction = null;
            }
            this.actions = actions;
        }

        @Override
        public Action[] getActions(boolean context) {
            return actions;
        }

        @Override
        public Action getPreferredAction() {
            return defaultAction;
        }

    }

}