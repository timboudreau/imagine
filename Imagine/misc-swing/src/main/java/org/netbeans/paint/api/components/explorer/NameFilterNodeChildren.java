package org.netbeans.paint.api.components.explorer;

import org.openide.loaders.DataObject;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;

final class NameFilterNodeChildren extends FilterNode.Children {

    NameFilterNodeChildren(Node node) {
        super(node);
    }

    @Override
    protected Node copyNode(Node n) {
        Node result = new FilterNode(n);
        DataObject dob = n.getLookup().lookup(DataObject.class);
        if (dob != null) {
            String nm = dob.getPrimaryFile().getName();
            //System.err.println("Name from dob file " + nm+ " is EDT "+EventQueue.isDispatchThread());
            result.setShortDescription(nm);
            result.setDisplayName(nm);
        }
        return result;
    }
}
