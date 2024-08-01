package com.imoonday.soulbound;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class DataGen implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(ItemTag::new);
    }

    public static class ItemTag extends FabricTagProvider.ItemTagProvider {

        public ItemTag(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
            super(output, completableFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
            FabricTagProvider<Item>.FabricTagBuilder builder = this.getOrCreateTagBuilder(SoulBound.SOULBOUND_ENCHANTABLE);
            builder.addOptionalTag(ItemTags.DURABILITY_ENCHANTABLE);
            Registries.ITEM.stream().filter(item -> !item.getDefaultStack().isStackable()).forEach(item -> {
                if (Registries.ITEM.getId(item).getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                    builder.add(item);
                } else {
                    Registries.ITEM.getKey(item).ifPresent(builder::addOptional);
                }
            });
        }
    }
}
