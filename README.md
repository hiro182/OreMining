# 鉱石採掘ゲーム

## 概要
制限時間内に鉱石を採掘してスコアを競うゲームです。
## 使用コマンド
- `/oremining`：ゲームを開始します。
- `/oremining list`：現在保存されているスコアの一覧を表示します。

## ゲームの流れ
1. ゲームが始まると、プレイヤーの前方に5x5x5の範囲でランダムに鉱石が配置されます。
2. メインハンドにツルハシが装備され、体力と空腹度が最大値まで回復します。  
   ※開けた場所でコマンドを実行してください。  
   ※メインハンドに装備されているアイテムは上書きされるため、事前に外しておくことを推奨します。  
3. 制限時間は30秒です。鉱石を採掘してスコアを獲得してください。
4. 時間終了後、最終スコアが表示され、スコアはデータベースに保存されます。

## 鉱石とポイント
- 石炭鉱石：5ポイント
- 銅鉱石：10ポイント
- 鉄鉱石：20ポイント
- 金鉱石：30ポイント
- ダイヤモンド鉱石：50ポイント

## プレイ動画
https://github.com/user-attachments/assets/bf1f47bf-5160-47a1-aa94-13c94de36f8b

## データベースの接続方法
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

## 対応バージョン  
・Paper: 1.21.1  
・Minecraft: 1.21.1
