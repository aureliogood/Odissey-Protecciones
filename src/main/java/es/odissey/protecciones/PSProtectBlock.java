package es.odissey.protecciones;

import es.odissey.protecciones.utils.config.Path;
import es.odissey.protecciones.utils.config.SpecDoubleInRange;
import es.odissey.protecciones.utils.config.SpecIntInRange;
import com.sk89q.worldguard.protection.flags.Flag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Object to represent a protection block as defined in config (blocks folder). The fields are the exact same as
 * the ones in the config.
 */

public class PSProtectBlock {

    // Anotaciones propias de OdisseyProtecciones (paquete utils.config) para mapear el YAML

    // main section
    public String type, alias;
    @Path("description")
    public String description;
    @Path("restrict_obtaining")
    public boolean restrictObtaining;
    @Path("world_list_type")
    public String worldListType;
    @Path("worlds")
    public List<String> worlds;
    @Path("prevent_block_place_in_restricted_world")
    public boolean preventBlockPlaceInRestrictedWorld;
    @Path("allow_placing_in_wild")
    public boolean allowPlacingInWild;
    @Path("placing_bypasses_wg_passthrough")
    public boolean placingBypassesWGPassthrough;

    // region section
    @Path("region.distance_between_claims")
    public int distanceBetweenClaims;
    @Path("region.x_radius")
    @SpecIntInRange(min = 0, max = Integer.MAX_VALUE)
    public int xRadius;
    @Path("region.y_radius")
    @SpecIntInRange(min = -1, max = Integer.MAX_VALUE)
    public int yRadius;
    @Path("region.z_radius")
    @SpecIntInRange(min = 0, max = Integer.MAX_VALUE)
    public int zRadius;
    @Path("region.chunk_radius")
    @SpecIntInRange(min = -1, max = Integer.MAX_VALUE)
    public int chunkRadius;
    @Path("region.home_x_offset")
    public double homeXOffset;
    @Path("region.home_y_offset")
    public double homeYOffset;
    @Path("region.home_z_offset")
    public double homeZOffset;
    @Path("region.flags")
    public List<String> flags;
    @Path("region.allowed_flags")
    public List<String> allowedFlagsRaw;
    @Path("region.hidden_flags_from_info")
    public List<String> hiddenFlagsFromInfo;
    @Path("region.priority")
    public int priority;
    @Path("region.allow_overlap_unowned_regions")
    public boolean allowOverlapUnownedRegions;
    @Path("region.allow_other_regions_to_overlap")
    public String allowOtherRegionsToOverlap;
    @Path("region.allow_merging")
    public boolean allowMerging;
    @Path("region.allowed_merging_into_types")
    public List<String> allowedMergingIntoTypes;

    // block data section
    @Path("block_data.display_name")
    public String displayName;
    @Path("block_data.lore")
    public List<String> lore;
    @Path("block_data.enchanted_effect")
    public boolean enchantedEffect;
    @Path("block_data.price")
    @SpecDoubleInRange(min = 0.0, max = Double.MAX_VALUE)
    public double price;
    @Path("block_data.allow_craft_with_custom_recipe")
    public boolean allowCraftWithCustomRecipe;
    @Path("block_data.custom_recipe")
    public List<List<String>> customRecipe;
    @Path("block_data.recipe_amount")
    @SpecIntInRange(min = 0, max = 64)
    public int recipeAmount;
    @Path("block_data.custom_model_data")
    public int customModelData;

    // economy section
    @Path("economy.tax_amount")
    public double taxAmount;
    @Path("economy.tax_period")
    @SpecIntInRange(min = -1, max = Integer.MAX_VALUE)
    public int taxPeriod;
    @Path("economy.tax_payment_time")
    @SpecIntInRange(min = 1, max = Integer.MAX_VALUE)
    public int taxPaymentTime;
    @Path("economy.start_with_tax_autopay")
    public boolean startWithTaxAutopay;
    @Path("economy.tenant_rent_role")
    public String tenantRentRole;
    @Path("economy.landlord_still_owner")
    public boolean landlordStillOwner;

    // behaviour section
    @Path("behaviour.auto_hide")
    public boolean autoHide;
    @Path("behaviour.auto_merge")
    public boolean autoMerge;
    @Path("behaviour.no_drop")
    public boolean noDrop;
    @Path("behaviour.prevent_piston_push")
    public boolean preventPistonPush;
    @Path("behaviour.prevent_explode")
    public boolean preventExplode;
    @Path("behaviour.prevent_wind_charge_explode")
    public boolean preventWindChargeExplode;
    @Path("behaviour.destroy_region_when_explode")
    public boolean destroyRegionWhenExplode;
    @Path("behaviour.prevent_silk_touch")
    public boolean preventSilkTouch;
    @Path("behaviour.cost_to_place")
    @SpecDoubleInRange(min = 0.0, max = Double.MAX_VALUE)
    public double costToPlace;
    @Path("behaviour.allow_smelt_item")
    public boolean allowSmeltItem;
    @Path("behaviour.allow_use_in_crafting")
    public boolean allowUseInCrafting;

    // player section
    @Path("player.allow_shift_right_break")
    public boolean allowShiftRightBreak;
    @Path("player.prevent_teleport_in")
    public boolean preventTeleportIn;
    @Path("player.no_moving_when_tp_waiting")
    public boolean noMovingWhenTeleportWaiting;
    @Path("player.tp_waiting_seconds")
    @SpecIntInRange(min = 0, max = Integer.MAX_VALUE)
    public int tpWaitingSeconds;
    @Path("player.prevent_ps_get")
    public boolean preventPsGet;
    @Path("player.prevent_ps_home")
    public boolean preventPsHome;
    @Path("player.permission")
    public String permission;

    // event section
    @Path("event.enable")
    public boolean eventsEnabled;
    @Path("event.on_region_create")
    public List<String> regionCreateCommands;
    @Path("event.on_region_destroy")
    public List<String> regionDestroyCommands;


    // shop section (opcional, compatible con AxoProtectionBlock)
    @Path("shop.enabled")
    public Boolean shopEnabled;
    @Path("shop.refund_enabled")
    public Boolean refundEnabled;

    // non-config items
    public HashMap<Flag<?>, Object> regionFlags = new HashMap<>();
    public LinkedHashMap<String, List<String>> allowedFlags = new LinkedHashMap<>();

    /**
     * Obtiene el objeto del bloque de proteccion para este bloque de proteccion concreto.
     *
     * @return el objeto con NBT y otros metadatos que indican que fue creado por OdisseyProtecciones
     */
    public ItemStack createItem() {
        return OdisseyProtecciones.createProtectBlockItem(this);
    }
}
