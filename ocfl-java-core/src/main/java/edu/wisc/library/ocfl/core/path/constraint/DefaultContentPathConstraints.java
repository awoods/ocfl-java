package edu.wisc.library.ocfl.core.path.constraint;

import java.util.regex.Pattern;

/**
 * This class provides default path constraints that can be applied to OCFL content paths to attempt to ensure portability
 * across various filesystems. It is useful to apply constraints for filesystems other than the local filesystem,
 * as the local filesystem will readily enforce its own constraints.
 *
 * <p>The constraints defined here are generalizations and do not exhaustively target specific filesystems. If you have
 * specific needs that are not addressed here, create a custom ContentPathConstraintProcessor.
 *
 * <p>The following constraints are ALWAYS applied:
 *
 * <ul>
 *     <li>Cannot have a trailing /</li>
 *     <li>Cannot contain the following filenames: '.', '..'</li>
 *     <li>Cannot contain an empty filename</li>
 *     <li>Windows only: Cannot contain a \</li>
 * </ul>
 */
public final class DefaultContentPathConstraints {

    private static final char NUL = 0;
    private static final char ASCII_CTRL_START = 0;
    private static final char ASCII_CTRL_END = 31;
    private static final char ASCII_CTRL_EXT_START = 127;
    private static final char ASCII_CTRL_EXT_END = 160;

    private static final FileNameConstraint NO_SPACE_OR_PERIOD_AT_END = RegexPathConstraint.mustNotContain(Pattern.compile("^.*[ .]$"));
    private static final FileNameConstraint NO_WINDOWS_RESERVED_WORDS = RegexPathConstraint.mustNotContain(Pattern.compile(
            "^(?:CON|PRN|AUX|NUL|" +
                    "COM1|COM2|COM3|COM4|" +
                    "COM5|COM6|COM7|COM8|" +
                    "COM9|LPT1|LPT2|LPT3|" +
                    "LPT4|LPT5|LPT6|LPT7|" +
                    "LPT8|LPT9)(?:\\.[^.]+)?$",
            Pattern.CASE_INSENSITIVE
    ));

    private static final PathCharConstraint NO_ASCII_CTRL = BitSetPathCharConstraint.blackListRange(ASCII_CTRL_START, ASCII_CTRL_END);
    private static final PathCharConstraint NO_ASCII_EXT_CTRL = BitSetPathCharConstraint.blackListRange(ASCII_CTRL_EXT_START, ASCII_CTRL_EXT_END);

    private DefaultContentPathConstraints() {

    }

    /**
     * Constructs a ContentPathConstraintProcessor that applies the minimum path constraints on a unix based filesystem.
     * This does not guarantee that every path that passes validation will work on any unix based filesystem, but the
     * most problematic characters are prohibited.
     *
     * <ul>
     *   <li><a href="https://en.wikipedia.org/wiki/Comparison_of_file_systems#Limits">Comparison of File Systems</a></li>
     * </ul>
     *
     * @return ContentPathConstraintProcessor
     */
    public static ContentPathConstraintProcessor unix() {
        return ContentPathConstraintProcessor.builder()
                .contentPathConstraintProcessor(PathConstraintProcessor.builder()
                        .fileNameConstraint(PathLengthConstraint.maxBytes(255))
                        .charConstraint(BitSetPathCharConstraint.blackList(NUL))
                        .build())
                .build();
    }

    /**
     * Constructs a ContentPathConstraintProcessor that applies the minimum path constraints on a Windows based filesystem.
     * This does not guarantee that every path that passes validation will work on any Windows based filesystem, but the
     * most problematic characters are prohibited.
     *
     * <ul>
     *   <li><a href="https://en.wikipedia.org/wiki/Comparison_of_file_systems#Limits">Comparison of File Systems</a></li>
     *   <li><a href="https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file">Windows File Naming</a></li>
     * </ul>
     *
     * @return ContentPathConstraintProcessor
     */
    public static ContentPathConstraintProcessor windows() {
        return ContentPathConstraintProcessor.builder()
                .contentPathConstraintProcessor(PathConstraintProcessor.builder()
                        .fileNameConstraint(PathLengthConstraint.maxChars(255))
                        .fileNameConstraint(NO_WINDOWS_RESERVED_WORDS)
                        .fileNameConstraint(NO_SPACE_OR_PERIOD_AT_END)
                        .charConstraint(NO_ASCII_CTRL)
                        .charConstraint(BitSetPathCharConstraint.blackList('<', '>', ':', '"', '\\', '|', '?', '*'))
                        .build())
                .build();
    }

