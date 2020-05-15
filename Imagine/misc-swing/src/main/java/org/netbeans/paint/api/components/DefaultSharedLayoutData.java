/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;

/**
 * Implementation of SharedLayoutData, used by LDPLayout. Primary use case:
 * Create a panel that implements SharedLayoutData, and delegate to an instance
 * of this class.
 *
 * @author Tim Boudreau
 */
public final class DefaultSharedLayoutData implements SharedLayoutData {

    private final Set<LayoutDataProvider> known = new HashSet<>();

    private boolean reentry;

    @Override
    public String toString() {
        if (reentry) {
            return "<loop-to-self>";
        }
        reentry = true;
        StringBuilder sb = new StringBuilder("DefaultSharedLayoutData(")
                .append(known.size()).append(" providers:\n");
        for (LayoutDataProvider p : known) {
            int ind = -1;
            Dimension prefSize = null;
            int kids = -1;
            ind = p.getIndent();
            int realIndent = -1;
            if (p instanceof Container) {
                prefSize = ((Container) p).getPreferredSize();
                kids = ((Container) p).getComponentCount();
                realIndent = indentFor((Container) p);
            }
            sb.append(" * ").append(dpToString(p));

            if (ind >= 0) {
                sb.append(" indent ").append(ind);
            }
            if (ind >= 0) {
                sb.append(" indentFor gets ").append(realIndent);
            }

            if (kids >= 0) {
                sb.append(" and ").append(kids).append(" children");
            }
            if (prefSize != null) {
                sb.append(" prefsize ").append(prefSize.width).append(" X ").append(prefSize.height);
            }
            sb.append(p.isExpanded() ? " expanded" : " unexpanded");
            sb.append('\n');
            sb.append("   colpos: / ");
            for (int i = 0; i < 7; i++) {
                int pos = p.getColumnPosition(i);
                if (pos >= 0) {
                    sb.append(i).append(": ").append(pos).append(" / ");
                }
            }
            sb.append('\n');
            if (p instanceof Container) {
                Container c = (Container) p;
                sb.append(" Child indents: ");
                Component[] comps = c.getComponents();
                for (int i = 0; i < comps.length; i++) {
                    Component c1 = comps[i];
                    if (c1 instanceof Container) {
                        sb.append(i).append(": ");
                        String name = c1.getName();
                        if (name != null) {
                            sb.append(name).append('-');
                        } else {
                            sb.append(Integer.toString(System.identityHashCode(c1), 36)).append('-');
                        }
                        sb.append(c1.getClass().getSimpleName()).append(' ');
                    }
                    sb.append(" = ").append(indentFor(c));
                }
            }
        }
        reentry = false;
        return sb.append(")").toString();
    }

    private String dpToString(LayoutDataProvider lay) {
        if (lay instanceof Component) {
            String nm = ((Component) lay).getName();
            String tp = lay.getClass().getSimpleName();
            int ix = System.identityHashCode(lay);
            if (nm == null) {
                nm = ((Component) lay).toString();
            }
            String result = tp + "-" + Integer.toString(ix, 36) + " " + nm;
            if (lay instanceof SharedLayoutPanel) {
                result += ((SharedLayoutPanel) lay).insetsString();
            }
            return result;
        } else {
            return lay.toString();
        }
    }

    @Override
    public void register(LayoutDataProvider p) {
        known.add(p);
        for (LayoutDataProvider d : known) {
            if (d instanceof JComponent) {
                ((JComponent) d).invalidate();
                ((JComponent) d).revalidate();
                ((JComponent) d).repaint();
            }
        }
    }

    @Override
    public void unregister(LayoutDataProvider p) {
        known.remove(p);
        for (LayoutDataProvider d : known) {
            if (d instanceof JComponent) {
                ((JComponent) d).invalidate();
                ((JComponent) d).revalidate();
                ((JComponent) d).repaint();
            }
        }
    }

    @Override
    public int indentFor(Container requester) {
        if (requester instanceof LayoutDataProvider) {
            LayoutDataProvider ldp = (LayoutDataProvider) requester;
            if (ldp.getIndent() > 0) {
                return 0;
            }
        }
//        LayoutDataProvider ldp = (LayoutDataProvider) SwingUtilities.getAncestorOfClass(LayoutDataProvider.class, requester);
//        if (ldp != null && ldp.getIndent() > 0) {
//            return 0;
//        }
        int maxIndent = 0;
        for (LayoutDataProvider d : known) {
            maxIndent = Math.max(maxIndent, d.getIndent());
        }
        return maxIndent;
    }

    @Override
    public int xPosForColumn(int column, Container requester) {
        int xpos = -1;
        for (LayoutDataProvider l : known) {
            if (l.is(requester)) {
                continue;
            }
            int colpos = l.getColumnPosition(column);
            xpos = Math.max(colpos, xpos);
        }
        return xpos;
    }

    @Override
    public void expanded(LayoutDataProvider p, boolean state) {
        Set<LayoutDataProvider> iter = new HashSet<LayoutDataProvider>(known);
        if (state) {
            for (LayoutDataProvider d : iter) {
                if (d != p) {
                    if (d.isExpanded() && !isAncestor(d, p)) {
                        d.doSetExpanded(false);
                    }
                } else {
                    if (d instanceof JComponent) {
                        JComponent jc = (JComponent) d;
                        jc.invalidate();
                        jc.revalidate();
                        jc.repaint();
                    }
                }
            }
        }
    }

    private boolean isAncestor(LayoutDataProvider possibleAncestor, LayoutDataProvider of) {
        if (!(possibleAncestor instanceof Container) || !(of instanceof Container)) {
            return false;
        }
        Container child = (Container) of;
        Container parent = (Container) possibleAncestor;
        return parent.isAncestorOf(child);
    }
}
