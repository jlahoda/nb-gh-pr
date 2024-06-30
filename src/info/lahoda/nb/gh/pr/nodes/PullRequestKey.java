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

import javax.swing.event.ChangeListener;
import org.kohsuke.github.GHPullRequest;
import org.openide.util.ChangeSupport;

public class PullRequestKey {

    private final ChangeSupport cs = new ChangeSupport(this);
    private final int prNumber;
    private GHPullRequest pullRequest;

    public PullRequestKey(int prNumber) {
        this.prNumber = prNumber;
    }

    public int getPrNumber() {
        return prNumber;
    }

    public synchronized GHPullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(GHPullRequest pullRequest) {
        synchronized (this) {
            this.pullRequest = pullRequest;
        }
        cs.fireChange();
    }

    public void addChangeListener(ChangeListener l) {
        cs.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        cs.removeChangeListener(l);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + this.prNumber;
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
        final PullRequestKey other = (PullRequestKey) obj;
        return this.prNumber == other.prNumber;
    }
    
}
