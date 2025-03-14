package plugin.oreMining.command;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import plugin.oreMining.PlayerScoreData;
import plugin.oreMining.Main;
import plugin.oreMining.data.ExecutingPlayer;
import plugin.oreMining.mapper.data.PlayerScore;

/**
 * 　制限時間内に鉱石を発掘して、スコアを獲得するゲームを起動するコマンドです。 スコアは鉱石の種類によって変わり、採掘した鉱石の合計によってスコアが変動します。
 * 結果はプレイヤー名、点数、日時などで保存されます。
 */
public class OreMiningCommand extends BaseCommand implements Listener {

  public static final int GAME_TIME = 30;
  public static final String LIST = "list";
  private final Main main;
  private final PlayerScoreData playerScoreData = new PlayerScoreData();

  private final List<ExecutingPlayer> executingPlayerList = new ArrayList<>();
  private final List<Location> oreLocations = new ArrayList<>();


  public OreMiningCommand(Main main) {
    this.main = main;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label,
      String[] args) {
    // 最初の引数が「list」だったらスコアを一覧表示して処理を終了する。
    if (args.length == 1 && (LIST.equals(args[0]))) {
      sendPlayerScoreList(player);
      return true;
    }

    ExecutingPlayer nowExecutingPlayer = getPlayerScore(player);

    initPlayerStatus(player);

    placeRandomOres(player);

    gamePlay(player, nowExecutingPlayer);
    return true;
  }

  @Override
  public boolean onExecuteNPCCommand(CommandSender sender, Command command, String label,
      String[] args) {
    return false;
  }

  /**
   * 現在登録されているスコアの一覧をメッセージに送る。
   *
   * @param player 　プレイヤー
   */
  private void sendPlayerScoreList(Player player) {
    List<PlayerScore> playerScoreList = playerScoreData.selectList();
    for (PlayerScore playerScore : playerScoreList) {
      player.sendMessage(playerScore.getId() + " | "
          + playerScore.getPlayerName() + " | "
          + playerScore.getScore() + " | "
          + playerScore.getRegisteredAt()
          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
  }

  /**
   * ゲーム開始時にプレイヤーの前方に5x5x5の範囲で鉱石をランダムに配置する。
   *
   * @param player コマンドを実行したプレイヤー
   **/
  private void placeRandomOres(Player player) {
    Location playerLocation = player.getLocation(); // プレイヤーの現在位置を取得
    org.bukkit.util.Vector direction = playerLocation.getDirection().normalize(); // プレイヤーの視線方向を取得
    Material[] ores = {Material.COAL_ORE, Material.COPPER_ORE, Material.IRON_ORE, Material.GOLD_ORE,
        Material.DIAMOND_ORE};
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

  /**
   * ゲーム終了時に出現させた鉱石を削除する
   */
  private void removeOres() {
    for (Location location : oreLocations) {
      Block block = location.getBlock();
      if (block.getType() != Material.AIR) {
        block.setType(Material.AIR);
      }
    }
    oreLocations.clear();
  }

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

  /**
   * 現在実行しているプレイヤーのスコア情報を取得する。
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return 現在実行しているプレイヤーのスコア情報
   */
  private ExecutingPlayer getPlayerScore(Player player) {
    ExecutingPlayer executingPlayer = new ExecutingPlayer(player.getName());

    if (executingPlayerList.isEmpty()) {
      executingPlayer = addNewPlayer(player);
    } else {
      executingPlayer = executingPlayerList.stream()
          .findFirst()
          .map(ps -> ps.getPlayerName().equals(player.getName())
              ? ps
              : addNewPlayer(player)).orElse(executingPlayer);
    }

    executingPlayer.setGameTime(GAME_TIME);
    executingPlayer.setScore(0);
    return executingPlayer;
  }

  /**
   * 新規のプレイヤー情報をリストに追加します。
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return 新規プレイヤー
   */
  private ExecutingPlayer addNewPlayer(Player player) {
    ExecutingPlayer newPlayer = new ExecutingPlayer(player.getName());
    executingPlayerList.add(newPlayer);
    return newPlayer;
  }

  /**
   * ゲーム開始前にプレイヤーの状態を設定する。 体力と空腹度を最大にして、メインハンドにツルハシを持たせる。
   *
   * @param player 　コマンドを実行したプレイヤー
   */
  private void initPlayerStatus(Player player) {
    player.setHealth(20);
    player.setFoodLevel(20);

    PlayerInventory inventory = player.getInventory();
    inventory.setItemInMainHand(new ItemStack(Material.NETHERITE_PICKAXE));
  }

  /**
   * ゲームを実行します。基底の時間内に鉱石を採掘するとスコアが加算されます。合計スコアを時間経過後に表示します。
   *
   * @param player             　コマンドを実行したプレイヤー
   * @param nowExecutingPlayer 　プレイヤースコア情報
   */
  private void gamePlay(Player player, ExecutingPlayer nowExecutingPlayer) {
    player.sendTitle("ゲーム開始！", "鉱石を採掘してスコアを競いましょう！", 0, 60, 0);

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
    }.runTaskTimer(main, 0, 20);  // 0ティック後に開始し、20ティック（1秒）ごとに実行
  }
}
