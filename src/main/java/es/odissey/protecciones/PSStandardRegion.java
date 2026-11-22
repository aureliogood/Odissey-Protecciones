package es.odissey.protecciones;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.event.PSRemoveEvent;
import es.odissey.protecciones.utils.MiscUtil;
import es.odissey.protecciones.utils.Objs;
import es.odissey.protecciones.utils.WGUtils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents an instance of a standard PS region, that has not been merged or contains merged regions.
 */

public class PSStandardRegion extends PSRegion {
    private ProtectedRegion wgregion;

    PSStandardRegion(ProtectedRegion wgregion, RegionManager rgmanager, World world) {
        super(rgmanager, world);
        this.wgregion = checkNotNull(wgregion);
    }

    // ~~~~~~~~~~~ instance ~~~~~~~~~~~~~~~~

    @Override
    public String getId() {
        return wgregion.getId();
    }

    @Override
    public String getName() {
        return wgregion.getFlag(FlagHandler.PS_NAME);
    }

    @Override
    public void setName(String name) {
        HashMap<String, ArrayList<String>> m = OdisseyProtecciones.regionNameToID.get(getWorld().getUID());
        if (m == null) { // if the world has not been added
            OdisseyProtecciones.regionNameToID.put(getWorld().getUID(), new HashMap<>());
            m = OdisseyProtecciones.regionNameToID.get(getWorld().getUID());
        }
        if (m.get(getName()) != null) {
            m.get(getName()).remove(getId());
        }
        if (name != null) {
            if (m.containsKey(name)) {
                m.get(name).add(getId());
            } else {
                m.put(name, new ArrayList<>(Collections.singletonList(getId())));
            }
        }
        wgregion.setFlag(FlagHandler.PS_NAME, name);
    }

    @Override
    public void setParent(PSRegion r) throws ProtectedRegion.CircularInheritanceException {
        wgregion.setParent(r == null ? null : r.getWGRegion());
    }

    @Override
    public PSRegion getParent() {
        return wgregion.getParent() == null ? null : fromWGRegion(world, wgregion.getParent());
    }

