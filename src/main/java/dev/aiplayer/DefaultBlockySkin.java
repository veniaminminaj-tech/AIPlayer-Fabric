package dev.aiplayer;

import java.util.Base64;

/** Embedded 64x64 default Blocky skin. */
public final class DefaultBlockySkin {
    private static final String PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAABW0lEQVR42u3av0tCURTA8fNCaG8OESLQTRqz/0ACJwkcnMJc+gEtLg4tLYHVkkFLa5Mgb2jXxnDTJRDxn3Cy+b4n5yp3ePru92yHd1X8cM7xvvsMxBLlYm6pXQ9H00B2OPbE8wAAAAAAAMDnyAzebtXf+cf3nvoGttfb4qz5HFABACTYArYFrUZFSlcdEREZdu9ERGK5FxVQCGtqvqsRuA4x10h6CGbWXfj00jfy+5tzP2ZA2r5wrAVsC2wtEi3ho+svdf3fa3Wrzg/YBwDgecT6MX9SVnv44/LYyC++D4x8P5tXP3Axmxj5vNdWZ0J0pnwWfozr9fGp04yhBQDgZsht63pYeVhSAQAAAAAAAACQznuByW+40d6a8wBaAIB0nwdsOhOoAAAAAAAAAAAAAAAAANjKsD4XSNvenwoAAAD9PCDpiP4nyfX5PxUAAAAAAAAAAAAAAAAAK+MfVcZOcU+tryoAAAAASUVORK5CYII=";

    private DefaultBlockySkin() {}

    public static byte[] pngBytes() {
        return Base64.getDecoder().decode(PNG);
    }
}
