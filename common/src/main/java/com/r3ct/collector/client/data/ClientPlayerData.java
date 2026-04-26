package com.r3ct.collector.client.data;

import java.util.HashSet;
import java.util.Set;

public class ClientPlayerData {
    public static Set<String> unlockedItems = new HashSet<>();
    public static Set<String> rewardedCategories = new HashSet<>();
    public static java.util.List<com.r3ct.collector.network.LeaderboardDataPayload.TopPlayerEntry> leaderboardData = new java.util.ArrayList<>();
}