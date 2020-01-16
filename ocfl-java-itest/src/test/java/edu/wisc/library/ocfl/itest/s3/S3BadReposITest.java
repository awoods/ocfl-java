package edu.wisc.library.ocfl.itest.s3;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.aws.OcflS3Client;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.cache.NoOpCache;
import edu.wisc.library.ocfl.core.db.ObjectDetailsDatabaseBuilder;
import edu.wisc.library.ocfl.core.extension.layout.config.DefaultLayoutConfig;
import edu.wisc.library.ocfl.core.lock.ObjectLockBuilder;
import edu.wisc.library.ocfl.core.path.constraint.DefaultContentPathConstraints;
import edu.wisc.library.ocfl.core.storage.cloud.CloudOcflStorage;
import edu.wisc.library.ocfl.core.util.FileUtil;
import edu.wisc.library.ocfl.itest.BadReposITest;
import edu.wisc.library.ocfl.itest.ITestHelper;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class S3BadReposITest extends BadReposITest {

    @RegisterExtension
    public static S3MockExtension S3_MOCK = S3MockExtension.builder().silent().build();

    private final S3Client s3Client = S3_MOCK.createS3ClientV2();

    private S3ITestHelper s3Helper;
    private ComboPooledDataSource dataSource;
    private Set<String> createdBuckets = new HashSet<>();

    @Override
    protected void onBefore() {
        s3Helper = new S3ITestHelper(s3Client);
        dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl("jdbc:h2:mem:test");
    }

    @Override
    protected void onAfter() {
        truncateObjectDetails(dataSource);
        deleteBuckets();
    }

    @Override
    protected MutableOcflRepository defaultRepo(String name) {
        createBucket(name);
        copyFiles(name);
        var repo = new OcflRepositoryBuilder()
                .layoutConfig(DefaultLayoutConfig.flatUrlConfig())
                .inventoryCache(new NoOpCache<>())
                .objectLock(new ObjectLockBuilder().buildDbLock(dataSource))
                .objectDetailsDb(new ObjectDetailsDatabaseBuilder().build(dataSource))
                .inventoryMapper(ITestHelper.testInventoryMapper())
                .contentPathConstraintProcessor(DefaultContentPathConstraints.cloud())
                .buildMutable(CloudOcflStorage.builder()
                        .objectMapper(ITestHelper.prettyPrintMapper())
                        .cloudClient(new OcflS3Client(s3Client, name))
                        .workDir(workDir)
                        .build(), workDir);
        ITestHelper.fixTime(repo, "2019-08-05T15:57:53.703314Z");
        return repo;
    }

    private void copyFiles(String name) {
        var repoDir = repoDir(name);
        FileUtil.findFiles(repoDir).forEach(file -> {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(name)
                    .key(FileUtil.pathToStringStandardSeparator(repoDir.relativize(file)))
                    .build(), file);
        });
    }

    private void truncateObjectDetails(DataSource dataSource) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("TRUNCATE TABLE ocfl_object_details")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createBucket(String name) {
        if (!createdBuckets.contains(name)) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(name).build());
            createdBuckets.add(name);
        }
    }

    private void deleteBuckets() {
        createdBuckets.forEach(bucket -> {
            s3Helper.deleteBucket(bucket);
        });
    }

}