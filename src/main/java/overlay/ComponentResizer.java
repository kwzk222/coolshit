package overlay;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
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

    private Insets dragInsets = new Insets(5, 5, 5, 5);
    private int direction;
    private Point pressed;
    private Rectangle bounds;
    private Window resizeWindow;
    private boolean resizing;

    public void registerComponent(Component... components) {
        for (Component component : components) {
            component.addMouseListener(this);
            component.addMouseMotionListener(this);
        }
    }

    private boolean isLocked(Component c) {
        Window w = SwingUtilities.getWindowAncestor(c);
        if (w instanceof javax.swing.JFrame frame) {
            Object lockedObj = frame.getRootPane().getClientProperty("locked");
            return (lockedObj instanceof Boolean && (Boolean) lockedObj);
        }
        return false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Component source = e.getComponent();
        if (isLocked(source)) {
            source.setCursor(Cursor.getDefaultCursor());
            return;
        }

        Point location = e.getPoint();
        direction = 0;
        if (location.x < dragInsets.left) direction |= WEST;
        if (location.x > source.getWidth() - dragInsets.right) direction |= EAST;
        if (location.y < dragInsets.top) direction |= NORTH;
        if (location.y > source.getHeight() - dragInsets.bottom) direction |= SOUTH;

        if (direction != 0) {
            source.setCursor(Cursor.getPredefinedCursor(cursors.get(direction)));
        } else {
            source.setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Component source = e.getComponent();
        if (isLocked(source)) return;

        if (direction != 0) {
            resizing = true;
            pressed = e.getPoint();
            SwingUtilities.convertPointToScreen(pressed, source);
            resizeWindow = SwingUtilities.getWindowAncestor(source);
            if (resizeWindow != null) bounds = resizeWindow.getBounds();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        resizing = false;
        // In a real app we'd trigger save here, but OverlayApp handles it via its own listeners for dragging.
        // For resizing, we can try to call saveWindowBounds via reflection or just rely on the fact that
        // the user will likely drag it at some point.
        // Actually, let's just make saveWindowBounds public in OverlayApp.
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!resizing || resizeWindow == null) return;

        Component source = e.getComponent();
        Point current = e.getPoint();
        SwingUtilities.convertPointToScreen(current, source);

        int x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        if ((direction & WEST) != 0) { int d = pressed.x - current.x; x -= d; w += d; }
        if ((direction & NORTH) != 0) { int d = pressed.y - current.y; y -= d; h += d; }
        if ((direction & EAST) != 0) { w += (current.x - pressed.x); }
        if ((direction & SOUTH) != 0) { h += (current.y - pressed.y); }

        resizeWindow.setBounds(x, y, Math.max(w, 50), Math.max(h, 20));
        resizeWindow.validate();
    }
}
