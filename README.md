# Tokyo Station Quiz App

## 1. プロジェクト概要 (Project Overview)

このアプリケーションは、東京23区の主要な鉄道駅に関するクイズを提供するAndroidアプリです。ランダムに選ばれた2つの駅（出発駅と到着駅）をユーザーに提示し、その間の経路を推測させます。ユーザーは「解答」としてGoogleマップで実際の経路を確認できます。

- **言語 (Language):** Kotlin
- **UIフレームワーク (UI Framework):** Jetpack Compose
- **アーキテクチャ (Architecture):** Single-Activity, State-hoistingパターンを利用したUI構成
- **主なライブラリ (Key Libraries):**
    - `androidx.compose.*`
    - `kotlinx.serialization.json` (for JSON parsing)

---

## 2. 主要なファイルと役割 (Key Files and Their Roles)

| ファイルパス (File Path) | 役割 (Role) |
| :--- | :--- |
| `app/src/main/assets/stations.json` | **データソース**: 駅の名前、所在区、乗り入れ路線を含むJSONファイル。 |
| `app/src/main/java/com/example/tokyostationquiz/MainActivity.kt` | **メインロジック**: アプリの全ロジックとUIコンポーネントを内包する唯一のActivity。 |
| `app/build.gradle.kts` | **ビルドスクリプト**: `kotlinx.serialization` のプラグインとライブラリ依存関係を定義。 |
| `gradle/libs.versions.toml` | **バージョンカタログ**: `kotlinx.serialization` を含むライブラリのバージョンとエイリアスを管理。 |

---

## 3. データフローと永続化 (Data Flow & Persistence)

1.  **データソース**: アプリケーションは `app/src/main/assets/` ディレクトリに配置された `stations.json` ファイルを唯一のデータソースとして利用します。
2.  **データモデル**: `kotlinx.serialization` を使用してJSONをパースします。対応するデータクラスは `MainActivity.kt` 内に定義されています。
    
    ```kotlin
    @Serializable
    data class Station(
        val name: String,
        val ward: String,
        val lines: List<String>
    )

    @Serializable
    data class StationData(
        val stations: List<Station>
    )
    
    // 問題履歴用のデータクラス（シリアライズ不要）
    data class QuizState(
        val originStation: Station,
        val destinationStation: Station,
        val isOriginCardExpanded: Boolean,
        val isDestinationCardExpanded: Boolean
    )
    ```

3.  **読み込み処理**: `MainActivity` の `onCreate` ライフサイクルで `readStationsJson()` 関数が一度だけ呼び出され、全駅のリストをメモリにロードします。このリストはコンポーザブルに `State` として渡されます。

---

## 4. UIと状態管理 (UI & State Management)

UIはすべて `MainActivity.kt` 内のJetpack Compose関数で構築されています。状態管理は `QuizScreen` コンポーザブルに集約され、`remember { mutableStateOf(...) }` によって各UIの状態が保持されます。

### 4.1. 主要な状態変数 (Key State Variables in `QuizScreen`)

| 変数名 | 型 | 説明 |
| :--- | :--- | :--- |
| `isDepartureFixed` | `Boolean` | 「出発駅を固定する」モード (`true`) か「両方ランダム」モード (`false`) かを制御する `Switch` の状態。 |
| `originStation` | `Station` | 現在のクイズの出発駅。 |
| `destinationStation` | `Station` | 現在のクイズの到着駅。 |
| `isOriginCardExpanded` | `Boolean` | 出発駅のヒントカード（`ExpandableCard`）の開閉状態。 |
| `isDestinationCardExpanded`| `Boolean` | 到着駅のヒントカード（`ExpandableCard`）の開閉状態。 |
| `history` | `List<QuizState>` | 問題の履歴を保存するリスト（最大100件）。アプリ起動中のメモリ上のみ保持。 |
| `currentHistoryIndex` | `Int` | 現在表示している問題の履歴内位置。 |

### 4.2. 主要なコンポーザブル (Key Composables)

| コンポーザブル名 | 説明 |
| :--- | :--- |
| `QuizApp` | `Scaffold` と `TopAppBar` を含むアプリケーションの最上位レイアウト。 |
| `QuizScreen` | クイズの主要なUIとロジックを管理する。状態変数を持ち、ユーザーインタラクションに応じてUIを更新する。 |
| `ExpandableCard` | 駅の付加情報（所在区、路線）を表示するカード。クリックで `AnimatedVisibility` により内容が展開・折りたたみされる。このコンポーザブルは **ステートレス** であり、開閉状態 (`expanded`) は親の `QuizScreen` から渡される。 |

---

## 5. コアロジック (Core Logic)

### 5.1. クイズ生成 (`nextQuestion()` 関数)

この関数は「次へ」ボタンがクリックされたときに呼び出されます。

1.  **ヒントを閉じる**: `isOriginCardExpanded` と `isDestinationCardExpanded` を `false` に設定する。
2.  **モード分岐**:
    - **出発固定モード (`isDepartureFixed == true`):** `destinationStation` のみ、現在の `originStation` とは異なる駅をランダムに再選出する。
    - **両方ランダムモード (`isDepartureFixed == false`):** `originStation` と `destinationStation` の両方を、全駅リストから重複なくランダムに再選出する。
3.  **履歴管理**:
    - 現在の履歴位置が末尾でない場合（戻った状態から次へ進む場合）、現在位置より後の履歴を削除する。
    - 新しい問題状態を履歴に追加する。
    - 履歴が100件を超えた場合、最も古い1件を削除する。
    - `currentHistoryIndex` を履歴の末尾に更新する。

### 5.2. 前の問題へ戻る (`previousQuestion()` 関数)

この関数は「戻る」ボタンがクリックされたときに呼び出されます。

1.  **履歴インデックスのチェック**: `currentHistoryIndex > 0` の場合のみ実行。
2.  **状態の復元**: `currentHistoryIndex` をデクリメントし、`history[currentHistoryIndex]` から出発駅、到着駅、ヒントカードの開閉状態を復元する。

### 5.3. Googleマップ連携

「Googleマップ」ボタンがクリックされると、以下の処理が実行されます。

1.  現在の `originStation` と `destinationStation` の駅名を使って、Googleマップの経路検索用URLを生成します。
    - 例: `https://www.google.com/maps/dir/?api=1&origin=東京駅&destination=新宿駅`
2.  `Intent.ACTION_VIEW` を使って、このURLを処理できる外部アプリ（通常はGoogleマップ）を起動します。

### 5.4. 履歴のクリア

以下のタイミングで問題履歴がクリアされ、新しい問題で履歴が初期化されます：

1.  **固定モード切り替え時**: `isDepartureFixed` スイッチの状態が変更されたとき。
2.  **固定駅の変更時**: 出発駅固定モードで、ドロップダウンから新しい出発駅を選択したとき。

