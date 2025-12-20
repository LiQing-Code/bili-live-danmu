# bili-live-danmu 测试指南

本文档介绍如何在本地测试 bili-live-danmu 库。

## 前置要求

- **Java 21** 或更高版本
- **Gradle** (使用项目自带的 gradlew)

### 检查 Java 版本

```bash
java -version
```

应该显示 `openjdk version "21"` 或更高版本。

如果没有 Java 21，请从以下地址下载安装：
- Oracle JDK: https://www.oracle.com/java/technologies/downloads/
- OpenJDK: https://adoptium.net/

## 运行测试

### 1. 运行所有测试

在项目根目录执行：

```bash
# Linux/macOS
./gradlew test

# Windows
gradlew.bat test
```

### 2. 运行特定测试类

```bash
# 只运行 MessageTest
./gradlew test --tests MessageTest

# 只运行 DanmuHandlerTest
./gradlew test --tests DanmuHandlerTest

# 运行所有 handler 测试
./gradlew test --tests "*.handler.*"
```

### 3. 查看测试报告

测试完成后，打开以下文件查看详细报告：

```
build/reports/tests/test/index.html
```

在浏览器中打开这个 HTML 文件，可以看到：
- 测试通过率
- 每个测试类的执行情况
- 失败测试的详细信息

### 4. 运行测试并生成详细输出

```bash
./gradlew test --info
```

## 测试覆盖范围

### 核心类测试

1. **MessageTest** - 测试 Gson 序列化/反序列化
   - JSON 到 Message 对象的转换
   - Message 对象到 JSON 的转换
   - JsonElement 字段的正确处理

2. **AuthTest** - 测试认证类
   - 构造函数和字段验证
   - Cookie 提取功能

3. **BiliWbiSignTest** - 测试 WBI 签名
   - 参数编码和签名生成
   - URL 编码处理
   - 特殊字符处理

4. **PacketTest** - 测试数据包处理
   - 数据包打包和解包
   - 多数据包处理
   - 操作码解析

### Handler 类测试

5. **DanmuHandlerTest** - 测试弹幕处理器
   - 弹幕消息解析
   - 粉丝勋章信息提取
   - 舰长等级处理

6. **GiftHandlerTest** - 测试礼物处理器
   - 金瓜子礼物处理
   - 银瓜子礼物处理
   - 舰长礼物处理

## 本地构建和发布

### 1. 清理并构建项目

```bash
./gradlew clean build
```

成功后会在 `build/libs/` 目录生成：
- `bili-live-danmu-4.0.0.jar` - 主 JAR 包
- `bili-live-danmu-4.0.0-sources.jar` - 源码 JAR
- `bili-live-danmu-4.0.0-javadoc.jar` - 文档 JAR

### 2. 发布到本地 Maven 仓库

```bash
./gradlew publishToMavenLocal
```

这会将库安装到 `~/.m2/repository/cn/liqing/bili-live-danmu/4.0.0/`

### 3. 在其他项目中使用

在你的项目的 `build.gradle` 或 `pom.xml` 中添加依赖：

**Gradle:**
```gradle
repositories {
    mavenLocal()  // 添加本地 Maven 仓库
    mavenCentral()
}

dependencies {
    implementation 'cn.liqing:bili-live-danmu:4.0.0'
}
```

**Maven:**
```xml
<dependency>
    <groupId>cn.liqing</groupId>
    <artifactId>bili-live-danmu</artifactId>
    <version>4.0.0</version>
</dependency>
```

## 验证迁移

### 检查依赖

```bash
./gradlew dependencies --configuration runtimeClasspath
```

应该看到：
- ✅ `com.google.code.gson:gson:2.11.0`
- ✅ `org.java-websocket:Java-WebSocket:1.5.7`
- ❌ 没有任何 Jackson 依赖

### 检查 JAR 大小

```bash
ls -lh build/libs/
```

主 JAR 包应该约为 34KB（之前约为 600KB+）

## 常见问题

### Q: 测试失败 "error: invalid source release: 21"

**A:** 你的 Java 版本太低。请安装 Java 21 或更高版本。

### Q: gradlew 没有执行权限

**A:** 运行：
```bash
chmod +x gradlew
```

### Q: 如何跳过测试进行构建？

**A:** 运行：
```bash
./gradlew build -x test
```

### Q: 测试通过但构建失败

**A:** 检查 Javadoc 警告。可以跳过 Javadoc：
```bash
./gradlew build -x javadoc
```

## 集成测试示例

创建一个简单的测试程序：

```java
import cn.liqing.bili.live.danmu.*;
import cn.liqing.bili.live.danmu.handler.*;
import cn.liqing.bili.live.danmu.model.*;

public class TestClient {
    public static void main(String[] args) throws Exception {
        // 创建客户端
        DanmuClient client = new DanmuClient();
        
        // 添加弹幕处理器
        client.addHandler(new DanmuHandler(danmu -> {
            System.out.println("收到弹幕: " + danmu.body);
            System.out.println("用户: " + danmu.user.name);
        }));
        
        // 添加礼物处理器
        client.addHandler(new GiftHandler(gift -> {
            System.out.println("收到礼物: " + gift.name + " x" + gift.num);
            System.out.println("价值: " + gift.price + "元");
        }));
        
        // 连接（需要真实的房间 ID）
        Auth auth = Auth.create(12345L); // 替换为真实房间号
        client.connect(auth);
        
        // 保持运行
        Thread.sleep(60000);
        
        // 断开连接
        client.disconnect();
    }
}
```

## 性能测试

测试内存和 CPU 使用：

```bash
# 运行测试并监控资源使用
./gradlew test --max-workers=1 --info
```

## 贡献测试

如果你想添加新的测试用例：

1. 在 `src/test/java/cn/liqing/bili/live/danmu/` 下创建测试类
2. 使用 JUnit 5 注解 (`@Test`, `@BeforeEach` 等)
3. 运行 `./gradlew test` 验证
4. 提交 PR

## 支持

如果遇到问题，请在 GitHub Issues 中提问：
https://github.com/LiQing-Code/bili-live-danmu/issues
