package plugin.oreMining.mapper.data;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * プレイヤーのスコア情報を扱うオブジェクト。
 * DBに依存するテーブルと連動する。
 */
@Getter
@Setter
@NoArgsConstructor
public class PlayerScore {

  private int id;
  private String playerName;
  private int score;
  private LocalDateTime registeredAt;

  public PlayerScore(String playerName, int score) {
    this.playerName = playerName;
    this.score = score;
  }
}
