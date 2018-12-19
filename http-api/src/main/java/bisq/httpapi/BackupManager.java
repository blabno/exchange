package bisq.httpapi;

import org.apache.commons.io.IOUtils;

import java.text.SimpleDateFormat;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import lombok.extern.slf4j.Slf4j;

//TODO @bernard: Why not use BackupManager from core?
@Slf4j
public class BackupManager {

    private Path appDataDirectoryPath;

    public BackupManager(Path appDataDirectoryPath) {
        this.appDataDirectoryPath = appDataDirectoryPath;
    }

    public BackupManager(String appDataDirectory) {
        this(Paths.get(appDataDirectory));
    }

    public String createBackup() throws IOException {
        makeSureBackupDirectoryExists();

        String dateString = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date());
        String backupFilename = "backup-" + dateString + ".zip";
        Path backupFilePath = getBackupFilePath(backupFilename);

        backup(appDataDirectoryPath, backupFilePath.toString());
        return backupFilename;
    }

    public Path getBackupFilePath(String backupFilename) {
        return getBackupDirectoryPath().resolve(backupFilename);
    }

    private Path getBackupDirectoryPath() {
        return appDataDirectoryPath.resolve("backup");
    }

    public List<String> getBackupList() {
        File[] files = getBackupDirectoryPath().toFile().listFiles();
        if (files == null)
            return Collections.emptyList();
        return Arrays.asList(files).stream().map(File::getName).collect(Collectors.toList());
    }

    public FileInputStream getBackup(String fileName) throws FileNotFoundException {
        try {
            return new FileInputStream(getBackupFilePath(fileName).toFile());
        } catch (FileNotFoundException e) {
            throw fileNotFound(fileName);
        }
    }

    public boolean removeBackup(String fileName) throws FileNotFoundException {
        File file = getBackupFilePath(fileName).toFile();
        if (!file.exists()) {
            throw fileNotFound(fileName);
        }
        return file.delete();
    }

    public void restore(String fileName) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(getBackup(fileName))) {
            createBackup();
            purgeAppDataDirectory();
            ZipEntry zipEntry;
            while (null != (zipEntry = zipInputStream.getNextEntry())) {
                String zipEntryName = zipEntry.getName();
                File newFile = appDataDirectoryPath.resolve(zipEntryName).toFile();
                File parentDirectory = newFile.getParentFile();
                if (!parentDirectory.exists() && !parentDirectory.mkdirs())
                    log.error("Problem restoring backup. Unable to create file: " + parentDirectory.getAbsolutePath());

                IOUtils.copy(zipInputStream, new FileOutputStream(newFile));

                zipInputStream.closeEntry();
            }
        }
    }

    public void saveBackup(String backupFilename, InputStream inputStream) throws IOException {
        Path backupFilePath = getBackupFilePath(backupFilename);
        File file = backupFilePath.toFile();
        if (file.exists())
            throw new FileAlreadyExistsException("File already exists: " + backupFilename);
        makeSureBackupDirectoryExists();
        IOUtils.copy(inputStream, new FileOutputStream(file));
    }

    private void backup(Path sourceDir, String outputZipFilename) throws IOException {
        Path relativeBackupDirPath = appDataDirectoryPath.relativize(getBackupDirectoryPath());
        Function<Path, Boolean> shouldSkip = path -> path.startsWith(relativeBackupDirPath);
        try (
                FileOutputStream out = new FileOutputStream(outputZipFilename);
                ZipOutputStream outputStream = new ZipOutputStream(out)
        ) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    try {
                        Path targetFile = sourceDir.relativize(file);
                        if (shouldSkip.apply(targetFile))
                            return FileVisitResult.SKIP_SUBTREE;
                        outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                        byte[] bytes = Files.readAllBytes(file);
                        outputStream.write(bytes, 0, bytes.length);
                        outputStream.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

    }

    private FileNotFoundException fileNotFound(String fileName) {
        return new FileNotFoundException("File not found: " + fileName);
    }

    private void makeSureBackupDirectoryExists() throws IOException {
        Path backupDirectoryPath = getBackupDirectoryPath();
        if (Files.notExists(backupDirectoryPath))
            Files.createDirectory(backupDirectoryPath);
    }

    private void purgeAppDataDirectory() throws IOException {
        Function<Path, Boolean> shouldSkip = path -> path.equals(appDataDirectoryPath) || path.startsWith(getBackupDirectoryPath());
        Files.walkFileTree(appDataDirectoryPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!shouldSkip.apply(dir))
                    Files.delete(dir);
                return super.postVisitDirectory(dir, exc);
            }

            @Override
            public FileVisitResult visitFile(Path targetFile, BasicFileAttributes attributes) {
                if (shouldSkip.apply(targetFile))
                    return FileVisitResult.SKIP_SUBTREE;
                if (!targetFile.toFile().delete())
                    log.warn("Unable to purge data directory. Unable to delete file: " + targetFile);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
