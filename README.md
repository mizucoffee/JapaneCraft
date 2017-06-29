
概要
--------------------------------------------------------------------------------
[KawakawaRitsuki様のプロジェクト](https://github.com/KawakawaRitsuki/JapaneCraft)
からforkしたものです。  
Forgeサーバーに導入することで、チャットのローマ字を日本語に変換します。

設定
--------------------------------------------------------------------------------
### JapaneCraft.cfg

- S:chat

    チャットの表示フォーマット。

    | 変数                  | 概要                                              |
    | :-------------------- | :-----------------------------------------------  |
    | `$username`           | 発言者の名前。                                    |
    | `$time`               | 発言した時間。                                    |
    | `$rawMessage`         | 発言内容。`$convertedMessage`と一致する場合は空。 |
    | `$convertedMessage`   | 日本語に変換された文字列。                        |
    | `$n`                  | 改行コード。U+000a。                              |
    | `$$`                  | '$'。 U+0024。                                    |

    デフォルト値は`<$username> $rawMessage$n  §b$convertedMessage`。

- S:time

    時刻の表示フォーマット。`java.text.SimpleDateFormat`参照。

### JapaneCraftRomajiTable.json

ローマ字テーブル。

License
--------------------------------------------------------------------------------
MIT. See [LICENSE](LICENSE).

