package rtx.nv.test;

import java.lang.reflect.Field;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import rtx.nv.api.party.PartyClient;
import rtx.nv.utils.net.BoundedInput;

/** Exercises real listener boundaries with an in-memory socket; never connects to a backend. */
final class NetworkBoundaryChecks {
    static void verify() {
        try {
            if (BoundedInput.read(new java.io.ByteArrayInputStream(new byte[8]), 8).length != 8) throw new AssertionError();
            try { BoundedInput.read(new java.io.ByteArrayInputStream(new byte[9]), 8); throw new AssertionError("Oversize HTTP body accepted"); }
            catch (java.io.IOException expected) {}
            var constructor = PartyClient.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PartyClient party = constructor.newInstance();
            try {
                set(party,"executor",Executors.newSingleThreadScheduledExecutor());
                set(party,"connectionEpoch",1L);
                var listenerClass = Class.forName(PartyClient.class.getName()+"$Listener");
                var listenerConstructor = listenerClass.getDeclaredConstructor(PartyClient.class,long.class);
                listenerConstructor.setAccessible(true);
                WebSocket.Listener old = (WebSocket.Listener)listenerConstructor.newInstance(party,1L);
                Socket first = new Socket(); old.onOpen(first);
                int[] voice = {0};
                party.setVoiceSink((name,sequence,pcm) -> {
                    if (!name.equals("A") || sequence != 1 || pcm.length != 2) throw new AssertionError("Voice fragments corrupted");
                    ++voice[0];
                });
                old.onBinary(first,ByteBuffer.wrap(new byte[]{1,65,0}),false);
                if (voice[0] != 0) throw new AssertionError("Incomplete voice packet delivered");
                old.onBinary(first,ByteBuffer.wrap(new byte[]{1,4,5}),true);
                if (voice[0] != 1) throw new AssertionError("Complete voice packet lost");
                old.onText(first,"x".repeat(1 << 20),false);
                old.onText(first,"x",true);
                if (!first.aborted || party.isConnected()) throw new AssertionError("Oversize WebSocket text accepted");
                set(party,"connectionEpoch",2L);
                WebSocket.Listener fresh = (WebSocket.Listener)listenerConstructor.newInstance(party,2L);
                Socket second = new Socket(); fresh.onOpen(second);
                old.onClose(first,1000,"stale"); old.onError(first,new RuntimeException("stale"));
                if (!party.isConnected()) throw new AssertionError("Stale socket cleared new connection");
                fresh.onBinary(second,ByteBuffer.wrap(new byte[(64 << 10)+1]),true);
                if (!second.aborted || party.isConnected()) throw new AssertionError("Oversize voice packet accepted");
            } finally { party.stop(); }
            rtx.nv.NV.LOGGER.info("[NV-TEST] HTTP/WS size limits, voice fragmentation and stale callbacks passed");
        } catch (ReflectiveOperationException | java.io.IOException e) { throw new AssertionError(e); }
    }
    private static void set(PartyClient party,String name,Object value) throws ReflectiveOperationException {
        Field field = PartyClient.class.getDeclaredField(name); field.setAccessible(true); field.set(party,value);
    }
    private static final class Socket implements WebSocket {
        boolean aborted;
        public CompletableFuture<WebSocket> sendText(CharSequence data,boolean last) { return CompletableFuture.completedFuture(this); }
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data,boolean last) { return CompletableFuture.completedFuture(this); }
        public CompletableFuture<WebSocket> sendPing(ByteBuffer data) { return CompletableFuture.completedFuture(this); }
        public CompletableFuture<WebSocket> sendPong(ByteBuffer data) { return CompletableFuture.completedFuture(this); }
        public CompletableFuture<WebSocket> sendClose(int status,String reason) { return CompletableFuture.completedFuture(this); }
        public void request(long n) {}
        public String getSubprotocol() { return ""; }
        public boolean isOutputClosed() { return aborted; }
        public boolean isInputClosed() { return aborted; }
        public void abort() { aborted = true; }
    }
}
