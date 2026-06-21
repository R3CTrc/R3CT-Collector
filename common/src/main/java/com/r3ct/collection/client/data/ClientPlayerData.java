package com.r3ct.collection.client.data;

import com.r3ct.collection.network.LeaderboardDataPayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientPlayerData {
    public static Set<String> unlockedItems = new HashSet<>();
    public static Set<String> rewardedCategories = new HashSet<>();
    public static List<LeaderboardDataPayload.TopPlayerEntry> leaderboardData = new ArrayList<>();
}