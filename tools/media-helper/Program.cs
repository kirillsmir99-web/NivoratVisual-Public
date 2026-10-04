using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;
using Windows.Foundation;
using Windows.Media.Control;
using Windows.Storage.Streams;

namespace NvMedia {
    public static class Program {
        private static readonly object Gate = new object();
        private static readonly object ControlGate = new object();
        private static readonly SemaphoreSlim Requests = new SemaphoreSlim(8);
        private static volatile bool running = true;
        private static Candidate selected;
        private static string selectedId = "", coverKey = "", coverBase64, preferred = "auto";
        private static DateTime holdUntil = DateTime.MinValue;
        private static List<Candidate> candidates = new List<Candidate>();
        private static TcpListener listener;

        private static T Complete<T>(IAsyncOperation<T> operation) {
            var deadline = DateTime.UtcNow.AddMilliseconds(1200);
            while (operation.Status == AsyncStatus.Started && DateTime.UtcNow < deadline) Thread.Sleep(10);
            if (operation.Status == AsyncStatus.Started) { operation.Cancel(); throw new TimeoutException("Media operation timed out"); }
            return operation.GetResults();
        }

        public static void Main() {
            try {
                var manager = Complete(GlobalSystemMediaTransportControlsSessionManager.RequestAsync());
                listener = new TcpListener(IPAddress.Loopback, 0); listener.Start(16);
                int port = ((IPEndPoint)listener.LocalEndpoint).Port;
                File.WriteAllText("nv_media_port.txt", port.ToString());
                File.WriteAllText("nv_media_pid.txt", System.Diagnostics.Process.GetCurrentProcess().Id.ToString());
                Task.Run(() => Serve());
                AudioSpectrum.Start();
                Console.WriteLine("NV Media: localhost port " + port);
                while (running) { Poll(manager); Thread.Sleep(500); }
            } catch (Exception e) { Console.Error.WriteLine("NV Media: " + e.GetType().Name + ": " + e.Message); }
            finally {
                running = false;
                AudioSpectrum.Stop();
                if (listener != null) listener.Stop();
                try { File.Delete("nv_media_port.txt"); File.Delete("nv_media_pid.txt"); } catch (IOException) { }
            }
        }

