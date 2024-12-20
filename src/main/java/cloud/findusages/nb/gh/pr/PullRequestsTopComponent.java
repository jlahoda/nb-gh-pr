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
package cloud.findusages.nb.gh.pr;

import cloud.findusages.nb.gh.pr.nodes.RootNode;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ActionMap;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//cloud.findusages.nb.gh.pr//PullRequests//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "PullRequestsTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "navigator", openAtStartup = true)
@ActionID(category = "Window", id = "cloud.findusages.nb.gh.pr.PullRequestsTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_PullRequestsAction",
        preferredID = "PullRequestsTopComponent"
)
@Messages({
    "CTL_PullRequestsAction=Pull Requests",
    "CTL_PullRequestsTopComponent=Pull Requests",
    "HINT_PullRequestsTopComponent=This is a PullRequests window"
})
public final class PullRequestsTopComponent extends TopComponent implements ExplorerManager.Provider {

    private final ExplorerManager manager = new ExplorerManager();

    public PullRequestsTopComponent() {
        initComponents();
        setName(Bundle.CTL_PullRequestsTopComponent());
        setToolTipText(Bundle.HINT_PullRequestsTopComponent());
        BeanTreeView btv = new BeanTreeView();
        btv.setRootVisible(false);
        scrollPane.setViewportView(btv);
        ProxyLookupImpl selectedNodeLookup = new ProxyLookupImpl();
        manager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName() == null ||
                    ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                    selectedNodeLookup.setLookupsInner(Lookups.fixed((Object[])manager.getSelectedNodes()));
                }
            }
        });


        ActionMap actions = this.getActionMap ();
        actions.put("delete", ExplorerUtils.actionDelete(manager, true));

        associateLookup (new ProxyLookup(ExplorerUtils.createLookup (manager, actions), selectedNodeLookup));

        manager.setRootContext(new RootNode());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return manager;
    }

    private static final class ProxyLookupImpl extends ProxyLookup {
        void setLookupsInner(Lookup... lkp) {
            setLookups(lkp);
        }
    }
}
