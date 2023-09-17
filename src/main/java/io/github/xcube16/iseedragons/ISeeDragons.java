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

import com.google.common.collect.BiMap;
import net.minecraft.entity.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod(modid = ISeeDragons.MODID, version = ISeeDragons.VERSION, acceptableRemoteVersions = "*", name = ISeeDragons.NAME)
public class ISeeDragons {
    public static final String MODID = ISD.MODID;
    public static final String NAME = ISD.NAME;
    public static final String VERSION = ISD.VERSION;
    public static final Logger logger = ISD.logger;

    @Mod.Instance(ISeeDragons.MODID)
    private static ISeeDragons instance;

    @SidedProxy(clientSide = "io.github.xcube16.iseedragons.client.ClientProxy",
            serverSide = "io.github.xcube16.iseedragons.DefaultProxy")
    private static DefaultProxy proxy;

    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings("deprecation")
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

        this.loadConfig();

        logger.info("Fixing tools/armor repair listed in config...");
        StaticConfig.repairFixes.forEach((toolId, repairItem) -> {
            String[] split = repairItem.split(",");
            if (split.length == 1) {
                this.fixToolRepair(toolId, split[0], 0);
            } else if (split.length == 2) {
                this.fixToolRepair(toolId, split[0], Integer.parseInt(split[1]));
            } else {
                logger.error("Bad item string " + repairItem);
            }
        });

