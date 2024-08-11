# bili-live-danmu

`bili-live-danmu` 是一个用于处理哔哩哔哩直播间弹幕的 Java 库。该库主要用于与 `mouse-danmu` 库配合使用，提供了连接直播间、接收和处理弹幕消息的功能。

## 主要类

### DanmuClient

`DanmuClient` 是库的核心类，负责与直播间建立连接，并处理从直播间接收到的弹幕消息。

#### 方法

- `connect()`: 连接到直播间。
- `disconnect()`: 断开与直播间的连接。
- `isOpen()`: 检查是否已经连接。
- `setListener(ConnectionListener listener)`: 设置连接状态监听器。
- `addHandler(MessageHandler handler)`: 添加消息处理器。
- `removeHandler(MessageHandler handler)`: 移除消息处理器。
- `send(String message)`: 发送消息到直播间。

## 主要用途

该库主要用于 `mouse-danmu` 库（[GitHub 仓库](https://github.com/LiQing-Code/mouse-danmu)），提供对哔哩哔哩直播间弹幕的处理功能。`mouse-danmu` 库可以通过该库进行弹幕消息的实时接收和处理。

## 示例代码

```java
DanmuClient client = new DanmuClient();
client.setListener(new ConnectionListener() {
    @Override
    public void onOpen() {
        System.out.println("连接成功");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("连接关闭: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
});

client.addHandler(new DanmuHandler(this::onDanmu));
client.addHandler(new EomjiHandler(this::onEmoji));
client.addHandler(new GiftHandler(this::onGift));
client.addHandler(new GuardHandler(this::onGuard));
client.addHandler(new InteractiveHandler(this::onInteractive));
client.addHandler(new SuperChatHandler(this::onSuperChat));
```

## 接口

### ConnectionListener

接口用于处理连接状态的变化。

- `void onOpen()`: 当连接成功时调用。
- `void onClose(int code, String reason, boolean remote)`: 当连接关闭时调用。
- `void onError(Exception ex)`: 当发生错误时调用。

### MessageHandler

接口用于处理弹幕消息。

- `boolean canHandle(Message message)`: 判断消息是否可以处理。
- `void handle(Message message)`: 处理消息。

### DanmuHandler

`DanmuHandler` 实现了 `MessageHandler` 接口，用于处理弹幕消息。

#### 构造函数

- `DanmuHandler(Consumer<Danmu> onDanmu)`: 构造函数，接受一个处理弹幕的 `Consumer`。


## 许可证

该项目使用 MIT 许可证，详见 [LICENSE](LICENSE.txt) 文件。