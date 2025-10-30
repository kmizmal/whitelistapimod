# whitelistapimod

一个 **Fabric 模组**，提供 HTTP API 用于安全高效地管理 Minecraft 服务器。
> 理论上支持全部版本，目但前仅在1.21.1,1.21.7,1.21.8上通过测试

---

## ✨ 功能特性

* 提供 **HTTP API** 管理接口
  * **基于令牌的身份验证**，保证访问安全
  * **简单配置**，上手即用
  * **支持热重载配置**，无需重启服务器

---

## 📦 前置依赖

* [fabric-api](https://modrinth.com/mod/fabric-api)
  * [fabric-language-kotlin](https://modrinth.com/mod/fabric-language-kotlin)

---

## ⚙️ 安装步骤

1. 将模组 JAR 文件放入 `mods` 文件夹
   2. 启动服务器一次，生成默认配置文件
   3. 编辑配置文件（详见下文）
   4. 重启服务器或执行(/wla reload)使配置生效 ~~骗你的~~ *其实生成的默认配置也能跑*

---

## 🔧 配置说明

配置文件路径：`config/whitelist_api.json`

```json
{
  "port": 6626,
  "token": "your_secure_token_here"
}
````

* **port**：HTTP 服务器监听端口（默认：`6626`）
  * **token**：API 身份验证令牌（务必修改默认值）

---

## 🚀 使用方法

### API 接口

**请求头需包含：**

```
Authorization: Bearer your_token_here
```

* **添加玩家白名单**

```
GET http://yourserver:port/whitelist/add?player=PlayerName
```

* **移除玩家白名单**

```
GET http://yourserver:port/whitelist/remove?player=PlayerName
```

* **获取服务器TPS**

```
GET http://yourserver:port/server/tps
```

### 示例请求

使用 `curl`：

```bash
curl -H "Authorization: Bearer your_token_here" "http://localhost:6626/whitelist/add?player=kmizmal"
curl -H "Authorization: Bearer your_token_here" "http://localhost:6626/whitelist/remove?player=kmizmal"
curl -H "Authorization: Bearer your_token_here" "http://localhost:6626/server/playStats?player=kmizmal"
curl -H "Authorization: Bearer your_token_here" "http://localhost:6626/server/tps"
```


## 🔗 兼容性

理论上与任意 Fabric 模组兼容。
如遇到冲突，欢迎打开一个[issue](https://github.com/kmizmal/whitelistapimod/issues/new)