    /**
     * Constructs a ContentPathConstraintProcessor that applies the minimum path constraints across cloud providers (Amazon, Azure, Google).
     * This does not guarantee that every path that passes validation will work with all cloud providers, but the most problematic
     * characters are prohibited.
     *
     * <ul>
     *   <li><a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html#object-keys">Amazon S3</a></li>
     *   <li><a href="https://docs.microsoft.com/en-us/rest/api/storageservices/naming-and-referencing-containers--blobs--and-metadata">Azure Blob Storage</a></li>
     *   <li><a href="https://cloud.google.com/storage/docs/naming#objectnames">Google Cloud Storage</a></li>
     *   <li><a href="https://stackoverflow.com/questions/54805654/do-we-have-list-of-unsupported-characters-for-azure-blob-file-names/58039891#58039891">Azure additional</a></li>
     * </ul>
     *
     * @return ContentPathConstraintProcessor
     */
    public static ContentPathConstraintProcessor cloud() {
        return ContentPathConstraintProcessor.builder()
                .storagePathConstraintProcessor(PathConstraintProcessor.builder()
                        .pathConstraint(PathLengthConstraint.maxBytes(1024)) // Amazon & Google
                        .build())
                .contentPathConstraintProcessor(PathConstraintProcessor.builder()
                        .fileNameConstraint(PathLengthConstraint.maxChars(254)) // Azure
                        .fileNameConstraint(NO_SPACE_OR_PERIOD_AT_END) // Azure
                        .charConstraint(NO_ASCII_CTRL) // Azure & Google
                        .charConstraint(NO_ASCII_EXT_CTRL) // Google
                        .charConstraint(BitSetPathCharConstraint.blackList('\\', '#', '[', ']', '*', '?')) // Azure & Google (mostly Google)
                        .build())
                .build();
    }

    /**
     * Constructs a ContentPathConstraintProcessor that applies the minimum path constraints across Unix, Windows, and cloud filesystems.
     * This does not guarantee that every path that passes validation will work on any filesystem, but the most problematic
     * characters across systems are prohibited.
     *
     * <ul>
     *   <li><a href="https://en.wikipedia.org/wiki/Comparison_of_file_systems#Limits">Comparison of File Systems</a></li>
     *   <li><a href="https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file">Windows File Naming</a></li>
     *   <li><a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html#object-keys">Amazon S3</a></li>
     *   <li><a href="https://docs.microsoft.com/en-us/rest/api/storageservices/naming-and-referencing-containers--blobs--and-metadata">Azure Blob Storage</a></li>
     *   <li><a href="https://cloud.google.com/storage/docs/naming#objectnames">Google Cloud Storage</a></li>
     *   <li><a href="https://stackoverflow.com/questions/54805654/do-we-have-list-of-unsupported-characters-for-azure-blob-file-names/58039891#58039891">Azure additional</a></li>
     * </ul>
     *
     * @return ContentPathConstraintProcessor
     */
    public static ContentPathConstraintProcessor all() {
        return ContentPathConstraintProcessor.builder()
                .storagePathConstraintProcessor(PathConstraintProcessor.builder()
                        .pathConstraint(PathLengthConstraint.maxBytes(1024)) // Amazon & Google
                        .build())
                .contentPathConstraintProcessor(PathConstraintProcessor.builder()
                        .fileNameConstraint(PathLengthConstraint.maxChars(254)) // Azure
                        .fileNameConstraint(NO_WINDOWS_RESERVED_WORDS) // Windows
                        .fileNameConstraint(NO_SPACE_OR_PERIOD_AT_END) // Windows & Azure
                        .charConstraint(NO_ASCII_CTRL) // Windows, Azure, Google
                        .charConstraint(NO_ASCII_EXT_CTRL) // Google
                        .charConstraint(BitSetPathCharConstraint.blackList(
                                '<', '>', ':', '"', '\\', '|', '?', '*', '#', '[', ']' // Windows & Google
                        ))
                        .build())
                .build();
    }

    /**
     * Constructs a ContentPathConstraintProcessor that does no special validation.
     *
     * @return ContentPathConstraintProcessor
     */
    public static ContentPathConstraintProcessor none() {
        return ContentPathConstraintProcessor.builder().build();
    }

}
