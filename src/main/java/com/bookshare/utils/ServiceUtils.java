package com.bookshare.utils;
import com.fasterxml.jackson.databind.JsonNode;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

public class ServiceUtils {

    public static void printBooks(JsonNode items){//for logs only
        if(items == null || items.isEmpty()) return;
        items.forEach(i -> {
            JsonNode volInfo = i.get("volumeInfo");
            if(volInfo == null || !volInfo.has("title") || !volInfo.has("authors")){return;}
            JsonNode imageLinks = volInfo.get("imageLinks");
            String smallThumbnail = imageLinks != null && imageLinks.has("smallThumbnail")
                    ? imageLinks.get("smallThumbnail").asText()
                    : null;
            String thumbnail = imageLinks != null && imageLinks.has("thumbnail")
                    ? imageLinks.get("thumbnail").asText()
                    : null;
            System.out.println(volInfo.get("title").asText() + " " +
                    volInfo.get("authors").get(0).asText() + " " +
                    smallThumbnail + " " +
                    thumbnail);});
    }

    public static String normalizeTitle(String title) {
        if (title == null || title.isEmpty()) return "";

        String cleaned = title.split("[*&%$#@./;|\\-_()\\[\\]]")[0].trim();

        String normalized = Normalizer.normalize(cleaned, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return normalized;
    }

    public static List<String> extractKeywords(String title) {
        String noAccent = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim();
        System.out.println(noAccent);
        System.out.println(Arrays.toString(noAccent.split("\\s+")));
        String[] words = noAccent.split("\\s+");
        return Arrays.stream(words)
                .filter(word -> word.length() >= 4)
                .distinct()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .limit(5)
                .toList();
    }

}