        private static void Poll(GlobalSystemMediaTransportControlsSessionManager manager) {
            var all = new List<Candidate>();
            try {
                var current = manager.GetCurrentSession();
                // A failed session must not hide working sessions from another player.
                foreach (var session in manager.GetSessions().Take(16)) {
                    try {
                        var media = Complete(session.TryGetMediaPropertiesAsync());
                        var playback = session.GetPlaybackInfo();
                        var time = session.GetTimelineProperties();
                        long position = (long)(time.Position - time.StartTime).TotalMilliseconds;
                        long duration = Math.Max(0, (long)(time.EndTime - time.StartTime).TotalMilliseconds);
                        if (playback.PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing) {
                            double elapsed = (DateTimeOffset.UtcNow - time.LastUpdatedTime).TotalMilliseconds;
                            if (elapsed > 0 && elapsed < 86400000) position += (long)(elapsed * (playback.PlaybackRate ?? 1.0));
                        }
                        if (duration > 0) position = Math.Min(position, duration);
                        string source = session.SourceAppUserModelId ?? "";
                        all.Add(new Candidate { Id = source + ":" + all.Count, Source = source,
                            Title = media.Title ?? "", Artist = media.Artist ?? "", Album = media.AlbumTitle ?? "",
                            Status = playback.PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing ? "playing" :
                                playback.PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Paused ? "paused" : "stopped",
                            PositionMs = Math.Max(0, position), DurationMs = duration, SampledAtMs = UtcMs(), PlaybackSpeed = playback.PlaybackRate ?? 1.0, Current = Object.Equals(current, session),
                            Session = session, Properties = media });
                    } catch (Exception) { }
                }
                Candidate next;
                lock (Gate) {
                    // Preserve session identity when GetSessions enumeration order changes.
                    foreach (var c in all) {
                        var old = candidates.FirstOrDefault(x => Object.Equals(x.Session, c.Session));
                        c.Id = old != null ? old.Id : Guid.NewGuid().ToString("N");
                    }
                    next = SessionPolicy.Select(all.Where(c => SessionPolicy.Matches(c, preferred)).ToList(), selectedId, DateTime.UtcNow < holdUntil);
                    if (next == null && selected != null && DateTime.UtcNow < holdUntil && SessionPolicy.Matches(selected, preferred)) {
                        next = selected; next.Status = "paused"; // Preserve metadata during the player's switch gap.
                    }
                    if (next != null && next.Status == "stopped" && DateTime.UtcNow < holdUntil) next.Status = "paused";
                    selected = next; selectedId = next == null ? "" : next.Id; candidates = all;
                    string nextKey = next == null ? "" : next.Id + "\n" + next.Title + "\n" + next.Artist + "\n" + next.Album;
                    if (coverKey != nextKey) coverBase64 = null;
                }
                string key = next == null ? "" : next.Id + "\n" + next.Title + "\n" + next.Artist + "\n" + next.Album;
                if (key != coverKey) {
                    string cover = null;
                    if (next != null) {
                        try {
                            var media = (GlobalSystemMediaTransportControlsSessionMediaProperties)next.Properties;
                            if (media.Thumbnail != null) {
                                using (var stream = Complete(media.Thumbnail.OpenReadAsync())) {
                                    if (stream.Size <= 2097152) {
                                        using (var reader = new DataReader(stream)) {
                                            uint count = Complete(reader.LoadAsync((uint)stream.Size));
                                            byte[] bytes = new byte[count]; reader.ReadBytes(bytes); cover = Convert.ToBase64String(bytes);
                                        }
                                    }
                                }
                            }
                        } catch (Exception) { }
                    }
                    lock (Gate) { coverKey = key; coverBase64 = cover; }
                }
            } catch (Exception) { lock (Gate) { selected = null; candidates.Clear(); } }
        }

