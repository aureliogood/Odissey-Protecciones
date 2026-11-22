package es.odissey.protecciones.utils;

import es.odissey.protecciones.PSProtectBlock;
import es.odissey.protecciones.OdisseyProtecciones;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class RecipeUtil {

    private static List<NamespacedKey> recipes = new ArrayList<>();
    public static void setupPSRecipes() {
        for (PSProtectBlock b : OdisseyProtecciones.getInstance().getConfiguredBlocks()) {
            // add custom recipes to Bukkit
            if (b.allowCraftWithCustomRecipe) {
                try {
                    Bukkit.addRecipe(parseRecipe(b));
                    recipes.add(getNamespacedKeyForBlock(b));
                } catch (IllegalStateException e) {
                    OdisseyProtecciones.getPluginLogger().warning("Recargar las recetas personalizadas no funciona ahora mismo; debes reiniciar el servidor para actualizar las recetas.");
                }
            }
        }
    }
    public static void removePSRecipes() {
        // elimina las recetas anteriores de OdisseyProtecciones (/ps reload)
        Iterator<Recipe> iter = Bukkit.getServer().recipeIterator();
        while (iter.hasNext()) {
            try {
                Recipe r = iter.next();
                if (r instanceof ShapedRecipe && (((ShapedRecipe) r).getKey().getNamespace().equalsIgnoreCase(OdisseyProtecciones.getInstance().getName()))) {
                    iter.remove();
                }
            } catch (Exception ignored) {
            }
        }
        recipes.clear();
    }

    public static List<NamespacedKey> getRecipeKeys() {
        return recipes;
    }

    public static NamespacedKey getNamespacedKeyForBlock(PSProtectBlock block) {
        return new NamespacedKey(OdisseyProtecciones.getInstance(), block.type.replaceAll("[+/=:]", ""));
    }

    public static ShapedRecipe parseRecipe(PSProtectBlock block) {
        // create item
        ItemStack item = block.createItem();
        item.setAmount(block.recipeAmount);

        // create recipe
        // key must adhere to [a-z0-9/._-]
        ShapedRecipe recipe = new ShapedRecipe(getNamespacedKeyForBlock(block), item);

        // parse config
        HashMap<String, Character> items = new HashMap<>();
        List<String> recipeLine = new ArrayList<>();
        char id = 'a';
        for (int i = 0; i < block.customRecipe.size(); i++) {
            recipeLine.add("");
            for (String mat : block.customRecipe.get(i)) {
                if (mat.equals("")) {
                    recipeLine.set(i, recipeLine.get(i) + " ");
                } else {
                    if (items.get(mat) == null) {
                        items.put(mat, id++);
                    }
                    recipeLine.set(i, recipeLine.get(i) + items.get(mat));
                }
            }
        }

        // recipe
        recipe.shape(recipeLine.toArray(new String[0]));
        for (String mat : items.keySet()) {
            if (Material.matchMaterial(mat) != null) { // general material type

                recipe.setIngredient(items.get(mat), Material.matchMaterial(mat));

            } else if (mat.startsWith("ODISSEY_PROTECCIONES:")) { // OdisseyProtecciones block

                // formato ODISSEY_PROTECCIONES:alias
                String alias = mat.substring(mat.indexOf(":") + 1);
                PSProtectBlock use = OdisseyProtecciones.getProtectBlockFromAlias(alias);
                if (use != null && use.createItem() != null) {
                    recipe.setIngredient(items.get(mat), new RecipeChoice.ExactChoice(use.createItem()));
                } else {
                    OdisseyProtecciones.getPluginLogger().warning("No se pudo resolver el material " + mat + " para la receta de " + block.alias + ".");
                }

            } else {
                OdisseyProtecciones.getPluginLogger().warning("No se pudo encontrar el material " + mat + " para la receta de " + block.alias + ".");
            }
        }

        return recipe;
    }
}