        if (StaticConfig.preventTANAttackEntityEvent && Loader.isModLoaded("toughasnails")) {
            logger.info("Fixing TAN's attack entity event damage problem...");
            boolean found_listener = false;
            //Find the ThirstStatHandler in the event bus and remove it
            ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners = ReflectionHelper.getPrivateValue(EventBus.class, MinecraftForge.EVENT_BUS, "listeners");
            for (Map.Entry<Object, ArrayList<IEventListener>> listener_entry : listeners.entrySet()) {
                if (listener_entry.getKey().getClass().getName().equals("toughasnails.handler.thirst.ThirstStatHandler")) {
                    found_listener = true;
                    //Create the FakeThirstStatHandler and send it the real one to use
                    try {
                        MinecraftForge.EVENT_BUS.register(new FakeThirstStatHandler(listener_entry.getKey()));
                    } catch (Exception e) {
                        //If an exception is thrown, FakeThirstStatHandler never gets added to the event bus
                        logger.error("Failed to initialize FakeThirstStatHandler");
                        e.printStackTrace();
                        break;
                    }

                    //Unregister the real ThirstStatHandler
                    MinecraftForge.EVENT_BUS.unregister(listener_entry.getKey());
                    break;
                }
            }
            if (!found_listener) {
                logger.error("Could not find toughasnails ThirstStatHandler event listener");
            }
        }
    }

    private void fixToolRepair(String toolId, String repairItemId, int meta) {
        try {
            Item tool = Item.getByNameOrId(toolId);
            if (tool == null) {
                logger.info("Could not find " + toolId + ", ignoring");
                return;
            }
            if (tool instanceof ItemTool) {
                Field toolMaterialField = ItemTool.class.getDeclaredField("field_77862_b"); // toolMaterial
                toolMaterialField.setAccessible(true);
                Object toolMaterial = toolMaterialField.get(tool);

                if (toolMaterial instanceof Item.ToolMaterial) {
                    @Nullable
                    Item repairItem = Item.getByNameOrId(repairItemId);
                    if (repairItem != null) {
                        ((Item.ToolMaterial) toolMaterial).setRepairItem(new ItemStack(repairItem, 1, meta));
                        logger.info(toolId + " can now be repaired with " + repairItemId);
                    } else {
                        logger.error(repairItemId + " does not exist! Failed to fix " + toolId + " repair!");
                    }
                } else {
                    logger.error(toolId + " has a bad tool material of " + toolMaterial);
                }
            } else if (tool instanceof ItemArmor) {
                Field materialField = ItemArmor.class.getDeclaredField("field_77878_bZ"); // material
                materialField.setAccessible(true);
                Object toolMaterial = materialField.get(tool);

                if (toolMaterial instanceof ItemArmor.ArmorMaterial) {
                    @Nullable
                    Item repairItem = Item.getByNameOrId(repairItemId);
                    if (repairItem != null) {
                        ((ItemArmor.ArmorMaterial) toolMaterial).setRepairItem(new ItemStack(repairItem, 1, meta));
                        logger.info(toolId + " can now be repaired with " + repairItemId);
                    } else {
                        logger.error(repairItemId + " does not exist! Failed to fix " + toolId + " repair!");
                    }
                } else {
                    logger.error(toolId + " has a bad armor material of " + toolMaterial);
                }
            } else if (tool instanceof ItemSword) {
                Field materialField = ItemSword.class.getDeclaredField("field_150933_b"); // material
                materialField.setAccessible(true);
                Object toolMaterial = materialField.get(tool);

                if (toolMaterial instanceof Item.ToolMaterial) {
                    @Nullable
                    Item repairItem = Item.getByNameOrId(repairItemId);
                    if (repairItem != null) {
                        ((ItemArmor.ToolMaterial) toolMaterial).setRepairItem(new ItemStack(repairItem, 1, meta));
                        logger.info(toolId + " can now be repaired with " + repairItemId);
                    } else {
                        logger.error(repairItemId + " does not exist! Failed to fix " + toolId + " repair!");
                    }
                } else {
                    logger.error(toolId + " has a bad sword material of " + toolMaterial);
                }
            } else {
                logger.info(toolId + " is not a tool, armor, or sword");
            }
        } catch (Exception e) {
            logger.error("Critical error while fixing tool/armor repair for " + toolId, e);
        }
    }

    @SubscribeEvent
    public void lightningStruckEntity(EntityStruckByLightningEvent event) {
        //Prevent lightning from destroying items
        if (StaticConfig.disableLightningItemDamage && (event.getEntity() instanceof EntityItem))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent e) {
        if (e.getModID().equals(ISeeDragons.MODID)) {
            ConfigManager.sync(ISeeDragons.MODID, Config.Type.INSTANCE);

            loadConfig();
        }
    }

    private void loadConfig() {
        if (StaticConfig.minBrightness > StaticConfig.maxBrightness) {
            logger.error("Min and max brightness are mixed up!");
            float min = StaticConfig.minBrightness;
            StaticConfig.minBrightness = StaticConfig.maxBrightness;
            StaticConfig.maxBrightness = min;
        }
        proxy.setBrightness(StaticConfig.minBrightness, StaticConfig.maxBrightness);


        try {
            Field regField = EntityRegistry.instance().getClass().getDeclaredField("entityClassRegistrations");
            regField.setAccessible(true);
            BiMap<Class<? extends Entity>, EntityRegistry.EntityRegistration> reg =
                    (BiMap<Class<? extends Entity>, EntityRegistry.EntityRegistration>) regField.get(EntityRegistry.instance());
            for (EntityRegistry.EntityRegistration entity : reg.values()) {
                //logger.info(entity.getRegistryName().toString());
                Optional<Integer> boost = this.getRenderBoost(entity.getRegistryName());
                if (boost.isPresent()) {
                    logger.info("Fixed " + entity.getRegistryName() + " tracking distance");
                    Field rangeField = entity.getClass().getDeclaredField("trackingRange");
                    rangeField.setAccessible(true);
                    rangeField.set(entity, boost.get());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fix entity tracking distance", e);
        }
    }

    public static ResourceLocation[] cleanAdvancementRequardsHook(ResourceLocation[] craftable) {
        return Arrays.stream(craftable)
                .filter(item -> CraftingManager.getRecipe(item) != null)
                .toArray(ResourceLocation[]::new);
    }

    private Optional<Integer> getRenderBoost(@Nullable ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(StaticConfig.distanceOverrides.get(id.toString()));
    }
}
