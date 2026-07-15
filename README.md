# R3CT Collection 📖✨

<div align="center">

[![Modrinth](https://img.shields.io/modrinth/dt/JNIJFVHZ?style=for-the-badge&label=Modrinth&logo=modrinth&logoColor=white&color=2EA043)](https://modrinth.com/project/r3ct-collection)
[![CurseForge](https://img.shields.io/curseforge/dt/1528272?style=for-the-badge&label=CurseForge&logo=curseforge&logoColor=white&color=F16436)](https://www.curseforge.com/minecraft/mc-mods/r3ct-collection)
[![Wiki](https://img.shields.io/badge/Documentation-Wiki-6E40C9?style=for-the-badge&logo=readthedocs&logoColor=white)](https://github.com/R3CTrc/R3CT-Collection/wiki)
[![License: MIT](https://img.shields.io/badge/License-MIT-0576BA?style=for-the-badge&logo=opensourceinitiative&logoColor=white)](https://opensource.org/licenses/MIT)

</div>

The ultimate completionist and collection mod for Minecraft! R3CT Collection automatically scans all items in your game (including those from other mods) and generates a beautifully categorized Collector's Book. Gather items, earn XP, claim rewards, and compete with other players on your server!

<div align="center">
  <img src="./images/icon_nb.png" width="50%" alt="R3CT Collection Mod Icon">
</div>

---

## ✨ Features

* **📖 Dynamic Catalog:** Auto-scans all creative tabs to automatically build categories. It adapts perfectly to any modpack, no matter the size.


* **📥 Submitting Items:** Easily submit items you find in the world to unlock them in your personal Catalog and track your completion progress.


* **⚡ Smart Bulk Submit:** Have tons of items? Click the Ender Chest icon to submit all available items for a category at once! Valuable items (like enchanted gear or filled bundles) are safely queued for manual confirmation.


* **🔍 JEI Integration:** Fully integrated with Just Enough Items! Simply click any item inside the Collector's Book to instantly view its recipes and uses.

<div align="center">
  <a href="./images/Tab.png" target="_blank">
    <img src="./images/Tab.png" width="75%" alt="Catalog Screen (Click to enlarge)">
  </a>
</div>

* **🎁 Progression & Rewards:** Earn Experience Points for every single item you submit, with the amount scaling based on the item's vanilla rarity. Plus, collect a specific number of items to earn an extra random reward from a configurable milestone loot pool!


* **🏆 Physical Trophies:** Complete a category 100% to earn a unique, placeable Trophy Block displaying the category icon and your name.


* **👑 Server Leaderboard:** Compete for the top 10 spots. Hover over players to see their exact category progress.

<div align="center">
  <a href="./images/Trophy.png" target="_blank">
    <img src="./images/Trophy.png" width="75%" alt="Trophy (Click to enlarge)">
  </a>
</div>

---

## 🔌 Dependencies

**Highly Recommended:**
* [Just Enough Items (JEI)](https://modrinth.com/mod/jei) - For recipe integration inside the Catalog.

**For Fabric:**
* [Fabric API](https://modrinth.com/mod/fabric-api) (Required)
* [Mod Menu](https://modrinth.com/mod/modmenu) (Optional - to access in-game client settings)

**For NeoForge:**
* Nothing extra required!

---

## 📖 Documentation

For detailed guides on how to configure blacklists, adjust rewards, and tweak the mod's mechanics, visit our official Wiki:
👉 **[View the Wiki](https://github.com/R3CTrc/R3CT-Collection/wiki)**

---

## ⚙️ Configuration & Customization

### 1. In-Game Settings (Client-side)
Adjust the GUI scale of the Catalog Book to perfectly fit your screen resolution via Mod Menu (Fabric) or the Mods tab (NeoForge).

### 2. File Configuration (Server-side)
Manage core mechanics using simple JSON files found in `config/r3ct_collection/`:
* **`r3ct_collection_items.json`** - Manage blacklists. Exclude entire mods, creative tabs, or specific items (even specific NBT variants like a designated potion effect!).
* **`r3ct_collection_rewards.json`** - Tweak XP values per rarity, milestone intervals, and custom loot pools.

---

## 📥 Installation

1. Download the latest release from the **Versions** tab.
2. Download the required dependencies listed above for your specific mod loader.
3. Place all `.jar` files into your Minecraft `mods` folder.
4. Press `K` (default keybind) in-game to open your Catalog and start collecting!

---

## 📦 Check out my other mods!

If you enjoy this mod, you might also like my other projects:

### [🎯 R3CT Daily Quests & Rewards](https://modrinth.com/mod/r3ct-daily-quests-rewards)
*A highly configurable Daily Quests & Rewards mod! Keep players engaged with dynamic tasks, login rewards, streaks, and competitive leaderboards.*

<a href="https://modrinth.com/mod/r3ct-daily-quests-rewards">
  <img src="./images/daily_icon_nb.png" width="150" alt="R3CT Daily Quests & Rewards">
</a>

---

## 💖 Support the Development

I'm a computer science student, and I develop game mods and software in my free time. If my work has improved your server or modpack, consider supporting my coding journey! Every coffee helps me survive late-night debugging sessions. ☕💻

[![Ko-Fi](https://img.shields.io/badge/Support_me_on_Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/r3ct_)

### 🌟 Memberships & Perks
Want to get more involved? Check out my Ko-fi memberships for exclusive perks:
* 🥇 **Diamond Supporter:** Name in the Hall of Fame and custom feature requests!

[Join a Tier and support the mod!](https://ko-fi.com/r3ct_/tiers)

---

## 📄 License
This project is available under the [MIT License](LICENSE). Feel free to learn from the code and include it in your modpacks!