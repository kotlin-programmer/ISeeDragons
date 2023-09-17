/*
MIT License

Copyright (c) 2021 xcube16

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package io.github.xcube16.iseedragons;

import net.minecraftforge.common.config.Config;

import java.util.LinkedHashMap;
import java.util.Map;

@Config(modid = ISeeDragons.MODID)
public class StaticConfig {

    @Config.Comment("A list of entities and view distances ")
    @Config.Name("EntityDistanceOverrides")
    public static Map<String, Integer> distanceOverrides;

    @Config.Comment("Prevents lightning strikes from destroying items")
    public static boolean disableLightningItemDamage = false;

    @Config.Comment("Prevents Tough As Nails from creating an extra attack entity event")
    @Config.RequiresMcRestart
    public static boolean preventTANAttackEntityEvent = true;

    @Config.Comment("A list of tools/armor and there new repair item (note: only list one tool of a given 'ToolMaterial')")
    @Config.Name("RepairFixes")
    public static Map<String, String> repairFixes;

    @Config.Comment("Minimum brightness override (can be negative)")
    public static float minBrightness = 0.0f;

    @Config.Comment("Maximum brightness override (can be negative)")
    public static float maxBrightness = 1.0f;

    @Config.Comment("Core modifications")
    @Config.Name("ASM")
    public static ASM asm = new ASM();

    @Config.RequiresMcRestart
    public static final class ASM {

        @Config.Comment("Mutes harmless noisy warnings/errors in the RLCraft modpack")
        @Config.Name("STFU")
        public boolean stfu = true;

        @Config.Comment("Removes everything from the vanilla achievements system! Can be used to stop log spam when recipes are tweaked.")
        @Config.Name("NukeAchievements")
        public boolean nukeAchievements = false;

        @Config.Comment("Patches the dummy from MmmMmmMmmMmm (yes, thats a mod name) to show damage and not hearts")
        @Config.Name("PatchMmmMmm")
        public boolean patchMmmMmm = true;
    }

    static {
        distanceOverrides = new LinkedHashMap<>();
        distanceOverrides.put("battletower:golem", 256);

        repairFixes = new LinkedHashMap<>();
        repairFixes.put("aquaculture:neptunium_pickaxe", "aquaculture:loot,1");
        repairFixes.put("aquaculture:neptunium_chestplate", "aquaculture:loot,1");
    }
}
