package overlay;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class ComponentResizer extends MouseAdapter {
    private final static int NORTH = 1;
    private final static int WEST = 2;
    private final static int SOUTH = 4;
    private final static int EAST = 8;

    private static final Map<Integer, Integer> cursors = new HashMap<>();
    static {
        cursors.put(NORTH, Cursor.N_RESIZE_CURSOR);
        cursors.put(WEST, Cursor.W_RESIZE_CURSOR);
        cursors.put(SOUTH, Cursor.S_RESIZE_CURSOR);
        cursors.put(EAST, Cursor.E_RESIZE_CURSOR);
        cursors.put(NORTH | WEST, Cursor.NW_RESIZE_CURSOR);
        cursors.put(NORTH | EAST, Cursor.NE_RESIZE_CURSOR);
        cursors.put(SOUTH | WEST, Cursor.SW_RESIZE_CURSOR);
        cursors.put(SOUTH | EAST, Cursor.SE_RESIZE_CURSOR);
    }

    private Insets dragInsets;
    private Dimension snapSize;

    private int direction;
    protected Point pressed;
    protected Rectangle bounds;

    private Component source;
    private Window resizeWindow;

    private boolean resizing;

    public ComponentResizer() {
        this(new Insets(5, 5, 5, 5), new Dimension(1, 1));
    }

    public ComponentResizer(Insets dragInsets, Dimension snapSize) {
        this.dragInsets = dragInsets;
        this.snapSize = snapSize;
    }

    public void registerComponent(Component... components) {
        for (Component component : components) {
            component.addMouseListener(this);
            component.addMouseMotionListener(this);
        }
    }

    public void deregisterComponent(Component... components) {
        for (Component component : components) {
            component.removeMouseListener(this);
            component.removeMouseMotionListener(this);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        source = e.getComponent();
        Window window = SwingUtilities.getWindowAncestor(source);
        if (window instanceof javax.swing.JFrame frame) {
            Object lockedObj = frame.getRootPane().getClientProperty("locked");
            if (lockedObj instanceof Boolean && (Boolean) lockedObj) {
                source.setCursor(Cursor.getDefaultCursor());
                return;
            }
        }
        Point location = e.getPoint();
        direction = 0;

        if (location.x < dragInsets.left)
            direction |= WEST;
        if (location.x > source.getWidth() - dragInsets.right - 1)
            direction |= EAST;
        if (location.y < dragInsets.top)
            direction |= NORTH;
        if (location.y > source.getHeight() - dragInsets.bottom - 1)
            direction |= SOUTH;

        if (direction != 0) {
            source.setCursor(Cursor.getPredefinedCursor(cursors.get(direction)));
        } else {
            source.setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        source = e.getComponent();
        Window window = SwingUtilities.getWindowAncestor(source);
        if (window instanceof javax.swing.JFrame frame) {
            Object lockedObj = frame.getRootPane().getClientProperty("locked");
            if (lockedObj instanceof Boolean && (Boolean) lockedObj) return;
        }
        if (direction != 0) {
            resizing = true;
            source = e.getComponent();
            pressed = e.getPoint();
            SwingUtilities.convertPointToScreen(pressed, source);

            resizeWindow = SwingUtilities.getWindowAncestor(source);
            if (resizeWindow != null) {
                bounds = resizeWindow.getBounds();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        resizing = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!resizing || resizeWindow == null) return;

        Point current = e.getPoint();
        SwingUtilities.convertPointToScreen(current, source);

        int x = bounds.x;
        int y = bounds.y;
        int width = bounds.width;
        int height = bounds.height;

        if ((direction & WEST) != 0) {
            int drag = getDragDistance(pressed.x, current.x, snapSize.width);
            x -= drag;
            width += drag;
        }
        if ((direction & NORTH) != 0) {
            int drag = getDragDistance(pressed.y, current.y, snapSize.height);
            y -= drag;
            height += drag;
        }
        if ((direction & EAST) != 0) {
            int drag = getDragDistance(current.x, pressed.x, snapSize.width);
            width += drag;
        }
        if ((direction & SOUTH) != 0) {
            int drag = getDragDistance(current.y, pressed.y, snapSize.height);
            height += drag;
        }

        resizeWindow.setBounds(x, y, width, height);
        resizeWindow.validate();
    }

    private int getDragDistance(int larger, int smaller, int snap) {
        int halfway = snap / 2;
        int drag = larger - smaller;
        drag += (drag < 0) ? -halfway : halfway;
        drag = (drag / snap) * snap;
        return drag;
    }
}