    @Override
    public Location getHome() {
        String oPos = wgregion.getFlag(FlagHandler.PS_HOME);
        if (oPos == null) return null;
        String[] pos = oPos.split(" ");
        double x = Double.parseDouble(pos[0]), y = Double.parseDouble(pos[1]), z = Double.parseDouble(pos[2]);
        float yaw = pos.length >= 4 ? Float.parseFloat(pos[3]) : 0, pitch = pos.length >= 5 ? Float.parseFloat(pos[4]) : 0;
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public void setHome(double blockX, double blockY, double blockZ) {
        wgregion.setFlag(FlagHandler.PS_HOME, blockX + " " + blockY + " " + blockZ);
    }

    @Override
    public void setHome(double blockX, double blockY, double blockZ, float yaw, float pitch) {
        wgregion.setFlag(FlagHandler.PS_HOME, blockX + " " + blockY + " " + blockZ + " " + yaw + " " + pitch);
    }

    @Override
    public boolean forSale() {
        return wgregion.getFlag(FlagHandler.PS_FOR_SALE) != null && wgregion.getFlag(FlagHandler.PS_FOR_SALE);
    }

    @Override
    public void setSellable(boolean forSale, UUID landlord, double price) {
        if (!forSale) {
            wgregion.setFlag(FlagHandler.PS_LANDLORD, null);
            wgregion.setFlag(FlagHandler.PS_PRICE, null);
            wgregion.setFlag(FlagHandler.PS_FOR_SALE, null);
        } else {
            wgregion.setFlag(FlagHandler.PS_LANDLORD, landlord.toString());
            wgregion.setFlag(FlagHandler.PS_PRICE, price);
            wgregion.setFlag(FlagHandler.PS_FOR_SALE, true);
        }
    }

    @Override
    public void sell(UUID player) {
        UUID landlord = getLandlord();
        if (landlord != null) {
            PSPlayer.fromUUID(player).pay(PSPlayer.fromUUID(landlord), getPrice());
        }
        setSellable(false, null, 0);
        getWGRegion().getOwners().removeAll();
        getWGRegion().getMembers().removeAll();
        clearAllMemberPermissions();
        addOwner(player);
    }

    @Override
    public RentStage getRentStage() {
        if (getLandlord() == null && getTenant() == null) {
            return RentStage.NOT_RENTING;
        } else if (getTenant() == null && !forSale()) {
            return RentStage.LOOKING_FOR_TENANT;
        } else if (getPrice() != null && !forSale()) {
            return RentStage.RENTING;
        }
        return RentStage.NOT_RENTING;
    }

    @Override
    public UUID getLandlord() {
        return wgregion.getFlag(FlagHandler.PS_LANDLORD) == null ? null : UUID.fromString(wgregion.getFlag(FlagHandler.PS_LANDLORD));
    }

    @Override
    public void setLandlord(UUID landlord) {
        wgregion.setFlag(FlagHandler.PS_LANDLORD, landlord == null ? null : landlord.toString());
    }

    @Override
    public UUID getTenant() {
        return wgregion.getFlag(FlagHandler.PS_TENANT) == null ? null : UUID.fromString(wgregion.getFlag(FlagHandler.PS_TENANT));
    }

    @Override
    public void setTenant(UUID tenant) {
        wgregion.setFlag(FlagHandler.PS_TENANT, tenant == null ? null : tenant.toString());
    }

    @Override
    public String getRentPeriod() {
        return wgregion.getFlag(FlagHandler.PS_RENT_PERIOD);
    }

    @Override
    public void setRentPeriod(String s) {
        wgregion.setFlag(FlagHandler.PS_RENT_PERIOD, s);
    }

    @Override
    public Double getPrice() {
        return wgregion.getFlag(FlagHandler.PS_PRICE);
    }

    @Override
    public void setPrice(Double price) {
        wgregion.setFlag(FlagHandler.PS_PRICE, price);
    }

    @Override
    public void setRentLastPaid(Long timestamp) {
        wgregion.setFlag(FlagHandler.PS_RENT_LAST_PAID, timestamp == null ? null : timestamp.doubleValue());
    }

    @Override
    public Long getRentLastPaid() {
        return wgregion.getFlag(FlagHandler.PS_RENT_LAST_PAID) == null ? null : wgregion.getFlag(FlagHandler.PS_RENT_LAST_PAID).longValue();
    }

    @Override
    public void setRentable(UUID landlord, String rentPeriod, double rentPrice) {
        setLandlord(landlord);
        setTenant(null);
        setRentPeriod(rentPeriod);
        setPrice(rentPrice);
    }

    @Override
    public void rentOut(UUID landlord, UUID tenant, String rentPeriod, double rentPrice) {
        setLandlord(landlord);
        setTenant(tenant);
        setRentPeriod(rentPeriod);
        setPrice(rentPrice);
        setRentLastPaid(Instant.now().getEpochSecond());

        OdisseyProtecciones.getEconomy().getRentedList().add(this);

        if (!getTypeOptions().landlordStillOwner) {
            getWGRegion().getOwners().removeAll();
            getWGRegion().getMembers().removeAll();
            clearAllMemberPermissions();
        }
        if (getTypeOptions().tenantRentRole.equals("member")) {
            addMember(tenant);
        } else if (getTypeOptions().tenantRentRole.equals("owner")) {
            addOwner(tenant);
        }
    }

    @Override
    public void removeRenting() {
        getWGRegion().getOwners().removeAll();
        getWGRegion().getMembers().removeAll();
        clearAllMemberPermissions();
        addOwner(getLandlord());

        setLandlord(null);
        setTenant(null);
        setRentPeriod(null);
        setPrice(null);
        setRentLastPaid(null);

        OdisseyProtecciones.getEconomy().getRentedList().remove(this);
    }

    @Override
    public String getTaxPeriod() {
        return MiscUtil.describeDuration(Duration.ofSeconds(getTypeOptions().taxPeriod));
    }

    @Override
    public String getTaxPaymentPeriod() {
        return MiscUtil.describeDuration(Duration.ofSeconds(getTypeOptions().taxPaymentTime));
    }

    @Override
    public List<TaxPayment> getTaxPaymentsDue() {
        // taxes disabled
        if (getTypeOptions().taxPeriod == -1) return new ArrayList<>();

        Set<String> s = wgregion.getFlag(FlagHandler.PS_TAX_PAYMENTS_DUE);
        if (s == null) return new ArrayList<>();

        // convert to TaxPayment objects (getter sin efectos secundarios: no se modifica WorldGuard)
        return s.stream()
                .map(TaxPayment::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void setTaxPaymentsDue(List<TaxPayment> taxPayments) {
        WGUtils.setFlagIfNeeded(wgregion, FlagHandler.PS_TAX_PAYMENTS_DUE, taxPayments.stream().map(TaxPayment::toFlagEntry).collect(Collectors.toSet()));
    }

    @Override
    public List<LastRegionTaxPaymentEntry> getRegionLastTaxPaymentAddedEntries() {
        // taxes disabled
        if (getTypeOptions().taxPeriod == -1) return new ArrayList<>();

        Set<String> s = wgregion.getFlag(FlagHandler.PS_TAX_LAST_PAYMENT_ADDED);
        if (s == null) return new ArrayList<>();

        // convert string entries to object (getter sin efectos secundarios)
        return s.stream()
                .map(LastRegionTaxPaymentEntry::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void setRegionLastTaxPaymentAddedEntries(List<LastRegionTaxPaymentEntry> entries) {
        Set<String> set = entries.stream().map(LastRegionTaxPaymentEntry::toFlagEntry).collect(Collectors.toSet());
        WGUtils.setFlagIfNeeded(wgregion, FlagHandler.PS_TAX_LAST_PAYMENT_ADDED, set);
    }

    @Override
    public UUID getTaxAutopayer() {
        return wgregion.getFlag(FlagHandler.PS_TAX_AUTOPAYER) == null ? null : UUID.fromString(wgregion.getFlag(FlagHandler.PS_TAX_AUTOPAYER));
    }

    @Override
    public void setTaxAutopayer(UUID player) {
        WGUtils.setFlagIfNeeded(wgregion, FlagHandler.PS_TAX_AUTOPAYER, player == null ? null : player.toString());
    }

    @Override
    public EconomyResponse payTax(PSPlayer p, double amount) {
        List<TaxPayment> paymentList = new ArrayList<>(getTaxPaymentsDue());
        Collections.sort(paymentList); // sort by date due

        double paymentAmount = 0;
        for (int i = 0; i < paymentList.size(); i++) {
            TaxPayment tp = paymentList.get(i);
            if (tp.amount > amount) { // if the amount cannot fully pay the tax
                tp.amount -= amount;
                paymentAmount += amount;
                break;
            } else { // if the amount being paid can fully pay off this tax
                amount -= tp.amount;
                paymentAmount += tp.amount;
                paymentList.remove(i);
                i--;
            }
        }

        // only mark the taxes as paid if the withdrawal actually succeeded
        EconomyResponse res = p.withdrawBalance(paymentAmount);
        if (res != null && res.transactionSuccess()) {
            setTaxPaymentsDue(paymentList);
        }
        return res;
    }

    @Override
    public boolean isTaxPaymentLate() {
        // check if taxes disabled for block
        if (getTypeOptions().taxPeriod == -1) return false;

        // update first
        updateTaxPayments();

        long currentTime = System.currentTimeMillis();
        // loop through pending tax payments and see if the payment due date has passed
        for (TaxPayment tp : getTaxPaymentsDue()) {
            if (tp.whenPaymentIsDue < currentTime)
                return true;
        }
        return false;
    }

    @Override
    public void updateTaxPayments() {
        // taxes disabled
        if (getTypeOptions().taxPeriod == -1) return;

        long currentTime = System.currentTimeMillis();

        List<TaxPayment> payments = Objs.replaceNull(getTaxPaymentsDue(), new ArrayList<>());
        List<LastRegionTaxPaymentEntry> lastAdded = Objs.replaceNull(getRegionLastTaxPaymentAddedEntries(), new ArrayList<>());

        boolean changed = false;
        List<LastRegionTaxPaymentEntry> updated = new ArrayList<>();
        for (LastRegionTaxPaymentEntry e : lastAdded) {
            // remove entries that are not for this region
            if (!e.getRegionId().equals(getId())) continue;
            // add payment if it is time for the next payment cycle
            if (e.getLastPaymentAdded() + Duration.ofSeconds(getTypeOptions().taxPeriod).toMillis() < currentTime) {
                e.setLastPaymentAdded(currentTime);
                payments.add(new TaxPayment(currentTime + Duration.ofSeconds(getTypeOptions().taxPaymentTime).toMillis(), getTaxRate(), getId()));
                changed = true;
            }
            updated.add(e);
        }

        // if no entry was found, add a tax payment
        if (updated.isEmpty()) {
            updated.add(new LastRegionTaxPaymentEntry(getId(), currentTime));
            payments.add(new TaxPayment(currentTime + Duration.ofSeconds(getTypeOptions().taxPaymentTime).toMillis(), getTaxRate(), getId()));
            changed = true;
        }

        // solo escribe las flags si algo ha cambiado: antes se reescribian en cada pasada
        if (changed) {
            setTaxPaymentsDue(payments);
            setRegionLastTaxPaymentAddedEntries(updated);
        }
    }

    @Override
    public Block getProtectBlock() {
        PSLocation psl = WGUtils.parsePSRegionToLocation(wgregion.getId());
        return world.getBlockAt(psl.x, psl.y, psl.z);
    }

    @Override
    public PSProtectBlock getTypeOptions() {
        return OdisseyProtecciones.getBlockOptions(getType());
    }

    @Override
    public String getType() {
        return wgregion.getFlag(FlagHandler.PS_BLOCK_MATERIAL);
    }

    @Override
    public void setType(PSProtectBlock type) {
        super.setType(type);
        getWGRegion().setFlag(FlagHandler.PS_BLOCK_MATERIAL, type.type);
    }

    @Override
    public boolean isOwner(UUID uuid) {
        return wgregion.getOwners().contains(uuid);
    }

    @Override
    public boolean isMember(UUID uuid) {
        return wgregion.getMembers().contains(uuid);
    }

    @Override
    public ArrayList<UUID> getOwners() {
        return new ArrayList<>(wgregion.getOwners().getUniqueIds());
    }

    @Override
    public ArrayList<UUID> getMembers() {
        return new ArrayList<>(wgregion.getMembers().getUniqueIds());
    }

    @Override
    public void addOwner(UUID uuid) {
        if (uuid == null) return;
        wgregion.getOwners().addPlayer(uuid);
    }

    @Override
    public void addMember(UUID uuid) {
        if (uuid == null) return;
        wgregion.getMembers().addPlayer(uuid);
    }

    @Override
    public void removeOwner(UUID uuid) {
        if (uuid == null) return;
        // remove tax autopayer if the player is the autopayer
        if (getTaxAutopayer() != null && getTaxAutopayer().equals(uuid)) {
            setTaxAutopayer(null);
        }
        if (getLandlord() != null && getLandlord().equals(uuid)) {
            // remove rents if the player is the landlord
            if (getRentStage() == RentStage.LOOKING_FOR_TENANT || getRentStage() == RentStage.RENTING) {

                if (getTenant() != null) {
                    PSPlayer tenant = PSPlayer.fromUUID(getTenant());
                    if (tenant.getOfflinePlayer().isOnline()) {
                        PSL.msg(Bukkit.getPlayer(getTenant()), PSL.RENT_TENANT_STOPPED_TENANT.msg()
                                .replace("%region%", getName() != null ? getName() : getId()));
                    }
                }

                removeRenting(); // this needs to be called before removing the player, since it adds the player back
            }
            // stop selling the region if the owner was selling it
            setSellable(false, null, 0);
        }
        if (wgregion.getOwners().contains(uuid))
            wgregion.getOwners().removePlayer(uuid);
    }

    @Override
    public void removeMember(UUID uuid) {
        if (uuid == null) return;
        if (wgregion.getMembers().contains(uuid))
            wgregion.getMembers().removePlayer(uuid);
    }

    @Override
    public List<BlockVector2> getPoints() {
        return wgregion.getPoints();
    }

    @Override
    public List<PSRegion> getMergeableRegions(Player p) {
        return WGUtils.findOverlapOrAdjacentRegions(getWGRegion(), getWGRegionManager(), getWorld())
                .stream()
                .map(r -> PSRegion.fromWGRegion(getWorld(), r))
                .filter(r -> r != null && r.getTypeOptions() != null && !r.getId().equals(getId()))
                .filter(r -> r.getTypeOptions().allowMerging)
                .filter(r -> r.isOwner(p.getUniqueId()) || p.hasPermission("odisseyprotecciones.admin"))
                .filter(r -> WGUtils.canMergeRegionTypes(getTypeOptions(), r))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteRegion(boolean deleteBlock) {
        return deleteRegion(deleteBlock, null);
    }

    @Override
    public boolean deleteRegion(boolean deleteBlock, Player cause) {
        PSRemoveEvent event = new PSRemoveEvent(this, cause);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) { // if event was cancelled, prevent execution
            return false;
        }

        // remove the protection hologram
        try {
            HologramManager.getInstance().remove(this);
        } catch (Exception ignored) {
        }

        // set the physical block to air
        if (deleteBlock && !this.isHidden()) {
            this.getProtectBlock().setType(Material.AIR);
        }

        // remove name from cache
        if (getName() != null) {
            HashMap<String, ArrayList<String>> rIds = OdisseyProtecciones.regionNameToID.get(getWorld().getUID());
            if (rIds != null && rIds.containsKey(getName())) {
                if (rIds.get(getName()).size() == 1) {
                    rIds.remove(getName());
                } else {
                    rIds.get(getName()).remove(getId());
                }
            }
        }

        // remove region from WorldGuard
        // specify UNSET_PARENT_IN_CHILDREN removal strategy so that region children don't get deleted
        rgmanager.removeRegion(wgregion.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);

        // stop charging rent for a deleted region
        try {
            if (OdisseyProtecciones.getEconomy() != null) {
                OdisseyProtecciones.getEconomy().getRentedList().remove(this);
            }
        } catch (Exception ignored) {
        }

        // eliminar respaldo de la base de datos
        try {
            es.odissey.protecciones.storage.Database db = OdisseyProtecciones.getInstance().getDatabase();
            if (db != null && db.isEnabled()) {
                db.deleteRegionAsync(getWorld().getName(), getId());
                db.audit(cause == null ? null : cause.getUniqueId().toString(),
                        cause == null ? null : cause.getName(), "delete",
                        getWorld().getName(), getId(), null);
            }
        } catch (Exception ignored) {
        }

        return true;
    }

    @Override
    public ProtectedRegion getWGRegion() {
        return wgregion;
    }
}
