package net.wynnbubbles.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Stacks bubbles upwards from an anchor point, newest at the bottom. Units are font pixels and
 * y grows downwards, so everything above the anchor has a negative y.
 */
public final class BubbleLayout {
    public static final int LINE_HEIGHT = 9;
    public static final int ARROW_WIDTH = 7;
    public static final int ARROW_HEIGHT = 4;
    /** Size of a corner of the 9-patch; a bubble is never smaller than two of them. */
    public static final int CORNER = 5;

    private static final int BASE_PADDING_X = 4;
    private static final int BASE_PADDING_Y = 3;

    public record Box(int x, int y, int width, int height, int textX, int textY) {
        public int bottom() {
            return y + height;
        }
    }

    public record TextSize(int width, int lines) {}

    public static List<Box> stack(List<TextSize> newestFirst, int padding, int gap) {
        int padX = BASE_PADDING_X + Math.max(0, padding);
        int padY = BASE_PADDING_Y + Math.max(0, padding);

        List<Box> boxes = new ArrayList<>(newestFirst.size());
        // the arrow overlaps the lowest bubble's border by one pixel
        int bottom = -(ARROW_HEIGHT - 1);
        for (TextSize text : newestFirst) {
            int width = Math.max(text.width() + padX * 2, CORNER * 2 + ARROW_WIDTH);
            // an odd width keeps the 7px arrow centred on a whole pixel
            if (width % 2 == 0) width++;
            int textHeight = text.lines() * LINE_HEIGHT - 1;
            int height = Math.max(textHeight + padY * 2, CORNER * 2);
            int x = -width / 2;
            int y = bottom - height;
            boxes.add(new Box(x, y, width, height, x + (width - text.width()) / 2, y + (height - textHeight) / 2));
            bottom = y - Math.max(0, gap);
        }
        return boxes;
    }

    private BubbleLayout() {}
}
