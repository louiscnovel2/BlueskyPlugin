package com.example.blueskyplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.json.JSONObject;
import org.json.JSONArray;

public class BlueskyPlugin extends JavaPlugin {
    private Map<UUID, String> userTokens = new HashMap<>();
    private Map<UUID, String> userHandles = new HashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void onEnable() {
        getLogger().info("BlueskyPluginが有効になりました！");
    }

    @Override
    public void onDisable() {
        getLogger().info("BlueskyPluginが無効になりました！");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみが実行できます。");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (args.length == 0) {
            player.sendMessage("使用方法: /bsky <login|logout|post|tl>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "login":
                if (args.length != 3) {
                    player.sendMessage("使用方法: /bsky login <handle> <password>");
                    return true;
                }
                handleLogin(player, args[1], args[2]);
                break;
            case "logout":
                handleLogout(player);
                break;
            case "post":
                if (!userTokens.containsKey(playerId)) {
                    player.sendMessage("先にログインしてください！");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("使用方法: /bsky post <メッセージ>");
                    return true;
                }
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                handlePost(player, message);
                break;
            case "tl":
                if (!userTokens.containsKey(playerId)) {
                    player.sendMessage("先にログインしてください！");
                    return true;
                }
                handleTimeline(player);
                break;
            default:
                player.sendMessage("無効なコマンドです。");
                break;
        }
        return true;
    }

    private void handleLogin(Player player, String handle, String password) {
        try {
            JSONObject loginData = new JSONObject();
            loginData.put("identifier", handle);
            loginData.put("password", password);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://bsky.social/xrpc/com.atproto.server.createSession"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginData.toString()))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject responseJson = new JSONObject(response.body());

            if (response.statusCode() == 200) {
                String accessJwt = responseJson.getString("accessJwt");
                userTokens.put(player.getUniqueId(), accessJwt);
                userHandles.put(player.getUniqueId(), handle);
                player.sendMessage("ログインに成功しました！");
            } else {
                player.sendMessage("ログインに失敗しました。");
            }
        } catch (Exception e) {
            player.sendMessage("エラーが発生しました: " + e.getMessage());
        }
    }

    private void handleLogout(Player player) {
        UUID playerId = player.getUniqueId();
        if (userTokens.containsKey(playerId)) {
            userTokens.remove(playerId);
            userHandles.remove(playerId);
            player.sendMessage("ログアウトしました。");
        } else {
            player.sendMessage("ログインしていません。");
        }
    }

    private void handlePost(Player player, String text) {
        try {
            String accessJwt = userTokens.get(player.getUniqueId());
            String handle = userHandles.get(player.getUniqueId());

            // 投稿データを作成
            JSONObject record = new JSONObject()
                .put("$type", "app.bsky.feed.post")
                .put("text", text)
                .put("createdAt", java.time.Instant.now().toString());

            JSONObject postData = new JSONObject()
                .put("collection", "app.bsky.feed.post")
                .put("repo", handle)
                .put("record", record);

            // 投稿を実行
            HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://bsky.social/xrpc/com.atproto.repo.createRecord"))
                .header("Authorization", "Bearer " + accessJwt)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postData.toString()))
                .build();

            HttpResponse<String> response = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                player.sendMessage("投稿に成功しました！");
            } else {
                player.sendMessage("投稿に失敗しました。エラー: " + response.body());
            }
        } catch (Exception e) {
            player.sendMessage("エラーが発生しました: " + e.getMessage());
        }
    }

    private void handleTimeline(Player player) {
        try {
            String accessJwt = userTokens.get(player.getUniqueId());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://bsky.social/xrpc/app.bsky.feed.getTimeline"))
                .header("Authorization", "Bearer " + accessJwt)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject responseJson = new JSONObject(response.body());

            if (response.statusCode() == 200) {
                player.sendMessage("=== Blueskyタイムライン ===");
                JSONArray feed = responseJson.getJSONArray("feed");
                if (feed.length() == 0) {
                    player.sendMessage("タイムラインに投稿がありません。");
                    return;
                }
                for (int i = 0; i < feed.length(); i++) {
                    try {
                        JSONObject post = feed.getJSONObject(i);
                        JSONObject postView = post.getJSONObject("post");
                        JSONObject record = postView.getJSONObject("record");
                        String text = record.getString("text");
                        
                        // 投稿者の情報を取得
                        JSONObject author = postView.getJSONObject("author");
                        String authorName = author.getString("displayName");
                        String authorHandle = author.getString("handle");
                        
                        // 投稿日時を取得
                        String createdAt = record.getString("createdAt");
                        java.time.Instant instant = java.time.Instant.parse(createdAt);
                        java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
                        String formattedDate = zdt.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm"));
                        
                        player.sendMessage("§b" + authorName + "§r (@§e" + authorHandle + "§r) [" + formattedDate + "]: " + text);
                    } catch (Exception e) {
                        getLogger().warning("投稿の解析に失敗: " + e.getMessage());
                        continue;
                    }
                }
            } else {
                player.sendMessage("タイムラインの取得に失敗しました。");
            }
        } catch (Exception e) {
            player.sendMessage("エラーが発生しました: " + e.getMessage());
        }
    }
} 