package org.popcraft.chunky.util;

import org.popcraft.chunky.Selection;
import org.popcraft.chunky.shape.ShapeType;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class Formatting {
    private static final ThreadLocal<DecimalFormat> NUMBER_FORMAT = ThreadLocal.withInitial(() -> {
        final DecimalFormat format = new DecimalFormat("#.##");
        format.setRoundingMode(RoundingMode.FLOOR);
        return format;
    });
    private static final char[] BINARY_PREFIXES = new char[]{'K', 'M', 'G', 'T', 'P'};

    private Formatting() {
    }

    public static String bytes(final long bytes) {
        final long value = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (value < 1024) {
            return String.format("%d B", bytes);
        }
        int i = BINARY_PREFIXES.length - 1;
        long prefixValue = 1L << (BINARY_PREFIXES.length * 10);
        for (; i > 0; --i) {
            if (value >= prefixValue) {
                break;
            }
            prefixValue >>= 10;
        }
        return String.format("%.1f %cB", bytes / (double) prefixValue, BINARY_PREFIXES[i]);
    }

    public static String radius(final Selection selection) {
        if (ShapeType.RECTANGLE.equals(selection.shape()) || ShapeType.ELLIPSE.equals(selection.shape())) {
            return String.format("%s, %s", number(selection.radiusX()), number(selection.radiusZ()));
        } else {
            return String.format("%s", number(selection.radiusX()));
        }
    }

    public static String number(final double number) {
        return NUMBER_FORMAT.get().format(number);
    }
}
