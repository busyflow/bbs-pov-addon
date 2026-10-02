package Glaxium.POV.actions.gui.schema;

import Glaxium.POV.actions.gui.GuiSlotSchema;

/**
 * Offline checks for GUI classifier policy. Run with the Yarn-mapped classpath;
 * does not boot Minecraft. GuiTypeEntry is not loaded here because Identifier
 * needs authlib, which is not on the offline compile classpath.
 */
public final class GuiClassifierPolicyTest
{
    private GuiClassifierPolicyTest()
    {
    }

    public static void main(String[] args)
    {
        int failures = 0;
        failures += check("resolve(null) is null", GuiTypeResolver.resolve(null) == null);
        failures += check("null mount is horse, not mule", "horse".equals(GuiTypeResolver.resolveMount(null)));
        failures += check("schema has horse and donkey, never mule", schemaMountPolicy());
        if (failures > 0)
        {
            throw new IllegalStateException("GuiClassifierPolicyTest failures: " + failures);
        }
        System.out.println("GuiClassifierPolicyTest ok");
    }

    private static int check(String name, boolean ok)
    {
        if (!ok)
        {
            System.err.println("FAIL: " + name);
            return 1;
        }
        return 0;
    }

    private static boolean schemaMountPolicy()
    {
        boolean horse = false;
        boolean donkey = false;
        for (GuiSlotSchema schema : GuiSlotSchema.getAll())
        {
            if ("mule".equals(schema.guiId))
            {
                System.err.println("schema contains serialized id mule");
                return false;
            }
            horse |= "horse".equals(schema.guiId);
            donkey |= "donkey".equals(schema.guiId);
        }
        return horse && donkey;
    }
}
