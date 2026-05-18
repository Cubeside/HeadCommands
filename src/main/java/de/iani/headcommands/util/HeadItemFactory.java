package de.iani.headcommands.util;

import de.iani.cubesideutils.bukkit.items.CustomHeads;
import de.iani.headcommands.model.CachedHead;
import de.iani.headcommands.model.HeadCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HeadItemFactory {
    private HeadItemFactory() {
        throw new UnsupportedOperationException();
    }

    public static ItemStack createHead(CachedHead head, HeadCategory category, int amount) {
        UUID uuid = parseUuid(head.websiteUuid(), head.id());
        ItemStack stack = CustomHeads.createHead(uuid, head.name(), HeadTextureUtil.createTextureValue(head.textureUrl()));
        stack.setAmount(amount);

        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(head.name(), NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("ID: " + head.id(), NamedTextColor.GRAY));
        if (category != null) {
            lore.add(Component.text("Kategorie: " + category.name() + " (" + category.id() + ")", NamedTextColor.GRAY));
        }
        if (head.publishedAt() != null) {
            lore.add(Component.text("Datum: " + head.publishedAt(), NamedTextColor.GRAY));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static UUID parseUuid(String value, int headId) {
        if (value != null) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                // fall through to deterministic fallback
            }
        }
        return UUID.nameUUIDFromBytes(("HeadCommands:" + headId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
