# Warbridge API Development Guide / 二次开发指南

---

## Overview / 概述

**EN**  
Warbridge provides an internal API and event system that allows developers to extend the functionality of the plugin.  
You can create expansion plugins such as economy systems, ranking systems, rewards, or custom gameplay logic.

**ZH**  
Warbridge 提供内部 API 和事件系统，允许开发者进行扩展开发。  
你可以开发经济系统、排行榜、奖励系统或自定义玩法插件。

---

## Getting Started / 快速开始

### Dependency / 依赖方式

**EN**
## Maven Dependency

Warbridge is not published to a public Maven repository.

You must first install the main Warbridge plugin into your local Maven repository:

```bash
mvn clean install

Then you can use the dependency below in your expansion project:

```xml
<dependency>
    <groupId>top.cnuo</groupId>
    <artifactId>warbridge</artifactId>
    <version>1.0.5</version>
    <scope>provided</scope>
</dependency>
```

**ZH**
## Maven Dependency

Warbridge is not published to a public Maven repository.

You must first install the main Warbridge plugin into your local Maven repository:

```bash
mvn clean install

Then you can use the dependency below in your expansion project:

```xml
<dependency>
    <groupId>top.cnuo</groupId>
    <artifactId>warbridge</artifactId>
    <version>1.0.5</version>
    <scope>provided</scope>
</dependency>
```

### plugin.yml

```yaml
depend: [Warbridge]
```

---

## Accessing API / 获取 API

```java
WarbridgeApi api = Bukkit.getServicesManager().load(WarbridgeApi.class);
```

---

## Core API Methods / 核心方法

### Game State / 游戏状态

```java
api.getState();
api.getRoundState();
```

---

### Player Info / 玩家信息

```java
api.isInGame(player);
api.isSpectator(player);
api.isRespawning(player);
api.getPlayerTeam(player);
api.getPlayerStats(player);
```

---

### Game Data / 游戏数据

```java
api.getPlayersCount();
api.getActivePlayersCount();
api.getRoundNumber();
api.getCountdown();
api.getGameSeconds();
```

---

### Team Data / 队伍数据

```java
api.getTeams();
api.getPlayerTeamObject(player);
```

---

## Example: Kill Reward / 示例：击杀奖励

```java
@EventHandler
public void onKill(WarbridgePlayerKillEvent event) {
    Player killer = event.getKiller();
    killer.sendMessage("You earned 10 coins! / 你获得了10金币！");
}
```

---

## Example: Goal Reward / 示例：得分奖励

```java
@EventHandler
public void onGoal(WarbridgePlayerGoalEvent event) {
    Player player = event.getPlayer();
    player.sendMessage("Score! / 得分！");
}
```

---

## Available Events / 可用事件

- WarbridgeGameStartEvent
- WarbridgeGameEndEvent
- WarbridgeRoundStartEvent
- WarbridgeRoundEndEvent
- WarbridgePlayerJoinGameEvent
- WarbridgePlayerLeaveGameEvent
- WarbridgePlayerKillEvent
- WarbridgePlayerDeathEvent
- WarbridgePlayerGoalEvent
- WarbridgePlayerRespawnEvent

---

## PlaceholderAPI Support / 变量支持

```
%warbridge_team%
%warbridge_score%
%warbridge_kills%
%warbridge_deaths%
```

---

## Best Practices / 最佳实践

**EN**
- Always check if API is null
- Do not modify internal data directly
- Use events instead of core modification
- Avoid heavy operations in listeners

**ZH**
- 使用前检查 API 是否为 null
- 不要直接修改内部数据
- 优先使用事件扩展
- 避免在监听器中执行耗时操作

---

## Important Notes / 注意事项

**EN**
- Warbridge is designed for BungeeCord networks
- One server = one game instance
- Not suitable for single-server multi-room usage

**ZH**
- Warbridge 专为群组服务器设计
- 一台服务器对应一局游戏
- 不支持单服多房间模式

---
