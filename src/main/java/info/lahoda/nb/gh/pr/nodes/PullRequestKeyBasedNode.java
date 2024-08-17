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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.kohsuke.github.GHPullRequest;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor.Task;
import org.openide.util.WeakListeners;

public abstract class PullRequestKeyBasedNode extends AbstractNode implements ChangeListener {

    private final Task reconfigure = Common.WORKER.create(() -> {
        configureNodeFromPullRequest(PullRequestKeyBasedNode.this.key.getPullRequest());
    });
    private final PullRequestKey key;

    public PullRequestKeyBasedNode(PullRequestKey key, Children children, Lookup lookup) {
        super(children, lookup);
        this.key = key;
        key.addChangeListener(WeakListeners.change(this, key));
        configureNodeFromPullRequest(key.getPullRequest());
    }

    protected abstract void configureNodeFromPullRequest(GHPullRequest pr);

    @Override
    public void stateChanged(ChangeEvent e) {
        reconfigure.schedule(0);
    }

}
