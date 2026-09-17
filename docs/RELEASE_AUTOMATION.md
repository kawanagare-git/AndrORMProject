# AndrORM リリース自動検査

## 配置

この一式を `E:\Projects\AndrORM` の直下へ配置します。

## 役割

- `release/androrm-release-manifest.json`
  - `kawanagare-git/AndrORMProject` の全 Gradle モジュールと公開条件を保持します。
- `scripts/update-release-manifest.ps1`
  - `settings.gradle(.kts)` とマニフェストを同期します。
  - 既知モジュールは自動登録します。
  - 将来追加された未知のモジュールだけ、公開対象・直接依存・Configuration を確認します。
  - `artifactId` は build.gradle(.kts) またはモジュール名から自動取得します。
- `scripts/release-preflight.ps1`
  - 公開前の検査を実行します。
  - ソース、公開設定、全モジュールの version、GitHub Actions、README、Unit Test、Maven Local、PGP署名、Detektサービス定義、外部プロジェクトを検証します。
  - GPG鍵リング内の同じ秘密鍵を使い、一時 `.asc` のみ生成して最後に必ず削除します。
  - Maven Centralへの公開は行いません。
- `scripts/post-publish-check.ps1`
  - Maven Central公開後の最終検査です。
  - 公開対象5成果物、必須3依存、外部ビルド、Detekt、Android Testを検証します。
- `.github/workflows/validate-release-tooling.yml`
  - Windows PowerShell 5.1で構文解析します。
  - マニフェスト更新バッチを実際に実行し、リポジトリとの差分がないことを検証します。

## 初回導入時

配置後、次の一行でマニフェストを同期します。

```powershell
Set-Location "E:\Projects\AndrORM"; .\scripts\update-release-manifest.ps1
```

生成・更新されたファイル一式を `mk-develop` へコミットします。リリース前検査は未コミット変更があると停止します。

## 公開前

PowerShellで次の一行を実行します。

```powershell
Set-Location "E:\Projects\AndrORM"; .\scripts\release-preflight.ps1
```

外部検証プロジェクトは、公開前だけ `mavenLocal()` を設定しておきます。

## Maven Central公開後

外部検証プロジェクトから `mavenLocal()` を削除し、AVDまたはAndroid端末を接続した状態で次の一行を実行します。

```powershell
Set-Location "E:\Projects\AndrORM"; .\scripts\post-publish-check.ps1
```

## PGP秘密鍵の運用

- GPG鍵リングの秘密鍵は継続利用します。
- 毎回、新しい鍵は生成しません。
- `release-preflight.ps1` が秘密鍵を一時 `.asc` へエクスポートします。
- Gradleの署名が終わると、環境変数と一時 `.asc` を `finally` で削除します。
- GitHub Actionsでの正式公開は、既存の `SIGNING_KEY` と `SIGNING_PASSWORD` を使用します。
