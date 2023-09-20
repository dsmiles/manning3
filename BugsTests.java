package com.assetco.hotspots.optimization;

import com.assetco.hotspots.optimization.*;
import com.assetco.search.results.*;
import org.junit.jupiter.api.*;

import java.math.*;
import java.util.*;

import static com.assetco.search.results.AssetVendorRelationshipLevel.*;
import static com.assetco.search.results.HotspotKey.*;
import static org.junit.jupiter.api.Assertions.*;

// Note that the test doesn't actually "do" anything directly instead it tells another method
// to do something. Even though this is not absolutely necessary, it allows me to clearly express
// the critical aspects of the scenario being tested without being tied to any particular
// implementation details.
public class BugsTests {
    private final int maximumShowcaseItems = 5;
    private SearchResults searchResults;
    private AssetVendor partnerVendor;
    private SearchResultHotspotOptimizer optimizer;

    // Mark this method to be run once before each test
    @BeforeEach
    public void setUp() {
        // While it is not strictly necessary, yet, to initialize these items separately,
        // I find it helps keep distracting "assumption"-type implementation details out
        // of the body of my tests - even if I have only one test.
        optimizer = new SearchResultHotspotOptimizer();
        searchResults = new SearchResults();
        partnerVendor = makeVendor(Partner);
    }

    // Mark the method as a test so it will be executed by the test runner
    @Test
    public void precedingPartnerWithLongTrailingAssetsDoesNotWin() {
        // I don't need to track this, yet, but I like my tests to be very explanatory
        var missing = givenAssetInResultsWithVendor(partnerVendor);
        // This is the "salt" that makes the system work differently from how business expected
        givenAssetInResultsWithVendor(makeVendor(Partner));
        // This is what is actually put in the showcase box
        var expected = givenAssetsInResultsWithVendor(maximumShowcaseItems - 1,  partnerVendor);

        // Build expected assets from above (order of assets in array is important)
        List<Asset> expectedHotspotAssets = new ArrayList<>();
        expectedHotspotAssets.add(missing);
        expectedHotspotAssets.addAll(expected);

        whenOptimize();

        //thenHotspotDoesNotHave(Showcase, missing);    // no longer needed
        thenHotspotHasExactly(Showcase, expectedHotspotAssets);
    }

    // **************************************************
    // * All these methods simplify the above test and  *
    // * will make other, related tests easier to write *
    // **************************************************

    private AssetVendor makeVendor(AssetVendorRelationshipLevel relationshipLevel) {
        return new AssetVendor("anything", "anything", relationshipLevel, 1);
    }

    private Asset givenAssetInResultsWithVendor(AssetVendor vendor) {
        Asset result = getAsset(vendor);
        searchResults.addFound(result);
        return result;
    }

    private Asset getAsset(AssetVendor vendor) {
        return new Asset("anything", "anything", null, null, getPurchaseInfo(), getPurchaseInfo(), new ArrayList<>(), vendor);
    }

    private AssetPurchaseInfo getPurchaseInfo() {
        return new AssetPurchaseInfo(0, 0,
                new Money(new BigDecimal("0")),
                new Money(new BigDecimal("0")));
    }

    private void thenHotspotHasExactly(HotspotKey hotspotKey, List<Asset> expected) {
        Assertions.assertArrayEquals(expected.toArray(), searchResults.getHotspot(hotspotKey).getMembers().toArray());
    }

    private ArrayList<Asset> givenAssetsInResultsWithVendor(int count, AssetVendor vendor) {
        var result = new ArrayList<Asset>();
        for (var i = 0; i < count; ++i) {
            result.add(givenAssetInResultsWithVendor(vendor));
        }
        return result;
    }

    private void whenOptimize() {
        optimizer.optimize(searchResults);
    }

    private void thenHotspotDoesNotHave(HotspotKey key, Asset... forbidden) {
        for (var asset : forbidden)
            assertFalse(searchResults.getHotspot(key).getMembers().contains(asset));
    }
}