        private static long UtcMs() { return (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalMilliseconds; }

        private static Dictionary<string, object> State() {
            lock (Gate) {
                if (selected == null) return new Dictionary<string, object> { { "isActive", false }, { "status", "stopped" } };
                long position = selected.PositionMs;
                if (selected.Status == "playing") position += (long)(Math.Max(0, UtcMs() - selected.SampledAtMs) * selected.PlaybackSpeed);
                if (selected.DurationMs > 0) position = Math.Min(position, selected.DurationMs);
                return new Dictionary<string, object> {
                    { "isActive", true }, { "title", selected.Title }, { "artist", selected.Artist }, { "album", selected.Album },
                    { "status", selected.Status }, { "positionMs", position }, { "durationMs", selected.DurationMs },
                    { "coverHash", coverBase64 == null ? null : coverKey }, { "platformName", SessionPolicy.Family(selected.Source, selected.Title) },
                    { "sourceId", selected.Source }, { "sessionId", selected.Id }, { "playbackSpeed", selected.PlaybackSpeed },
                    { "audioBands", selected.Status == "playing" ? AudioSpectrum.Snapshot() : new float[12] },
                    { "audioAvailable", AudioSpectrum.Available }
                };
            }
        }

        private static bool Control(string path) {
            lock (ControlGate) {
                Candidate target;
                lock (Gate) { target = selected; holdUntil = DateTime.UtcNow.AddSeconds(3); }
                if (target == null) return false;
                var session = (GlobalSystemMediaTransportControlsSession)target.Session;
                if (path == "/control/play-pause") return Complete(session.TryTogglePlayPauseAsync());
                if (path == "/control/next") return Complete(session.TrySkipNextAsync());
                if (path == "/control/previous") return Complete(session.TrySkipPreviousAsync());
                if (path.StartsWith("/control/seek?pos=")) {
                    long ms;
                    if (!Int64.TryParse(path.Substring(18), out ms) || ms < 0 || ms > Int64.MaxValue / 10000) return false;
                    return Complete(session.TryChangePlaybackPositionAsync(ms * 10000));
                }
                return false;
            }
        }

        private static void Serve() {
            while (running) {
                try {
                    var client = listener.AcceptTcpClient();
                    if (!Requests.Wait(0)) { client.Close(); continue; }
                    Task.Run(() => { try { Handle(client); } finally { client.Close(); Requests.Release(); } });
                } catch (SocketException) { if (running) Thread.Sleep(100); }
                catch (ObjectDisposedException) { break; }
            }
        }

        private static void Handle(TcpClient client) {
            client.ReceiveTimeout = 2000; client.SendTimeout = 2000;
            using (var stream = client.GetStream()) {
                try {
                    // Bound request headers before decoding; no public listener or browser control requests.
                    var header = new List<byte>();
                    while (header.Count < 8192) {
                        int b = stream.ReadByte(); if (b < 0) return; header.Add((byte)b);
                        int n = header.Count;
                        if (n >= 4 && header[n-4] == 13 && header[n-3] == 10 && header[n-2] == 13 && header[n-1] == 10) break;
                    }
                    if (header.Count >= 8192) { Reply(stream, 431, new { error = "headers" }); return; }
                    string request = Encoding.ASCII.GetString(header.ToArray());
                    string[] first = request.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries)[0].Split(' ');
                    if (first.Length < 2 || request.IndexOf("\r\nOrigin:", StringComparison.OrdinalIgnoreCase) >= 0) { Reply(stream, 403, new { error = "origin" }); return; }
                    string method = first[0], path = first[1];
                    if (method == "GET" && path == "/health") Reply(stream, 200, new { status = "ok", version = "nv-media-1" });
                    else if (method == "GET" && path == "/now-playing") Reply(stream, 200, State());
                    else if (method == "GET" && path == "/cover") { string cover; lock (Gate) cover = coverBase64; Reply(stream, 200, new { coverBase64 = cover }); }
                    else if (method == "GET" && path == "/sessions") {
                        object snapshot;
                        lock (Gate) snapshot = candidates.Select(c => new { id = c.Id, source = c.Source, family = SessionPolicy.Family(c.Source, c.Title), status = c.Status, eligible = c.Eligible, selected = c.Id == selectedId }).ToArray();
                        Reply(stream, 200, snapshot);
                    }
                    else if (method == "POST" && path.StartsWith("/source?filter=")) {
                        string filter = path.Substring(15);
                        if (new[] { "auto", "yandex", "vk", "spotify", "browser" }.Contains(filter)) {
                            lock (Gate) { preferred = filter; selectedId = ""; selected = null; holdUntil = DateTime.MinValue; }
                            Reply(stream, 200, new { success = true });
                        } else Reply(stream, 400, new { error = "filter" });
                    }
                    else if (method == "POST" && path == "/shutdown") { Reply(stream, 200, new { success = true }); running = false; listener.Stop(); }
                    else if (method == "POST" && path.StartsWith("/control/")) { bool ok = Control(path); Reply(stream, ok ? 200 : 409, new { success = ok }); }
                    else Reply(stream, 404, new { error = "route" });
                } catch (Exception) { try { Reply(stream, 503, new { error = "media unavailable" }); } catch (Exception) { } }
            }
        }

        private static void Reply(Stream stream, int code, object data) {
            byte[] bytes = Encoding.UTF8.GetBytes(new JavaScriptSerializer { MaxJsonLength = 4194304 }.Serialize(data));
            byte[] header = Encoding.ASCII.GetBytes("HTTP/1.1 " + code + " Result\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: " + bytes.Length + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n");
            stream.Write(header, 0, header.Length); stream.Write(bytes, 0, bytes.Length); stream.Flush();
        }
    }
}
