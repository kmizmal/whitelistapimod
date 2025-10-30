# whitelistapimod

A **Fabric mod** that provides an HTTP API for securely and efficiently managing Minecraft servers.

> Supports all versions in theory, but has been tested on 1.21.1, 1.21.7, and 1.21.8.

---

## ✨ Features

* Provides an **HTTP API** management interface

    * **Token-based authentication** to ensure secure access
    * **Simple configuration** for quick setup
    * **Supports hot configuration reloading** without the need to restart the server

---

## 📦 Prerequisites

* [fabric-api](https://modrinth.com/mod/fabric-api)

    * [fabric-language-kotlin](https://modrinth.com/mod/fabric-language-kotlin)

---

## ⚙️ Installation Steps

1. Place the mod JAR file in the `mods` folder
   2\. Start the server once to generate the default configuration file
   3\. Edit the configuration file (see below for details)
   4\. Restart the server or execute (/wla reload) to apply the configuration ~~just kidding~~ *The default configuration will also work right out of the box*

---

## 🔧 Configuration Details

Configuration file path: `config/whitelist_api.json`

```json
{
  "port": 6626,
  "token": "your_secure_token_here"
}
```

* **port**: The port on which the HTTP server listens (default: `6626`)

    * **token**: API authentication token (make sure to change the default value)

---

## 🚀 Usage

### API Endpoints

**The request headers must include:**

```
Authorization: Bearer your_token_here
```

* **Add player to the whitelist**

```
GET http://yourserver:port/whitelist/add?player=PlayerName
```

* **Remove player from the whitelist**

```
GET http://yourserver:port/whitelist/remove?player=PlayerName
```

* **Get server TPS (ticks per second)**

```
GET http://yourserver:port/server/tps
```

### Example Requests

Using `curl`:

```bash
curl -H "Authorization: Bearer your_token_here" "http://localhost:6626/whitelist/add?player=kmizmal"
curl -H "Authorization: Bearer your_token_here" "http://localhost:6626/whitelist/remove?player=kmizmal"
curl -H "Authorization: Bearer your_token_here" "http://localhost:6626/server/playStats?player=kmizmal"
curl -H "Authorization: Bearer your_token_here" "http://localhost:6626/server/tps"
```

## 🔗 Compatibility

Theoretically compatible with any Fabric mod.
If you encounter conflicts, feel free to open an [issue](https://github.com/kmizmal/whitelistapimod/issues/new).
