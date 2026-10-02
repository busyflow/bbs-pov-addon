package Glaxium.POV.hand.editor;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** State shared by the POV hand render and BBS's native stencil picker. */
public final class PovHandPicking
{
    private static boolean stencilPass;
    private static StencilMap stencilMap;
    private static Form pickedForm;
    private static String pickedBone;
    private static int pickedBodyPart = -1;
    private static Matrix4f projection;
    private static ModelInstance mappedModel;
    private static int stencilTarget;
    private static final Map<String, ScreenBounds> ITEM_BOUNDS = new LinkedHashMap<>();
    private static final Map<Form, Integer> BODY_PART_FORMS = new IdentityHashMap<>();

    private PovHandPicking()
    {
    }

    public static void captureProjection(Matrix4f matrix)
    {
        projection = matrix == null ? null : new Matrix4f(matrix);
    }

    public static Matrix4f getProjection()
    {
        return projection == null ? null : new Matrix4f(projection);
    }

    public static void clearItemBounds()
    {
        ITEM_BOUNDS.clear();
    }

    /** Capture the projected unit bounds used by Minecraft's baked item models. */
    public static void captureItemBounds(String bone, Matrix4f itemMatrix)
    {
        if (bone == null || itemMatrix == null || projection == null || stencilPass)
        {
            return;
        }

        Matrix4f projected = new Matrix4f(projection).mul(itemMatrix);
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        boolean visible = false;

        for (int x = 0; x <= 1; x++)
        {
            for (int y = 0; y <= 1; y++)
            {
                for (int z = 0; z <= 1; z++)
                {
                    Vector4f point = projected.transform(new Vector4f(x, y, z, 1F));

                    if (point.w <= 0.00001F)
                    {
                        continue;
                    }

                    float px = point.x / point.w;
                    float py = point.y / point.w;
                    minX = Math.min(minX, px);
                    minY = Math.min(minY, py);
                    maxX = Math.max(maxX, px);
                    maxY = Math.max(maxY, py);
                    visible = true;
                }
            }
        }

        if (visible)
        {
            ITEM_BOUNDS.put(bone, new ScreenBounds(minX, minY, maxX, maxY));
        }
    }

    public static String pickItem(UIContext context, Area viewport)
    {
        if (context == null || viewport == null || viewport.w <= 0 || viewport.h <= 0)
        {
            return null;
        }

        float x = ((context.mouseX - viewport.x) / (float) viewport.w) * 2F - 1F;
        float y = 1F - ((context.mouseY - viewport.y) / (float) viewport.h) * 2F;
        String picked = null;
        float smallest = Float.POSITIVE_INFINITY;

        for (Map.Entry<String, ScreenBounds> entry : ITEM_BOUNDS.entrySet())
        {
            ScreenBounds bounds = entry.getValue();

            if (bounds.contains(x, y) && bounds.area() < smallest)
            {
                picked = entry.getKey();
                smallest = bounds.area();
            }
        }

        return picked;
    }

    public static void beginStencil(StencilMap map)
    {
        stencilPass = true;
        stencilMap = map;
        pickedForm = null;
        pickedBone = null;
        pickedBodyPart = -1;
        BODY_PART_FORMS.clear();
        mappedModel = null;
        stencilTarget = map == null ? 0 : map.objectIndex;
    }

    public static void finishStencil(Pair<Form, String> picked)
    {
        pickedForm = picked == null ? null : picked.a;
        pickedBone = picked == null ? null : picked.b;
        pickedBodyPart = picked == null ? -1 : BODY_PART_FORMS.getOrDefault(picked.a, -1);
        stencilMap = null;
        stencilPass = false;
        mappedModel = null;
        BODY_PART_FORMS.clear();
    }

    public static void abortStencil()
    {
        pickedForm = null;
        pickedBone = null;
        pickedBodyPart = -1;
        stencilMap = null;
        stencilPass = false;
        mappedModel = null;
        BODY_PART_FORMS.clear();
    }

    public static boolean isStencilPass()
    {
        return stencilPass;
    }

    public static StencilMap getStencilMap()
    {
        return stencilMap;
    }

    public static Form getPickedForm()
    {
        return pickedForm;
    }

    public static String getPickedBone()
    {
        return pickedBone;
    }

    public static int getPickedBodyPart()
    {
        return pickedBodyPart;
    }

    public static void registerBodyPart(Form form, int index)
    {
        if (stencilPass && form != null)
        {
            BODY_PART_FORMS.put(form, index);
        }
    }

    public static boolean beginModelMapping(ModelInstance model)
    {
        if (model == null || mappedModel == model)
        {
            return false;
        }

        mappedModel = model;
        stencilTarget = stencilMap == null ? 0 : stencilMap.objectIndex;

        return true;
    }

    public static int getStencilTarget()
    {
        return stencilTarget;
    }

    private record ScreenBounds(float minX, float minY, float maxX, float maxY)
    {
        private boolean contains(float x, float y)
        {
            return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY;
        }

        private float area()
        {
            return Math.max(0F, this.maxX - this.minX) * Math.max(0F, this.maxY - this.minY);
        }
    }
}
