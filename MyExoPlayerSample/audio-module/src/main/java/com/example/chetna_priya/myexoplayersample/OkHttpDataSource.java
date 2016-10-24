package com.example.chetna_priya.myexoplayersample;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.HttpDataSource;
import com.google.android.exoplayer.upstream.TransferListener;
import com.google.android.exoplayer.util.Assertions;
import com.google.android.exoplayer.util.Predicate;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static okhttp3.internal.Util.closeQuietly;


/**
 * Created by chetna_priya on 9/3/2016.
 */
public class OkHttpDataSource implements HttpDataSource {

    private static final String TAG = "OkHttpDataSource";
    private static final AtomicReference<byte[]> skipBufferReference = new AtomicReference<>();

    private final String userAgent;
    private final Predicate<String> contentTypePredicate;
    private final HashMap<String, String> requestProperties;
    private static final OkHttpClient okHttpClient = new OkHttpClient();

    private final TransferListener listener;
    private DataSpec dataSpec;
    private Response response;
    private boolean opened;

    private long bytesToSkip;
    private long bytesToRead;

    private long bytesSkipped;
    private long bytesRead;
/*
    static {
        okHttpClient.networkInterceptors().add(new StethoInterceptor());
    }*/

    /**
     * @param userAgent            The User-Agent string that should be used.
     * @param contentTypePredicate An optional {@link Predicate}. If a content type is
     *                             rejected by the predicate then a {@link InvalidContentTypeException} is
     *                             thrown from {@link #open(DataSpec)}.
     */
    public OkHttpDataSource(String userAgent, Predicate<String> contentTypePredicate) {
        this(userAgent, contentTypePredicate, null);
    }

    /**
     * @param userAgent            The User-Agent string that should be used.
     * @param contentTypePredicate An optional {@link Predicate}. If a content type is
     *                             rejected by the predicate then a {@link InvalidContentTypeException} is
     *                             thrown from {@link #open(DataSpec)}.
     * @param listener             An optional listener.
     */
    public OkHttpDataSource(String userAgent, Predicate<String> contentTypePredicate,
                            TransferListener listener) {
        this.userAgent = Assertions.checkNotEmpty(userAgent);
        this.contentTypePredicate = contentTypePredicate;
        this.listener = listener;
        this.requestProperties = new HashMap<>();
    }

    @Override
    public String getUri() {
        return response == null ? null : response.request().url().toString();
    }

    @Override
    public void setRequestProperty(String name, String value) {
        Assertions.checkNotNull(name);
        Assertions.checkNotNull(value);
        synchronized (requestProperties) {
            requestProperties.put(name, value);
        }
    }

    @Override
    public void clearRequestProperty(String name) {
        Assertions.checkNotNull(name);
        synchronized (requestProperties) {
            requestProperties.remove(name);
        }
    }

    @Override
    public void clearAllRequestProperties() {
        synchronized (requestProperties) {
            requestProperties.clear();
        }
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return null;
    }

