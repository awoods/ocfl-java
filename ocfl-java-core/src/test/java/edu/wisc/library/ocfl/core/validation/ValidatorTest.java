package edu.wisc.library.ocfl.core.validation;

import edu.wisc.library.ocfl.core.validation.storage.FileSystemStorage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Paths;
import java.security.Security;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ValidatorTest {

    private static final String OFFICIAL_BAD_FIXTURES = "official/bad-objects";
    private static final String OFFICIAL_WARN_FIXTURES = "official/warn-objects";
    private static final String CUSTOM_BAD_FIXTURES = "custom/bad-objects";

    @BeforeAll
    public static void beforeAll() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "minimal_content_dir_called_stuff",
            "minimal_mixed_digests",
            "minimal_no_content",
            "minimal_one_version_one_file",
            "minimal_uppercase_digests",
            "ocfl_object_all_fixity_digests",
            "spec-ex-full",
            "updates_all_actions",
            "updates_three_versions_one_file",
    })
    public void validateGoodFixtureObject(String name) {
        var validator = createValidator("official/good-objects");
        var results = validator.validateObject(name, true);
        assertNoIssues(results);
    }

    @Test
    public void errorOnExtraDirInRoot() {
        var name = "E001_extra_dir_in_root";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E001, "Object root E001_extra_dir_in_root contains an unexpected file extra_dir");
        assertWarningsCount(results, 2);
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E001_extra_dir_in_root/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E001_extra_dir_in_root/inventory.json");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnExtraFileInRoot() {
        var name = "E001_extra_file_in_root";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E001, "Object root E001_extra_file_in_root contains an unexpected file extra_file");
        assertWarningsCount(results, 2);
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E001_extra_file_in_root/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E001_extra_file_in_root/inventory.json");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnEmptyObjectRot() {
        var name = "E003_E034_empty";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 2);
        assertHasError(results, ValidationCode.E003, "OCFL object version declaration must exist at E003_E034_empty/0=ocfl_object_1.0");
        assertHasError(results, ValidationCode.E063, "Object root inventory not found at E003_E034_empty/inventory.json");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnNoObjectDeclarationWithOtherIssues() {
        var name = "E003_no_decl";
        var validator = createValidator(CUSTOM_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 7);
        assertHasError(results, ValidationCode.E003, "OCFL object version declaration must exist at E003_no_decl/0=ocfl_object_1.0");
        assertHasError(results, ValidationCode.E102, "Inventory version v1 cannot contain unknown property type in E003_no_decl/inventory.json");
        assertHasError(results, ValidationCode.E038, "Inventory type must equal 'https://ocfl.io/1.0/spec/#inventory' in E003_no_decl/inventory.json");
        assertHasError(results, ValidationCode.E036, "Inventory head must be set in E003_no_decl/inventory.json");
        assertHasError(results, ValidationCode.E048, "Inventory version v1 must contain a created timestamp in E003_no_decl/inventory.json");
        assertHasError(results, ValidationCode.E060, "Inventory at E003_no_decl/inventory.json.sha512 does not match expected sha512 digest. Expected: 1c27836424fc93b67d9eac795f234fcc8c3825d54c26ab7254dfbb47bf432a184df5e96e65bd4c1e2db4c0d5172ce2f0fc589fd6a6a30ebbec0aae7938318815; Found: 14f15a87d1f9d02c1bf9cf08d6c7f9af96d2a69a9715a8dbb2e938cba271e1f204f3b2b6d3df93ead1bb5b7b925fc23dc207207220aa190947349729c2c1f74a");
        assertHasError(results, ValidationCode.E061, "Inventory sidecar file at E003_no_decl/inventory.json.sha512 is in an invalid format");
        assertWarningsCount(results, 2);
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E003_no_decl/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E003_no_decl/inventory.json");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnNoObjectDeclaration() {
        var name = "E003_no_decl";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E003, "OCFL object version declaration must exist at E003_no_decl/0=ocfl_object_1.0");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnBadDeclarationContents() {
        var name = "E007_bad_declaration_contents";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E007, "OCFL object version declaration must be '0=ocfl_object_1.0' in E007_bad_declaration_contents/0=ocfl_object_1.0");
        assertWarningsCount(results, 2);
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E007_bad_declaration_contents/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E007_bad_declaration_contents/inventory.json");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnMissingHeadVersion() {
        var name = "E008_E036_no_versions_no_head";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 2);
        assertHasError(results, ValidationCode.E008, "Inventory must contain at least one version E008_E036_no_versions_no_head/inventory.json");
        assertHasError(results, ValidationCode.E036, "Inventory head must be set in E008_E036_no_versions_no_head/inventory.json");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnContentNotInContentDir() {
        var name = "E015_content_not_in_content_dir";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 20);
        assertHasError(results, ValidationCode.E102, "Inventory cannot contain unknown property digest in E015_content_not_in_content_dir/v1/inventory.json");
        assertHasError(results, ValidationCode.E036, "Inventory id must be set in E015_content_not_in_content_dir/v1/inventory.json");
        assertHasError(results, ValidationCode.E036, "Inventory digest algorithm must be set in E015_content_not_in_content_dir/v1/inventory.json");
        assertHasError(results, ValidationCode.E036, "Inventory head must be set in E015_content_not_in_content_dir/v1/inventory.json");
        assertHasError(results, ValidationCode.E037, "Inventory id is inconsistent between versions in E015_content_not_in_content_dir/v1/inventory.json");
        assertHasError(results, ValidationCode.E040, "Inventory head must be v1 in E015_content_not_in_content_dir/v1/inventory.json");
        assertHasError(results, ValidationCode.E015, "Version directory v1 in E015_content_not_in_content_dir contains an unexpected file inventory.json.sha512");
        assertHasError(results, ValidationCode.E015, "Version directory v1 in E015_content_not_in_content_dir contains an unexpected file a_file.txt");
        assertHasError(results, ValidationCode.E102, "Inventory cannot contain unknown property digest in E015_content_not_in_content_dir/v2/inventory.json");
        assertHasError(results, ValidationCode.E036, "Inventory id must be set in E015_content_not_in_content_dir/v2/inventory.json");
        assertHasError(results, ValidationCode.E036, "Inventory digest algorithm must be set in E015_content_not_in_content_dir/v2/inventory.json");
        assertHasError(results, ValidationCode.E036, "Inventory head must be set in E015_content_not_in_content_dir/v2/inventory.json");
        assertHasError(results, ValidationCode.E037, "Inventory id is inconsistent between versions in E015_content_not_in_content_dir/v2/inventory.json");
        assertHasError(results, ValidationCode.E040, "Inventory head must be v2 in E015_content_not_in_content_dir/v2/inventory.json");
        assertHasError(results, ValidationCode.E015, "Version directory v2 in E015_content_not_in_content_dir contains an unexpected file inventory.json.sha512");
        assertHasError(results, ValidationCode.E015, "Version directory v2 in E015_content_not_in_content_dir contains an unexpected file a_file.txt");
        assertHasError(results, ValidationCode.E015, "Version directory v3 in E015_content_not_in_content_dir contains an unexpected file a_file.txt");
        assertHasError(results, ValidationCode.E092, "Inventory manifest contains content path v1/a_file.txt but this file does not exist in a version content directory in E015_content_not_in_content_dir");
        assertHasError(results, ValidationCode.E092, "Inventory manifest contains content path v3/a_file.txt but this file does not exist in a version content directory in E015_content_not_in_content_dir");
        assertHasError(results, ValidationCode.E092, "Inventory manifest contains content path v2/a_file.txt but this file does not exist in a version content directory in E015_content_not_in_content_dir");
        assertWarningsCount(results, 12);
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E015_content_not_in_content_dir/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E015_content_not_in_content_dir/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v2 should contain a user in E015_content_not_in_content_dir/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v2 should contain a message in E015_content_not_in_content_dir/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v3 should contain a user in E015_content_not_in_content_dir/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v3 should contain a message in E015_content_not_in_content_dir/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E015_content_not_in_content_dir/v1/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E015_content_not_in_content_dir/v1/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E015_content_not_in_content_dir/v2/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E015_content_not_in_content_dir/v2/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v2 should contain a user in E015_content_not_in_content_dir/v2/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v2 should contain a message in E015_content_not_in_content_dir/v2/inventory.json");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnExtraContentFile() {
        var name = "E023_extra_file";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E023, "Object contains a file in version content at E023_extra_file/v1/content/file2.txt that is not referenced in the manifest");
        assertWarningsCount(results, 1);
        assertHasWarn(results, ValidationCode.W009, "Inventory version v1 user address should be a URI in E023_extra_file/inventory.json. Found: somewhere");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnMissingContentFile() {
        var name = "E023_missing_file";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 2);
        assertHasError(results, ValidationCode.E092, "Inventory manifest contains content path v1/content/file2.txt but this file does not exist in a version content directory in E023_missing_file");
        assertHasError(results, ValidationCode.E092, "Failed to validate fixity of E023_missing_file/v1/content/file2.txt: NoSuchFileException: src/test/resources/fixtures/official/bad-objects/E023_missing_file/v1/content/file2.txt");
        assertWarningsCount(results, 1);
        assertHasWarn(results, ValidationCode.W009, "Inventory version v1 user address should be a URI in E023_missing_file/inventory.json. Found: somewhere");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnMissingInventory() {
        var name = "E034_no_inv";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E063, "Object root inventory not found at E034_no_inv/inventory.json");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnMissingId() {
        var name = "E036_no_id";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E036, "Inventory id must be set in E036_no_id/inventory.json");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnHeadDoesNotExist() {
        var name = "E040_wrong_head_doesnt_exist";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 2);
        assertHasError(results, ValidationCode.E044, "Inventory versions is missing an entry for version v2 in E040_wrong_head_doesnt_exist/inventory.json");
        assertHasError(results, ValidationCode.E040, "Inventory head must be the highest version number in E040_wrong_head_doesnt_exist/inventory.json");
        assertWarningsCount(results, 2);
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E040_wrong_head_doesnt_exist/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E040_wrong_head_doesnt_exist/inventory.json");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnWrongHeadFormat() {
        var name = "E040_wrong_head_format";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 2);
        assertHasError(results, ValidationCode.E040, "Inventory head must be a string in E040_wrong_head_format/inventory.json");
        assertHasError(results, ValidationCode.E036, "Inventory head must be set in E040_wrong_head_format/inventory.json");
        assertWarningsCount(results, 2);
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E040_wrong_head_format/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E040_wrong_head_format/inventory.json");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnNoManifest() {
        var name = "E041_no_manifest";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E041, "Inventory manifest must be set in E041_no_manifest/inventory.json");
        assertWarningsCount(results, 2);
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E041_no_manifest/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E041_no_manifest/inventory.json");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnMissingTimezone() {
        var name = "E049_created_no_timezone";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E049, "Inventory version v1 created timestamp must be formatted in accordance to RFC3339 in E049_created_no_timezone/inventory.json. Found: 2019-01-01T02:03:04");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnTimeNotInSeconds() {
        var name = "E049_created_not_to_seconds";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E049, "Inventory version v1 created timestamp must be formatted in accordance to RFC3339 in E049_created_not_to_seconds/inventory.json. Found: 2019-01-01T01:02Z");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnBadVersionBlock() {
        var name = "E049_E050_E054_bad_version_block_values";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 7);
        assertHasError(results, ValidationCode.E049, "Inventory version v1 created timestamp must be a string in E049_E050_E054_bad_version_block_values/inventory.json");
        assertHasError(results, ValidationCode.E050, "Inventory version v1 state must be an object in E049_E050_E054_bad_version_block_values/inventory.json");
        assertHasError(results, ValidationCode.E094, "Inventory version v1 message must be a string in E049_E050_E054_bad_version_block_values/inventory.json");
        assertHasError(results, ValidationCode.E054, "Inventory version v1 user must be an object in E049_E050_E054_bad_version_block_values/inventory.json");
        assertHasError(results, ValidationCode.E048, "Inventory version v1 must contain a created timestamp in E049_E050_E054_bad_version_block_values/inventory.json");
        assertHasError(results, ValidationCode.E054, "Inventory version v1 user name must be set in E049_E050_E054_bad_version_block_values/inventory.json");
        assertHasError(results, ValidationCode.E048, "Inventory version v1 must contain a state in E049_E050_E054_bad_version_block_values/inventory.json");
        assertWarningsCount(results, 2);
        assertHasWarn(results, ValidationCode.W008, "Inventory version v1 user address should be set in E049_E050_E054_bad_version_block_values/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E049_E050_E054_bad_version_block_values/inventory.json");
        assertInfoCount(results, 0);
    }

    // TODO dubious recommended code for this one
    // TODO not currently verifying all manifest entries are used
