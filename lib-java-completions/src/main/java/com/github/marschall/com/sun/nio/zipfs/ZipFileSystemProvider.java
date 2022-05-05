package com.github.marschall.com.sun.nio.zipfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipError;

/*
 *
 * @author  Xueming Shen, Rajendra Gutupalli, Jaya Hangal
 */

public class ZipFileSystemProvider extends FileSystemProvider {


    private final Map<Path, ZipFileSystem> filesystems = new HashMap<>();

    public ZipFileSystemProvider() {}

    @Override
    public String getScheme() {
        return "zipfs";
    }

    protected Path uriToPath(URI uri) {
        String scheme = uri.getScheme();
        if ((scheme == null) || !scheme.equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("URI scheme is not '" + getScheme() + "'");
        }
        try {
            // only support legacy JAR URL syntax  jar:{uri}!/{entry} for now
            String spec = uri.getRawSchemeSpecificPart();
            int sep = spec.indexOf("!/");
            if (sep != -1)
                spec = spec.substring(0, sep);
            return Paths.get(new URI(spec)).toAbsolutePath();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private boolean ensureFile(Path path) {
        try {
            BasicFileAttributes attrs =
                    Files.readAttributes(path, BasicFileAttributes.class);
            if (!attrs.isRegularFile())
                throw new UnsupportedOperationException();
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env)
            throws IOException
    {
        Path path = uriToPath(uri);
        synchronized(filesystems) {
            Path realPath = null;
            if (ensureFile(path)) {
                realPath = path.toRealPath();
                if (filesystems.containsKey(realPath))
                    throw new FileSystemAlreadyExistsException();
            }
            ZipFileSystem zipfs = null;
            try {
                zipfs = new ZipFileSystem(this, path, env);
            } catch (ZipError ze) {
                String pname = path.toString();
                if (pname.endsWith(".zip") || pname.endsWith(".jar"))
                    throw ze;
                // assume NOT a zip/jar file
                throw new UnsupportedOperationException();
            }
            filesystems.put(realPath, zipfs);
            return zipfs;
        }
    }

    public static FileSystem newJarFileSystem(Path path, Map<String, ?> env) throws IOException {
        JarFileSystemProvider provider = new JarFileSystemProvider();
        return new JarFileSystem(provider, path, env);
    }

    public static FileSystem newJarFileSystem(URI uri, Map<String, ?> env) throws IOException {
        JarFileSystemProvider provider = new JarFileSystemProvider();
        return new JarFileSystem(provider, provider.uriToPath(uri), env);
    }
    @Override
    public FileSystem newFileSystem(Path path, Map<String, ?> env)
            throws IOException
    {
        if (path.getFileSystem() != FileSystems.getDefault()) {
            throw new UnsupportedOperationException();
        }
        ensureFile(path);
        try {
            return new ZipFileSystem(this, path, env);
        } catch (ZipError ze) {
            String pname = path.toString();
            if (pname.endsWith(".zip") || pname.endsWith(".jar"))
                throw ze;
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Path getPath(URI uri) {

        String spec = uri.getSchemeSpecificPart();
        int sep = spec.indexOf("!/");
        if (sep == -1)
            throw new IllegalArgumentException("URI: "
                    + uri
                    + " does not contain path info ex. jar:file:/c:/foo.zip!/BAR");
        return getFileSystem(uri).getPath(spec.substring(sep + 1));
    }


    @Override
    public FileSystem getFileSystem(URI uri) {
        synchronized (filesystems) {
            ZipFileSystem zipfs = null;
            try {
                zipfs = filesystems.get(uriToPath(uri).toRealPath());
            } catch (IOException x) {
                // ignore the ioe from toRealPath(), return FSNFE
            }
            if (zipfs == null)
                throw new FileSystemNotFoundException();
            return zipfs;
        }
    }

    // Checks that the given file is a UnixPath
    static final ZipPath toZipPath(Path path) {
        if (path == null)
            throw new NullPointerException();
        if (!(path instanceof ZipPath))
            throw new ProviderMismatchException();
        return (ZipPath)path;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        toZipPath(path).checkAccess(modes);
    }

    @Override
    public void copy(Path src, Path target, CopyOption... options)
            throws IOException
    {
        toZipPath(src).copy(toZipPath(target), options);
    }

    @Override
    public void createDirectory(Path path, FileAttribute<?>... attrs)
            throws IOException
    {
        toZipPath(path).createDirectory(attrs);
    }

    @Override
    public final void delete(Path path) throws IOException {
        toZipPath(path).delete();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V
    getFileAttributeView(Path path, Class<V> type, LinkOption... options)
    {
        return ZipFileAttributeView.get(toZipPath(path), type);
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return toZipPath(path).getFileStore();
    }

    @Override
    public boolean isHidden(Path path) {
        return toZipPath(path).isHidden();
    }

    @Override
    public boolean isSameFile(Path path, Path other) throws IOException {
        return toZipPath(path).isSameFile(other);
    }

    @Override
    public void move(Path src, Path target, CopyOption... options)
            throws IOException
    {
        toZipPath(src).move(toZipPath(target), options);
    }

    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path,
                                                              Set<? extends OpenOption> options,
                                                              ExecutorService exec,
                                                              FileAttribute<?>... attrs)
            throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs)
            throws IOException
    {
        return toZipPath(path).newByteChannel(options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(
            Path path, Filter<? super Path> filter) throws IOException
    {
        return toZipPath(path).newDirectoryStream(filter);
    }

    @Override
    public FileChannel newFileChannel(Path path,
                                      Set<? extends OpenOption> options,
                                      FileAttribute<?>... attrs)
            throws IOException
    {
        return toZipPath(path).newFileChannel(options, attrs);
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options)
            throws IOException
    {
        return toZipPath(path).newInputStream(options);
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options)
            throws IOException
    {
        return toZipPath(path).newOutputStream(options);
    }

    @Override
    public <A extends BasicFileAttributes> A
    readAttributes(Path path, Class<A> type, LinkOption... options)
            throws IOException
    {
        if (type == BasicFileAttributes.class || type == ZipFileAttributes.class)
            return (A)toZipPath(path).getAttributes();
        return null;
    }

    @Override
    public Map<String, Object>
    readAttributes(Path path, String attribute, LinkOption... options)
            throws IOException
    {
        return toZipPath(path).readAttributes(attribute, options);
    }

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setAttribute(Path path, String attribute,
                             Object value, LinkOption... options)
            throws IOException
    {
        toZipPath(path).setAttribute(attribute, value, options);
    }

    //////////////////////////////////////////////////////////////
    void removeFileSystem(Path zfpath, ZipFileSystem zfs) throws IOException {
        synchronized (filesystems) {
            zfpath = zfpath.toRealPath();
            if (filesystems.get(zfpath) == zfs)
                filesystems.remove(zfpath);
        }
    }
}