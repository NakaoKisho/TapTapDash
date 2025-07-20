# 概要
このAndroidアプリは画面上にランダムに表示される赤い点を制限時間内にできるだけタップして得点を稼ぐゲームです。

# 目的
ユーザーに「楽しい」、「またやりたい」と思ってもらえるような中毒性のあるアプリを作ることです。

# 技術スタック
- Kotlin
- Jetpack Compose
- アプリアーキテクチャ
- バージョンカタログ
など

# コーディング時の参照
必ず最新の公式文章を参照してコードを組んでください。
Android Kotlin: 「https://developer.android.com/」で始まるURL
Kotlin: 「https://kotlinlang.org/docs」で始まるURL
Gradle: 「https://docs.gradle.org/」で始まるURL
Google AdMob: 「https://developers.google.com/admob/android/」で始まるURL
などです。プロジェクトで使用しているバージョンと同じものを参照してください。

絶対にiOSや個人ブログなどの情報をもとにコードを組まないでください。

# Gradle
- バージョンカタログを使用してすべてのモジュールが使用するライブラリのバージョンを統一する
- kaptは使用せず**kspを使用する**
- compileSdkは最新のバージョンを設定する
- sourceCompatibilityはJavaVersion.VERSION_1_8を指定する
- targetCompatibilityはJavaVersion.VERSION_1_8を指定する
- jvmTargetは1.8を指定する

# コマンド
プロジェクトのビルド: gradlew.bat build

# 修正完了時のビルド
何らかの修正を行った後はビルドを実施して、エラーがないことを確認してください。
エラーが発生した場合はそちらの修正を行い、再度ビルドを行います。
このローテショーンを繰り返してエラーを解消してください。
もし修正を繰り返しても、同じエラーが3回以上出てしまう場合、その旨の報告を行ってください。

# ライブラリ
使用するライブラリは常に最新のものにしてください。またバージョンカタログを使用してバージョンの管理をしてください。

# 提案
新しい機能を提案する場合、最初にプロジェクトを読み込んでから適切かつ未実装の機能を提案してください。
提案によって満たしたいことは、「お客様に喜んでもらえる、新しいお客様を引き付ける、ゲームをもっと楽しくする」ことです。
また、外部との通信が発生する機能の提案はできる限り避けてください。

# コマンドの実行
- Editに対するApply this change?に対してはYes, allow alwaysを適用する
- gradlew.bat buildに対するApply this change?に対してはYes, allow always "gradlew.bat ..."を適用する