//    @Test
    public void errorOnFileInManifestNotUsed() {
        var name = "E050_file_in_manifest_not_used";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E050, "Inventory version v1 created timestamp must be a string in E049_E050_E054_bad_version_block_values/inventory.json");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnNoSidecarWithOtherIssues() {
        var name = "E058_no_sidecar";
        var validator = createValidator(CUSTOM_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 4);
        assertHasError(results, ValidationCode.E058, "Inventory sidecar missing at E058_no_sidecar/inventory.json.sha512");
        assertHasError(results, ValidationCode.E023, "Object contains a file in version content at E058_no_sidecar/v1/content/file.txt that is not referenced in the manifest");
        assertHasError(results, ValidationCode.E092, "Inventory manifest contains content path v1/content/a_file.txt but this file does not exist in a version content directory in E058_no_sidecar");
        assertHasError(results, ValidationCode.E092, "Failed to validate fixity of E058_no_sidecar/v1/content/a_file.txt: NoSuchFileException: src/test/resources/fixtures/custom/bad-objects/E058_no_sidecar/v1/content/a_file.txt");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnNoSidecar() {
        var name = "E058_no_sidecar";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E058, "Inventory sidecar missing at E058_no_sidecar/inventory.json.sha512");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnInventoryMismatch() {
        var name = "E064_different_root_and_latest_inventories";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 2);
        assertHasError(results, ValidationCode.E060, "Inventory at E064_different_root_and_latest_inventories/v1/inventory.json.sha512 does not match expected sha512 digest. Expected: 5177ce7b5024f8f41efcf65af6c02d097004a95fe57d430cd407c013b0d836e075df59f8451ab94e494ec2088903e4ef15db0cc27f1f3cc8b9be034b86ae5955; Found: 07105e1ed1a523e668913476c1713b430318d873f175cea39d0554324eca2d5b62fcec98b14c1619a4c8acacfdb1520348e125d51c5e9c9074f1ed08497501e9");
        assertHasError(results, ValidationCode.E064, "The inventory at E064_different_root_and_latest_inventories/v1/inventory.json must be identical to the inventory in the object root");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnFileInExtensionsDir() {
        var name = "E067_file_in_extensions_dir";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E067, "Object extensions directory E067_file_in_extensions_dir/extensions cannot contain file extra_file");
        assertWarningsCount(results, 3);
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in E067_file_in_extensions_dir/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in E067_file_in_extensions_dir/inventory.json");
        assertHasWarn(results, ValidationCode.W013, "Object extensions directory E067_file_in_extensions_dir/extensions contains unregistered extension unregistered");
        assertInfoCount(results, 0);
    }

    @Test
    public void errorOnConflictingLogicalPaths() {
        var name = "E095_conflicting_logical_paths";
        var validator = createValidator(OFFICIAL_BAD_FIXTURES);

        var results = validator.validateObject(name, true);

        assertErrorCount(results, 1);
        assertHasError(results, ValidationCode.E095, "Inventory version v1 paths must be non-conflicting in E095_conflicting_logical_paths/inventory.json. Found conflicting path: sub-path");
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnZeroPaddedVersionsAndOtherErrors() {
        var name = "W001_W004_W005_zero_padded_versions";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 12);
        assertHasWarn(results, ValidationCode.W001, "Object contains zero-padded version v0003 in W001_W004_W005_zero_padded_versions");
        assertHasWarn(results, ValidationCode.W001, "Object contains zero-padded version v0001 in W001_W004_W005_zero_padded_versions");
        assertHasWarn(results, ValidationCode.W001, "Object contains zero-padded version v0004 in W001_W004_W005_zero_padded_versions");
        assertHasWarn(results, ValidationCode.W001, "Object contains zero-padded version v0002 in W001_W004_W005_zero_padded_versions");
        assertHasWarn(results, ValidationCode.W005, "Inventory id should be a URI in W001_W004_W005_zero_padded_versions/inventory.json. Found: bb123cd4567");
        assertHasWarn(results, ValidationCode.W005, "Inventory id should be a URI in W001_W004_W005_zero_padded_versions/v0001/inventory.json. Found: bb123cd4567");
        assertHasWarn(results, ValidationCode.W005, "Inventory id should be a URI in W001_W004_W005_zero_padded_versions/v0003/inventory.json. Found: bb123cd4567");
        assertHasWarn(results, ValidationCode.W005, "Inventory id should be a URI in W001_W004_W005_zero_padded_versions/v0002/inventory.json. Found: bb123cd4567");
        assertHasWarn(results, ValidationCode.W004, "Inventory digest algorithm should be sha512 in W001_W004_W005_zero_padded_versions/inventory.json. Found: sha256");
        assertHasWarn(results, ValidationCode.W004, "Inventory digest algorithm should be sha512 in W001_W004_W005_zero_padded_versions/v0001/inventory.json. Found: sha256");
        assertHasWarn(results, ValidationCode.W004, "Inventory digest algorithm should be sha512 in W001_W004_W005_zero_padded_versions/v0002/inventory.json. Found: sha256");
        assertHasWarn(results, ValidationCode.W004, "Inventory digest algorithm should be sha512 in W001_W004_W005_zero_padded_versions/v0003/inventory.json. Found: sha256");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnZeroPaddedVersions() {
        var name = "W001_zero_padded_versions";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 3);
        assertHasWarn(results, ValidationCode.W001, "Object contains zero-padded version v003 in W001_zero_padded_versions");
        assertHasWarn(results, ValidationCode.W001, "Object contains zero-padded version v002 in W001_zero_padded_versions");
        assertHasWarn(results, ValidationCode.W001, "Object contains zero-padded version v001 in W001_zero_padded_versions");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnExtraDirInVersion() {
        var name = "W002_extra_dir_in_version_dir";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 1);
        assertHasWarn(results, ValidationCode.W002, "Version directory v1 in W002_extra_dir_in_version_dir contains an unexpected directory extra_dir");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnSha256() {
        var name = "W004_uses_sha256";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 1);
        assertHasWarn(results, ValidationCode.W004, "Inventory digest algorithm should be sha512 in W004_uses_sha256/inventory.json. Found: sha256");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnSha256InVersion() {
        var name = "W004_versions_diff_digests";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 1);
        assertHasWarn(results, ValidationCode.W004, "Inventory digest algorithm should be sha512 in W004_versions_diff_digests/v1/inventory.json. Found: sha256");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnIdNotUri() {
        var name = "W005_id_not_uri";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 1);
        assertHasWarn(results, ValidationCode.W005, "Inventory id should be a URI in W005_id_not_uri/inventory.json. Found: not_a_uri");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnNoMessageOrUser() {
        var name = "W007_no_message_or_user";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 2);
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in W007_no_message_or_user/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in W007_no_message_or_user/inventory.json");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnUserNoAddress() {
        var name = "W008_user_no_address";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 1);
        assertHasWarn(results, ValidationCode.W008, "Inventory version v1 user address should be set in W008_user_no_address/inventory.json");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnUserAddressNotUri() {
        var name = "W009_user_address_not_uri";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 1);
        assertHasWarn(results, ValidationCode.W009, "Inventory version v1 user address should be a URI in W009_user_address_not_uri/inventory.json. Found: 1 Wonky Way, Wibblesville, WW");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnNoVersionInventory() {
        var name = "W010_no_version_inventory";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 1);
        assertHasWarn(results, ValidationCode.W010, "Every version should contain an inventory. Missing: W010_no_version_inventory/v1/inventory.json");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnVersionMetaChangeBetweenVersions() {
        var name = "W011_version_inv_diff_metadata";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 3);
        assertHasWarn(results, ValidationCode.W011, "The version created timestamp of version v1 in W011_version_inv_diff_metadata/v1/inventory.json is inconsistent with the root inventory");
        assertHasWarn(results, ValidationCode.W011, "The version message of version v1 in W011_version_inv_diff_metadata/v1/inventory.json is inconsistent with the root inventory");
        assertHasWarn(results, ValidationCode.W011, "The version user of version v1 in W011_version_inv_diff_metadata/v1/inventory.json is inconsistent with the root inventory");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    @Test
    public void warnOnUnregisteredExtension() {
        var name = "W013_unregistered_extension";
        var validator = createValidator(OFFICIAL_WARN_FIXTURES);

        var results = validator.validateObject(name, true);

        assertWarningsCount(results, 3);
        assertHasWarn(results, ValidationCode.W013, "Object extensions directory W013_unregistered_extension/extensions contains unregistered extension unregistered");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a user in W013_unregistered_extension/inventory.json");
        assertHasWarn(results, ValidationCode.W007, "Inventory version v1 should contain a message in W013_unregistered_extension/inventory.json");
        assertErrorCount(results, 0);
        assertInfoCount(results, 0);
    }

    private void assertHasError(ValidationResults results, ValidationCode code, String message) {
        for (var error : results.getErrors()) {
            if (error.getCode() == code && Objects.equals(error.getMessage(), message)) {
                return;
            }
        }

        fail(String.format("Expected error <code=%s; message=%s>. Found: %s",
                code, message, results.getErrors()));
    }

    private void assertHasWarn(ValidationResults results, ValidationCode code, String message) {
        for (var warning : results.getWarnings()) {
            if (warning.getCode() == code && Objects.equals(warning.getMessage(), message)) {
                return;
            }
        }

        fail(String.format("Expected warning <code=%s; message=%s>. Found: %s",
                code, message, results.getWarnings()));
    }

    private void assertNoIssues(ValidationResults results) {
        assertErrorCount(results, 0);
        assertWarningsCount(results, 0);
        assertInfoCount(results, 0);
    }

    private void assertErrorCount(ValidationResults results, int count) {
        assertEquals(count, results.getErrors().size(),
                () -> String.format("Expected %s errors. Found: %s", count, results.getErrors()));
    }

    private void assertWarningsCount(ValidationResults results, int count) {
        assertEquals(count, results.getWarnings().size(),
                () -> String.format("Expected %s warnings. Found: %s", count, results.getWarnings()));
    }

    private void assertInfoCount(ValidationResults results, int count) {
        assertEquals(count, results.getInfos().size(),
                () -> String.format("Expected %s info. Found: %s", count, results.getInfos()));
    }

    private Validator createValidator(String rootName) {
        var storage = new FileSystemStorage(Paths.get("src/test/resources/fixtures", rootName));
        return new Validator(storage);
    }

}
