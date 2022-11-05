package net.java.dev.imagine.toolcustomizers;

import com.mastfrog.swing.FlexEmptyBorder;
import static com.mastfrog.swing.FlexEmptyBorder.Side.TOP;
import java.awt.Container;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.netbeans.paint.api.components.LayoutDataProvider;
import org.netbeans.paint.api.components.SharedLayoutData;
import com.mastfrog.swing.layout.VerticalFlowLayout;
import org.openide.awt.Mnemonics;

/**
 *
 * @author Tim Boudreau
 */
class NestingPanel extends JPanel implements SharedLayoutData {
    
    private SharedLayoutData data;
    private Runnable onShow;

    NestingPanel() {
        super(new VerticalFlowLayout());
        setBorder(new FlexEmptyBorder(TOP));
    }

    JLabel addHeadingLabel(String title) {
        JLabel label = new JLabel();
        Mnemonics.setLocalizedText(label, title);
        label.setBorder(BorderFactory.createMatteBorder(0,0,1,0, UIManager.getColor("controlDkShadow")));
//        label.setBorder(BorderFactory.createCompoundBorder(SharedLayoutPanel.createIndentBorder(),
//                BorderFactory.createMatteBorder(0,0,1,0, UIManager.getColor("controlDkShadow"))));
        add(label);
        return label;
    }

    NestingPanel onShow(Runnable run) {
        onShow = run;
        return this;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        data = SharedLayoutData.find(this);
        if (data != null) {
            System.out.println("got data " + data);
            for (LayoutDataProvider p : preregistered) {
                data.register(p);
            }
        }
        if (onShow != null) {
            onShow.run();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        data = null;
    }

    @Override
    public int xPosForColumn(int column, Container requester) {
        switch(column) {
            case 0 :
                return 0;
            default :
                if (data == null) {
                    System.out.println("no data - " + column + " for " + requester);
                    return 0;
                }
                int result = data.xPosForColumn(0, this);
                System.out.println("COL " + column + " pos " + result);
                return result;
        }
    }

    private Set<LayoutDataProvider> preregistered = new LinkedHashSet<>();
    @Override
    public void register(LayoutDataProvider p) {
        if (data != null) {
            data.register(p);
        } else {
            preregistered.add(p);
        }
    }

    @Override
    public void unregister(LayoutDataProvider p) {
        if (data != null) {
            data.unregister(p);
        } else {
            preregistered.remove(p);
        }
    }

    @Override
    public void expanded(LayoutDataProvider p, boolean state) {
        if (data != null) {
            data.expanded(p, state);
        }
    }

    @Override
    public int indentFor(Container requester) {
        return 0;
    }

}
