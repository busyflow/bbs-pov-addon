package wemppy.bbs_pov.client.render;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import wemppy.bbs_pov.clips.MouseTrackerData;

import java.util.ArrayList;
import java.util.List;

public class MouseCursorRenderer
{
    // Pixel-art cursor definition: 12x17 standard OS/Minecraft cursor
    // 0 = empty, 1 = black border, 2 = white fill
    private static final int[][] CURSOR_PIXELS = {
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {1, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0},
        {1, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0},
        {1, 2, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0},
        {1, 2, 2, 2, 2, 2, 1, 0, 0, 0, 0, 0},
        {1, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0, 0},
        {1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
        {1, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
        {1, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 0},
        {1, 2, 2, 1, 2, 2, 1, 0, 0, 0, 0, 0},
        {1, 2, 1, 0, 1, 2, 2, 1, 0, 0, 0, 0},
        {1, 1, 0, 0, 1, 2, 2, 1, 0, 0, 0, 0},
        {1, 0, 0, 0, 0, 1, 2, 2, 1, 0, 0, 0},
        {0, 0, 0, 0, 0, 1, 2, 2, 1, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0}
    };

    public static void render(MatrixStack stack, Batcher2D batcher, ClipContext context)
    {
        List<MouseTrackerData> list = context.clipData.get("bbs_mouse_tracker", ArrayList::new);

        if (list == null || list.isEmpty())
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        for (MouseTrackerData data : list)
        {
            if (data.factor <= 0F)
            {
                continue;
            }

            int cx = (int) (data.x * width);
            int cy = (int) (data.y * height);

            if (data.clicking)
            {
                // Click outline indicator
                int rippleColor = (Colors.setA(0x33AAEE, (int) (160 * data.factor)));
                batcher.outline(cx - 3, cy - 3, cx + 4, cy + 4, rippleColor);
            }

            // Draw crisp pixel pointer
            for (int r = 0; r < CURSOR_PIXELS.length; r++)
            {
                for (int c = 0; c < CURSOR_PIXELS[r].length; c++)
                {
                    int val = CURSOR_PIXELS[r][c];
                    if (val == 1)
                    {
                        // Black border (0xFF000000)
                        batcher.box(cx + c, cy + r, cx + c + 1, cy + r + 1, Colors.setA(0x000000, (int) (255 * data.factor)));
                    }
                    else if (val == 2)
                    {
                        // White body (or slightly dimmed when clicking)
                        int color = data.clicking ? Colors.LIGHTEST_GRAY : Colors.WHITE;
                        batcher.box(cx + c, cy + r, cx + c + 1, cy + r + 1, Colors.setA(color, (int) (255 * data.factor)));
                    }
                }
            }
        }
    }
}
