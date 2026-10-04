using System;
using System.Collections.Generic;
using System.Linq;

namespace NvMedia {
    public sealed class Candidate {
        public string Id, Source, Title, Artist, Album, Status;
        public long PositionMs, DurationMs, SampledAtMs;
        public double PlaybackSpeed = 1.0;
        public bool Current;
        public object Session;
        public object Properties;
        public bool Eligible {
            get {
                string source = (Source ?? "").ToLowerInvariant();
                string title = (Title ?? "").ToLowerInvariant();
                return title.Length > 0 && !source.Contains("telegram") && !source.Contains("discord")
                    && !source.Contains("whatsapp") && !title.StartsWith("voice_")
                    && !title.StartsWith("audio_") && !title.Contains("голосовое сообщение")
                    && !title.Contains("voice message") && !title.Contains("видеосообщение");
            }
        }
    }

    public static class SessionPolicy {
        // Match all supported source families together; never reject an artist's name.
        public static string Family(string source, string title) {
            string id = (source ?? "").ToLowerInvariant();
            string text = (title ?? "").ToLowerInvariant();
            if (id.Contains("spotify") || text.Contains(" — spotify")) return "Spotify";
            bool browser = id.Contains("chrome") || id.Contains("msedge") || id.Contains("firefox")
                || id.Contains("browser") || id.Contains("opera") || id.Contains("brave") || id.Contains("vivaldi") || id.Contains("waterfox") || id.Contains("floorp") || id.Contains("zenbrowser");
            if ((!browser && id.Contains("yandex")) || text.Contains("яндекс музыка") || text.Contains("music.yandex")) return "Яндекс Музыка";
            if (id == "vk" || id.Contains("vk_music") || id.Contains("vk.music") || id.Contains("vkmusic") || id.Contains("vkontakte")
                || text.Contains("vk music") || text.Contains("vk.com/music") || text.Contains("вк музыка")) return "VK Музыка";
            if (browser) return "Браузер";
            return "Медиаплеер";
        }

        public static bool Matches(Candidate candidate, string preference) {
            string family = Family(candidate.Source, candidate.Title);
            if (preference == "yandex") return family == "Яндекс Музыка";
            if (preference == "vk") return family == "VK Музыка";
            if (preference == "spotify") return family == "Spotify";
            if (preference == "browser") {
                string id = (candidate.Source ?? "").ToLowerInvariant();
                return id.Contains("chrome") || id.Contains("msedge") || id.Contains("firefox")
                    || id.Contains("browser") || id.Contains("opera") || id.Contains("brave") || id.Contains("vivaldi") || id.Contains("waterfox") || id.Contains("floorp") || id.Contains("zenbrowser");
            }
            return true;
        }

        public static Candidate Select(IList<Candidate> all, string selectedId, bool controlHold) {
            var valid = all.Where(x => x.Eligible && (x.Status == "playing" || x.Status == "paused" || (controlHold && x.Id == selectedId && x.Status == "stopped"))).ToList();
            var previous = valid.FirstOrDefault(x => x.Id == selectedId);
            if (previous != null && (controlHold || previous.Status == "playing")) return previous;
            var playing = valid.Where(x => x.Status == "playing").ToList();
            var system = playing.FirstOrDefault(x => x.Current);
            if (system != null) return system;
            if (playing.Count > 0) return playing.OrderBy(x => x.Id, StringComparer.Ordinal).First();
            if (previous != null) return previous;
            return valid.FirstOrDefault(x => x.Current) ?? valid.OrderBy(x => x.Id, StringComparer.Ordinal).FirstOrDefault();
        }
    }
}
