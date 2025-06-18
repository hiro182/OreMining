# 鉱石採掘ゲーム

## 概要
このプラグインは、Minecraft向けのミニゲームです。  
プレイヤーの前方に生成された鉱石を制限時間内に採掘し、スコアを競います。  
スコアはMySQLに保存され、後から確認することができます。

## 🧊鉱石の配置処理（5×5×5範囲）
ゲーム開始時に、プレイヤーの視線の先に5×5×5の範囲で鉱石がランダム生成されます。

![無題の動画 ‐ Clipchampで作成 (1)](https://github.com/user-attachments/assets/11e151b3-2ce9-4534-a047-3cd289eb69bb)


```java
private void placeRandomOres(Player player) {
    Location playerLocation = player.getLocation(); // プレイヤーの現在位置を取得
    org.bukkit.util.Vector direction = playerLocation.getDirection().normalize(); // プレイヤーの視線方向を取得
    Material[] ores = {Material.COAL_ORE, Material.COPPER_ORE, Material.IRON_ORE, Material.GOLD_ORE,Material.DIAMOND_ORE};
    SplittableRandom splittableRandom = new SplittableRandom();

    // プレイヤーの前方に鉱石を配置
    for (int x = 0; x < 5; x++) {
      for (int y = 0; y < 5; y++) {
        for (int z = 0; z < 5; z++) {
          Location targetLocation = playerLocation.clone().add(direction.clone().multiply(5))
              .add(x, y, z);
          Block block = targetLocation.getBlock();

          if (block.getType() == Material.AIR) {
            Material randomOre = ores[splittableRandom.nextInt(ores.length)];
            block.setType(randomOre);
            oreLocations.add(targetLocation);
          }
        }
      }
    }
  }
```
##  🕹ゲームの進行制御（カウントダウンタイマー）
ゲーム時間を1秒ごとに減らし、時間切れでスコア保存＆終了処理を実行します。


![無題の動画 ‐ Clipchampで作成 (2)](https://github.com/user-attachments/assets/fef98e05-1f01-4546-9006-e6786a6bfa7c)

```java
new BukkitRunnable() {
      @Override
      public void run() {
        if (nowExecutingPlayer.getGameTime() <= 0) {
          player.sendTitle("ゲームが終了しました。",
              nowExecutingPlayer.getPlayerName() + " 合計 " + nowExecutingPlayer.getScore() + "点！",
              0, 60, 20);

          playerScoreData.insert(
              new PlayerScore(nowExecutingPlayer.getPlayerName(),
                  nowExecutingPlayer.getScore()));
          removeOres();
          cancel();
        } else {
          // ラスト5秒のカウントダウン
          if (nowExecutingPlayer.getGameTime() <= 5) {
            player.sendTitle("残り時間: " + nowExecutingPlayer.getGameTime() + "秒", "", 0, 20, 0);
          }
          nowExecutingPlayer.setGameTime(nowExecutingPlayer.getGameTime() - 1);  // 残り時間を1秒減らす
        }
      }
    }.runTaskTimer(main, 0, 20);
```

## ⛏ 鉱石採掘によるスコア加算処理
採掘されたブロックの種類に応じて、プレイヤーのスコアを加算します。

![無題の動画 ‐ Clipchampで作成 (3)](https://github.com/user-attachments/assets/06b5df3b-8996-4f87-a296-2cb58bc2a1a4)

```java
@EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    Player player = e.getPlayer();
    Material blockType = e.getBlock().getType();

    if (Objects.isNull(player) || oreLocations.stream()
        .noneMatch(location -> location.getBlock().getType().equals(blockType))) {
      return;
    }

    executingPlayerList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {
          int point;
          String message;

          switch (blockType) {
            case COAL_ORE: // 石炭鉱石
              point = 5;
              message = "石炭鉱石を採掘した！";
              break;
            case COPPER_ORE: // 銅鉱石
              point = 10;
              message = "銅鉱石を採掘した！";
              break;
            case IRON_ORE: // 鉄鉱石
              point = 20;
              message = "鉄鉱石を採掘した！";
              break;
            case GOLD_ORE:  // 金鉱石
              point = 30;
              message = "金鉱石を採掘した！";
              break;
            case DIAMOND_ORE: // ダイヤモンド鉱石
              point = 50;
              message = "ダイヤモンド鉱石を採掘した！";
              break;
            default:
              return; // 鉱石でない場合は何もしない
          }

          p.setScore(p.getScore() + point);
          player.sendMessage(message + "現在のスコアは" + p.getScore() + "点！");
        });
  }
```

## 🗄データベース設計

スコア情報はMySQLに保存されます。以下は `player_score` テーブルです。


![スクリーンショット 2025-06-15 144433](https://github.com/user-attachments/assets/a95d5f39-42f1-421e-a613-733f5e28ed97)

## 🚀ゲームの流れ
1. ゲーム開始と同時に、プレイヤーの前方に5×5×5の範囲でランダムに鉱石が生成されます。
2. メインハンドにツルハシが自動で装備され、体力と空腹度が最大値まで回復します。  
　※十分なスペースのある場所でコマンドを実行してください。  
　※メインハンドのアイテムは上書きされるため、事前に外しておくことを推奨します。  
3. 制限時間は30秒です。鉱石を採掘してスコアを獲得してください。
4. 時間終了後に最終スコアが表示され、スコアはデータベースに保存されます。

## 🔧使用コマンド
- `/oremining`：ゲームを開始します。
- `/oremining list`：現在保存されているスコアの一覧を表示します。

## 💎鉱石とポイント
| 鉱石             | ポイント |
|------------------|----------|
| 石炭鉱石         | 5pt      |
| 銅鉱石           | 10pt     |
| 鉄鉱石           | 20pt     |
| 金鉱石           | 30pt     |
| ダイヤモンド鉱石 | 50pt     |

## 🎥プレイ動画
https://github.com/user-attachments/assets/bf1f47bf-5160-47a1-aa94-13c94de36f8b

## 🛠データベースの接続方法
**MySQLの設定**  
このプラグインはMySQLを使用してスコアを保存します。  
以下の手順でデータベースとテーブルを作成し、設定を行ってください。

1. 自身のローカル環境でMySQLに接続します。
2. 以下のSQLコマンドを実行して、`paper_server`というデータベースを作成し、`player_score`テーブルを作成します。

   ```sql
   CREATE DATABASE paper_server;
   USE paper_server;
   CREATE TABLE player_score (
       id INT AUTO_INCREMENT PRIMARY KEY, 
       player_name VARCHAR(100), 
       score INT, 
       registered_at DATETIME, 
       DEFAULT CHARSET=utf8
   );

3. MySQLのurl,username,passwordはご自身のローカル環境に合わせてご使用ください。(mybatis-config.xmlで設定します。)

## 📦対応バージョン  
・Java 21.0.5  
・MySQL 8.0.4  
・Paper 1.21.1   
・Minecraft 1.21.1
