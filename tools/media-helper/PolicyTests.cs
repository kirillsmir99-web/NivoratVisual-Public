using System;
using System.Collections.Generic;
using NvMedia;

public static class PolicyTests {
    static int checks;
    static void Check(bool ok, string name) { if (!ok) throw new Exception(name); checks++; }
    static Candidate C(string id, string source, string status, bool current = false) {
        return new Candidate { Id = id, Source = source, Title = "Yesterday", Artist = "Леонид Агутин", Status = status, Current = current };
    }
    public static void Main() {
        var y = C("y", "YandexMusic", "paused");
        var s = C("s", "Spotify.exe", "playing", true);
        var v = C("v", "VK_Music_app", "playing");
        var b = C("b", "chrome.exe", "playing");
        var all = new List<Candidate> { y, s, v, b };
        Check(SessionPolicy.Select(all, "", false) == s, "active OS player");
        Check(SessionPolicy.Select(all, "v", false) == v, "playing selection survives other current player");
        Check(SessionPolicy.Select(all, "y", true) == y, "pause/resume hold");
        Check(SessionPolicy.Select(all, "y", false) == s, "switch to playing source");
        s.Status = v.Status = b.Status = "paused";
        Check(SessionPolicy.Select(all, "v", false) == v, "paused selection stays stable");
        Check(SessionPolicy.Matches(y, "yandex"), "Yandex desktop");
        Check(SessionPolicy.Matches(v, "vk"), "VK desktop");
        Check(SessionPolicy.Matches(s, "spotify"), "Spotify desktop");
        Check(SessionPolicy.Matches(b, "browser"), "Chrome browser");
        foreach (string browser in new[] { "msedge", "firefox", "opera", "brave", "vivaldi", "waterfox", "floorp", "zenbrowser", "YandexBrowser" })
            Check(SessionPolicy.Matches(C("browser", browser, "playing"), "browser"), browser + " source");
        Check(!SessionPolicy.Matches(b, "yandex"), "unknown browser must not pretend to be Yandex");
        b.Title = "Song — Яндекс Музыка";
        Check(SessionPolicy.Matches(b, "yandex"), "Yandex browser metadata");
        b.Title = "Song — VK Music";
        Check(SessionPolicy.Matches(b, "vk"), "VK browser metadata");
        b.Title = "Song — Spotify";
        Check(SessionPolicy.Matches(b, "spotify"), "Spotify browser metadata");
        Check(y.Eligible, "artist Agutin and song Yesterday allowed");
        Check(!C("t", "Telegram.exe", "playing").Eligible, "messenger excluded by source");
        b.Title = "voice_123.ogg";
        Check(!b.Eligible, "voice message excluded");
        Check(SessionPolicy.Select(new List<Candidate> { C("held", "YandexMusic", "stopped") }, "held", true) != null, "transient stop keeps controlled session");
        Check(SessionPolicy.Select(new List<Candidate>(), "", false) == null, "empty sessions");
        Check(SessionPolicy.Select(new List<Candidate> { C("x", "Spotify", "stopped") }, "", false) == null, "stopped source ignored");
        Console.WriteLine("NV media policy: " + checks + " checks passed");
    }
}
