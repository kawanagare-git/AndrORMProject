# 開発基準
## 開発を円滑に進めるためのポイント
1. 質問の内容と回答が異なる場合<br>
　川流が指摘します。その後、以下を実施してください<br>
　1-1. 謝罪文を提示<br>
　1-2. 本来の質問の回答行うこと<br>
2.コードの提示を実施したときに誤りがある場合<br>
　川流が指摘します。その後、以下を実施してください<br>
　2-1. 謝罪文を提示<br>
　2-2. 間違ったコード提示した原因の説明<br>
　2-3. 回避策の提示<br>
　2-4. 繰り返さないことを宣言<br>
　但し、2-3 と 2-4 は、川流の方で検討することがある。<br>
　上記の場合、川流が検討した内容に差し替えること。
## 仕様の検討
1. 仕様は川流が、思い付きで検討することがある<br>
　この場合、一度、プラテスも仕様に妥当性があるか検討すること<br>
## コード生成ルール
　コード生成は、川流の得意不得意があるので、不得意分野に関しては、詳細な説明をすること<br>
　コード提示は、メソッド単位で何処を書き換えたかコメントなどで明示する<br>
　上記のコード提示は、変更前、変更前で対応しても良い<br>
### gradle のコード
1. 不得意分野<br>
2. build.gradle.kts は、どの build.gradle.kts を改修するのか明確にする<br>
### Kotlin コード
#### KSP モジュール
1. 不得意分野<br>
2. 川流の KSP 特有のライブラリィの知識が貧弱なので、詳細な説明を要する<br>
#### KSP 以外のモジュール
1. 得意分野<br>
2. 特殊なライブラリィを使用しない限り詳細な説明は不要<br>
3. 説明が欲しい時は、川流から問い合わせ<br>
#### Unit Test 
1. 得意分野<br>
2. JUnit5 でテストプログラムを作成<br>
　川流は、JUnit5 の @ParameterizedTest を好む<br>
3. Mockito の機能で不安があるので、その時は都度質問を実施<br>
4. その他、説明が欲しい時は、川流から問い合わせ<br>
#### Androoid Test 
1. 得意分野<br>
2. JUnit4 でテストプログラムを作成<br>
　川流は、JUnit4 の @ParameterizedTest を好む<br>
3. Mockito の機能で不安があるので、その時は都度質問を実施<br>
4. 説明が欲しい時は、川流から問い合わせ<br>
# AndrORM 概要
1.ORM プログラミン時に、可読性、メンテナンス性を向上させる<br>
2.テーブル作成、select 文、insert 文、update 文、delete 文、upsert 機能をクラス化して ORM を実現<br>
3.クエリ実行をサポート<br>
4.定義用エンティティ(data class)を基に、用途に合わせた DTO(data class)を自動生成<br>
## AndrORM 機能
### 実装状況
1. 実装済み<br>
- KSP による Select / Insert / Upsert 用 Entity 自動生成<br>
- Select 文生成<br>
- FunctionProjection による関数列生成<br>
- Entity メタ情報検証<br>

2. 未実装 / 調整中<br>
- Insert の複数件 values 展開<br>
- defineFunctionalColumn<br>
- Update / Upsert の実行系クエリビルダ### SQL・DML の自動生成<br>
### Entity クラスの自動生成ルール
- 生成クラス名は「元クラス名 + entityNameExtend」<br>
- 出力先パッケージは commonInterface と @EntityPackageInfo から決定<br>
- customInterface を指定した場合は追加実装される<br>
1. hideFromSelect<br>
- ColumnProjection と FunctionProjection に割り当てられている引数
- hideFromSelect = true を付与した列は、SELECT 句の抽出列には含まれない
- Entity メタ情報上は保持されるため、条件式や検証では参照される場合がある
2. KSP 検証ルール<br>
2-1. ERROR<br>
- 同一プロパティで @Column と @Function を併用した場合<br>
- 全プロパティが hideFromSelect = true の場合<br>
- SELECT 対象同士で alias が重複した場合<br>
2-2. WARNING<br>
- hidden 列を含む alias 重複など、クエリ構築には影響しないが定義として不自然な場合<br>
### アノテーション補完ルール
1.@Table<br>
　1-1.name 未指定時はクラス名をスネークケースへ変換<br>
　1-2.aliasExtend 指定時は既存 alias に `_` 連結して拡張<br>

2.@Column<br>
　2-1.既存 @Column はコピーされる<br>
　2-2.未指定時は name をプロパティ名のスネークケースとして補完生成する<br>

3.@Function<br>
　3-1.raw が指定されている場合、function / args より raw を優先する<br>
　3-2.生成プロパティ名は alias を camelCase へ変換した名前になる<br>
### 関数定義
#### FunctionProjection の引数仕様
args には以下を指定可能
- 既存プロパティ名
- 文字列リテラル（'text'）
- 数値リテラル（整数 / 小数）
- 真偽値リテラル（true / false）

戻り値型は、関数種別と引数型から自動推論される。推論が難しい場合は returnHint を指定
### Select 機能
現在の Select クラスでは以下の機能をサポートする。
- distinct
- join / on
- where
- having
- order
- bindValues の保持

条件式は DSL と raw 条件の併用が可能。