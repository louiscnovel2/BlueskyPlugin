# blueskyplugin  

このプラグインを導入して分散型SNS「Bluesky」のアカウントでマイクラ内でログインすれば、
投稿をしたり、タイムラインを見たりすることが可能です！！

## 導入方法
サーバーを`/stop`で停止し、サーバー内ディレクトリの`/plugins/`にこのページからダウンロードした`blueskyplugin-1.0-SNAPSHOT.jar`を入れてサーバーを起動するだけです。

## ログインする

以下のコマンドでログインします。

※**冒頭の/は絶対に忘れないで下さい。忘れるとBlueskyのユーザー名とパスワードが公開されます。**

※ハンドルネームは自分のBlueskyのプロフィールページを確認して下さい。
ハンドルネームは、`@`以降です。

※ハンドルネームにはちゃんと`.bsky.social`も含めて下さい。

※パスワードは自分のアカウントのパスワードをきちんと入力して下さい。

※パスワードの代わりに予めBlueskyで作っておいた自分のApp Passwordを利用するという手もあります。

```
/bsky login handle.bsky.social password
```

## 投稿する

投稿するには、ログインした状態で以下のコマンドを打ちます。

※投稿内容の部分は自由に改変して下さい。

```
/bsky post 投稿内容
```

## タイムラインを見る

タイムラインを見るには、ログインした状態で以下のコマンドを打ちます。

```
/bsky tl
```

## ログアウトする

ログアウトするには、以下のコマンドを入力します。

```
/bsky logout
```
