package com.assetco.hotspots.optimization;

import com.assetco.search.results.*;

import java.util.*;

import static com.assetco.search.results.AssetVendorRelationshipLevel.*;
import static com.assetco.search.results.HotspotKey.*;

// This code manages filling the showcase if it's not already set
// it make sure the first partner-lvl vendor with enough assets on the page gets the showcase
//
// From Jamie's reqs:
//   1. If a Partner-level vendor has at least three (3) assets in the result set, that partner's assets shall own the showcase
//   2. If two (2) Partner-level vendors meet the criteria to own the showcase, the first vendor to meet the criteria shall own the showcase
//   3. If a Partner-level has more than five (5) showcase assets, additional assets shall be treated as Top Picks
//
// -johnw
// 1/3/07

/**
 * Assigns assets to the showcase hotspot group based on their vendor status.
 */
class RelationshipBasedOptimizer {

    public void optimize(SearchResults searchResults) {
        Iterator<Asset> iterator = searchResults.getFound().iterator();
        // Don't affect a showcase built by an earlier rule
        var showcaseFull = searchResults.getHotspot(Showcase).getMembers().size() > 0;
        var showcaseAssets = new ArrayList<Asset>();
        var goldAssets = new ArrayList<Asset>();
        var silverAssets = new ArrayList<Asset>();

        // Create a map to store lists of assets for each partner
        Map<AssetVendor, List<Asset>> partnerAssetsMap = new HashMap<>();

        while (iterator.hasNext()) {
            Asset asset = iterator.next();
            // HACK! Trap gold and silver assets for use later
            if (asset.getVendor().getRelationshipLevel() == Gold)
                goldAssets.add(asset);
            else if (asset.getVendor().getRelationshipLevel() == Silver)
                silverAssets.add(asset);

            if (asset.getVendor().getRelationshipLevel() != Partner)
                continue;

            // Put the partner asset into the map - each partner has its own list
            partnerAssetsMap.computeIfAbsent(asset.getVendor(), k -> new ArrayList<>()).add(asset);

            // If a partner has reached the 3-asset minimum, lock in their showcase
            if (partnerAssetsMap.get(asset.getVendor()).size() >= 3) {
                showcaseAssets.clear();
                showcaseAssets.addAll(partnerAssetsMap.get(asset.getVendor()));
            }

            // Too many assets in the showcase - put in top picks instead
            if (showcaseAssets.size() >= 5) {
                if (!Objects.equals(showcaseAssets.get(0).getVendor(), asset.getVendor()))
                    searchResults.getHotspot(TopPicks).addMember(asset);
            } else {
                // Add this asset to an empty showcase or showcase with the same vendor in it
                // If there's already another vendor, that vendor should take precedence
                if (showcaseAssets.isEmpty() || Objects.equals(showcaseAssets.get(0).getVendor(), asset.getVendor()))
                    showcaseAssets.add(asset);
            }
        }

        // Add showcaseAssets to the Showcase hotspot if there are enough for a partner to claim the showcase
        if (!showcaseFull && showcaseAssets.size() >= 3) {
            // Clear any existing members - how can I do this ??
            for (var asset : showcaseAssets)
                searchResults.getHotspot(Showcase).addMember(asset);
        }

        // Add goldAssets to HighValue hotspot if there are no partner assets in the search
        if (partnerAssetsMap.isEmpty()) {
            for (var asset : goldAssets)
                searchResults.getHotspot(HighValue).addMember(asset);
        }

        // Add all partner assets to the Fold hotspot
        for (List<Asset> partnerAssets : partnerAssetsMap.values()) {
            for (var asset : partnerAssets)
                searchResults.getHotspot(Fold).addMember(asset);
        }

        // acw-14341: gold assets should appear in fold box when appropriate
        // Add goldAssets and silverAssets to Fold hotspot
        for (var asset : goldAssets)
            searchResults.getHotspot(Fold).addMember(asset);

        // LOL acw-14511: gold assets should appear in fold box when appropriate
        for (var asset: silverAssets)
            searchResults.getHotspot(Fold).addMember(asset);
    }
}
