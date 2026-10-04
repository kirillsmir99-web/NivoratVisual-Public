package rtx.nv.utils.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Bounds the body while it arrives, retaining HttpClient's whole-request timeout. */
public final class LimitedHttpBody {
    private LimitedHttpBody() {}
    public static HttpResponse.BodyHandler<byte[]> bytes(int maximum) {
        if (maximum < 0) throw new IllegalArgumentException("Negative body limit");
        return info -> new Subscriber(maximum);
    }
    private static final class Subscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int maximum;
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;
        Subscriber(int maximum) { this.maximum = maximum; }
        public CompletionStage<byte[]> getBody() { return result; }
        public void onSubscribe(Flow.Subscription value) { subscription = value; value.request(1); }
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > maximum-data.size()) {
                    subscription.cancel(); result.completeExceptionally(new IOException("HTTP response exceeds "+maximum+" bytes")); return;
                }
                byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes); data.writeBytes(bytes);
            }
            subscription.request(1);
        }
        public void onError(Throwable failure) { result.completeExceptionally(failure); }
        public void onComplete() { result.complete(data.toByteArray()); }
    }
}
