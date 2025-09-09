# whitelistapimod

一个 **Fabric 模组**，提供 HTTP API 用于安全高效地管理 Minecraft 服务器白名单。
> 理论上支持全部版本，目但前仅在1.21.1和1.21.8上通过测试

---

## ✨ 功能特性

* 提供 **HTTP API** 白名单管理接口
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
4. 重启服务器使配置生效

---

## 🔧 配置说明

配置文件路径：`config/whitelist_api.json`

```json
{
  "port": 6626,
  "token": "your_secure_token_here"
}
```

* **port**：HTTP 服务器监听端口（默认：`6626`）
* **token**：API 身份验证令牌（务必修改默认值）

---

## 🚀 使用方法

### API 接口

**添加玩家至白名单**

```
GET http://yourserver:port/whitelist/add?player=PlayerName
```

**请求头需包含：**

```
Authorization: Bearer your_token_here
```

### 示例请求

使用 `curl`：

```bash
curl -H "Authorization: Bearer your_token_here" "http://localhost:6626/whitelist/add?player=kmizmal"
```

### 注册命令

热重载配置（无需重启服务器）：

```
/wla reload
```

---

## ⚠️ 注意事项

1. **务必修改默认令牌**，避免泄露风险
2. 建议通过 **防火墙** 限制 API 端口访问
3. 当前 API 仅支持 **添加玩家**（不支持移除）

---

## 🛠️ 故障排除

* 若默认端口已被占用，模组会 **自动尝试下一个可用端口**

---

## 🔗 兼容性

理论上可与任意 Fabric 模组兼容。
如遇到冲突，欢迎打开一个[issue](https://github.com/kmizmal/whitelistapimod/issues/new)

