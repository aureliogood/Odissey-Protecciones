package es.odissey.protecciones;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.utils.MiscUtil;
import es.odissey.protecciones.utils.Objs;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a region that exists but is a group of merged {@link PSStandardRegion}s.
 * Contains multiple {@link PSMergedRegion} representing the individual merged regions (which don't technically exist in WorldGuard).
 */

public class PSGroupRegion extends PSStandardRegion {
    PSGroupRegion(ProtectedRegion wgregion, RegionManager rgmanager, World world) {
        super(wgregion, rgmanager, world);
        assert getWGRegion().getFlag(FlagHandler.PS_MERGED_REGIONS) != null;
    }

    @Override
    public double getTaxRate() {
        double taxRate = 0;
        for (PSMergedRegion r : getMergedRegions()) {
            taxRate += r.getTaxRate();
        }
        return taxRate;
    }

    @Override
    public String getTaxPeriod() {
        Set<String> s = new HashSet<>();
        getMergedRegions().forEach(r -> s.add(r.getTaxPeriod()));
        return MiscUtil.concatWithoutLast(new ArrayList<>(s), ", ");
    }

    @Override
    public String getTaxPaymentPeriod() {
        Set<String> s = new HashSet<>();
        getMergedRegions().forEach(r -> s.add(r.getTaxPaymentPeriod()));
        return MiscUtil.concatWithoutLast(new ArrayList<>(s), ", ");
    }

    @Override
    public void updateTaxPayments() {
        long currentTime = System.currentTimeMillis();

        List<TaxPayment> payments = Objs.replaceNull(getTaxPaymentsDue(), new ArrayList<>());
        List<LastRegionTaxPaymentEntry> lastAdded = Objs.replaceNull(getRegionLastTaxPaymentAddedEntries(), new ArrayList<>());

        // loop over merged regions
        for (PSMergedRegion r : getMergedRegions()) {
            // taxes disabled
            if (r.getTypeOptions() == null || r.getTypeOptions().taxPeriod == -1) continue;

            boolean found = false;
            for (LastRegionTaxPaymentEntry last : lastAdded) {
                // if the last region payment entry refers to this region
                if (last.getRegionId().equals(r.getId())) {
                    found = true;
                    // if it's time to pay
                    if (last.getLastPaymentAdded() + Duration.ofSeconds(r.getTypeOptions().taxPeriod).toMillis() < currentTime) {
                        payments.add(new TaxPayment(currentTime + Duration.ofSeconds(r.getTypeOptions().taxPaymentTime).toMillis(), r.getTaxRate(), r.getId()));
                        last.setLastPaymentAdded(currentTime);
                    }
                    break;
                }
            }

            if (!found) {
                payments.add(new TaxPayment(currentTime + Duration.ofSeconds(r.getTypeOptions().taxPaymentTime).toMillis(), r.getTaxRate(), r.getId()));
                lastAdded.add(new LastRegionTaxPaymentEntry(r.getId(), currentTime));
            }
        }
        setTaxPaymentsDue(payments);
        setRegionLastTaxPaymentAddedEntries(lastAdded);
    }

    @Override
    public boolean hide() {
        for (PSMergedRegion r : getMergedRegions()) r.hide();
        return true;
    }

    @Override
    public boolean unhide() {
        for (PSMergedRegion r : getMergedRegions()) r.unhide();
        return true;
    }

    @Override
    public boolean deleteRegion(boolean deleteBlock, Player cause) {
        List<PSMergedRegion> l = getMergedRegions();
        if (super.deleteRegion(deleteBlock, cause)) {
            es.odissey.protecciones.storage.Database db = OdisseyProtecciones.getInstance().getDatabase();
            for (PSMergedRegion r : l) {
                if (deleteBlock && !r.isHidden()) {
                    r.getProtectBlock().setType(Material.AIR);
                }
                // las subregiones fusionadas no existen en WorldGuard pero pueden tener
                // filas de respaldo: si no se borran, restoreMissing las resucita
                if (db != null && db.isEnabled()) {
                    db.deleteRegionAsync(getWorld().getName(), r.getId());
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the merged region whose ID is the same as the group region ID.
     * @return the root region
     */
    public PSMergedRegion getRootRegion() {
        for (PSMergedRegion r : getMergedRegions()) {
            if (r.getId().equals(getId())) return r;
        }
        return null;
    }

    /**
     * Check if this region contains a specific merged region
     * @param id the psID that would've been generated if the merged region was a standard region
     * @return whether or not the id is a merged region
     */
    public boolean hasMergedRegion(String id) {
        Set<String> merged = getWGRegion().getFlag(FlagHandler.PS_MERGED_REGIONS);
        return merged != null && merged.contains(id);
    }

    /**
     * Removes the merged region's information from the object.
     * Note: This DOES NOT remove the actual PSMergedRegion object, you have to call deleteRegion() on that as well.
     * @param id the id of the merged region
     */
    public void removeMergedRegionInfo(String id) {
        Set<String> merged = getWGRegion().getFlag(FlagHandler.PS_MERGED_REGIONS);
        if (merged != null) merged.remove(id);

        // remove from ps merged region types
        Set<String> types = getWGRegion().getFlag(FlagHandler.PS_MERGED_REGIONS_TYPES);
        if (types != null) {
            Iterator<String> i = types.iterator();
            while (i.hasNext()) {
                String[] spl = i.next().split(" ");
                String rid = spl[0];
                if (rid.equals(id)) {
                    i.remove();
                    break;
                }
            }
        }

        // remove from taxes
        Set<String> lastPayments = getWGRegion().getFlag(FlagHandler.PS_TAX_LAST_PAYMENT_ADDED);
        if (lastPayments != null) {
            String entry = "";
            for (String e : lastPayments) {
                if (e.startsWith(id)) entry = e;
            }
            lastPayments.remove(entry);
        }
    }

    /**
     * Get the list of {@link PSMergedRegion} objects of the regions that were merged into this region.
     * @return the list of regions merged into this region
     */
    public List<PSMergedRegion> getMergedRegions() {
        return getMergedRegionsUnsafe().stream()
                .filter(r -> r.getTypeOptions() != null)
                .collect(Collectors.toList());
    }

    /**
     * Get the list of {@link PSMergedRegion} objects of the regions that were merged into this region.
     * Note: This is unsafe as it includes {@link PSMergedRegion}s that are of types not configured in the config.
     * @return the list of regions merged into this region
     */
    public List<PSMergedRegion> getMergedRegionsUnsafe() {
        List<PSMergedRegion> l = new ArrayList<>();
        Set<String> types = getWGRegion().getFlag(FlagHandler.PS_MERGED_REGIONS_TYPES);
        if (types == null) return l;
        for (String line : types) {
            String[] spl = line.split(" ");
            if (spl.length < 2) continue;
            String id = spl[0], type = spl[1];
            l.add(new PSMergedRegion(id, this, getWGRegionManager(), getWorld()));
        }
        return l;
    }
}
