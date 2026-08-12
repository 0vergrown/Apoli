package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.function.Function;

public record IconData(ItemStack stack, Optional<ResourceLocation> texture, int width, int height) {

    public static final IconData EMPTY = new IconData(ItemStack.EMPTY, Optional.empty(), 0, 0);

    public IconData {
        stack = stack.copy();
    }

    public static IconData ofItem(ItemStack stack) {
        return new IconData(stack, Optional.empty(), 0, 0);
    }

    public static IconData ofItem(net.minecraft.world.item.Item item) {
        return new IconData(new ItemStack(item), Optional.empty(), 0, 0);
    }

    public static IconData ofTexture(ResourceLocation texture, int width, int height) {
        return new IconData(ItemStack.EMPTY, Optional.of(texture), width, height);
    }

    public boolean isTexture() {
        return texture.isPresent();
    }

    public boolean isEmpty() {
        return texture.isEmpty() && stack.isEmpty();
    }

    private static final Codec<IconData> TEXTURE_CODEC = RecordCodecBuilder.create(i -> i.group(
        IdCodecs.ID.fieldOf("texture").forGetter(icon -> icon.texture.orElseThrow()),
        Codec.INT.optionalFieldOf("width", 0).forGetter(IconData::width),
        Codec.INT.optionalFieldOf("height", 0).forGetter(IconData::height)
    ).apply(i, IconData::ofTexture));

    private static final Codec<IconData> ITEM_ID_CODEC = IdCodecs.ID.xmap(
        rl -> ofItem(new ItemStack(BuiltInRegistries.ITEM.get(rl))),
        icon -> BuiltInRegistries.ITEM.getKey(icon.stack.getItem()));

    private static final Codec<IconData> ITEM_STACK_CODEC = ItemStackData.CODEC.xmap(
        data -> ofItem(data.stack()),
        icon -> new ItemStackData(icon.stack));

    public static final Codec<IconData> CODEC = Codec.either(
        TEXTURE_CODEC,
        Codec.either(ITEM_ID_CODEC, ITEM_STACK_CODEC)
    ).xmap(
        either -> either.map(Function.identity(), inner -> inner.map(Function.identity(), Function.identity())),
        icon -> icon.isTexture() ? Either.left(icon) : Either.right(Either.right(icon)));

    public static IconData grassBlock() {
        return ofItem(Items.GRASS_BLOCK);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        buf.writeOptional(texture, (b, id) -> b.writeResourceLocation(id));
        buf.writeVarInt(width);
        buf.writeVarInt(height);
    }

    public static IconData read(RegistryFriendlyByteBuf buf) {
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        Optional<ResourceLocation> texture = buf.readOptional(b -> b.readResourceLocation());
        int width = buf.readVarInt();
        int height = buf.readVarInt();
        return new IconData(stack, texture, width, height);
    }
}
