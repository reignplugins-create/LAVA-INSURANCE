package com.lavainsurance;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Adds one item, the Lava Insurance Charm. Everything is namespaced under "lavainsurance",
 * there are no dependencies on other mods, and nothing vanilla or modded is replaced.
 */
@Mod(LavaInsuranceMod.MODID)
public class LavaInsuranceMod {
    public static final String MODID = "lavainsurance";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> CHARM = ITEMS.register("lava_insurance_charm",
            () -> new LavaInsuranceCharmItem(new Item.Properties()
                    .stacksTo(1)
                    .tab(CreativeModeTab.TAB_MISC)));

    public LavaInsuranceMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
    }
}
