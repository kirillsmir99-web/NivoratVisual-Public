package rtx.nv.api.music.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeniusLyricsProvider {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(3000L))
        .followRedirects(Redirect.NORMAL)
        .build();

    private static final Pattern LYRICS_CONTAINER_PATTERN = Pattern.compile("<div[^>]*data-lyrics-container=[\"']true[\"'][^>]*>(.*?)</div>", Pattern.DOTALL);
    private static final Pattern OLD_LYRICS_PATTERN = Pattern.compile("<div[^>]*class=[\"']lyrics[\"'][^>]*>(.*?)</div>", Pattern.DOTALL);
    private static final Pattern BR_TAG_PATTERN = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_TAG_PATTERN = Pattern.compile("</?p[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern CONTRIBUTORS_PATTERN = Pattern.compile("^\\d+\\s+Contributors.*$", Pattern.CASE_INSENSITIVE);

    public static String fetchLyrics(String title, String artist, long timeoutMs) {
        if (title == null || title.isBlank()) {
            return null;
        }
        try {
            String query = (artist != null && !artist.isBlank()) ? artist.trim() + " " + title.trim() : title.trim();
            String url = "https://genius.com/api/search/multi?per_page=5&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .timeout(Duration.ofMillis(timeoutMs > 0L ? timeoutMs : 3000L))
                .GET()
                .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
                if (root.has("response") && !root.get("response").isJsonNull()) {
                    JsonObject response = root.getAsJsonObject("response");
                    if (response.has("sections") && !response.get("sections").isJsonNull()) {
                        JsonArray sections = response.getAsJsonArray("sections");
                        String bestUrl = null;
                        double bestScore = -1.0;

                        for (JsonElement secEl : sections) {
                            if (secEl.isJsonObject()) {
                                JsonObject sec = secEl.getAsJsonObject();
                                if (sec.has("type") && "song".equalsIgnoreCase(sec.get("type").getAsString()) && sec.has("hits") && !sec.get("hits").isJsonNull()) {
                                    for (JsonElement hitEl : sec.getAsJsonArray("hits")) {
                                        if (hitEl.isJsonObject()) {
                                            JsonObject hit = hitEl.getAsJsonObject();
                                            if (hit.has("result") && !hit.get("result").isJsonNull()) {
                                                JsonObject result = hit.getAsJsonObject("result");
                                                String hitTitle = result.has("title") && !result.get("title").isJsonNull() ? result.get("title").getAsString() : "";
                                                String hitArtist = "";
                                                if (result.has("primary_artist") && !result.get("primary_artist").isJsonNull()) {
                                                    JsonObject pa = result.getAsJsonObject("primary_artist");
                                                    hitArtist = pa.has("name") && !pa.get("name").isJsonNull() ? pa.get("name").getAsString() : "";
                                                }

                                                String pageUrl = result.has("url") && !result.get("url").isJsonNull() ? result.get("url").getAsString() : "";
                                                if (!pageUrl.isBlank()) {
                                                    double titleSim = computeStringSimilarity(title, hitTitle);
                                                    double artistSim = (artist != null && !artist.isBlank()) ? computeStringSimilarity(artist, hitArtist) : 1.0;
                                                    double combined = titleSim * 0.65 + artistSim * 0.35;
                                                    if (combined > bestScore) {
                                                        bestScore = combined;
                                                        bestUrl = pageUrl;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (bestUrl != null && !(bestScore < 0.38)) {
                            HttpRequest pageReq = HttpRequest.newBuilder()
                                .uri(URI.create(bestUrl))
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                                .header("Accept", "text/html,application/xhtml+xml")
                                .timeout(Duration.ofMillis(timeoutMs > 0L ? timeoutMs : 3500L))
                                .GET()
                                .build();
                            HttpResponse<String> pageResp = HTTP_CLIENT.send(pageReq, BodyHandlers.ofString(StandardCharsets.UTF_8));
                            return (pageResp.statusCode() == 200 && pageResp.body() != null && !pageResp.body().isBlank())
                                ? parseGeniusHtml(pageResp.body())
                                : null;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String parseGeniusHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }

        ArrayList<String> containers = new ArrayList<>();
        Matcher m1 = LYRICS_CONTAINER_PATTERN.matcher(html);
        while (m1.find()) {
            containers.add(m1.group(1));
        }

        if (containers.isEmpty()) {
            Matcher m2 = OLD_LYRICS_PATTERN.matcher(html);
            while (m2.find()) {
                containers.add(m2.group(1));
            }
        }

        if (containers.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (String c : containers) {
            String step = BR_TAG_PATTERN.matcher(c).replaceAll("\n");
            step = P_TAG_PATTERN.matcher(step).replaceAll("\n");
            step = HTML_TAG_PATTERN.matcher(step).replaceAll("");
            step = unescapeHtml(step);

            for (String line : step.split("\\r?\\n")) {
                String tr = line.trim();
                if (!tr.isEmpty()
                    && !CONTRIBUTORS_PATTERN.matcher(tr).find()
                    && (!tr.contains("Translations") || (!tr.contains("English") && !tr.contains("Russian") && !tr.contains("Español")))
                    && (!tr.endsWith("Lyrics") || tr.length() >= 50 || tr.startsWith("["))) {
                    sb.append(tr).append("\n");
                }
            }
        }

        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static String unescapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&#8217;", "’")
            .replace("&#8216;", "‘")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .replace("&#8212;", "-")
            .replace("&#8211;", "-");
    }

    public static double computeStringSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        String n1 = normalize(str1);
        String n2 = normalize(str2);
        if (n1.isEmpty() || n2.isEmpty()) {
            return 0.0;
        } else if (n1.equals(n2)) {
            return 1.0;
        } else if (!n1.contains(n2) && !n2.contains(n1)) {
            String[] w1 = n1.split("\\s+");
            String[] w2 = n2.split("\\s+");
            HashSet<String> s1 = new HashSet<>(Arrays.asList(w1));
            HashSet<String> s2 = new HashSet<>(Arrays.asList(w2));
            s1.removeIf(w -> w.length() < 2);
            s2.removeIf(w -> w.length() < 2);
            if (!s1.isEmpty() && !s2.isEmpty()) {
                HashSet<String> intersection = new HashSet<>(s1);
                intersection.retainAll(s2);
                HashSet<String> union = new HashSet<>(s1);
                union.addAll(s2);
                return (double) intersection.size() / union.size();
            } else {
                return 0.0;
            }
        } else {
            return (double) Math.min(n1.length(), n2.length()) / Math.max(n1.length(), n2.length());
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-zа-я0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }
}