    @Override
    public long open(DataSpec dataSpec) throws HttpDataSourceException {
        this.dataSpec = dataSpec;
        this.bytesRead = 0;
        this.bytesSkipped = 0;
        long position = dataSpec.position;
        long length = dataSpec.length;
        boolean allowGzip = (dataSpec.flags & DataSpec.FLAG_ALLOW_GZIP) != 0;
        HttpUrl url = HttpUrl.parse(dataSpec.uri.toString());
        Request request = configureRequest(url, position, length, allowGzip);
        try {
            response = okHttpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new HttpDataSourceException("Unable to connect to " + dataSpec.uri.toString(), e,
                    dataSpec, HttpDataSourceException.TYPE_OPEN);
        }

        int responseCode = response.code();

        // Check for a valid response code.
        if (!response.isSuccessful()) {
            Map<String, List<String>> headers = request.headers().toMultimap();
            closeConnection();
            throw new InvalidResponseCodeException(responseCode, headers, dataSpec);
        }

        // Check for a valid content type.
        String contentType = response.body().contentType().toString();
        if (contentTypePredicate != null && !contentTypePredicate.evaluate(contentType)) {
            closeConnection();
            throw new InvalidContentTypeException(contentType, dataSpec);
        }

        // If we requested a range starting from a non-zero position and received a 200 rather than a
        // 206, then the server does not support partial requests. We'll need to manually skip to the
        // requested position.
        bytesToSkip = responseCode == 200 && dataSpec.position != 0 ? dataSpec.position : 0;

        // Determine the length of the data to be read, after skipping.
        if ((dataSpec.flags & DataSpec.FLAG_ALLOW_GZIP) == 0) {
            long contentLength = 0;
            try {
                contentLength = response.body().contentLength();
            } catch (Exception e) {
                e.printStackTrace();
                closeConnection();
                throw new HttpDataSourceException(dataSpec, HttpDataSourceException.TYPE_READ);
            }
            bytesToRead = dataSpec.length != C.LENGTH_UNBOUNDED ? dataSpec.length
                    : contentLength != C.LENGTH_UNBOUNDED ? contentLength - bytesToSkip
                    : C.LENGTH_UNBOUNDED;
        } else {
            // Gzip is enabled. If the server opts to use gzip then the content length in the response
            // will be that of the compressed data, which isn't what we want. Furthermore, there isn't a
            // reliable way to determine whether the gzip was used or not. Always use the dataSpec length
            // in this case.
            bytesToRead = dataSpec.length;
        }

        opened = true;
        if (listener != null) {
            listener.onTransferStart();
        }

        return bytesToRead;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws HttpDataSourceException {
        try {
            skipInternal();
            return readInternal(buffer, offset, readLength);
        } catch (IOException e) {
            throw new HttpDataSourceException(e, dataSpec, HttpDataSourceException.TYPE_READ);
        }
    }

    @Override
    public void close() throws HttpDataSourceException {
        if (opened) {
            opened = false;
            if (listener != null) {
                listener.onTransferEnd();
            }
            closeConnection();
        }
    }

    /**
     * Returns the current connection, or null if the source is not currently opened.
     *
     * @return The current open connection, or null.
     */
    protected final OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    /**
     * Returns the number of bytes that have been skipped since the most recent call to
     * {@link #open(DataSpec)}.
     *
     * @return The number of bytes skipped.
     */
    protected final long bytesSkipped() {
        return bytesSkipped;
    }

    /**
     * Returns the number of bytes that have been read since the most recent call to
     * {@link #open(DataSpec)}.
     *
     * @return The number of bytes read.
     */
    protected final long bytesRead() {
        return bytesRead;
    }

    /**
     * Returns the number of bytes that are still to be read for the current {@link DataSpec}.
     * <p/>
     * If the total length of the data being read is known, then this length minus {@code bytesRead()}
     * is returned. If the total length is unknown, {@link C#LENGTH_UNBOUNDED} is returned.
     *
     * @return The remaining length, or {@link C#LENGTH_UNBOUNDED}.
     */
    protected final long bytesRemaining() {
        return bytesToRead == C.LENGTH_UNBOUNDED ? bytesToRead : bytesToRead - bytesRead;
    }


    /**
     * Configures a {@link Request}.
     *
     * @param url       The url to connect to.
     * @param position  The byte offset of the requested data.
     * @param length    The length of the requested data, or {@link C#LENGTH_UNBOUNDED}.
     * @param allowGzip Whether to allow the use of gzip.
     */
    private Request configureRequest(HttpUrl url, long position, long length,
                                     boolean allowGzip) {
        Request.Builder builder = new Request.Builder().url(url);
        synchronized (requestProperties) {
            for (Map.Entry<String, String> property : requestProperties.entrySet()) {
                builder.addHeader(property.getKey(), property.getValue());
            }
        }
        if (!(position == 0 && length == C.LENGTH_UNBOUNDED)) {
            String rangeRequest = "bytes=" + position + "-";
            if (length != C.LENGTH_UNBOUNDED) {
                rangeRequest += (position + length - 1);
            }
            builder.addHeader("Range", rangeRequest);
        }
        builder.addHeader("User-Agent", userAgent);
        if (!allowGzip) {
            builder.addHeader("Accept-Encoding", "identity");
        }
        return builder.build();
    }

    /**
     * Handles a redirect.
     *
     * @param originalUrl The original URL.
     * @param location    The Location header in the response.
     * @return The next URL.
     * @throws IOException If redirection isn't possible.
     */
    private static URL handleRedirect(URL originalUrl, String location) throws IOException {
        if (location == null) {
            throw new ProtocolException("Null location redirect");
        }
        // Form the new url.
        URL url = new URL(originalUrl, location);
        // Check that the protocol of the new url is supported.
        String protocol = url.getProtocol();
        if (!"https".equals(protocol) && !"http".equals(protocol)) {
            throw new ProtocolException("Unsupported protocol redirect: " + protocol);
        }
        // Currently this method is only called if allowCrossProtocolRedirects is true, and so the code
        // below isn't required. If we ever decide to handle redirects ourselves when cross-protocol
        // redirects are disabled, we'll need to uncomment this block of code.
        // if (!allowCrossProtocolRedirects && !protocol.equals(originalUrl.getProtocol())) {
        //   throw new ProtocolException("Disallowed cross-protocol redirect ("
        //       + originalUrl.getProtocol() + " to " + protocol + ")");
        // }
        return url;
    }

    /**
     * Skips any bytes that need skipping. Else does nothing.
     * <p/>
     * This implementation is based roughly on {@code libcore.io.Streams.skipByReading()}.
     *
     * @throws InterruptedIOException If the thread is interrupted during the operation.
     * @throws EOFException           If the end of the input stream is reached before the bytes are skipped.
     */
    private void skipInternal() throws IOException {
        if (bytesSkipped == bytesToSkip) {
            return;
        }

        // Acquire the shared skip buffer.
        byte[] skipBuffer = skipBufferReference.getAndSet(null);
        if (skipBuffer == null) {
            skipBuffer = new byte[4096];
        }

        while (bytesSkipped != bytesToSkip) {
            int readLength = (int) Math.min(bytesToSkip - bytesSkipped, skipBuffer.length);
            int read = response.body().byteStream().read(skipBuffer, 0, readLength);
            if (Thread.interrupted()) {
                throw new InterruptedIOException();
            }
            if (read == -1) {
                throw new EOFException();
            }
            bytesSkipped += read;
            if (listener != null) {
                listener.onBytesTransferred(read);
            }
        }

        // Release the shared skip buffer.
        skipBufferReference.set(skipBuffer);
    }

    /**
     * Reads up to {@code length} bytes of data and stores them into {@code buffer}, starting at
     * index {@code offset}.
     * <p/>
     * This method blocks until at least one byte of data can be read, the end of the opened range is
     * detected, or an exception is thrown.
     *
     * @param buffer     The buffer into which the read data should be stored.
     * @param offset     The start offset into {@code buffer} at which data should be written.
     * @param readLength The maximum number of bytes to read.
     * @return The number of bytes read, or {@link C#RESULT_END_OF_INPUT} if the end of the opened
     * range is reached.
     * @throws IOException If an error occurs reading from the source.
     */
    private int readInternal(byte[] buffer, int offset, int readLength) throws IOException {
        readLength = bytesToRead == C.LENGTH_UNBOUNDED ? readLength
                : (int) Math.min(readLength, bytesToRead - bytesRead);
        if (readLength == 0) {
            // We've read all of the requested data.
            return C.RESULT_END_OF_INPUT;
        }

        int read = response.body().byteStream().read(buffer, offset, readLength);
        if (read == -1) {
            if (bytesToRead != C.LENGTH_UNBOUNDED && bytesToRead != bytesRead) {
                // The server closed the connection having not sent sufficient data.
                throw new EOFException();
            }
            return C.RESULT_END_OF_INPUT;
        }

        bytesRead += read;
        if (listener != null) {
            listener.onBytesTransferred(read);
        }
        return read;
    }

    /**
     * Closes the current connection, if there is one.
     */
    private void closeConnection() {
        closeQuietly(response.body());
        response = null;
    }
}